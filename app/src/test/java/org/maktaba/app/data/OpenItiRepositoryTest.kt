package org.maktaba.app.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OpenItiRepositoryTest {
    private lateinit var context: Application
    private lateinit var database: MaktabaDatabase
    private lateinit var databaseName: String
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "maktaba-repository-${System.nanoTime()}.db"
        database = MaktabaDatabase(context, databaseName)
        server = MockWebServer().also { it.start() }
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun importsCatalogFromMockServer() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "version_uri\tlanguage\tsubcorpus\tuncorrected_OCR\tdate\tauthor_ar\tauthor_lat\tbook\ttitle_ar\ttitle_lat\ted_info\tid\tstatus\ttok_length\tchar_length\tlocal_path\ttags\tauthor_from_uri\tparts\n" +
                    "0100Author.Book.Source001-ara1\tara\tara\tFalse\t0100\tالمؤلف\tAuthor\t0100Author.Book\tعنوان الكتاب\tBook Title\tedition\tSource001\tpri\t12\t34\tdata/book\tTAG\tAuthor\t\n",
            ),
        )

        repository().importCatalog()

        assertEquals(1, database.bookDao.count())
        assertEquals("Book Title", database.bookDao.getVersion("0100Author.Book.Source001-ara1")?.titleLatin)
    }

    @Test
    fun downloadsBarePathAfterTryingTheLegacyMarkdownPath() = runBlocking {
        val version = sampleVersion()
        database.bookDao.insertAll(listOf(version))
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("######OpenITI#\n#META#Header#End#\n# downloaded text\n"),
        )

        repository().downloadBook(version.versionUri)

        val stored = database.bookDao.getVersion(version.versionUri)
        assertTrue(stored?.downloaded == true)
        assertTrue(stored?.downloadPath?.let(::File)?.isFile == true)
        assertEquals(2, server.requestCount)
        assertEquals("/data/book", server.takeRequest().path)
        assertEquals("/data/book.mARkdown", server.takeRequest().path)
    }

    private fun repository(): MaktabaRepository = OpenItiRepository(
        context = context,
        database = database,
        urlForPath = { path -> server.url("/$path").toString() },
    )

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
