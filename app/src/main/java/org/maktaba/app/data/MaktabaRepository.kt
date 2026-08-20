package org.maktaba.app.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface MaktabaRepository {
    fun observeCatalog(query: String): Flow<List<CatalogBookRow>>

    fun observeDownloadedBooks(): Flow<List<CatalogBookRow>>

    fun observeVersions(bookUri: String): Flow<List<BookVersionEntity>>

    fun observeBlocks(versionUri: String): Flow<List<ReaderBlockEntity>>

    fun observeBookmarks(versionUri: String): Flow<List<BookmarkEntity>>

    fun observeProgress(versionUri: String): Flow<ReadingProgressEntity?>

    suspend fun importCatalog(onProgress: (CatalogImportProgress) -> Unit = {})

    suspend fun ensureCatalog(onProgress: (CatalogImportProgress) -> Unit = {})

    suspend fun downloadBook(
        versionUri: String,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> },
    )

    suspend fun deleteBook(versionUri: String)

    suspend fun exportBook(versionUri: String, destination: Uri)

    suspend fun exportDownloadedBook(bookUri: String, destination: Uri)

    suspend fun searchInBook(versionUri: String, query: String): List<ReaderSearchEntity>

    suspend fun toggleBookmark(versionUri: String, block: ReaderBlockEntity)

    suspend fun saveProgress(versionUri: String, block: ReaderBlockEntity, position: Int, percent: Float)
}
