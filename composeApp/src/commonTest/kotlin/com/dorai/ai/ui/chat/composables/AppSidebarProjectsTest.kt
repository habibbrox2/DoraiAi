package com.dorai.ai.ui.chat.composables

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

class AppSidebarProjectsTest {

    @Test
    fun `not ready wins even when projects exist`() {
        assertEquals(
            SidebarProjectSection.NotReady,
            sidebarProjectSection(persistentListOf("demo"), isReady = false),
        )
    }

    @Test
    fun `ready with no projects is empty`() {
        assertEquals(
            SidebarProjectSection.Empty,
            sidebarProjectSection(persistentListOf(), isReady = true),
        )
    }

    @Test
    fun `ready with projects is ready`() {
        assertEquals(
            SidebarProjectSection.Ready,
            sidebarProjectSection(persistentListOf("demo", "other"), isReady = true),
        )
    }
}
