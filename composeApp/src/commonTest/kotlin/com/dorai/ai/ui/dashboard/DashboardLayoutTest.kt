package com.dorai.ai.ui.dashboard

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class DashboardLayoutTest {

    @Test
    fun compactWidthUsesSingleColumn() {
        assertEquals(1, dashboardColumnsFor(599.dp))
    }

    @Test
    fun tabletWidthUsesTwoColumns() {
        assertEquals(2, dashboardColumnsFor(600.dp))
        assertEquals(2, dashboardColumnsFor(999.dp))
    }

    @Test
    fun expandedWidthUsesFourColumns() {
        assertEquals(4, dashboardColumnsFor(1000.dp))
    }
}
