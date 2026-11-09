package com.dorai.ai.tools

/**
 * Shared deny-list classification for IP addresses, used by outbound fetches that
 * must never reach internal destinations (SSRF prevention).
 *
 * Blocks loopback, RFC1918 private, link-local (v4 + fe80::/10), IPv6 unique-local
 * (fc00::/7), multicast (224.0.0.0/4 + ff00::/8), unspecified/reserved, carrier-grade
 * NAT (100.64/10), benchmarking (198.18/15), cloud metadata (169.254.169.254), and
 * NAT64-encapsulated IPv4 (`64:ff9b::/96`, `64:ff9b:1::/48`).
 *
 * IPv4-mapped IPv6 (`::ffff:127.0.0.1`) is normalized to its embedded IPv4 form before
 * classification, per OWASP SSRF guidance ("normalize before you compare").
 *
 * Source: OWASP Server Side Request Forgery Prevention Cheat Sheet, deny-list section.
 * https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html
 */
internal object NetworkAddressGuard {

    /** True when [ip] is a textual IPv4/IPv6 address that must never be contacted. */
    fun isBlockedIp(ip: String): Boolean {
        val normalized = ip.lowercase().trim().trim('[', ']')
        if (normalized.isEmpty()) return true
        // IPv4-mapped/compatible IPv6 must be judged by its embedded IPv4 address.
        if (normalized.contains(":")) return isBlockedIpv6(normalized)
        val octets = normalized.split(".")
        if (octets.size == 4 && octets.all { (it.toIntOrNull() ?: -1) in 0..255 }) {
            return isBlockedIpv4(octets.map { it.toInt() })
        }
        // Not a recognizable address; treat unknown forms as blocked (fail closed).
        return true
    }

    fun isBlockedIpv4(o: List<Int>): Boolean {
        val (a, b, c) = Triple(o[0], o[1], o[2])
        return a == 127 || // loopback
            a == 10 || // RFC1918
            a == 0 || // "this network" / unspecified
            (a == 169 && b == 254) || // link-local (incl. cloud metadata 169.254.169.254)
            (a == 192 && b == 168) || // RFC1918
            (a == 172 && b in 16..31) || // RFC1918
            (a == 100 && b in 64..127) || // carrier-grade NAT
            (a == 198 && b == 18) || // benchmarking 198.18.0.0/15
            a in 224..239 || // multicast
            a >= 240 || // reserved (240.0.0.0/4 + broadcast)
            (a == 192 && b == 0 && c == 0) // 192.0.0.0/24 (IETF protocol assignments)
    }

    private fun isBlockedIpv6(h: String): Boolean {
        if (h == "::" || h == "::1" || h == "0:0:0:0:0:0:0:1") return true // unspecified + loopback
        if (h.startsWith("fe8") || h.startsWith("fe9") || h.startsWith("fea") || h.startsWith("feb")) return true // fe80::/10
        if (h.startsWith("fec")) return true // fec0::/10 (deprecated site-local, still internal)
        if (h.startsWith("fc") || h.startsWith("fd")) return true // fc00::/7 unique local
        if (h.startsWith("ff")) return true // ff00::/8 multicast
        if (h.startsWith("64:ff9b:1:")) return true // local-use NAT64 64:ff9b:1::/48
        // Well-known NAT64 64:ff9b::/96 embeds an IPv4 address in the last 32 bits.
        if (h.startsWith("64:ff9b::")) {
            val embedded = h.removePrefix("64:ff9b::")
            val groups = embedded.split(":").filter { it.isNotEmpty() }
            if (groups.size == 2) {
                val hi = groups[0].toIntOrNull(16) ?: return true
                val lo = groups[1].toIntOrNull(16) ?: return true
                return isBlockedIpv4(listOf(hi shr 8, hi and 0xff, lo shr 8, lo and 0xff))
            }
            return true
        }
        // IPv4-mapped/compatible (::ffff:127.0.0.1, ::ffff:7f00:1): judge the embedded IPv4 address.
        val mapped = h.removePrefix("::ffff:").removePrefix("0:0:0:0:0:ffff:")
        if (mapped != h) {
            val dotted = mapped.split(".")
            if (dotted.size == 4) return dotted.all { (it.toIntOrNull() ?: -1) in 0..255 } && isBlockedIpv4(dotted.map { it.toInt() })
            val groups = mapped.split(":")
            if (groups.size == 2) {
                val hi = groups[0].toIntOrNull(16) ?: return true
                val lo = groups[1].toIntOrNull(16) ?: return true
                return isBlockedIpv4(listOf(hi shr 8, hi and 0xff, lo shr 8, lo and 0xff))
            }
            return true
        }
        return false
    }
}
