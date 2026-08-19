package org.maktaba.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow

class MaktabaDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "maktaba.db", null, DATABASE_VERSION) {
    val changes = MutableStateFlow(0L)

    val bookDao = BookDao(this)
    val readerDao = ReaderDao(this)
    val bookmarkDao = BookmarkDao(this)
    val progressDao = ProgressDao(this)

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE book_versions (
                version_uri TEXT PRIMARY KEY NOT NULL,
                book_uri TEXT NOT NULL,
                language TEXT NOT NULL,
                subcorpus TEXT NOT NULL,
                uncorrected_ocr INTEGER NOT NULL,
                date TEXT NOT NULL,
                author_ar TEXT NOT NULL,
                author_lat TEXT NOT NULL,
                title_ar TEXT NOT NULL,
                title_lat TEXT NOT NULL,
                edition_info TEXT NOT NULL,
                source_id TEXT NOT NULL,
                status TEXT NOT NULL,
                token_length INTEGER NOT NULL,
                character_length INTEGER NOT NULL,
                local_path TEXT,
                tags TEXT NOT NULL,
                author_from_uri TEXT NOT NULL,
                parts TEXT NOT NULL,
                downloaded INTEGER NOT NULL DEFAULT 0,
                download_path TEXT,
                downloaded_at INTEGER
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX book_versions_book_uri ON book_versions(book_uri)")
        database.execSQL("CREATE INDEX book_versions_downloaded ON book_versions(downloaded)")
        database.execSQL(
            """
            CREATE TABLE reader_blocks (
                version_uri TEXT NOT NULL,
                block_id TEXT NOT NULL,
                kind TEXT NOT NULL,
                depth INTEGER NOT NULL,
                title TEXT NOT NULL,
                text TEXT NOT NULL,
                page_label TEXT,
                position INTEGER NOT NULL,
                PRIMARY KEY(version_uri, block_id)
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX reader_blocks_position ON reader_blocks(version_uri, position)")
        database.execSQL(
            """
            CREATE VIRTUAL TABLE reader_search USING fts4(
                version_uri,
                block_id,
                text,
                normalized_text
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE bookmarks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                version_uri TEXT NOT NULL,
                block_id TEXT NOT NULL,
                excerpt TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                UNIQUE(version_uri, block_id)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE reading_progress (
                version_uri TEXT PRIMARY KEY NOT NULL,
                block_id TEXT NOT NULL,
                position INTEGER NOT NULL,
                percent REAL NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        database.execSQL("DROP TABLE IF EXISTS reading_progress")
        database.execSQL("DROP TABLE IF EXISTS bookmarks")
        database.execSQL("DROP TABLE IF EXISTS reader_search")
        database.execSQL("DROP TABLE IF EXISTS reader_blocks")
        database.execSQL("DROP TABLE IF EXISTS book_versions")
        onCreate(database)
    }

    internal fun <T> read(block: (SQLiteDatabase) -> T): T = block(readableDatabase)

    internal fun write(block: (SQLiteDatabase) -> Unit) {
        block(writableDatabase)
        changes.value += 1
    }

    companion object {
        private const val DATABASE_VERSION = 1

        @Volatile
        private var instance: MaktabaDatabase? = null

        fun get(context: Context): MaktabaDatabase =
            instance ?: synchronized(this) {
                instance ?: MaktabaDatabase(context).also { instance = it }
            }
    }
}
