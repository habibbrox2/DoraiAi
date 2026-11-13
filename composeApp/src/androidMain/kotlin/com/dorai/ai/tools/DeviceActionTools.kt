package com.dorai.ai.tools

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import com.dorai.ai.network.tools.ParameterSchema
import com.dorai.ai.network.tools.Tool
import com.dorai.ai.network.tools.ToolInfo
import com.dorai.ai.network.tools.ToolSchema
import dorai.composeapp.generated.resources.Res
import dorai.composeapp.generated.resources.tool_create_contact_description
import dorai.composeapp.generated.resources.tool_create_contact_name
import dorai.composeapp.generated.resources.tool_flashlight_off_description
import dorai.composeapp.generated.resources.tool_flashlight_off_name
import dorai.composeapp.generated.resources.tool_flashlight_on_description
import dorai.composeapp.generated.resources.tool_flashlight_on_name
import dorai.composeapp.generated.resources.tool_open_wifi_settings_description
import dorai.composeapp.generated.resources.tool_open_wifi_settings_name
import dorai.composeapp.generated.resources.tool_send_email_description
import dorai.composeapp.generated.resources.tool_send_email_name
import dorai.composeapp.generated.resources.tool_show_map_description
import dorai.composeapp.generated.resources.tool_show_map_name

/**
 * Device-action tools ported from the Google AI Edge Gallery "Mobile Actions" pattern
 * (https://github.com/google-ai-edge/gallery/tree/1c7b534f81a0e5e65515f0e5211398cdcc528738/Android/src/app/src/main/java/com/google/ai/edge/gallery/customtasks/mobileactions).
 *
 * Destructive or content-creating actions launch the system UI rather than writing the
 * database directly (contact editor, email composer, maps), so the user stays in charge.
 * These tools run without a confirmation card by product policy (approval is MCP-only);
 * the system intents they launch are themselves user-confirmable surfaces anyway.
 */
object DeviceActionTools {

    private fun setTorch(context: Context, enabled: Boolean): Map<String, Any> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return mapOf("success" to false, "error" to "Camera service unavailable")
        val cameraId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        } ?: return mapOf("success" to false, "error" to "No flash-capable camera on this device")
        return try {
            cameraManager.setTorchMode(cameraId, enabled)
            mapOf("success" to true, "torch" to (if (enabled) "on" else "off"))
        } catch (e: Exception) {
            mapOf("success" to false, "error" to (e.message ?: "Failed to toggle flashlight"))
        }
    }

    val flashlightOnToolInfo = ToolInfo(
        id = "flashlight_on",
        name = "Flashlight On",
        description = "Turn on the device flashlight",
        nameRes = Res.string.tool_flashlight_on_name,
        descriptionRes = Res.string.tool_flashlight_on_description,
    )

    val flashlightOffToolInfo = ToolInfo(
        id = "flashlight_off",
        name = "Flashlight Off",
        description = "Turn off the device flashlight",
        nameRes = Res.string.tool_flashlight_off_name,
        descriptionRes = Res.string.tool_flashlight_off_description,
    )

    val createContactToolInfo = ToolInfo(
        id = "create_contact",
        name = "Create Contact",
        description = "Create a new contact with the given details",
        nameRes = Res.string.tool_create_contact_name,
        descriptionRes = Res.string.tool_create_contact_description,
    )

    val sendEmailToolInfo = ToolInfo(
        id = "send_email",
        name = "Send Email",
        description = "Compose and send an email",
        nameRes = Res.string.tool_send_email_name,
        descriptionRes = Res.string.tool_send_email_description,
    )

    val showLocationOnMapToolInfo = ToolInfo(
        id = "show_location_on_map",
        name = "Show on Map",
        description = "Show a location on the map",
        nameRes = Res.string.tool_show_map_name,
        descriptionRes = Res.string.tool_show_map_description,
    )

    val openWifiSettingsToolInfo = ToolInfo(
        id = "open_wifi_settings",
        name = "Wi-Fi Settings",
        description = "Open the Wi-Fi settings screen",
        nameRes = Res.string.tool_open_wifi_settings_name,
        descriptionRes = Res.string.tool_open_wifi_settings_description,
    )

    val toolInfos = listOf(
        flashlightOnToolInfo,
        flashlightOffToolInfo,
        createContactToolInfo,
        sendEmailToolInfo,
        showLocationOnMapToolInfo,
        openWifiSettingsToolInfo,
    )

    fun create(context: Context): List<Tool> = listOf(
        object : Tool {
            override val schema = ToolSchema(
                name = flashlightOnToolInfo.id,
                description = "Turn on the device flashlight",
                parameters = emptyMap(),
            )

            override suspend fun execute(args: Map<String, Any>): Any = setTorch(context, enabled = true)
        },
        object : Tool {
            override val schema = ToolSchema(
                name = flashlightOffToolInfo.id,
                description = "Turn off the device flashlight",
                parameters = emptyMap(),
            )

            override suspend fun execute(args: Map<String, Any>): Any = setTorch(context, enabled = false)
        },
        object : Tool {
            override val schema = ToolSchema(
                name = createContactToolInfo.id,
                description = "Open the contact editor to create a new contact with the given details. The user reviews and saves in the system contacts app.",
                parameters = mapOf(
                    "first_name" to ParameterSchema("string", "First name of the contact", false),
                    "last_name" to ParameterSchema("string", "Last name of the contact", false),
                    "phone" to ParameterSchema("string", "Phone number of the contact", false),
                    "email" to ParameterSchema("string", "Email address of the contact", false),
                ),
            )

            override suspend fun execute(args: Map<String, Any>): Any {
                val firstName = args["first_name"] as? String
                val lastName = args["last_name"] as? String
                val fullName = listOfNotNull(firstName?.trim()?.takeIf { it.isNotEmpty() }, lastName?.trim()?.takeIf { it.isNotEmpty() })
                    .joinToString(" ")
                    .ifBlank { (args["name"] as? String).orEmpty() }
                val phone = args["phone"] as? String ?: args["phone_number"] as? String
                val email = args["email"] as? String
                if (fullName.isBlank() && phone.isNullOrBlank() && email.isNullOrBlank()) {
                    return mapOf("success" to false, "error" to "Provide at least a name, phone, or email")
                }
                val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                    putExtra(ContactsContract.Intents.Insert.NAME, fullName.ifBlank { null })
                    putExtra(ContactsContract.Intents.Insert.PHONE, phone)
                    putExtra(ContactsContract.Intents.Insert.EMAIL, email)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return startActivity(context, intent, "contact editor")
            }
        },
        object : Tool {
            override val schema = ToolSchema(
                name = sendEmailToolInfo.id,
                description = "Compose an email to the given address and open it in the user's email app. Fields: to, subject, body.",
                parameters = mapOf(
                    "to" to ParameterSchema("string", "Recipient email address", true),
                    "subject" to ParameterSchema("string", "Subject of the email", false),
                    "body" to ParameterSchema("string", "Body of the email", false),
                ),
            )

            override suspend fun execute(args: Map<String, Any>): Any {
                val to = (args["to"] as? String)?.trim().orEmpty()
                if (to.isBlank()) {
                    return mapOf("success" to false, "error" to "Recipient address is required")
                }
                val subject = args["subject"] as? String
                val body = args["body"] as? String
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$to")).apply {
                    if (!subject.isNullOrBlank()) putExtra(Intent.EXTRA_SUBJECT, subject)
                    if (!body.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return startActivity(context, intent, "email composer")
            }
        },
        object : Tool {
            override val schema = ToolSchema(
                name = showLocationOnMapToolInfo.id,
                description = "Show a location on the map (name of a place, business, or address).",
                parameters = mapOf(
                    "location" to ParameterSchema("string", "The location to search for", true),
                ),
            )

            override suspend fun execute(args: Map<String, Any>): Any {
                val location = (args["location"] as? String)?.trim().orEmpty()
                if (location.isBlank()) {
                    return mapOf("success" to false, "error" to "Location is required")
                }
                val uri = Uri.parse("geo:0,0?q=${Uri.encode(location)}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return if (intent.resolveActivity(context.packageManager) != null) {
                    startActivity(context, intent, "map")
                } else {
                    // No map app installed — fall back to a plain web search so the request is never lost.
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(location)}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(context, webIntent, "map")
                }
            }
        },
        object : Tool {
            override val schema = ToolSchema(
                name = openWifiSettingsToolInfo.id,
                description = "Open the Wi-Fi settings screen",
                parameters = emptyMap(),
            )

            override suspend fun execute(args: Map<String, Any>): Any {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                return startActivity(context, intent, "Wi-Fi settings")
            }
        },
    )

    private fun startActivity(context: Context, intent: Intent, what: String): Any = try {
        context.startActivity(intent)
        mapOf("success" to true, "opened" to what)
    } catch (e: Exception) {
        mapOf("success" to false, "error" to (e.message ?: "Could not open $what"))
    }
}
