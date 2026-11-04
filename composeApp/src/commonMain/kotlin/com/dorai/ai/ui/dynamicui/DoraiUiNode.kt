package com.dorai.ai.ui.dynamicui

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
sealed interface DoraiUiNode {
    val id: String?
}

// --- Layout nodes ---

@Immutable
@Serializable
@SerialName("column")
data class ColumnNode(
    override val id: String? = null,
    @Contextual val children: ImmutableList<DoraiUiNode> = persistentListOf(),
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("row")
data class RowNode(
    override val id: String? = null,
    @Contextual val children: ImmutableList<DoraiUiNode> = persistentListOf(),
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("card")
data class CardNode(
    override val id: String? = null,
    @Contextual val children: ImmutableList<DoraiUiNode> = persistentListOf(),
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("divider")
data class DividerNode(
    override val id: String? = null,
) : DoraiUiNode

// --- Content nodes ---

@Immutable
@Serializable
@SerialName("text")
data class TextNode(
    override val id: String? = null,
    val value: String = "",
    val style: TextNodeStyle? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val color: String? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("image")
data class ImageNode(
    override val id: String? = null,
    val url: String = "",
    val alt: String? = null,
    val height: Int? = null,
    val aspectRatio: Float? = null,
) : DoraiUiNode

// --- Interactive nodes ---

@Immutable
@Serializable
@SerialName("button")
data class ButtonNode(
    override val id: String? = null,
    val label: String = "",
    val action: UiAction? = null,
    val variant: ButtonVariant? = null,
    val enabled: Boolean? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("text_input")
data class TextInputNode(
    override val id: String = "",
    val label: String? = null,
    val placeholder: String? = null,
    val value: String? = null,
    val multiline: Boolean? = null,
    val type: String? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("checkbox")
data class CheckboxNode(
    override val id: String = "",
    val label: String = "",
    val checked: Boolean? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("select")
data class SelectNode(
    override val id: String = "",
    val label: String? = null,
    @Contextual val options: ImmutableList<String> = persistentListOf(),
    val selected: String? = null,
) : DoraiUiNode

// --- Interactive nodes (additional) ---

@Immutable
@Serializable
@SerialName("switch")
data class SwitchNode(
    override val id: String = "",
    val label: String = "",
    val checked: Boolean? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("slider")
data class SliderNode(
    override val id: String = "",
    val label: String? = null,
    val value: Float? = null,
    val min: Float? = null,
    val max: Float? = null,
    val step: Float? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("radio_group")
data class RadioGroupNode(
    override val id: String = "",
    val label: String? = null,
    @Contextual val options: ImmutableList<String> = persistentListOf(),
    val selected: String? = null,
) : DoraiUiNode

// --- Feedback nodes ---

@Immutable
@Serializable
@SerialName("progress")
data class ProgressNode(
    override val id: String? = null,
    val value: Float? = null,
    val label: String? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("alert")
data class AlertNode(
    override val id: String? = null,
    val message: String = "",
    val title: String? = null,
    val severity: AlertSeverity? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("countdown")
data class CountdownNode(
    override val id: String? = null,
    val seconds: Int = 0,
    val label: String? = null,
    val action: UiAction? = null,
) : DoraiUiNode

// --- Selection nodes ---

@Immutable
@Serializable
@SerialName("chip_group")
data class ChipGroupNode(
    override val id: String = "",
    @Contextual val chips: ImmutableList<ChipItem> = persistentListOf(),
    /** "single" (default), "multi", or "none" for display-only tags. */
    val selection: String = "single",
) : DoraiUiNode

@Immutable
@Serializable
data class ChipItem(
    val label: String = "",
    val value: String = "",
)

// --- Content nodes (additional) ---

@Immutable
@Serializable
@SerialName("icon")
data class IconNode(
    override val id: String? = null,
    val name: String = "",
    val size: Int? = null,
    val color: String? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("code")
data class CodeNode(
    override val id: String? = null,
    val code: String = "",
    val language: String? = null,
) : DoraiUiNode

// --- Layout nodes (additional) ---

@Immutable
@Serializable
@SerialName("box")
data class BoxNode(
    override val id: String? = null,
    @Contextual val children: ImmutableList<DoraiUiNode> = persistentListOf(),
    val contentAlignment: String? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("tabs")
data class TabsNode(
    override val id: String? = null,
    @Contextual val tabs: ImmutableList<TabItem> = persistentListOf(),
    val selectedIndex: Int? = null,
) : DoraiUiNode

@Immutable
@Serializable
data class TabItem(
    val label: String = "",
    @Contextual val children: ImmutableList<DoraiUiNode> = persistentListOf(),
)

@Immutable
@Serializable
@SerialName("accordion")
data class AccordionNode(
    override val id: String? = null,
    val title: String = "",
    @Contextual val children: ImmutableList<DoraiUiNode> = persistentListOf(),
    val expanded: Boolean? = null,
) : DoraiUiNode

// --- Display nodes ---

@Immutable
@Serializable
@SerialName("quote")
data class QuoteNode(
    override val id: String? = null,
    val text: String = "",
    val source: String? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("badge")
data class BadgeNode(
    override val id: String? = null,
    val value: String = "",
    val color: String? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("stat")
data class StatNode(
    override val id: String? = null,
    val value: String = "",
    val label: String = "",
    val description: String? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("avatar")
data class AvatarNode(
    override val id: String? = null,
    val name: String? = null,
    val imageUrl: String? = null,
    val size: Int? = null,
) : DoraiUiNode

// --- Data display nodes ---

@Immutable
@Serializable
@SerialName("list")
data class ListNode(
    override val id: String? = null,
    @Contextual val items: ImmutableList<DoraiUiNode> = persistentListOf(),
    val ordered: Boolean? = null,
) : DoraiUiNode

@Immutable
@Serializable
@SerialName("table")
data class TableNode(
    override val id: String? = null,
    @Contextual val headers: ImmutableList<String> = persistentListOf(),
    @Contextual val rows: ImmutableList<@Contextual ImmutableList<String>> = persistentListOf(),
) : DoraiUiNode

// --- Enums ---

@Serializable
enum class TextNodeStyle {
    @SerialName("headline")
    HEADLINE,

    @SerialName("title")
    TITLE,

    @SerialName("body")
    BODY,

    @SerialName("caption")
    CAPTION,
}

@Serializable
enum class ButtonVariant {
    @SerialName("filled")
    FILLED,

    @SerialName("outlined")
    OUTLINED,

    @SerialName("text")
    TEXT,

    @SerialName("tonal")
    TONAL,
}

@Serializable
enum class AlertSeverity {
    @SerialName("info")
    INFO,

    @SerialName("success")
    SUCCESS,

    @SerialName("warning")
    WARNING,

    @SerialName("error")
    ERROR,
}
