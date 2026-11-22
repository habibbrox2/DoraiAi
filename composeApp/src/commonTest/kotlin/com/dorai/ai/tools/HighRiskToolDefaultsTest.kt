package com.dorai.ai.tools

import com.dorai.ai.data.AppSettings
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Regression tests for high-risk plugin opt-in defaults (fresh installs only). */
class HighRiskToolDefaultsTest {

    private fun settings() = AppSettings(MapSettings())

    @Test
    fun `high-risk set is exactly the telephony location and device identity tools`() {
        val highRisk = listOf(
            CommonTools.ipLocationToolInfo,
            CommonTools.dialPhoneToolInfo,
            CommonTools.dialUssdToolInfo,
            CommonTools.checkBdSimBalanceToolInfo,
            CommonTools.detectSimOperatorToolInfo,
            CommonTools.deviceInfoToolInfo,
        )
        highRisk.forEach { assertTrue(it.isHighRisk, "${it.id} must require opt-in") }

        val ordinary = listOf(
            WebSearchTool.toolInfo,
            CommonTools.localTimeToolInfo,
            CommonTools.openUrlToolInfo,
            FetchUrlTool.toolInfo,
        )
        ordinary.forEach { assertFalse(it.isHighRisk, "${it.id} must not require opt-in") }
    }

    @Test
    fun `fresh install resolves high-risk tools off and ordinary tools on`() {
        val s = settings()
        val resolved = CommonTools.commonToolDefinitions
            .filter { it.userToggleable }
            .associate { it.id to s.isToolEnabled(it.id, defaultEnabled = !it.isHighRisk) }

        assertFalse(resolved.getValue("dial_phone"))
        assertFalse(resolved.getValue("dial_ussd"))
        assertFalse(resolved.getValue("check_bd_sim_balance"))
        assertFalse(resolved.getValue("detect_sim_operator"))
        assertFalse(resolved.getValue("get_location_from_ip"))
        assertFalse(resolved.getValue("get_device_info"))
        assertTrue(resolved.getValue("web_search"))
        assertTrue(resolved.getValue("get_local_time"))
    }

    @Test
    fun `existing stored preference is never overwritten by the new default`() {
        val s = settings()
        s.setToolEnabled("dial_phone", true)
        assertTrue(s.isToolEnabled("dial_phone", defaultEnabled = false))
    }

    @Test
    fun `execution list excludes high-risk tools on a fresh install`() {
        val enabledIds = CommonTools.getCommonTools(settings()).map { it.schema.name }
        assertFalse("dial_phone" in enabledIds)
        assertFalse("dial_ussd" in enabledIds)
        assertFalse("check_bd_sim_balance" in enabledIds)
        assertFalse("detect_sim_operator" in enabledIds)
        assertFalse("get_location_from_ip" in enabledIds)
        assertFalse("get_device_info" in enabledIds)
        assertTrue("web_search" in enabledIds)
        assertTrue("get_local_time" in enabledIds)
    }
}
