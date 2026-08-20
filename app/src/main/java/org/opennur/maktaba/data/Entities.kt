package org.maktaba.app.data

data class BookVersionEntity(
    val versionUri: String,
    val bookUri: String,
    val language: String,
    val subcorpus: String,
    val uncorrectedOcr: Boolean,
    val date: String,
    val authorArabic: String,
    val authorLatin: String,
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
    val downloaded: Boolean = false,
    val downloadPath: String? = null,
    val downloadedAt: Long? = null,
)

data class CatalogBookRow(
    val bookUri: String,
    val titleArabic: String,
    val titleLatin: String,
    val authorArabic: String,
    val authorLatin: String,
    val versionCount: Int,
    val tokenLength: Int,
)

data class ReaderBlockEntity(
    val versionUri: String,
    val blockId: String,
    val kind: String,
    val depth: Int,
    val title: String,
    val text: String,
    val pageLabel: String?,
    val position: Int,
)

data class ReaderSearchEntity(
    val versionUri: String,
    val blockId: String,
    val text: String,
    val normalizedText: String,
)

data class BookmarkEntity(
    val id: Long = 0,
    val versionUri: String,
    val blockId: String,
    val excerpt: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class ReadingProgressEntity(
    val versionUri: String,
    val blockId: String,
    val position: Int,
    val percent: Float,
    val updatedAt: Long = System.currentTimeMillis(),
)
