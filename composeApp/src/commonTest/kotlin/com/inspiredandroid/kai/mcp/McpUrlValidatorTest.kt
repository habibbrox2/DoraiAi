package com.inspiredandroid.kai.mcp

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class McpUrlValidatorTest {

    // Scheme tests
    @Test fun rejectsFileScheme() {
        assertFalse(McpUrlValidator.validate("file:///etc/passwd").isValid)
    }

    @Test fun rejectsFtpScheme() {
        assertFalse(McpUrlValidator.validate("ftp://example.com").isValid)
    }

    @Test fun rejectsGopherScheme() {
        assertFalse(McpUrlValidator.validate("gopher://example.com").isValid)
    }

    @Test fun rejectsDataScheme() {
        assertFalse(McpUrlValidator.validate("data:text/plain,hello").isValid)
    }

    @Test fun rejectsJavaScriptScheme() {
        assertFalse(McpUrlValidator.validate("javascript:alert(1)").isValid)
    }

    @Test fun rejectsMissingScheme() {
        assertFalse(McpUrlValidator.validate("example.com").isValid)
    }

    @Test fun rejectsHttpForRemoteHost() {
        assertFalse(McpUrlValidator.validate("http://example.com").isValid)
    }

    @Test fun allowsHttpForLocalhost() {
        assertTrue(McpUrlValidator.validate("http://localhost:8080").isValid)
    }

    @Test fun allowsHttpFor127001() {
        assertTrue(McpUrlValidator.validate("http://127.0.0.1:8080").isValid)
    }

    @Test fun allowsHttpForIpv6Loopback() {
        assertTrue(McpUrlValidator.validate("http://[::1]:8080").isValid)
    }

    @Test fun allowsHttpForEmulatorHost() {
        assertTrue(McpUrlValidator.validate("http://10.0.2.2:8080").isValid)
    }

    @Test fun allowsHttpsForPublicHost() {
        assertTrue(McpUrlValidator.validate("https://93.184.216.34/mcp").isValid)
    }

    // Credential tests
    @Test fun rejectsUrlWithCredentials() {
        assertFalse(McpUrlValidator.validate("https://user:pass@example.com").isValid)
    }

    // Host encoding tests
    @Test fun rejectsEncodedLocalhost() {
        assertFalse(McpUrlValidator.validate("https://%6c%6f%63%61%6c%68%6f%73%74").isValid)
    }

    // IP literal tests
    @Test fun rejectsPrivateIp10x() {
        assertFalse(McpUrlValidator.validate("https://10.0.0.1").isValid)
    }

    @Test fun rejectsPrivateIp17216() {
        assertFalse(McpUrlValidator.validate("https://172.16.0.1").isValid)
    }

    @Test fun rejectsPrivateIp192168() {
        assertFalse(McpUrlValidator.validate("https://192.168.1.1").isValid)
    }

    @Test fun rejectsLinkLocalIp() {
        assertFalse(McpUrlValidator.validate("https://169.254.1.1").isValid)
    }

    @Test fun rejectsCloudMetadataIp() {
        assertFalse(McpUrlValidator.validate("https://169.254.169.254").isValid)
    }

    @Test fun rejectsMulticastIp() {
        assertFalse(McpUrlValidator.validate("https://224.0.0.1").isValid)
    }

    @Test fun rejectsIpv4MappedIpv6() {
        assertFalse(McpUrlValidator.validate("https://[::ffff:10.0.0.1]").isValid)
    }

    // Header tests
    @Test fun rejectsHeaderWithCr() {
        assertFalse(McpUrlValidator.validateHeaders(mapOf("X-Test" to "value\rinjected")).isValid)
    }

    @Test fun rejectsHeaderWithLf() {
        assertFalse(McpUrlValidator.validateHeaders(mapOf("X-Test" to "value\ninjected")).isValid)
    }

    @Test fun rejectsHeaderWithControlChar() {
        assertFalse(McpUrlValidator.validateHeaders(mapOf("X-Test" to "value\u0000injected")).isValid)
    }

    @Test fun allowsCleanHeaders() {
        assertTrue(McpUrlValidator.validateHeaders(mapOf("Authorization" to "Bearer token123")).isValid)
    }

    // Edge cases
    @Test fun rejectsEmptyUrl() {
        assertFalse(McpUrlValidator.validate("").isValid)
    }

    @Test fun rejectsUrlWithEncodedCredentials() {
        assertFalse(McpUrlValidator.validate("https://%61%64%6d%69%6e:pass@example.com").isValid)
    }

    // allowPrivateIps tests
    @Test fun allowsPrivateIpWhenFlagSet() {
        assertTrue(McpUrlValidator.validate("https://192.168.1.1", allowPrivateIps = true).isValid)
    }

    @Test fun stillRejectsCloudMetadataWhenFlagSet() {
        assertFalse(McpUrlValidator.validate("https://169.254.169.254", allowPrivateIps = true).isValid)
    }

    @Test fun stillRejectsLoopbackWhenFlagSet() {
        // Loopback is not allowed even with allowPrivateIps (only localhost HTTP is allowed)
        assertFalse(McpUrlValidator.validate("https://127.0.0.1", allowPrivateIps = true).isValid)
    }
}
