package com.dorai.ai.tools

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression coverage for the shared SSRF IP deny-list used by outbound fetches
 * and the OpenAI-Compatible locality classifier.
 *
 * Ranges follow the OWASP SSRF Prevention Cheat Sheet deny-list:
 * https://cheatsheetseries.owasp.org/cheatsheets/Server_Side_Request_Forgery_Prevention_Cheat_Sheet.html
 */
class SsrfAddressGuardTest {

    @Test
    fun `loopback private and link-local ipv4 are blocked`() {
        listOf(
            "127.0.0.1", "127.8.8.8", "10.1.2.3", "192.168.0.10", "172.16.0.1", "172.31.255.255",
            "169.254.169.254", // cloud metadata
            "0.0.0.0", "100.64.0.1", "198.18.0.5", "224.0.0.1", "240.0.0.1", "255.255.255.255",
            "192.0.0.1",
        ).forEach { assertTrue(NetworkAddressGuard.isBlockedIp(it), it) }
    }

    @Test
    fun `public ipv4 is allowed`() {
        listOf("8.8.8.8", "1.1.1.1", "93.184.216.34", "172.32.0.1", "100.128.0.1", "198.20.0.1").forEach {
            assertFalse(NetworkAddressGuard.isBlockedIp(it), it)
        }
    }

    @Test
    fun `ipv6 loopback ula link-local and multicast are blocked`() {
        listOf("::", "::1", "fe80::1", "fec0::1", "fc00::1", "fd12:3456::1", "ff02::1").forEach {
            assertTrue(NetworkAddressGuard.isBlockedIp(it), it)
        }
    }

    @Test
    fun `nat64 encapsulated ipv4 is judged by the embedded address`() {
        // 64:ff9b::7f00:1 → 127.0.0.1
        assertTrue(NetworkAddressGuard.isBlockedIp("64:ff9b::7f00:1"))
        // 64:ff9b::/96 + 10.0.0.1
        assertTrue(NetworkAddressGuard.isBlockedIp("64:ff9b::a00:1"))
        // 64:ff9b:1::/48 local-use NAT64 prefix
        assertTrue(NetworkAddressGuard.isBlockedIp("64:ff9b:1::1"))
        // public embedded address is allowed
        assertFalse(NetworkAddressGuard.isBlockedIp("64:ff9b::808:808"))
    }

    @Test
    fun `ipv4-mapped ipv6 is normalized before classification`() {
        assertTrue(NetworkAddressGuard.isBlockedIp("::ffff:127.0.0.1"))
        assertTrue(NetworkAddressGuard.isBlockedIp("::ffff:7f00:1"))
        assertTrue(NetworkAddressGuard.isBlockedIp("::ffff:c0a8:0101")) // 192.168.0.1
        assertFalse(NetworkAddressGuard.isBlockedIp("::ffff:8.8.8.8"))
    }

    @Test
    fun `unknown or malformed addresses fail closed`() {
        listOf("", "not-an-ip", "999.1.1.1", "1.2.3").forEach {
            assertTrue(NetworkAddressGuard.isBlockedIp(it), it)
        }
    }
}
