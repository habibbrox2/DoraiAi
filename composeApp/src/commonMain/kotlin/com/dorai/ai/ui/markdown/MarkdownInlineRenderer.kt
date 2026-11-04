package com.dorai.ai.ui.markdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.dorai.ai.ui.AccentPrimary

@Composable
internal fun List<InlineNode>.toAnnotatedString(
    inlineCodeBackground: Color = Color(0xFF222B38),
    inlineCodeColor: Color = AccentPrimary,
    linkColor: Color = AccentPrimary,
): AnnotatedString = buildAnnotatedString { appendInlines(this@toAnnotatedString, inlineCodeBackground, inlineCodeColor, linkColor) }

private fun AnnotatedString.Builder.appendInlines(
    nodes: List<InlineNode>,
    inlineCodeBackground: Color,
    inlineCodeColor: Color,
    linkColor: Color,
) {
    for (n in nodes) appendInline(n, inlineCodeBackground, inlineCodeColor, linkColor)
}

private fun AnnotatedString.Builder.appendInline(
    node: InlineNode,
    inlineCodeBackground: Color,
    inlineCodeColor: Color,
    linkColor: Color,
) {
    when (node) {
        is Text -> append(node.value)

        is Strong -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            appendInlines(node.children, inlineCodeBackground, inlineCodeColor, linkColor)
        }

        is Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            appendInlines(node.children, inlineCodeBackground, inlineCodeColor, linkColor)
        }

        is Strike -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
            appendInlines(node.children, inlineCodeBackground, inlineCodeColor, linkColor)
        }

        is InlineCode -> withStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = inlineCodeBackground,
                color = inlineCodeColor,
            ),
        ) {
            append(node.code)
        }

        is Link -> withLink(
            LinkAnnotation.Url(
                url = node.href,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
            ),
        ) {
            appendInlines(node.children, inlineCodeBackground, inlineCodeColor, linkColor)
        }

        is Image -> append(node.alt)

        LineBreak -> append('\n')

        is InlineMath -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
            // Fallback path: if math reaches the AnnotatedString builder it means the caller
            // didn't use [InlineContent]. Emit the raw LaTeX so nothing is lost.
            append(node.latex)
        }
    }
}
