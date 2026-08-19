package org.maktaba.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.maktaba.app.data.BookVersionEntity
import org.maktaba.app.data.CatalogBookRow
import org.maktaba.app.data.MaktabaDatabase
import org.maktaba.app.data.OpenItiRepository
import org.maktaba.app.data.ReaderBlockEntity
import org.maktaba.app.data.ReaderSearchEntity

sealed interface CatalogState {
    data object Loading : CatalogState
    data object Ready : CatalogState
    data class Error(val message: String) : CatalogState
}

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val bytesRead: Long, val totalBytes: Long) : DownloadState
    data object Ready : DownloadState
    data class Error(val message: String) : DownloadState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class MaktabaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = MaktabaDatabase.get(application)
    private val repository = OpenItiRepository(application, database)
    private val query = MutableStateFlow("")
    private val _catalogState = MutableStateFlow<CatalogState>(CatalogState.Loading)
    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    private val downloadJobs = mutableMapOf<String, Job>()

    val catalogState: StateFlow<CatalogState> = _catalogState.asStateFlow()
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()
    val searchQuery: StateFlow<String> = query.asStateFlow()
    val catalogBooks: StateFlow<List<CatalogBookRow>> = query
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest(repository::observeCatalog)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val downloadedBooks: StateFlow<List<CatalogBookRow>> = repository
        .observeDownloadedBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadCatalog()
    }

    fun setSearchQuery(value: String) {
        query.value = value
    }

    fun retryCatalog() {
        if (_catalogState.value == CatalogState.Loading) return
        loadCatalog()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            _catalogState.value = CatalogState.Loading
            try {
                repository.ensureCatalog()
                _catalogState.value = CatalogState.Ready
            } catch (error: Throwable) {
                _catalogState.value = CatalogState.Error(error.message ?: "Could not load the OpenITI catalog")
            }
        }
    }

    fun versions(bookUri: String): StateFlow<List<BookVersionEntity>> = repository
        .observeVersions(bookUri)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun blocks(versionUri: String): StateFlow<List<ReaderBlockEntity>> = repository
        .observeBlocks(versionUri)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun bookmarks(versionUri: String) = repository.observeBookmarks(versionUri)

    fun progress(versionUri: String) = repository.observeProgress(versionUri)

    fun download(version: BookVersionEntity) {
        if (downloadJobs[version.versionUri]?.isActive == true) return
        val job = viewModelScope.launch {
            _downloadStates.value = _downloadStates.value + (version.versionUri to DownloadState.Downloading(0, 0))
            try {
                repository.downloadBook(version.versionUri) { bytesRead, totalBytes ->
                    _downloadStates.value = _downloadStates.value +
                        (version.versionUri to DownloadState.Downloading(bytesRead, totalBytes))
                }
                _downloadStates.value = _downloadStates.value + (version.versionUri to DownloadState.Ready)
            } catch (error: Throwable) {
                _downloadStates.value = _downloadStates.value +
                    (version.versionUri to DownloadState.Error(error.message ?: "Download failed"))
            } finally {
                downloadJobs.remove(version.versionUri)
            }
        }
        downloadJobs[version.versionUri] = job
    }

    fun searchInBook(versionUri: String, text: String, onResult: (List<ReaderSearchEntity>) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(repository.searchInBook(versionUri, text))
            } catch (_: Throwable) {
                onResult(emptyList())
            }
        }
    }

    fun toggleBookmark(versionUri: String, block: ReaderBlockEntity) {
        viewModelScope.launch { repository.toggleBookmark(versionUri, block) }
    }

    fun saveProgress(versionUri: String, block: ReaderBlockEntity, position: Int, percent: Float) {
        viewModelScope.launch { repository.saveProgress(versionUri, block, position, percent) }
    }
}
