package com.dorai.ai

actual fun createDaemonController(): DaemonController = NoOpDaemonController()
