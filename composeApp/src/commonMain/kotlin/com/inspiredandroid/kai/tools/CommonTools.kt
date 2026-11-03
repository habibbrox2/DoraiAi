package com.inspiredandroid.kai.tools

import com.inspiredandroid.kai.data.AppSettings
import com.inspiredandroid.kai.data.MemoryCategory
import com.inspiredandroid.kai.data.MemoryStore
import com.inspiredandroid.kai.getDeviceInfo
import com.inspiredandroid.kai.getSimOperatorCode
import com.inspiredandroid.kai.httpClient
import com.inspiredandroid.kai.network.tools.ParameterSchema
import com.inspiredandroid.kai.network.tools.Tool
import com.inspiredandroid.kai.network.tools.ToolInfo
import com.inspiredandroid.kai.network.tools.ToolSchema
import com.inspiredandroid.kai.openUrl
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kai.composeapp.generated.resources.Res
import kai.composeapp.generated.resources.tool_get_local_time_description
import kai.composeapp.generated.resources.tool_get_local_time_name
import kai.composeapp.generated.resources.tool_get_location_description
import kai.composeapp.generated.resources.tool_get_location_name
import kai.composeapp.generated.resources.tool_memory_forget_description
import kai.composeapp.generated.resources.tool_memory_forget_name
import kai.composeapp.generated.resources.tool_memory_learn_description
import kai.composeapp.generated.resources.tool_memory_learn_name
import kai.composeapp.generated.resources.tool_memory_reinforce_description
import kai.composeapp.generated.resources.tool_memory_reinforce_name
import kai.composeapp.generated.resources.tool_memory_store_description
import kai.composeapp.generated.resources.tool_memory_store_name
import kai.composeapp.generated.resources.tool_open_url_description
import kai.composeapp.generated.resources.tool_open_url_name
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@Serializable
private data class IpLocationResponse(
    val ip: String? = null,
    val success: Boolean = false,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val postal: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val connection: IpConnectionInfo? = null,
    val timezone: IpTimezoneInfo? = null,
    val message: String? = null,
    val continent: String? = null,
    val org: String? = null,
    val asn: String? = null,
)

@Serializable
private data class IpConnectionInfo(
    val isp: String? = null,
    val org: String? = null,
    val asn: Int? = null,
)

@Serializable
private data class IpTimezoneInfo(
    val id: String? = null,
    val utc: String? = null,
)

// Fallback (ipapi.co) response shape (HTTPS, free tier)
@Serializable
private data class IpApiResponse(
    val ip: String? = null,
    val city: String? = null,
    val region: String? = null,
    val region_code: String? = null,
    val country_name: String? = null,
    val country_code: String? = null,
    val postal: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val utc_offset: String? = null,
    val org: String? = null,
    val asn: String? = null,
    val error: Boolean? = null,
    val reason: String? = null,
)

/**
 * Common tool definitions that work across all platforms.
 */
object CommonTools {

    private val locationClient = httpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
        }
    }

    val ipLocationTool = object : Tool {
        override val schema = ToolSchema(
            name = "get_location_from_ip",
            description = "Get an estimated location for an IP address (or the caller's IP if none is given). Returns city, region, country, coordinates, timezone, ISP and more. Uses ipwho.is with ip-api.com fallback.",
            parameters = mapOf(
                "ip" to ParameterSchema(type = "string", description = "Optional IP address to look up. If omitted, the caller's public IP is used.", required = false),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val ip = args["ip"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }

            // Primary: ipwho.is
            try {
                val url = if (ip == null) "https://ipwho.is/" else "https://ipwho.is/$ip"
                val response: IpLocationResponse = locationClient.get(url).body()
                if (response.success) {
                    return mapOf(
                        "success" to true,
                        "source" to "ipwho.is",
                        "ip" to (response.ip ?: ip),
                        "city" to response.city,
                        "region" to response.region,
                        "country" to response.country,
                        "country_code" to response.countryCode,
                        "continent" to response.continent,
                        "latitude" to response.latitude,
                        "longitude" to response.longitude,
                        "timezone" to response.timezone?.id,
                        "utc_offset" to response.timezone?.utc,
                        "zip" to response.postal,
                        "isp" to (response.connection?.isp ?: response.org),
                        "org" to (response.connection?.org ?: response.org),
                        "asn" to (response.connection?.asn ?: response.asn),
                        "google_maps_url" to (if (response.latitude != null && response.longitude != null) "https://www.google.com/maps?q=${response.latitude},${response.longitude}" else null),
                    )
                }
            } catch (_: Exception) {
                // fall through to fallback
            }

            // Fallback: ipapi.co (HTTPS)
            try {
                val url = if (ip == null) "https://ipapi.co/json/" else "https://ipapi.co/$ip/json/"
                val response: IpApiResponse = locationClient.get(url).body()
                if (response.error != true) {
                    return mapOf(
                        "success" to true,
                        "source" to "ipapi.co",
                        "ip" to (response.ip ?: ip),
                        "city" to response.city,
                        "region" to response.region,
                        "country" to response.country_name,
                        "country_code" to response.country_code,
                        "latitude" to response.latitude,
                        "longitude" to response.longitude,
                        "timezone" to response.timezone,
                        "utc_offset" to response.utc_offset,
                        "zip" to response.postal,
                        "org" to response.org,
                        "asn" to response.asn,
                        "google_maps_url" to (if (response.latitude != null && response.longitude != null) "https://www.google.com/maps?q=${response.latitude},${response.longitude}" else null),
                    )
                }
                return mapOf(
                    "success" to false,
                    "error" to (response.reason ?: "Failed to get location"),
                )
            } catch (e: Exception) {
                return mapOf(
                    "success" to false,
                    "error" to "Failed to get location: ${e.message}",
                )
            }
        }
    }

    val ipLocationToolInfo = ToolInfo(
        id = "get_location_from_ip",
        name = "Get Location",
        description = "Get estimated location from IP address",
        nameRes = Res.string.tool_get_location_name,
        descriptionRes = Res.string.tool_get_location_description,
    )

    val localTimeTool = object : Tool {
        override val schema = ToolSchema(
            name = "get_local_time",
            description = "Get the current local date and time. Call this first when the user mentions relative dates like 'tomorrow', 'next week', 'in 2 hours', etc.",
            parameters = emptyMap(),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val timeZone = TimeZone.currentSystemDefault()
            val now = Clock.System.now()
            val localDateTime = now.toLocalDateTime(timeZone)

            // Format display string manually since kotlinx-datetime doesn't have formatters
            val dayOfWeek = localDateTime.dayOfWeek.name.lowercase()
                .replaceFirstChar { it.uppercase() }
            val month = localDateTime.month.name.lowercase()
                .replaceFirstChar { it.uppercase() }
            val day = localDateTime.date.day
            val year = localDateTime.year
            val hour = localDateTime.hour
            val minute = localDateTime.minute.toString().padStart(2, '0')
            val amPm = if (hour < 12) "AM" else "PM"
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }

            return mapOf(
                "iso_datetime" to "${localDateTime.date}T${localDateTime.hour.toString().padStart(2, '0')}:$minute:${localDateTime.second.toString().padStart(2, '0')}",
                "display_datetime" to "$dayOfWeek, $month $day, $year at $hour12:$minute $amPm",
                "timezone" to timeZone.id,
                "day_of_week" to localDateTime.dayOfWeek.name,
            )
        }
    }

    val localTimeToolInfo = ToolInfo(
        id = "get_local_time",
        name = "Get Local Time",
        description = "Get the current local date and time for interpreting relative dates",
        nameRes = Res.string.tool_get_local_time_name,
        descriptionRes = Res.string.tool_get_local_time_description,
    )

    val memoryStoreToolInfo = ToolInfo(
        id = "memory_store",
        name = "Store Memory",
        description = "Store or update a memory with a descriptive key",
        nameRes = Res.string.tool_memory_store_name,
        descriptionRes = Res.string.tool_memory_store_description,
        userToggleable = false,
    )

    val memoryForgetToolInfo = ToolInfo(
        id = "memory_forget",
        name = "Forget Memory",
        description = "Delete a stored memory by its key",
        nameRes = Res.string.tool_memory_forget_name,
        descriptionRes = Res.string.tool_memory_forget_description,
        userToggleable = false,
    )

    val memoryLearnToolInfo = ToolInfo(
        id = "memory_learn",
        name = "Learn Memory",
        description = "Store a categorized learning, error resolution, or preference",
        nameRes = Res.string.tool_memory_learn_name,
        descriptionRes = Res.string.tool_memory_learn_description,
        userToggleable = false,
    )

    val memoryReinforceToolInfo = ToolInfo(
        id = "memory_reinforce",
        name = "Reinforce Memory",
        description = "Reinforce a memory that produced a good outcome",
        nameRes = Res.string.tool_memory_reinforce_name,
        descriptionRes = Res.string.tool_memory_reinforce_description,
        userToggleable = false,
    )

    val openUrlTool = object : Tool {
        override val schema = ToolSchema(
            name = "open_url",
            description = "Open a URL in the user's browser or default app. This ONLY opens the link for the user to view — you will NOT receive the page content back. Do not use this to fetch or read information from URLs. Use this when the user asks to open or visit a link.",
            parameters = mapOf(
                "url" to ParameterSchema(type = "string", description = "The URL to open", required = true),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val url = args["url"]?.toString()
                ?: return mapOf("success" to false, "error" to "URL is required")
            return try {
                val opened = openUrl(url)
                if (opened) {
                    mapOf("success" to true, "url" to url, "message" to "URL opened successfully")
                } else {
                    mapOf("success" to false, "error" to "Failed to open URL")
                }
            } catch (e: Exception) {
                mapOf("success" to false, "error" to "Failed to open URL: ${e.message}")
            }
        }
    }

    val openUrlToolInfo = ToolInfo(
        id = "open_url",
        name = "Open URL",
        description = "Open a URL or link on the device",
        nameRes = Res.string.tool_open_url_name,
        descriptionRes = Res.string.tool_open_url_description,
    )

    val dialPhoneTool = object : Tool {
        override val schema = ToolSchema(
            name = "dial_phone",
            description = "Open the phone dialer with a given phone number. Does not place the call automatically - the user must confirm. Use when the user asks to call or dial a number.",
            parameters = mapOf(
                "number" to ParameterSchema(type = "string", description = "Phone number to dial (digits, spaces, +, -, parentheses allowed)", required = true),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val raw = args["number"]?.toString()?.trim()
                ?: return mapOf("success" to false, "error" to "Phone number is required")
            if (raw.isEmpty()) {
                return mapOf("success" to false, "error" to "Phone number is required")
            }
            // Keep only dialable characters
            val sanitized = raw.filter { it.isDigit() || it == '+' || it == '*' || it == '#' || it == ',' || it == ';' }
            if (sanitized.isEmpty() || !sanitized.any { it.isDigit() }) {
                return mapOf("success" to false, "error" to "Invalid phone number")
            }
            val telUrl = "tel:$sanitized"
            return try {
                val opened = openUrl(telUrl)
                if (opened) {
                    mapOf("success" to true, "number" to sanitized, "message" to "Dialer opened")
                } else {
                    mapOf("success" to false, "error" to "Failed to open dialer")
                }
            } catch (e: Exception) {
                mapOf("success" to false, "error" to "Failed to open dialer: ${e.message}")
            }
        }
    }

    val dialPhoneToolInfo = ToolInfo(
        id = "dial_phone",
        name = "Dial Phone",
        description = "Open the phone dialer with a given number",
    )

    val dialUssdTool = object : Tool {
        override val schema = ToolSchema(
            name = "dial_ussd",
            description = "Dial a USSD code (e.g. *121# or *101#) to check SIM balance, data, or operator info. Opens the dialer; the result is shown on screen. Use when the user wants to check SIM/operator balance.",
            parameters = mapOf(
                "code" to ParameterSchema(type = "string", description = "USSD code to dial, e.g. *121# or *101#", required = true),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val raw = args["code"]?.toString()?.trim()
                ?: return mapOf("success" to false, "error" to "USSD code is required")
            if (raw.isEmpty()) {
                return mapOf("success" to false, "error" to "USSD code is required")
            }
            // Keep dialable USSD characters: digits, *, #, and +
            val sanitized = raw.filter { it.isDigit() || it == '*' || it == '#' || it == '+' }
            if (sanitized.isEmpty() || !sanitized.contains('#')) {
                // Most USSD codes end with #; warn but still attempt
            }
            if (sanitized.isEmpty()) {
                return mapOf("success" to false, "error" to "Invalid USSD code")
            }
            val telUrl = "tel:$sanitized"
            return try {
                val opened = openUrl(telUrl)
                if (opened) {
                    mapOf("success" to true, "code" to sanitized, "message" to "USSD dialer opened; check your screen for the result")
                } else {
                    mapOf("success" to false, "error" to "Failed to open dialer")
                }
            } catch (e: Exception) {
                mapOf("success" to false, "error" to "Failed to dial USSD: ${e.message}")
            }
        }
    }

    val dialUssdToolInfo = ToolInfo(
        id = "dial_ussd",
        name = "Dial USSD",
        description = "Dial a USSD code to check SIM balance",
    )

    private val bdOperatorBalanceCodes = mapOf(
        "gp" to "*566#",
        "grameenphone" to "*566#",
        "robi" to "*222#",
        "banglalink" to "*124#",
        "bl" to "*124#",
        "teletalk" to "*152#",
        "airtel" to "*778#",
    )

    private val bdMccMncToOperator = mapOf(
        "47001" to "gp",
        "47002" to "robi",
        "47003" to "banglalink",
        "47004" to "teletalk",
        "47007" to "airtel",
    )

    val detectSimOperatorTool = object : Tool {
        override val schema = ToolSchema(
            name = "detect_sim_operator",
            description = "Detect the SIM operator on the device (Bangladesh operators supported). Returns the operator code (MCC+MNC) and resolved operator name if known.",
            parameters = emptyMap(),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val code = getSimOperatorCode()
            if (code.isNullOrBlank()) {
                return mapOf("success" to false, "error" to "SIM operator could not be detected (unsupported platform or no SIM)")
            }
            val op = bdMccMncToOperator[code]
            return mapOf(
                "success" to true,
                "sim_operator_code" to code,
                "operator" to (op ?: "unknown"),
                "supported" to (op != null),
            )
        }
    }

    val detectSimOperatorToolInfo = ToolInfo(
        id = "detect_sim_operator",
        name = "Detect SIM Operator",
        description = "Detect the SIM operator on the device",
    )

    val checkBdSimBalanceTool = object : Tool {
        override val schema = ToolSchema(
            name = "check_bd_sim_balance",
            description = "Check SIM balance for Bangladesh operators (Grameenphone, Robi, Banglalink, Teletalk, Airtel) by dialing the operator's USSD balance code. If no operator is given, the device's SIM operator is auto-detected. Opens the dialer; result shown on screen.",
            parameters = mapOf(
                "operator" to ParameterSchema(type = "string", description = "Operator name: gp/grameenphone, robi, banglalink/bl, teletalk, airtel. Optional; auto-detected from SIM if omitted.", required = false),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val opRaw = args["operator"]?.toString()?.trim()?.lowercase()
            val op = if (opRaw.isNullOrEmpty()) {
                val code = getSimOperatorCode()
                val detected = code?.let { bdMccMncToOperator[it] }
                detected ?: return mapOf("success" to false, "error" to "Could not auto-detect SIM operator; please specify operator")
            } else {
                opRaw
            }
            val code = bdOperatorBalanceCodes[op]
                ?: return mapOf("success" to false, "error" to "Unknown operator: $op. Supported: gp, robi, banglalink, teletalk, airtel")
            val telUrl = "tel:$code"
            return try {
                val opened = openUrl(telUrl)
                if (opened) {
                    mapOf("success" to true, "operator" to op, "code" to code, "message" to "USSD dialer opened; check your screen for the balance")
                } else {
                    mapOf("success" to false, "error" to "Failed to open dialer")
                }
            } catch (e: Exception) {
                mapOf("success" to false, "error" to "Failed to dial USSD: ${e.message}")
            }
        }
    }

    val checkBdSimBalanceToolInfo = ToolInfo(
        id = "check_bd_sim_balance",
        name = "Check BD SIM Balance",
        description = "Check Bangladesh SIM operator balance via USSD",
    )

    val deviceInfoTool = object : Tool {
        override val schema = ToolSchema(
            name = "get_device_info",
            description = "Get information about the current device and app: platform, OS, app version, available processors, memory, etc.",
            parameters = emptyMap(),
        )

        override suspend fun execute(args: Map<String, Any>): Any = try {
            getDeviceInfo()
        } catch (e: Exception) {
            mapOf("success" to false, "error" to (e.message ?: "Failed to get device info"))
        }
    }

    val deviceInfoToolInfo = ToolInfo(
        id = "get_device_info",
        name = "Device Info",
        description = "Get device and app information",
    )

    // Every tool the chat UI may have to name, on every platform. Entries whose availability is
    // decided by a master toggle in Settings → Agent (memory / scheduling / email / SMS /
    // notifications) or by a platform capability are marked `userToggleable = false` at their
    // declaration, so the Tools tab skips them instead of drawing a switch nothing reads.
    // They still belong here: without a definition, chat falls back to showing the raw tool id.
    val commonToolDefinitions = listOf(
        WebSearchTool.toolInfo,
        localTimeToolInfo,
        ipLocationToolInfo,
        openUrlToolInfo,
        FetchUrlTool.toolInfo,
        deviceInfoToolInfo,
        dialPhoneToolInfo,
        dialUssdToolInfo,
        checkBdSimBalanceToolInfo,
        detectSimOperatorToolInfo,
    ) +
        listOf(memoryStoreToolInfo, memoryForgetToolInfo, memoryLearnToolInfo, memoryReinforceToolInfo) +
        SchedulingTools.schedulingToolDefinitions +
        HeartbeatTools.heartbeatToolDefinitions +
        EmailTools.emailToolDefinitions +
        SmsTools.smsToolDefinitions +
        NotificationTools.notificationToolDefinitions

    fun getCommonTools(appSettings: AppSettings): List<Tool> = buildList {
        if (appSettings.isToolEnabled(localTimeTool.schema.name)) {
            add(localTimeTool)
        }
        if (appSettings.isToolEnabled(ipLocationTool.schema.name)) {
            add(ipLocationTool)
        }
        if (appSettings.isToolEnabled(WebSearchTool.schema.name)) {
            add(WebSearchTool)
        }
        if (appSettings.isToolEnabled(openUrlTool.schema.name)) {
            add(openUrlTool)
        }
        if (appSettings.isToolEnabled(FetchUrlTool.schema.name)) {
            add(FetchUrlTool)
        }
        if (appSettings.isToolEnabled(deviceInfoTool.schema.name)) {
            add(deviceInfoTool)
        }
        if (appSettings.isToolEnabled(dialPhoneTool.schema.name)) {
            add(dialPhoneTool)
        }
        if (appSettings.isToolEnabled(dialUssdTool.schema.name)) {
            add(dialUssdTool)
        }
        if (appSettings.isToolEnabled(checkBdSimBalanceTool.schema.name)) {
            add(checkBdSimBalanceTool)
        }
        if (appSettings.isToolEnabled(detectSimOperatorTool.schema.name)) {
            add(detectSimOperatorTool)
        }
    }

    // Memory tools - always enabled, core agent functionality

    fun memoryStoreTool(memoryStore: MemoryStore) = object : Tool {
        override val schema = ToolSchema(
            name = "memory_store",
            description = "Store or update a memory with a descriptive key. Use this proactively to remember user preferences, facts, and important information across conversations.",
            parameters = mapOf(
                "key" to ParameterSchema(type = "string", description = "Descriptive key for the memory (e.g. user_name, preferred_language, project_details)", required = true),
                "content" to ParameterSchema(type = "string", description = "The content to store", required = true),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val key = args["key"]?.toString() ?: return mapOf("success" to false, "error" to "Missing key")
            val content = args["content"]?.toString() ?: return mapOf("success" to false, "error" to "Missing content")
            val entry = memoryStore.store(key, content)
            return mapOf("success" to true, "key" to entry.key, "content" to entry.content)
        }
    }

    fun memoryForgetTool(memoryStore: MemoryStore) = object : Tool {
        override val schema = ToolSchema(
            name = "memory_forget",
            description = "Delete a stored memory by its exact key.",
            parameters = mapOf(
                "key" to ParameterSchema(type = "string", description = "The exact key of the memory to delete", required = true),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val key = args["key"]?.toString() ?: return mapOf("success" to false, "error" to "Missing key")
            val removed = memoryStore.forget(key)
            return mapOf("success" to removed, "key" to key)
        }
    }

    fun memoryLearnTool(memoryStore: MemoryStore) = object : Tool {
        override val schema = ToolSchema(
            name = "memory_learn",
            description = "Store a structured learning with a category. Use LEARNING for things that worked, ERROR for error resolutions, PREFERENCE for user corrections/preferences.",
            parameters = mapOf(
                "key" to ParameterSchema(type = "string", description = "Descriptive key for the learning", required = true),
                "content" to ParameterSchema(type = "string", description = "What was learned", required = true),
                "category" to ParameterSchema(type = "string", description = "Category: LEARNING, ERROR, or PREFERENCE", required = true),
                "source" to ParameterSchema(type = "string", description = "How this was learned: user_correction, observation, or error_resolution", required = false),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val key = args["key"]?.toString() ?: return mapOf("success" to false, "error" to "Missing key")
            val content = args["content"]?.toString() ?: return mapOf("success" to false, "error" to "Missing content")
            val categoryStr = args["category"]?.toString()?.uppercase() ?: return mapOf("success" to false, "error" to "Missing category")
            val source = args["source"]?.toString()

            val category = try {
                MemoryCategory.valueOf(categoryStr)
            } catch (_: Exception) {
                return mapOf("success" to false, "error" to "Invalid category: $categoryStr. Use LEARNING, ERROR, or PREFERENCE")
            }

            if (category == MemoryCategory.GENERAL) {
                return mapOf("success" to false, "error" to "Use memory_store for GENERAL memories. memory_learn is for LEARNING, ERROR, or PREFERENCE")
            }

            val entry = memoryStore.store(key, content, category, source)
            return mapOf("success" to true, "key" to entry.key, "category" to entry.category.name, "content" to entry.content)
        }
    }

    fun memoryReinforceTool(memoryStore: MemoryStore) = object : Tool {
        override val schema = ToolSchema(
            name = "memory_reinforce",
            description = "Reinforce a stored memory by incrementing its hit count. Use this when a stored learning or preference produced a good outcome.",
            parameters = mapOf(
                "key" to ParameterSchema(type = "string", description = "The exact key of the memory to reinforce", required = true),
            ),
        )

        override suspend fun execute(args: Map<String, Any>): Any {
            val key = args["key"]?.toString() ?: return mapOf("success" to false, "error" to "Missing key")
            val entry = memoryStore.reinforceMemory(key)
                ?: return mapOf("success" to false, "error" to "Memory not found: $key")
            return mapOf("success" to true, "key" to entry.key, "hit_count" to entry.hitCount)
        }
    }

    fun getMemoryTools(memoryStore: MemoryStore): List<Tool> = listOf(
        memoryStoreTool(memoryStore),
        memoryForgetTool(memoryStore),
        memoryLearnTool(memoryStore),
        memoryReinforceTool(memoryStore),
    )
}
