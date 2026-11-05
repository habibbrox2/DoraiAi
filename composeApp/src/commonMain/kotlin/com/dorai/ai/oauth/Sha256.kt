package com.dorai.ai.oauth

fun sha256(input: ByteArray): ByteArray {
    val k = longArrayOf(
        0x428a2f98L, 0x71374491L, 0xb5c0fbcfL, 0xe9b5dba5L, 0x3956c25bL, 0x59f111f1L, 0x923f82a4L, 0xab1c5ed5L,
        0xd807aa98L, 0x12835b01L, 0x243185beL, 0x550c7dc3L, 0x72be5d74L, 0x80deb1feL, 0x9bdc06a7L, 0xc19bf174L,
        0xe49b69c1L, 0xefbe4786L, 0x0fc19dc6L, 0x240ca1ccL, 0x2de92c6fL, 0x4a7484aaL, 0x5cb0a9dcL, 0x76f988daL,
        0x983e5152L, 0xa831c66dL, 0xb00327c8L, 0xbf597fc7L, 0xc6e00bf3L, 0xd5a79147L, 0x06ca6351L, 0x14292967L,
        0x27b70a85L, 0x2e1b2138L, 0x4d2c6dfcL, 0x53380d13L, 0x650a7354L, 0x766a0abbL, 0x81c2c92eL, 0x92722c85L,
        0xa2bfe8a1L, 0xa81a664bL, 0xc24b8b70L, 0xc76c51a3L, 0xd192e819L, 0xd6990624L, 0xf40e3585L, 0x106aa070L,
        0x19a4c116L, 0x1e376c08L, 0x2748774cL, 0x34b0bcb5L, 0x391c0cb3L, 0x4ed8aa4aL, 0x5b9cca4fL, 0x682e6ff3L,
        0x748f82eeL, 0x78a5636fL, 0x84c87814L, 0x8cc70208L, 0x90befffaL, 0xa4506cebL, 0xbef9a3f7L, 0xc67178f2L,
    )

    val bitLen = input.size.toLong() * 8
    val padded = ByteArray(((input.size + 9 + 63) / 64) * 64)
    input.copyInto(padded)
    padded[input.size] = 0x80.toByte()
    var len = bitLen
    for (i in 7 downTo 0) {
        padded[padded.size - 1 - (7 - i)] = (len and 0xff).toByte()
        len = len ushr 8
    }

    var h0 = 0x6a09e667L
    var h1 = 0xbb67ae85L
    var h2 = 0x3c6ef372L
    var h3 = 0xa54ff53aL
    var h4 = 0x510e527fL
    var h5 = 0x9b056886L
    var h6 = 0x1f83d9abL
    var h7 = 0x5be0cd19L

    val w = LongArray(64)
    var offset = 0
    while (offset < padded.size) {
        for (i in 0..15) {
            w[i] = (
                ((padded[offset + i * 4].toInt() and 0xff).toLong() shl 24) or
                    ((padded[offset + i * 4 + 1].toInt() and 0xff).toLong() shl 16) or
                    ((padded[offset + i * 4 + 2].toInt() and 0xff).toLong() shl 8) or
                    (padded[offset + i * 4 + 3].toInt() and 0xff).toLong()
                ) and 0xffffffffL
        }
        for (i in 16..63) {
            val s0 = rotr(w[i - 15], 7) xor rotr(w[i - 15], 18) xor (w[i - 15] ushr 3)
            val s1 = rotr(w[i - 2], 17) xor rotr(w[i - 2], 19) xor (w[i - 2] ushr 10)
            w[i] = (w[i - 16] + s0 + w[i - 7] + s1) and 0xffffffffL
        }
        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4
        var f = h5
        var g = h6
        var h = h7
        for (i in 0..63) {
            val s1 = rotr(e, 6) xor rotr(e, 11) xor rotr(e, 25)
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = (h + s1 + ch + k[i] + w[i]) and 0xffffffffL
            val s0 = rotr(a, 2) xor rotr(a, 13) xor rotr(a, 22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = (s0 + maj) and 0xffffffffL
            h = g
            g = f
            f = e
            e = (d + temp1) and 0xffffffffL
            d = c
            c = b
            b = a
            a = (temp1 + temp2) and 0xffffffffL
        }
        h0 = (h0 + a) and 0xffffffffL
        h1 = (h1 + b) and 0xffffffffL
        h2 = (h2 + c) and 0xffffffffL
        h3 = (h3 + d) and 0xffffffffL
        h4 = (h4 + e) and 0xffffffffL
        h5 = (h5 + f) and 0xffffffffL
        h6 = (h6 + g) and 0xffffffffL
        h7 = (h7 + h) and 0xffffffffL
        offset += 64
    }

    val out = ByteArray(32)
    val vals = longArrayOf(h0, h1, h2, h3, h4, h5, h6, h7)
    for (i in vals.indices) {
        out[i * 4] = (vals[i] ushr 24).toByte()
        out[i * 4 + 1] = (vals[i] ushr 16).toByte()
        out[i * 4 + 2] = (vals[i] ushr 8).toByte()
        out[i * 4 + 3] = vals[i].toByte()
    }
    return out
}

private fun rotr(x: Long, n: Int): Long = (x ushr n) or (x shl (32 - n))

fun base64UrlEncodeNoPadding(bytes: ByteArray): String {
    val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    val sb = StringBuilder()
    var i = 0
    while (i < bytes.size) {
        val b0 = bytes[i].toInt() and 0xff
        val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xff else -1
        val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xff else -1
        sb.append(table[b0 ushr 2])
        sb.append(table[((b0 and 3) shl 4) or (if (b1 >= 0) b1 ushr 4 else 0)])
        if (b1 >= 0) sb.append(table[((b1 and 0x0f) shl 2) or (if (b2 >= 0) b2 ushr 6 else 0)])
        if (b2 >= 0) sb.append(table[b2 and 0x3f])
        i += 3
    }
    return sb.toString()
}
