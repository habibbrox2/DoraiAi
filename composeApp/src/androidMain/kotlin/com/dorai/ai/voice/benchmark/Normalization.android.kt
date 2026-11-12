package com.dorai.ai.voice.benchmark

import java.text.Normalizer

internal actual fun normalizeNfc(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFC)
