package com.dorai.ai.ui.chat.composables

import kotlinx.collections.immutable.ImmutableList

/** Which sidebar Projects state to render — pure, exported for tests. */
enum class SidebarProjectSection {
    /** Debian not ready: show the setup row instead of an empty list. */
    NotReady,

    /** Ready but no folders yet. */
    Empty,

    /** Ready with folders to list. */
    Ready,
}

/**
 * Pure section classifier for the sidebar Projects block. Keeps the composable
 * presentational over `ImmutableList<String>` with no filesystem access.
 */
fun sidebarProjectSection(projects: ImmutableList<String>, isReady: Boolean): SidebarProjectSection = when {
    !isReady -> SidebarProjectSection.NotReady
    projects.isEmpty() -> SidebarProjectSection.Empty
    else -> SidebarProjectSection.Ready
}
