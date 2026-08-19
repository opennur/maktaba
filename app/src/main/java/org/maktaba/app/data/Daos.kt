package org.maktaba.app.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private fun ContentValues.putNullable(key: String, value: String?) {
    if (value == null) putNull(key) else put(key, value)
}

private fun Cursor.string(column: String): String = getString(getColumnIndexOrThrow(column))
private fun Cursor.stringOrNull(column: String): String? = getString(getColumnIndexOrThrow(column))
private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))
private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))
private fun Cursor.float(column: String): Float = getFloat(getColumnIndexOrThrow(column))

class BookDao(private val database: MaktabaDatabase) {
    suspend fun insertAll(books: List<BookVersionEntity>) = withContext(Dispatchers.IO) {
        database.write { sqlite ->
            sqlite.beginTransaction()
            try {
                books.forEach { book ->
                    val metadata = ContentValues().apply {
                        put("version_uri", book.versionUri)
                        put("book_uri", book.bookUri)
                        put("language", book.language)
                        put("subcorpus", book.subcorpus)
                        put("uncorrected_ocr", if (book.uncorrectedOcr) 1 else 0)
                        put("date", book.date)
                        put("author_ar", book.authorArabic)
                        put("author_lat", book.authorLatin)
                        put("title_ar", book.titleArabic)
                        put("title_lat", book.titleLatin)
                        put("edition_info", book.editionInfo)
                        put("source_id", book.sourceId)
                        put("status", book.status)
                        put("token_length", book.tokenLength)
                        put("character_length", book.characterLength)
                        putNullable("local_path", book.localPath)
                        put("tags", book.tags)
                        put("author_from_uri", book.authorFromUri)
                        put("parts", book.parts)
                    }
                    sqlite.insertWithOnConflict(
                        "book_versions",
                        null,
                        metadata,
                        SQLiteDatabase.CONFLICT_IGNORE,
                    )
                    sqlite.update("book_versions", metadata, "version_uri = ?", arrayOf(book.versionUri))
                }
                sqlite.setTransactionSuccessful()
            } finally {
                sqlite.endTransaction()
            }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        database.write { sqlite -> sqlite.delete("book_versions", null, null) }
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) {
        database.read { sqlite ->
            sqlite.rawQuery("SELECT COUNT(*) FROM book_versions", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        }
    }

    suspend fun deleteNotIn(versionUris: Set<String>) = withContext(Dispatchers.IO) {
        if (versionUris.isEmpty()) return@withContext
        database.write { sqlite ->
            val staleUris = mutableListOf<String>()
            sqlite.rawQuery("SELECT version_uri FROM book_versions", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val versionUri = cursor.getString(0)
                    if (versionUri !in versionUris) staleUris += versionUri
                }
            }
            if (staleUris.isNotEmpty()) {
                sqlite.beginTransaction()
                try {
                    staleUris.forEach { versionUri ->
                        sqlite.delete("book_versions", "version_uri = ?", arrayOf(versionUri))
                    }
                    sqlite.setTransactionSuccessful()
                } finally {
                    sqlite.endTransaction()
                }
            }
        }
    }

    suspend fun getVersion(versionUri: String): BookVersionEntity? = withContext(Dispatchers.IO) {
        database.read { sqlite ->
            sqlite.query(
                "book_versions",
                null,
                "version_uri = ?",
                arrayOf(versionUri),
                null,
                null,
                null,
                "1",
            ).use { cursor -> if (cursor.moveToFirst()) cursor.toBookVersion() else null }
        }
    }

    fun observeVersions(bookUri: String): Flow<List<BookVersionEntity>> = database.changes
        .map {
            database.read { sqlite ->
                sqlite.query(
                    "book_versions",
                    null,
                    "book_uri = ?",
                    arrayOf(bookUri),
                    null,
                    null,
                    "status ASC, title_ar ASC",
                ).use { cursor -> cursor.toList { toBookVersion() } }
            }
        }
        .flowOn(Dispatchers.IO)

    fun observeCatalog(query: String): Flow<List<CatalogBookRow>> = database.changes
        .map {
            database.read { sqlite ->
                val like = "%$query%"
                val sql = """
                    SELECT book_uri AS bookUri,
                           MIN(title_ar) AS titleArabic,
                           MIN(title_lat) AS titleLatin,
                           MIN(author_ar) AS authorArabic,
                           MIN(author_lat) AS authorLatin,
                           COUNT(*) AS versionCount,
                           MAX(token_length) AS tokenLength
                    FROM book_versions
                    WHERE ? = '' OR title_ar LIKE ? OR title_lat LIKE ?
                       OR author_ar LIKE ? OR author_lat LIKE ? OR book_uri LIKE ?
                    GROUP BY book_uri
                    ORDER BY title_ar COLLATE NOCASE ASC
                """.trimIndent()
                sqlite.rawQuery(sql, arrayOf(query, like, like, like, like, like)).use { cursor ->
                    cursor.toList { toCatalogBook() }
                }
            }
        }
        .flowOn(Dispatchers.IO)

    fun observeDownloadedBooks(): Flow<List<CatalogBookRow>> = database.changes
        .map {
            database.read { sqlite ->
                val sql = """
                    SELECT book_uri AS bookUri,
                           MIN(title_ar) AS titleArabic,
                           MIN(title_lat) AS titleLatin,
                           MIN(author_ar) AS authorArabic,
                           MIN(author_lat) AS authorLatin,
                           COUNT(*) AS versionCount,
                           MAX(token_length) AS tokenLength
                    FROM book_versions
                    WHERE downloaded = 1
                    GROUP BY book_uri
                    ORDER BY title_ar COLLATE NOCASE ASC
                """.trimIndent()
                sqlite.rawQuery(sql, null).use { cursor -> cursor.toList { toCatalogBook() } }
            }
        }
        .flowOn(Dispatchers.IO)

    suspend fun setDownloaded(versionUri: String, downloaded: Boolean, path: String?, downloadedAt: Long?) =
        withContext(Dispatchers.IO) {
            database.write { sqlite ->
                sqlite.update(
                    "book_versions",
                    ContentValues().apply {
                        put("downloaded", if (downloaded) 1 else 0)
                        putNullable("download_path", path)
                        if (downloadedAt == null) putNull("downloaded_at") else put("downloaded_at", downloadedAt)
                    },
                    "version_uri = ?",
                    arrayOf(versionUri),
                )
            }
        }

    private fun Cursor.toBookVersion() = BookVersionEntity(
        versionUri = string("version_uri"),
        bookUri = string("book_uri"),
        language = string("language"),
        subcorpus = string("subcorpus"),
        uncorrectedOcr = int("uncorrected_ocr") != 0,
        date = string("date"),
        authorArabic = string("author_ar"),
        authorLatin = string("author_lat"),
        titleArabic = string("title_ar"),
        titleLatin = string("title_lat"),
        editionInfo = string("edition_info"),
        sourceId = string("source_id"),
        status = string("status"),
        tokenLength = int("token_length"),
        characterLength = int("character_length"),
        localPath = stringOrNull("local_path"),
        tags = string("tags"),
        authorFromUri = string("author_from_uri"),
        parts = string("parts"),
        downloaded = int("downloaded") != 0,
        downloadPath = stringOrNull("download_path"),
        downloadedAt = if (isNull(getColumnIndexOrThrow("downloaded_at"))) null else long("downloaded_at"),
    )

    private fun Cursor.toCatalogBook() = CatalogBookRow(
        bookUri = string("bookUri"),
        titleArabic = string("titleArabic"),
        titleLatin = string("titleLatin"),
        authorArabic = string("authorArabic"),
        authorLatin = string("authorLatin"),
        versionCount = int("versionCount"),
        tokenLength = int("tokenLength"),
    )
}

class ReaderDao(private val database: MaktabaDatabase) {
    suspend fun insertBlocks(blocks: List<ReaderBlockEntity>) = withContext(Dispatchers.IO) {
        database.write { sqlite ->
            sqlite.beginTransaction()
            try {
                blocks.forEach { block ->
                    sqlite.insertWithOnConflict(
                        "reader_blocks",
                        null,
                        ContentValues().apply {
                            put("version_uri", block.versionUri)
                            put("block_id", block.blockId)
                            put("kind", block.kind)
                            put("depth", block.depth)
                            put("title", block.title)
                            put("text", block.text)
                            putNullable("page_label", block.pageLabel)
                            put("position", block.position)
                        },
                        SQLiteDatabase.CONFLICT_REPLACE,
                    )
                }
                sqlite.setTransactionSuccessful()
            } finally {
                sqlite.endTransaction()
            }
        }
    }

    suspend fun insertSearchRows(rows: List<ReaderSearchEntity>) = withContext(Dispatchers.IO) {
        database.write { sqlite ->
            sqlite.beginTransaction()
            try {
                rows.forEach { row ->
                    sqlite.insert("reader_search", null, ContentValues().apply {
                        put("version_uri", row.versionUri)
                        put("block_id", row.blockId)
                        put("text", row.text)
                        put("normalized_text", row.normalizedText)
                    })
                }
                sqlite.setTransactionSuccessful()
            } finally {
                sqlite.endTransaction()
            }
        }
    }

    suspend fun deleteBlocks(versionUri: String) = withContext(Dispatchers.IO) {
        database.write { sqlite -> sqlite.delete("reader_blocks", "version_uri = ?", arrayOf(versionUri)) }
    }

    suspend fun deleteSearchRows(versionUri: String) = withContext(Dispatchers.IO) {
        database.write { sqlite -> sqlite.delete("reader_search", "version_uri = ?", arrayOf(versionUri)) }
    }

    fun observeBlocks(versionUri: String): Flow<List<ReaderBlockEntity>> = database.changes
        .map {
            database.read { sqlite ->
                sqlite.query(
                    "reader_blocks",
                    null,
                    "version_uri = ?",
                    arrayOf(versionUri),
                    null,
                    null,
                    "position ASC",
                ).use { cursor -> cursor.toList { toBlock() } }
            }
        }
        .flowOn(Dispatchers.IO)

    suspend fun search(versionUri: String, matchQuery: String): List<ReaderSearchEntity> = withContext(Dispatchers.IO) {
        database.read { sqlite ->
            sqlite.rawQuery(
                "SELECT version_uri, block_id, text, normalized_text FROM reader_search WHERE version_uri = ? AND reader_search MATCH ? LIMIT 100",
                arrayOf(versionUri, matchQuery),
            ).use { cursor -> cursor.toList { toSearchRow() } }
        }
    }

    private fun Cursor.toBlock() = ReaderBlockEntity(
        versionUri = string("version_uri"),
        blockId = string("block_id"),
        kind = string("kind"),
        depth = int("depth"),
        title = string("title"),
        text = string("text"),
        pageLabel = stringOrNull("page_label"),
        position = int("position"),
    )

    private fun Cursor.toSearchRow() = ReaderSearchEntity(
        versionUri = string("version_uri"),
        blockId = string("block_id"),
        text = string("text"),
        normalizedText = string("normalized_text"),
    )
}

class BookmarkDao(private val database: MaktabaDatabase) {
    suspend fun insert(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        database.write { sqlite ->
            sqlite.insertWithOnConflict(
                "bookmarks",
                null,
                ContentValues().apply {
                    if (bookmark.id != 0L) put("id", bookmark.id)
                    put("version_uri", bookmark.versionUri)
                    put("block_id", bookmark.blockId)
                    put("excerpt", bookmark.excerpt)
                    put("created_at", bookmark.createdAt)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
    }

    suspend fun delete(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        database.write { sqlite -> sqlite.delete("bookmarks", "id = ?", arrayOf(bookmark.id.toString())) }
    }

    fun observeForVersion(versionUri: String): Flow<List<BookmarkEntity>> = database.changes
        .map {
            database.read { sqlite ->
                sqlite.query(
                    "bookmarks",
                    null,
                    "version_uri = ?",
                    arrayOf(versionUri),
                    null,
                    null,
                    "created_at DESC",
                ).use { cursor -> cursor.toList { toBookmark() } }
            }
        }
        .flowOn(Dispatchers.IO)

    suspend fun find(versionUri: String, blockId: String): BookmarkEntity? = withContext(Dispatchers.IO) {
        database.read { sqlite ->
            sqlite.query(
                "bookmarks",
                null,
                "version_uri = ? AND block_id = ?",
                arrayOf(versionUri, blockId),
                null,
                null,
                null,
                "1",
            ).use { cursor -> if (cursor.moveToFirst()) cursor.toBookmark() else null }
        }
    }

    private fun Cursor.toBookmark() = BookmarkEntity(
        id = long("id"),
        versionUri = string("version_uri"),
        blockId = string("block_id"),
        excerpt = string("excerpt"),
        createdAt = long("created_at"),
    )
}

class ProgressDao(private val database: MaktabaDatabase) {
    suspend fun save(progress: ReadingProgressEntity) = withContext(Dispatchers.IO) {
        database.write { sqlite ->
            sqlite.insertWithOnConflict(
                "reading_progress",
                null,
                ContentValues().apply {
                    put("version_uri", progress.versionUri)
                    put("block_id", progress.blockId)
                    put("position", progress.position)
                    put("percent", progress.percent)
                    put("updated_at", progress.updatedAt)
                },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
    }

    fun observe(versionUri: String): Flow<ReadingProgressEntity?> = database.changes
        .map {
            database.read { sqlite ->
                sqlite.query(
                    "reading_progress",
                    null,
                    "version_uri = ?",
                    arrayOf(versionUri),
                    null,
                    null,
                    null,
                    "1",
                ).use { cursor -> if (cursor.moveToFirst()) cursor.toProgress() else null }
            }
        }
        .flowOn(Dispatchers.IO)

    private fun Cursor.toProgress() = ReadingProgressEntity(
        versionUri = string("version_uri"),
        blockId = string("block_id"),
        position = int("position"),
        percent = float("percent"),
        updatedAt = long("updated_at"),
    )
}

private inline fun <T> Cursor.toList(mapper: Cursor.() -> T): List<T> {
    val result = ArrayList<T>()
    while (moveToNext()) result += mapper()
    return result
}
