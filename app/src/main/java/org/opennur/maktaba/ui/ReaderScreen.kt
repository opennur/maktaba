package org.opennur.maktaba.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.opennur.maktaba.MaktabaViewModel
import org.opennur.maktaba.data.BlockKinds
import org.opennur.maktaba.data.BookmarkEntity
import org.opennur.maktaba.data.ReaderBlockEntity
import org.opennur.maktaba.data.ReaderSearchEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: MaktabaViewModel,
    versionUri: String,
    onBack: () -> Unit,
) {
    val blocksFlow = remember(versionUri) { viewModel.blocks(versionUri) }
    val blocks by blocksFlow.collectAsStateWithLifecycle()
    val bookmarksFlow = remember(versionUri) { viewModel.bookmarks(versionUri) }
    val bookmarks by bookmarksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val progressFlow = remember(versionUri) { viewModel.progress(versionUri) }
    val progress by progressFlow.collectAsStateWithLifecycle(initialValue = null)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val bookmarkedIds = remember(bookmarks) { bookmarks.map(BookmarkEntity::blockId).toSet() }
    val isRtl = versionUri.contains("-ara", ignoreCase = true) ||
        versionUri.contains("-fas", ignoreCase = true) ||
        versionUri.contains("-per", ignoreCase = true) ||
        versionUri.contains("-ota", ignoreCase = true)
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<ReaderSearchEntity>>(emptyList()) }
    var tocOpen by rememberSaveable { mutableStateOf(false) }
    var fontSize by rememberSaveable { mutableIntStateOf(17) }
    var restored by remember(versionUri) { mutableStateOf(false) }

    LaunchedEffect(blocks.size, progress?.position) {
        if (!restored && blocks.isNotEmpty()) {
            progress?.position?.coerceIn(0, blocks.lastIndex)?.let { listState.scrollToItem(it) }
            restored = true
        }
    }

    LaunchedEffect(blocks.size, versionUri) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val block = blocks.getOrNull(index) ?: return@collect
                val percent = if (blocks.size <= 1) 0f else index.toFloat() / (blocks.size - 1).toFloat()
                viewModel.saveProgress(versionUri, block, index, percent)
            }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            versionUri.substringAfterLast('.'),
                            maxLines = 1,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Text(
                            "${((progress?.percent ?: 0f) * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { tocOpen = !tocOpen }) {
                            Text("TOC")
                        }
                        TextButton(onClick = { fontSize = (fontSize - 1).coerceAtLeast(14) }) {
                            Text("A-")
                        }
                        TextButton(onClick = { fontSize = (fontSize + 1).coerceAtMost(32) }) {
                            Text("A+")
                        }
                        IconButton(
                            onClick = {
                                searchOpen = !searchOpen
                                if (!searchOpen) searchResults = emptyList()
                            },
                        ) {
                            Icon(
                                if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search in book",
                            )
                        }
                    },
                )
                if (searchOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("Search this book") },
                        )
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = {
                                viewModel.searchInBook(versionUri, searchText) { searchResults = it }
                            },
                            enabled = searchText.isNotBlank(),
                        ) {
                            Text("Find")
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        if (blocks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("Preparing the reader...")
                }
            }
            return@Scaffold
        }

        CompositionLocalProviderForDirection(isRtl) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                if (tocOpen) {
                    TableOfContents(
                        blocks = blocks,
                        isRtl = isRtl,
                        onSelect = { index ->
                            tocOpen = false
                            scope.launch { listState.animateScrollToItem(index) }
                        },
                    )
                }
                if (searchOpen && searchText.isNotBlank()) {
                    SearchResults(
                        results = searchResults,
                        blocks = blocks,
                        onResultClick = { result ->
                            val index = blocks.indexOfFirst { it.blockId == result.blockId }
                            if (index >= 0) {
                                searchOpen = false
                                scope.launch { listState.animateScrollToItem(index) }
                            }
                        },
                    )
                }
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(blocks, key = { it.blockId }) { block ->
                            ReaderBlock(
                                block = block,
                                fontSize = fontSize,
                                isBookmarked = block.blockId in bookmarkedIds,
                                isRtl = isRtl,
                                onToggleBookmark = { viewModel.toggleBookmark(versionUri, block) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableOfContents(
    blocks: List<ReaderBlockEntity>,
    isRtl: Boolean,
    onSelect: (Int) -> Unit,
) {
    val headings = remember(blocks) {
        blocks.withIndex().filter { it.value.kind == BlockKinds.HEADING }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .heightIn(max = 220.dp),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(
                "Table of contents",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
            if (headings.isEmpty()) {
                Text(
                    "This text has no structural headings.",
                    modifier = Modifier.padding(6.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                headings.take(120).forEach { indexed ->
                    TextButton(
                        onClick = { onSelect(indexed.index) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            indexed.value.title.ifBlank { "Untitled section" },
                            modifier = Modifier.fillMaxWidth().padding(start = 10.dp * (indexed.value.depth - 1).coerceAtLeast(0)),
                            maxLines = 1,
                            textAlign = if (isRtl) TextAlign.End else TextAlign.Start,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: List<ReaderSearchEntity>,
    blocks: List<ReaderBlockEntity>,
    onResultClick: (ReaderSearchEntity) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            if (results.isEmpty()) "No matches" else "${results.size} matches",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        results.take(3).forEach { result ->
            val block = blocks.firstOrNull { it.blockId == result.blockId }
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onResultClick(result) },
            ) {
                Text(
                    block?.text ?: result.text,
                    modifier = Modifier.padding(6.dp),
                    maxLines = 2,
                )
            }
        }
        if (results.size > 3) {
            Text(
                "Tap a result to jump to it",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReaderBlock(
    block: ReaderBlockEntity,
    fontSize: Int,
    isBookmarked: Boolean,
    isRtl: Boolean,
    onToggleBookmark: () -> Unit,
) {
    when (block.kind) {
        BlockKinds.PAGE -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    block.pageLabel.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        BlockKinds.HEADING -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    block.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = (fontSize + 2).sp),
                    fontFamily = FontFamily.Serif,
                    textAlign = if (isRtl) TextAlign.End else TextAlign.Start,
                )
                BookmarkButton(isBookmarked, onToggleBookmark)
            }
            HorizontalDivider(Modifier.padding(top = 4.dp))
        }
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    block.text,
                    modifier = Modifier.weight(1f),
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.45f).sp,
                        textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr,
                    ),
                    textAlign = if (isRtl) TextAlign.End else TextAlign.Start,
                )
                BookmarkButton(isBookmarked, onToggleBookmark)
            }
        }
    }
}

@Composable
private fun BookmarkButton(isBookmarked: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
            contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompositionLocalProviderForDirection(
    isRtl: Boolean,
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        content = content,
    )
}
