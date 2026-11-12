package com.dorai.ai.voice.benchmark

import platform.Foundation.NSString
import platform.Foundation.precomposedStringWithCanonicalMapping

internal actual fun normalizeNfc(text: String): String = (text as NSString).precomposedStringWithCanonicalMapping
