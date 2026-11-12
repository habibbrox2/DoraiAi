package com.dorai.ai.mcp

// Internal platform crypto primitives used only by the MCP OAuth flow.

internal expect fun sha256(input: ByteArray): ByteArray

internal expect fun secureRandomBytes(length: Int): ByteArray
