package com.dorai.ai.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidUpdateSessionStateTest {

    @Test
    fun initialStateIsIdle() {
        AndroidUpdateSessionState.reset()
        assertEquals(AndroidUpdateSessionState.InstallState.Idle, AndroidUpdateSessionState.state.value)
    }

    @Test
    fun stateTransitionsUpdateStateFlow() {
        AndroidUpdateSessionState.reset()

        AndroidUpdateSessionState.updateState(AndroidUpdateSessionState.InstallState.Staging(101))
        assertEquals(
            AndroidUpdateSessionState.InstallState.Staging(101),
            AndroidUpdateSessionState.state.value,
        )

        AndroidUpdateSessionState.updateState(AndroidUpdateSessionState.InstallState.Installing(101))
        assertEquals(
            AndroidUpdateSessionState.InstallState.Installing(101),
            AndroidUpdateSessionState.state.value,
        )

        AndroidUpdateSessionState.updateState(AndroidUpdateSessionState.InstallState.Success(229, "2.1.17"))
        assertEquals(
            AndroidUpdateSessionState.InstallState.Success(229, "2.1.17"),
            AndroidUpdateSessionState.state.value,
        )

        AndroidUpdateSessionState.updateState(
            AndroidUpdateSessionState.InstallState.Failed(3, "Installation was cancelled by the user."),
        )
        val failureState = AndroidUpdateSessionState.state.value
        assertTrue(failureState is AndroidUpdateSessionState.InstallState.Failed)
        assertEquals(3, (failureState as AndroidUpdateSessionState.InstallState.Failed).statusCode)
        assertEquals("Installation was cancelled by the user.", failureState.reason)

        AndroidUpdateSessionState.reset()
        assertEquals(AndroidUpdateSessionState.InstallState.Idle, AndroidUpdateSessionState.state.value)
    }
}
