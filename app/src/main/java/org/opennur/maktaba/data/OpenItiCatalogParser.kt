package org.maktaba.app.data

import java.io.Reader

data class CatalogRecord(
    val versionUri: String,
    val language: String,
    val subcorpus: String,
    val uncorrectedOcr: Boolean,
    val date: String,
    val authorArabic: String,
    val authorLatin: String,
    val bookUri: String,
    val titleArabic: String,
    val titleLatin: String,
    val editionInfo: String,
    val sourceId: String,
    val status: String,
    val tokenLength: Int,
    val characterLength: Int,
    val localPath: String?,
    val tags: String,
    val authorFromUri: String,
    val parts: String,
)

object OpenItiCatalogParser {
    fun parse(reader: Reader): Sequence<CatalogRecord> = sequence {
        val buffered = reader.buffered()
        val headerLine = buffered.readLine() ?: return@sequence
        val headers = headerLine.split('\t')
        val positions = headers.withIndex().associate { it.value to it.index }

        fun List<String>.field(name: String): String =
            positions[name]?.let { getOrNull(it) }.orEmpty().trim()

        while (true) {
            val line = buffered.readLine() ?: break
            if (line.isBlank()) continue
            val values = line.split('\t')
            val versionUri = values.field("version_uri")
            if (versionUri.isBlank()) continue

            val bookUri = values.field("book")
            yield(
                CatalogRecord(
                    versionUri = versionUri,
                    language = values.field("language"),
                    subcorpus = values.field("subcorpus"),
                    uncorrectedOcr = values.field("uncorrected_OCR").equals("True", ignoreCase = true),
                    date = values.field("date"),
                    authorArabic = values.field("author_ar"),
                    authorLatin = values.field("author_lat"),
                    bookUri = bookUri.ifBlank { versionUri.substringBeforeLast('.') },
                    titleArabic = values.field("title_ar"),
                    titleLatin = values.field("title_lat"),
                    editionInfo = values.field("ed_info"),
                    sourceId = values.field("id"),
                    status = values.field("status"),
                    tokenLength = values.field("tok_length").toIntOrNull() ?: 0,
                    characterLength = values.field("char_length").toIntOrNull() ?: 0,
                    localPath = values.field("local_path").takeUnless { it.isBlank() || it == "NA" },
                    tags = values.field("tags"),
                    authorFromUri = values.field("author_from_uri"),
                    parts = values.field("parts"),
                ),
            )
        }
    }
}

fun CatalogRecord.toEntity(): BookVersionEntity = BookVersionEntity(
    versionUri = versionUri,
    bookUri = bookUri,
    language = language,
    subcorpus = subcorpus,
    uncorrectedOcr = uncorrectedOcr,
    date = date,
    authorArabic = authorArabic,
    authorLatin = authorLatin,
    titleArabic = titleArabic,
    titleLatin = titleLatin,
    editionInfo = editionInfo,
    sourceId = sourceId,
    status = status,
    tokenLength = tokenLength,
    characterLength = characterLength,
    localPath = localPath,
    tags = tags,
    authorFromUri = authorFromUri,
    parts = parts,
)
