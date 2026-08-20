package org.maktaba.app.data

import android.database.sqlite.SQLiteDatabase
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MaktabaDatabaseRobolectricTest {
    @Test
    fun v1UpgradePreservesRowsAndCreatesTheLatestDownloadIndex() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val databaseName = "maktaba-migration-${System.nanoTime()}.db"
        val databaseFile = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()

        try {
            val oldDatabase = SQLiteDatabase.openOrCreateDatabase(databaseFile.absolutePath, null)
            oldDatabase.execSQL(
                "CREATE TABLE book_versions (book_uri TEXT, downloaded INTEGER, downloaded_at INTEGER)",
            )
            oldDatabase.execSQL(
                "INSERT INTO book_versions(book_uri, downloaded, downloaded_at) VALUES ('book', 1, 123)",
            )
            oldDatabase.version = 1
            oldDatabase.close()

            val upgraded = MaktabaDatabase(context, databaseName)
            val database = upgraded.writableDatabase
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
            upgraded.close()
        } finally {
            databaseFile.delete()
        }
    }
}
