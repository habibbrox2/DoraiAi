package com.inspiredandroid.kai.ui.workspace

/**
 * Minimal, dependency-free syntax highlighter for the Workspace code editor.
 *
 * It is intentionally forgiving: an unknown character simply renders as plain
 * text, so a half-typed line never throws or drops content. Block comments are
 * the only state carried between lines.
 */
private val KotlinKeywords = setOf(
    "as", "break", "by", "catch", "class", "companion", "const", "constructor",
    "continue", "data", "do", "else", "enum", "expect", "external", "false",
    "final", "finally", "for", "fun", "get", "if", "import", "in", "init",
    "inline", "inner", "interface", "internal", "is", "lateinit", "null",
    "object", "open", "operator", "out", "override", "package", "private",
    "protected", "public", "return", "sealed", "set", "super", "suspend",
    "this", "throw", "true", "try", "typealias", "val", "var", "when",
    "while",
)

/** Splits [source] into highlighted, numbered lines ready for the editor. */
fun highlightSource(source: String): List<CodeLine> {
    if (source.isEmpty()) return emptyList()
    val lines = ArrayList<CodeLine>()
    var inBlockComment = false
    source.lineSequence().forEachIndexed { index, text ->
        val (segments, stillInBlock) = tokenizeLine(text, inBlockComment)
        inBlockComment = stillInBlock
        lines += CodeLine(number = index + 1, segments = segments)
    }
    return lines
}

private fun tokenizeLine(
    line: String,
    startInBlockComment: Boolean,
): Pair<List<CodeSegment>, Boolean> {
    val segments = ArrayList<CodeSegment>()
    val buffer = StringBuilder()
    var index = 0
    var inBlock = startInBlockComment

    fun flush(style: CodeStyle = CodeStyle.Plain) {
        if (buffer.isNotEmpty()) {
            segments += CodeSegment(buffer.toString(), style)
            buffer.clear()
        }
    }

    while (index < line.length) {
        if (inBlock) {
            val end = line.indexOf("*/", index)
            val stop = if (end < 0) line.length else end + 2
            buffer.append(line, index, stop)
            flush(CodeStyle.Comment)
            index = stop
            inBlock = end < 0
            continue
        }

        val char = line[index]
        when {
            line.startsWith("//", index) -> {
                flush()
                buffer.append(line, index, line.length)
                flush(CodeStyle.Comment)
                index = line.length
            }

            line.startsWith("/*", index) -> {
                flush()
                inBlock = true
            }

            char == '"' || char == '\'' -> {
                flush()
                val quote = char
                buffer.append(quote)
                index++
                while (index < line.length) {
                    val next = line[index]
                    buffer.append(next)
                    index++
                    if (next == '\\' && index < line.length) {
                        buffer.append(line[index])
                        index++
                    } else if (next == quote) {
                        break
                    }
                }
                flush(CodeStyle.String)
            }

            char == '@' -> {
                flush()
                buffer.append(char)
                index++
                while (index < line.length && (line[index].isLetterOrDigit() || line[index] == '_')) {
                    buffer.append(line[index])
                    index++
                }
                flush(CodeStyle.Annotation)
            }

            char.isDigit() -> {
                flush()
                while (index < line.length && (line[index].isLetterOrDigit() || line[index] == '.' || line[index] == '_')) {
                    buffer.append(line[index])
                    index++
                }
                flush(CodeStyle.Number)
            }

            char.isLetter() || char == '_' -> {
                flush()
                val start = index
                while (index < line.length && (line[index].isLetterOrDigit() || line[index] == '_')) {
                    index++
                }
                val word = line.substring(start, index)
                val style = when {
                    word in KotlinKeywords -> CodeStyle.Keyword
                    word.first().isUpperCase() -> CodeStyle.Type
                    index < line.length && line[index] == '(' -> CodeStyle.Function
                    else -> CodeStyle.Plain
                }
                buffer.append(word)
                flush(style)
            }

            else -> {
                buffer.append(char)
                index++
            }
        }
    }

    flush()
    return segments to inBlock
}
