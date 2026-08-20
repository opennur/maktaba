package org.maktaba.app.data

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaktabaDatabaseMigrationTest {
    @Test
    fun v1ToV2PreservesBookRowsAndAddsLatestDownloadIndex() {
        val database = SQLiteDatabase.create(null)
        try {
            database.execSQL(
                "CREATE TABLE book_versions (book_uri TEXT, downloaded INTEGER, downloaded_at INTEGER)",
            )
            database.execSQL(
                "INSERT INTO book_versions(book_uri, downloaded, downloaded_at) VALUES ('book', 1, 123)",
            )

            MaktabaDatabase.migrate(database, oldVersion = 1, newVersion = 2)

            database.rawQuery("SELECT COUNT(*) FROM book_versions", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            database.rawQuery("PRAGMA index_list('book_versions')", null).use { cursor ->
                var found = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == "book_versions_downloaded_latest") {
                        found = true
                        break
                    }
                }
                assertTrue(found)
            }
        } finally {
            database.close()
        }
    }
}
