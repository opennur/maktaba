package org.maktaba.app

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.maktaba.app.data.BookmarkEntity
import org.maktaba.app.data.BookVersionEntity
import org.maktaba.app.data.CatalogBookRow
import org.maktaba.app.data.CatalogImportProgress
import org.maktaba.app.data.MaktabaRepository
import org.maktaba.app.data.ReaderBlockEntity
import org.maktaba.app.data.ReaderSearchEntity
import org.maktaba.app.data.ReadingProgressEntity
import org.maktaba.app.testing.MainDispatcherRule
import java.io.IOException
import android.app.Application
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class MaktabaViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun reportsReadyWhenInitialCatalogLoadSucceeds() = runTest {
        val repository = repository()

        val viewModel = viewModel(repository)
        advanceUntilIdle()

        assertEquals(CatalogState.Ready, viewModel.catalogState.value)
    }

    @Test
    fun reportsErrorWhenInitialCatalogLoadFails() = runTest {
        val repository = repository()
        repository.ensureFailure = IOException("catalog unavailable")

        val viewModel = viewModel(repository)
        advanceUntilIdle()

        assertEquals(CatalogState.Error("catalog unavailable"), viewModel.catalogState.value)
    }

    @Test
    fun reportsDownloadProgressAndReadyState() = runTest {
        val repository = repository()
        val version = sampleVersion()

        val viewModel = viewModel(repository)
        viewModel.download(version)
        advanceUntilIdle()

        assertEquals(DownloadState.Ready, viewModel.downloadStates.value[version.versionUri])
    }

    @Test
    fun reportsDownloadError() = runTest {
        val repository = repository()
        val version = sampleVersion()
        repository.downloadFailure = IOException("download failed")

        val viewModel = viewModel(repository)
        viewModel.download(version)
        advanceUntilIdle()

        val state = viewModel.downloadStates.value[version.versionUri]
        assertTrue(state is DownloadState.Error)
        assertEquals("download failed", (state as DownloadState.Error).message)
    }

    @Test
    fun returnsSearchResultsFromRepository() = runTest {
        val repository = repository()
        val expected = listOf(
            ReaderSearchEntity(
                versionUri = "book.version-ara1",
                blockId = "block-1",
                text = "كتاب",
                normalizedText = "كتاب",
            ),
        )
        repository.searchResults = expected

        val viewModel = viewModel(repository)
        var actual: List<org.maktaba.app.data.ReaderSearchEntity>? = null
        viewModel.searchInBook("book.version-ara1", "كتاب") { actual = it }
        advanceUntilIdle()

        assertEquals(expected, actual)
    }

    private fun repository() = FakeMaktabaRepository()

    private fun viewModel(repository: MaktabaRepository): MaktabaViewModel =
        MaktabaViewModel(Application(), repository)

    private fun sampleVersion() = BookVersionEntity(
        versionUri = "book.version-ara1",
        bookUri = "book",
        language = "ara",
        subcorpus = "ara",
        uncorrectedOcr = false,
        date = "2025",
        authorArabic = "",
        authorLatin = "Author",
        titleArabic = "",
        titleLatin = "Book",
        editionInfo = "",
        sourceId = "Source001",
        status = "pri",
        tokenLength = 10,
        characterLength = 20,
        localPath = "data/book",
        tags = "",
        authorFromUri = "Author",
        parts = "",
    )
}

private class FakeMaktabaRepository : MaktabaRepository {
    var ensureFailure: Throwable? = null
    var downloadFailure: Throwable? = null
    var searchResults: List<ReaderSearchEntity> = emptyList()

    override fun observeCatalog(query: String) = flowOf(emptyList<CatalogBookRow>())

    override fun observeDownloadedBooks() = flowOf(emptyList<CatalogBookRow>())

    override fun observeVersions(bookUri: String) = flowOf(emptyList<BookVersionEntity>())

    override fun observeBlocks(versionUri: String) = flowOf(emptyList<ReaderBlockEntity>())

    override fun observeBookmarks(versionUri: String) = flowOf(emptyList<BookmarkEntity>())

    override fun observeProgress(versionUri: String) = flowOf<ReadingProgressEntity?>(null)

    override suspend fun importCatalog(onProgress: (CatalogImportProgress) -> Unit) = Unit

    override suspend fun ensureCatalog(onProgress: (CatalogImportProgress) -> Unit) {
        ensureFailure?.let { throw it }
    }

    override suspend fun downloadBook(
        versionUri: String,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit,
    ) {
        downloadFailure?.let { throw it }
        onProgress(50, 100)
    }

    override suspend fun exportBook(versionUri: String, destination: android.net.Uri) = Unit

    override suspend fun exportDownloadedBook(bookUri: String, destination: android.net.Uri) = Unit

    override suspend fun searchInBook(versionUri: String, query: String) = searchResults

    override suspend fun toggleBookmark(versionUri: String, block: ReaderBlockEntity) = Unit

    override suspend fun saveProgress(versionUri: String, block: ReaderBlockEntity, position: Int, percent: Float) = Unit
}
