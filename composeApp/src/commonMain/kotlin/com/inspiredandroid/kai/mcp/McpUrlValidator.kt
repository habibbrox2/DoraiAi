package com.inspiredandroid.kai.mcp

import com.inspiredandroid.kai.resolveHostAddresses

/**
 * Validates MCP server URLs to mitigate SSRF attacks.
 *
 * This validator applies defense-in-depth checks: scheme validation, IP-range
 * filtering, header sanitization, and DNS resolution. It does NOT provide a
 * security guarantee — DNS rebinding, proxy bypass, and TOCTOU windows remain
 * as documented limitations.
 *
 * On platforms where DNS resolution is unavailable (iOS, Web), validation
 * fails closed (all URLs rejected).
 */
object McpUrlValidator {

    data class ValidationResult(val isValid: Boolean, val error: String? = null)

    /**
     * Validates a URL for use as an MCP server endpoint.
     *
     * @param url The URL to validate.
     * @param allowPrivateIps When true, allows private IP ranges (for self-hosted
     *   MCP on LAN). Does NOT allow loopback (except localhost), cloud metadata,
     *   multicast, or reserved ranges.
     */
    fun validate(url: String, allowPrivateIps: Boolean = false): ValidationResult {
        // 1. Basic URL parsing
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult(false, "URL is empty")
        }

        // 2. Scheme validation
        val scheme: String
        val rest: String
        if (trimmed.startsWith("https://", ignoreCase = true)) {
            scheme = "https"
            rest = trimmed.substring(8)
        } else if (trimmed.startsWith("http://", ignoreCase = true)) {
            scheme = "http"
            rest = trimmed.substring(7)
        } else {
            return ValidationResult(false, "Unsupported scheme: only http:// and https:// are allowed")
        }

        // 3. Reject credentials in URL
        if (rest.contains("@")) {
            return ValidationResult(false, "Credentials in URL are not allowed")
        }

        // 4. Extract host (handle IPv6 brackets)
        val host: String
        val pathPart: String
        val slashIndex = rest.indexOf('/')
        val hostPort = if (slashIndex >= 0) rest.substring(0, slashIndex) else rest
        pathPart = if (slashIndex >= 0) rest.substring(slashIndex) else ""

        // Handle IPv6 literal [::1]
        if (hostPort.startsWith("[")) {
            val closeBracket = hostPort.indexOf(']')
            if (closeBracket < 0) {
                return ValidationResult(false, "Invalid IPv6 literal: missing closing bracket")
            }
            host = hostPort.substring(1, closeBracket)
        } else {
            // Strip port
            val colonIndex = hostPort.lastIndexOf(':')
            host = if (colonIndex >= 0 && hostPort.indexOf(':') == colonIndex) {
                hostPort.substring(0, colonIndex)
            } else {
                hostPort
            }
        }

        if (host.isEmpty()) {
            return ValidationResult(false, "Host is empty")
        }

        // 5. Decode percent-encoded host and re-validate
        val decodedHost = host.replace("%40", "@", ignoreCase = true)
        if (decodedHost != host && decodedHost.contains("@")) {
            return ValidationResult(false, "Encoded credentials detected")
        }
        val effectiveHost = host.lowercase()

        // 6. Localhost check for HTTP
        val isLocalhost = effectiveHost == "localhost" ||
            effectiveHost == "127.0.0.1" ||
            effectiveHost == "[::1]" ||
            effectiveHost == "::1" ||
            effectiveHost == "10.0.2.2"

        if (scheme == "http" && !isLocalhost) {
            return ValidationResult(false, "HTTP is only allowed for localhost, 127.0.0.1, [::1], and 10.0.2.2")
        }

        // 7. Check if host is an IP literal
        val isIpLiteral = isIpAddress(effectiveHost)

        // 8. DNS resolution and IP-range check
        val addresses = if (isIpLiteral) {
            listOf(effectiveHost)
        } else {
            resolveHostAddresses(effectiveHost)
        }

        // Fail closed if DNS resolution is unavailable
        if (addresses.isEmpty() && !isIpLiteral) {
            return ValidationResult(false, "DNS resolution unavailable on this platform. MCP connections are not supported.")
        }

        for (addr in addresses) {
            val ipCheck = checkIpRange(addr, allowPrivateIps, isLocalhost, scheme)
            if (!ipCheck.isValid) {
                return ipCheck
            }
        }

        return ValidationResult(true)
    }

    /**
     * Validates headers for CRLF/control character injection.
     */
    fun validateHeaders(headers: Map<String, String>): ValidationResult {
        for ((key, value) in headers) {
            if (containsControlChars(key) || containsControlChars(value)) {
                return ValidationResult(false, "Header contains control characters (CRLF injection risk)")
            }
        }
        return ValidationResult(true)
    }

    private fun isIpAddress(host: String): Boolean {
        // Simple IPv4 check
        if (host.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"""))) {
            return true
        }
        // IPv6 check (contains colons)
        if (host.contains(":")) {
            return true
        }
        return false
    }

    private fun checkIpRange(ip: String, allowPrivateIps: Boolean, isLocalhost: Boolean, scheme: String): ValidationResult {
        // Normalize IPv4-mapped IPv6
        val normalizedIp = if (ip.startsWith("::ffff:", ignoreCase = true)) {
            ip.substring(7)
        } else {
            ip
        }

        // Cloud metadata — always blocked
        if (normalizedIp == "169.254.169.254") {
            return ValidationResult(false, "Cloud metadata endpoint is blocked")
        }

        // Loopback
        if (isLoopback(normalizedIp)) {
            if (isLocalhost && scheme == "http") {
                return ValidationResult(true)
            }
            return ValidationResult(false, "Loopback address blocked")
        }

        // Link-local
        if (isLinkLocal(normalizedIp)) {
            return ValidationResult(false, "Link-local address blocked")
        }

        // Multicast
        if (isMulticast(normalizedIp)) {
            return ValidationResult(false, "Multicast address blocked")
        }

        // Private ranges
        if (isPrivate(normalizedIp)) {
            if (allowPrivateIps || isLocalhost) {
                return ValidationResult(true)
            }
            return ValidationResult(false, "Private IP range blocked (enable allowPrivateIps for self-hosted LAN)")
        }

        // Reserved
        if (isReserved(normalizedIp)) {
            return ValidationResult(false, "Reserved address blocked")
        }

        return ValidationResult(true)
    }

    private fun isLoopback(ip: String): Boolean {
        if (ip == "::1") return true
        if (ip.startsWith("127.")) return true
        return false
    }

    private fun isLinkLocal(ip: String): Boolean {
        if (ip.startsWith("169.254.")) return true
        if (ip.startsWith("fe80:", ignoreCase = true)) return true
        return false
    }

    private fun isMulticast(ip: String): Boolean {
        // IPv4 multicast: 224.0.0.0/4
        if (ip.matches(Regex("""^2(2[4-9]|[3-9]\d)\..*"""))) return true
        // IPv6 multicast: ff00::/8
        if (ip.startsWith("ff", ignoreCase = true) && ip.contains(":")) return true
        return false
    }

    private fun isPrivate(ip: String): Boolean {
        // 10.0.0.0/8
        if (ip.startsWith("10.")) return true
        // 172.16.0.0/12
        if (ip.matches(Regex("""^172\.(1[6-9]|2\d|3[01])\..*"""))) return true
        // 192.168.0.0/16
        if (ip.startsWith("192.168.")) return true
        // IPv6 unique local: fc00::/7
        if (ip.startsWith("fc", ignoreCase = true) && ip.contains(":")) return true
        if (ip.startsWith("fd", ignoreCase = true) && ip.contains(":")) return true
        return false
    }

    private fun isReserved(ip: String): Boolean {
        // 0.0.0.0/8
        if (ip.startsWith("0.")) return true
        // 240.0.0.0/4
        if (ip.matches(Regex("""^(24\d|25[0-5])\..*"""))) return true
        return false
    }

    private fun containsControlChars(s: String): Boolean = s.any { it.code < 0x20 || it.code == 0x7F }
}
