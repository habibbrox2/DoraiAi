package com.dorai.ai.tools

import com.dorai.ai.resolveHostAddresses
import com.dorai.ai.supportsDnsResolution
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Desktop JVM regression test for resolved-IP SSRF pinning: hostnames that
 * resolve to loopback/private addresses must be classifiable even when the
 * *literal* hostname looks innocent. Uses only `localhost` so the test never
 * needs external network access.
 */
class DnsResolvedHostGuardTest {

    @Test
    fun `desktop platform supports dns resolution`() {
        assertTrue(supportsDnsResolution, "desktop actual must declare DNS support so fetch_url validates resolved IPs")
    }

    @Test
    fun `localhost resolves and every resolved address is blocked by the guard`() {
        val addresses = resolveHostAddresses("localhost")
        assertTrue(addresses.isNotEmpty(), "JVM must resolve localhost (hosts file)")
        addresses.forEach { address ->
            assertTrue(
                NetworkAddressGuard.isBlockedIp(address),
                "resolved address $address for 'localhost' must be classified as blocked",
            )
        }
    }
}
