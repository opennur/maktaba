package org.maktaba.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest

data class CatalogImportProgress(
    val bytesRead: Long,
    val totalBytes: Long,
    val recordsImported: Int,
)

object OpenItiRelease {
    const val tag = "v2025.1.9"
    const val metadataFile = "metadata/OpenITI_metadata_2025-1-9.tsv"
    private const val rawRoot = "https://raw.githubusercontent.com/OpenITI/RELEASE"

    fun rawUrl(path: String): String = "$rawRoot/$tag/$path"

    fun contentCandidates(localPath: String): List<String> {
        val normalizedPath = localPath.trim().trimStart('/')
        val knownExtension = listOf(".mARkdown", ".completed", ".inProgress", ".txt")
            .any(normalizedPath::endsWith)
        return buildList {
            add(normalizedPath)
            if (!knownExtension) {
                add("$normalizedPath.mARkdown")
                add("$normalizedPath.completed")
                add("$normalizedPath.inProgress")
            }
        }.distinct()
    }

    fun exportFileName(versionUri: String): String {
        val baseName = versionUri.substringAfterLast('/')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim { it == '_' || it == '.' }
        return baseName.ifBlank { "book" }
    }
}

class OpenItiRepository(
    context: Context,
    private val database: MaktabaDatabase,
    private val client: OkHttpClient = OkHttpClient.Builder().build(),
    private val urlForPath: (String) -> String = OpenItiRelease::rawUrl,
) : MaktabaRepository {
    private val appContext = context.applicationContext
    private val bookDirectory = File(appContext.filesDir, "openiti-books")
    private val bookDao = database.bookDao
    private val readerDao = database.readerDao
    private val bookmarkDao = database.bookmarkDao
    private val progressDao = database.progressDao

    init {
        bookDirectory.mkdirs()
    }

    override fun observeCatalog(query: String): Flow<List<CatalogBookRow>> = bookDao.observeCatalog(query)

    override fun observeDownloadedBooks(): Flow<List<CatalogBookRow>> = bookDao.observeDownloadedBooks()

    override fun observeVersions(bookUri: String): Flow<List<BookVersionEntity>> = bookDao.observeVersions(bookUri)

    override fun observeBlocks(versionUri: String): Flow<List<ReaderBlockEntity>> = readerDao.observeBlocks(versionUri)

    override fun observeBookmarks(versionUri: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.observeForVersion(versionUri)

    override fun observeProgress(versionUri: String): Flow<ReadingProgressEntity?> = progressDao.observe(versionUri)

    suspend fun catalogCount(): Int = bookDao.count()

    override suspend fun importCatalog(
        onProgress: (CatalogImportProgress) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(urlForPath(OpenItiRelease.metadataFile)).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Catalog request failed: HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Catalog response was empty")
            val batch = ArrayList<BookVersionEntity>(500)
            val seenVersionUris = HashSet<String>()
            val countingInput = CountingInputStream(body.byteStream())
            val totalBytes = body.contentLength()
            var recordsImported = 0
            var lastReportedBytes = -1L
            var lastReportedAt = 0L

            fun reportProgress(force: Boolean = false) {
                val now = System.currentTimeMillis()
                val enoughBytes = countingInput.bytesRead - lastReportedBytes >= 64 * 1024
                val enoughTime = now - lastReportedAt >= 250
                if (force || enoughBytes || enoughTime) {
                    lastReportedBytes = countingInput.bytesRead
                    lastReportedAt = now
                    onProgress(CatalogImportProgress(countingInput.bytesRead, totalBytes, recordsImported))
                }
            }

            onProgress(CatalogImportProgress(0, totalBytes, 0))
            countingInput.bufferedReader(Charsets.UTF_8).use { reader ->
                OpenItiCatalogParser.parse(reader).forEach { record ->
                    batch += record.toEntity()
                    seenVersionUris += record.versionUri
                    recordsImported += 1
                    reportProgress()
                    if (batch.size >= 500) {
                        bookDao.insertAll(batch.toList())
                        batch.clear()
                    }
                }
            }
            if (batch.isNotEmpty()) bookDao.insertAll(batch)
            bookDao.deleteNotIn(seenVersionUris)
            reportProgress(force = true)
        }
    }

    override suspend fun ensureCatalog(onProgress: (CatalogImportProgress) -> Unit) {
        if (bookDao.count() == 0) importCatalog(onProgress)
    }

    suspend fun getVersion(versionUri: String): BookVersionEntity? = bookDao.getVersion(versionUri)

    override suspend fun downloadBook(
        versionUri: String,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val version = bookDao.getVersion(versionUri)
            ?: throw IllegalArgumentException("Unknown OpenITI version: $versionUri")
        val localPath = version.localPath ?: throw IOException("This record has no downloadable text")
        val target = File(bookDirectory, fileNameFor(versionUri))

        if (version.downloaded && target.exists() && target.length() > 0L) return@withContext

        if (!target.exists() || target.length() == 0L) {
            val temporary = File(bookDirectory, "${target.name}.part")
            try {
                openTextResponse(localPath).use { response ->
                    val body = response.body ?: throw IOException("Text response was empty")
                    val total = body.contentLength()
                    var bytesRead = 0L
                    body.byteStream().use { input ->
                        temporary.outputStream().buffered().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                bytesRead += read
                                onProgress(bytesRead, total)
                            }
                        }
                    }
                }
                if (!temporary.renameTo(target)) {
                    throw IOException("Could not finalize downloaded text")
                }
            } catch (error: Throwable) {
                temporary.delete()
                throw error
            }
        }

        indexDocument(versionUri, target)
        bookDao.setDownloaded(versionUri, downloaded = true, path = target.absolutePath, downloadedAt = System.currentTimeMillis())
    }

    suspend fun deleteBook(versionUri: String) = withContext(Dispatchers.IO) {
        val version = bookDao.getVersion(versionUri)
        version?.downloadPath?.let { File(it).delete() }
        readerDao.deleteBlocks(versionUri)
        readerDao.deleteSearchRows(versionUri)
        bookDao.setDownloaded(versionUri, downloaded = false, path = null, downloadedAt = null)
    }

    override suspend fun exportBook(versionUri: String, destination: Uri) = withContext(Dispatchers.IO) {
        val version = bookDao.getVersion(versionUri)
            ?: throw IllegalArgumentException("Unknown OpenITI version: $versionUri")
        exportVersion(version, destination)
    }

    override suspend fun exportDownloadedBook(bookUri: String, destination: Uri) = withContext(Dispatchers.IO) {
        val version = bookDao.getDownloadedVersion(bookUri)
            ?: throw IOException("No downloaded version is available for this book")
        exportVersion(version, destination)
    }

    private fun exportVersion(version: BookVersionEntity, destination: Uri) {
        val source = version.downloadPath?.let(::File)
            ?: throw IOException("This book has not been downloaded")
        if (!version.downloaded || !source.isFile || source.length() == 0L) {
            throw IOException("Downloaded text is not available")
        }

        val output = appContext.contentResolver.openOutputStream(destination, "wt")
            ?: throw IOException("Could not open the export destination")
        source.inputStream().buffered().use { input ->
            output.buffered().use { bufferedOutput -> input.copyTo(bufferedOutput) }
        }
    }

    override suspend fun searchInBook(versionUri: String, query: String): List<ReaderSearchEntity> {
        val matchQuery = TextNormalizer.toMatchQuery(query)
        if (matchQuery.isBlank()) return emptyList()
        return readerDao.search(versionUri, matchQuery)
    }

    override suspend fun toggleBookmark(versionUri: String, block: ReaderBlockEntity) {
        val existing = bookmarkDao.find(versionUri, block.blockId)
        if (existing == null) {
            bookmarkDao.insert(
                BookmarkEntity(
                    versionUri = versionUri,
                    blockId = block.blockId,
                    excerpt = block.text.ifBlank { block.title }.take(280),
                ),
            )
        } else {
            bookmarkDao.delete(existing)
        }
    }

    override suspend fun saveProgress(versionUri: String, block: ReaderBlockEntity, position: Int, percent: Float) {
        progressDao.save(
            ReadingProgressEntity(
                versionUri = versionUri,
                blockId = block.blockId,
                position = position,
                percent = percent,
            ),
        )
    }

    private suspend fun indexDocument(versionUri: String, file: File) {
        readerDao.deleteBlocks(versionUri)
        readerDao.deleteSearchRows(versionUri)
        val blockBatch = ArrayList<ReaderBlockEntity>(200)
        val searchBatch = ArrayList<ReaderSearchEntity>(200)

        file.bufferedReader(Charsets.UTF_8).use { reader ->
            OpenItiMarkdownParser.parse(reader) { parsed ->
                blockBatch += ReaderBlockEntity(
                    versionUri = versionUri,
                    blockId = parsed.blockId,
                    kind = parsed.kind,
                    depth = parsed.depth,
                    title = parsed.title,
                    text = parsed.text,
                    pageLabel = parsed.pageLabel,
                    position = parsed.position,
                )
                val searchable = parsed.title.ifBlank { parsed.text }
                if (searchable.isNotBlank()) {
                    searchBatch += ReaderSearchEntity(
                        versionUri = versionUri,
                        blockId = parsed.blockId,
                        text = searchable,
                        normalizedText = TextNormalizer.normalize(searchable),
                    )
                }
                if (blockBatch.size >= 200) {
                    readerDao.insertBlocks(blockBatch.toList())
                    if (searchBatch.isNotEmpty()) readerDao.insertSearchRows(searchBatch.toList())
                    blockBatch.clear()
                    searchBatch.clear()
                }
            }
        }
        if (blockBatch.isNotEmpty()) readerDao.insertBlocks(blockBatch)
        if (searchBatch.isNotEmpty()) readerDao.insertSearchRows(searchBatch)
    }

    private fun openTextResponse(localPath: String): Response {
        val candidates = OpenItiRelease.contentCandidates(localPath)
        var lastCode = -1
        for (candidate in candidates) {
            val response = client.newCall(
                Request.Builder().url(urlForPath(candidate)).build(),
            ).execute()
            if (response.isSuccessful) return response
            lastCode = response.code
            response.close()
        }
        throw IOException("Text request failed for $localPath: HTTP $lastCode")
    }

    private fun fileNameFor(versionUri: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(versionUri.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) } + ".mARkdown"
    }

    private class CountingInputStream(input: InputStream) : FilterInputStream(input) {
        var bytesRead: Long = 0
            private set

        override fun read(): Int {
            val value = super.read()
            if (value >= 0) bytesRead += 1
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            val count = super.read(buffer, offset, length)
            if (count > 0) bytesRead += count
            return count
        }
    }
}
