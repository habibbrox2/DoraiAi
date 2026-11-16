package com.dorai.ai.cloud

import java.security.SecureRandom

internal actual fun secureRandomBytes(size: Int): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }
