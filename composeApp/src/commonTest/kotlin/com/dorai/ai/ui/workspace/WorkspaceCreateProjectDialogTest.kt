package com.dorai.ai.ui.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkspaceCreateProjectDialogTest {

    @Test
    fun `blank name has no error but stays disabled`() {
        assertNull(newProjectNameError("", listOf("demo")))
        assertNull(newProjectNameError("   ", listOf("demo")))
    }

    @Test
    fun `slash in name is invalid`() {
        assertEquals(NewProjectNameError.Invalid, newProjectNameError("a/b", listOf("demo")))
        assertEquals(NewProjectNameError.Invalid, newProjectNameError(" demo/ ", listOf("demo")))
    }

    @Test
    fun `exact trimmed collision is reported`() {
        assertEquals(NewProjectNameError.Collision, newProjectNameError("demo", listOf("demo", "other")))
        assertEquals(NewProjectNameError.Collision, newProjectNameError(" demo ", listOf("demo")))
    }

    @Test
    fun `new name with no collision is valid`() {
        assertNull(newProjectNameError("new-demo", listOf("demo", "other")))
        assertNull(newProjectNameError("Demo", listOf("demo")))
    }
}
