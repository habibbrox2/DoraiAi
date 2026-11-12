package com.dorai.ai.mcp

internal actual fun sha256(input: ByteArray): ByteArray = throw UnsupportedOperationException("MCP OAuth requires the Android or Desktop platform")

internal actual fun secureRandomBytes(length: Int): ByteArray = throw UnsupportedOperationException("MCP OAuth requires the Android or Desktop platform")
