package org.opennur.maktaba.data

import java.io.Reader

data class ParsedBlock(
    val blockId: String,
    val kind: String,
    val depth: Int,
    val title: String,
    val text: String,
    val pageLabel: String?,
    val position: Int,
)

object BlockKinds {
    const val PARAGRAPH = "paragraph"
    const val HEADING = "heading"
    const val PAGE = "page"
}

object OpenItiMarkdownParser {
    private val headingPattern = Regex("""^###\s+(\|+)(?:\s*(.*))?$""")
    private val informationPattern = Regex("""^###\s+([@$].*)$""")
    private val pagePattern = Regex("""^PageV\d{2}P\d{3,4}$""")

    suspend fun parse(reader: Reader, emit: suspend (ParsedBlock) -> Unit) {
        val buffered = reader.buffered()
        var bodyStarted = false
        var paragraph = StringBuilder()
        var position = 0

        fun appendParagraphPart(value: String) {
            val clean = value.trim()
            if (clean.isEmpty()) return
            if (paragraph.isNotEmpty()) paragraph.append(' ')
            paragraph.append(clean)
        }

        suspend fun flushParagraph() {
            val text = paragraph.toString().replace(Regex("\\s+"), " ").trim()
            if (text.isNotEmpty()) {
                emit(
                    ParsedBlock(
                        blockId = "block-${position.toString().padStart(6, '0')}",
                        kind = BlockKinds.PARAGRAPH,
                        depth = 0,
                        title = "",
                        text = text,
                        pageLabel = null,
                        position = position++,
                    ),
                )
            }
            paragraph = StringBuilder()
        }

        suspend fun emitHeading(depth: Int, title: String) {
            flushParagraph()
            emit(
                ParsedBlock(
                    blockId = "block-${position.toString().padStart(6, '0')}",
                    kind = BlockKinds.HEADING,
                    depth = depth,
                    title = title.trim(),
                    text = "",
                    pageLabel = null,
                    position = position++,
                ),
            )
        }

        suspend fun emitPage(label: String) {
            flushParagraph()
            emit(
                ParsedBlock(
                    blockId = "block-${position.toString().padStart(6, '0')}",
                    kind = BlockKinds.PAGE,
                    depth = 0,
                    title = "",
                    text = "",
                    pageLabel = label,
                    position = position++,
                ),
            )
        }

        while (true) {
            val rawLine = buffered.readLine() ?: break
            val line = rawLine.removePrefix("\uFEFF").trimEnd()

            if (!bodyStarted) {
                if (line.trim() == "#META#Header#End#") bodyStarted = true
                continue
            }

            when {
                line.startsWith("#META#") -> Unit
                line.startsWith("Milestone") -> Unit
                line.matches(pagePattern) -> emitPage(line)
                line.matches(headingPattern) -> {
                    headingPattern.matchEntire(line)?.let { match ->
                        emitHeading(match.groupValues[1].length, match.groupValues[2])
                    }
                }
                line.matches(informationPattern) -> {
                    informationPattern.matchEntire(line)?.let { match ->
                        emitHeading(1, match.groupValues[1])
                    }
                }
                line.startsWith("#~:") -> Unit
                line.startsWith("~~") -> appendParagraphPart(line.removePrefix("~~"))
                line.startsWith("#") -> {
                    flushParagraph()
                    appendParagraphPart(line.removePrefix("#"))
                }
                line.isBlank() -> flushParagraph()
                else -> appendParagraphPart(line)
            }
        }
        flushParagraph()
    }

    suspend fun parseToList(text: String): List<ParsedBlock> {
        val blocks = mutableListOf<ParsedBlock>()
        parse(text.reader()) { blocks += it }
        return blocks
    }
}
