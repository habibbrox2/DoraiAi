package com.inspiredandroid.kai.ui.settings

/**
 * Calculates the horizontal scroll offset needed to reveal a selected tab.
 *
 * [itemStart] and [itemEnd] are content-relative coordinates reported by
 * the scroll container before applying the current scroll offset.
 */
internal fun settingsTabScrollTarget(
    currentScroll: Int,
    viewportWidth: Int,
    maxScroll: Int,
    itemStart: Int,
    itemEnd: Int,
): Int? {
    if (viewportWidth <= 0 || maxScroll <= 0) return null

    val visibleEnd = currentScroll + viewportWidth
    val target = when {
        itemStart < currentScroll -> itemStart
        itemEnd > visibleEnd -> itemEnd - viewportWidth
        else -> return null
    }

    return target.coerceIn(0, maxScroll).takeIf { it != currentScroll }
}
