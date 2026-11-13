package com.dorai.ai.mcp

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * JVM implementation of [OauthCallbackListener]. Shared by the Android and Desktop targets
 * through the `jvmShared` source set.
 *
 * Binds `127.0.0.1:0` (ephemeral port), accepts `GET /callback?code=...` requests and returns
 * the branded hand-off page the user can close, and exposes the query parameters.
 *
 * The listener stays open until the real callback arrives: browsers sometimes probe
 * `/favicon.ico` on the same origin first, and answering that request would otherwise consume
 * the one-shot socket the OAuth handshake is waiting on.
 */
internal class JvmOauthCallbackListener(
    /** The MCP server being authorized; shown on the hand-off page when known. */
    private val serverName: String?,
    /**
     * A deep link that re-opens the app (e.g. `dorai://oauth/done`), offered as a button on
     * the hand-off page. Null on platforms without a handler for it (desktop).
     */
    private val returnUri: String? = null,
) : OauthCallbackListener {
    private var socket: ServerSocket? = null

    override val port: Int get() = socket?.localPort ?: 0

    override fun start() {
        if (socket != null) return
        socket = ServerSocket(0, 1, InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1)))
        socket!!.soTimeout = 5 * 60 * 1000
    }

    override suspend fun awaitHandshake(timeoutSeconds: Long): OauthCallbackRequest {
        start()
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val current = socket!!
            current.soTimeout = (timeoutSeconds * 1000).toInt()
            kotlinx.coroutines.withTimeout(timeoutSeconds * 1000 + 1_000) {
                current.use { listener ->
                    var handled: HandledRequest? = null
                    // Only a real OAuth callback ends the handshake; anything else
                    // (favicon, stray probe) gets a 404 and the loop continues.
                    while (handled == null) {
                        val client = listener.accept()
                        handled = client.use(::handleClient).takeIf { it.isCallback }
                    }
                    handled.request
                }
            }
        }
    }

    override fun stop() {
        runCatching { socket?.close() }
        socket = null
    }

    private fun handleClient(socket: Socket): HandledRequest {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        val request = reader.readLine() ?: ""
        val callback = parseCallbackLine(request)
        if (!callback.isCallback) {
            writeResponse(socket, status = "404 Not Found", body = "Not found".encodeToByteArray())
            return HandledRequest(callback, isCallback = false)
        }
        val html = OauthCallbackPage.render(
            serverName = serverName,
            error = callback.error,
            errorDescription = callback.errorDescription,
            returnUri = returnUri,
        )
        writeResponse(socket, status = "200 OK", body = html.encodeToByteArray())
        return HandledRequest(callback, isCallback = true)
    }

    private fun writeResponse(socket: Socket, status: String, body: ByteArray) {
        val head = buildString {
            appendLine("HTTP/1.1 $status")
            appendLine("Content-Type: text/html; charset=utf-8")
            appendLine("Content-Length: ${body.size}")
            appendLine("Cache-Control: no-store")
            appendLine("Connection: close")
            appendLine()
        }.encodeToByteArray()
        socket.getOutputStream().apply {
            write(head, 0, head.size)
            write(body, 0, body.size)
            flush()
        }
    }

    private fun parseCallbackLine(request: String): OauthCallbackRequest {
        val query = request.substringAfter('?', "").substringBefore(' ')
        if (query.isEmpty()) {
            return OauthCallbackRequest(code = null, state = null, error = "unsupported_callback", errorDescription = "Missing query parameters")
        }
        return OauthCallbackRequest(
            code = queryParameter(query, "code"),
            state = queryParameter(query, "state"),
            error = queryParameter(query, "error"),
            errorDescription = queryParameter(query, "error_description"),
        )
    }

    private data class HandledRequest(
        val request: OauthCallbackRequest,
        val isCallback: Boolean,
    )
}

/** True when the request carries an OAuth result the handshake can act on. */
private val OauthCallbackRequest.isCallback: Boolean
    get() = !code.isNullOrBlank() || !state.isNullOrBlank() || !error.isNullOrBlank()

private fun queryParameter(query: String, key: String): String? = query.split('&')
    .mapNotNull { part ->
        val kvs = part.split('=', limit = 2)
        if (kvs.size == 2 && kvs[0] == key) urlDecode(kvs[1]) else null
    }
    .firstOrNull()

private fun urlDecode(value: String): String {
    val bytes = value.toByteArray(Charsets.UTF_8)
    val out = java.io.ByteArrayOutputStream()
    var i = 0
    while (i < bytes.size) {
        val b = bytes[i].toInt() and 0xff
        when (b) {
            '+'.code -> out.write(' '.code)

            '%'.code -> {
                val hi = hexDigit(bytes.getOrNull(i + 1)?.toInt() ?: 0)
                val lo = hexDigit(bytes.getOrNull(i + 2)?.toInt() ?: 0)
                out.write((hi shl 4) or lo)
                i += 2
            }

            else -> out.write(b)
        }
        i += 1
    }
    return out.toString(Charsets.UTF_8)
}

private fun hexDigit(ch: Int): Int = when {
    ch in '0'.code..'9'.code -> ch - '0'.code
    ch in 'a'.code..'f'.code -> ch - 'a'.code + 10
    ch in 'A'.code..'F'.code -> ch - 'A'.code + 10
    else -> 0
}
