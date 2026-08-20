package org.opennur.maktaba.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.opennur.maktaba.CatalogState
import org.opennur.maktaba.MaktabaViewModel
import org.opennur.maktaba.data.CatalogBookRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: MaktabaViewModel,
    modifier: Modifier = Modifier,
    onBookClick: (String) -> Unit,
) {
    val state by viewModel.catalogState.collectAsStateWithLifecycle()
    val importProgress by viewModel.catalogProgress.collectAsStateWithLifecycle()
    val books by viewModel.catalogBooks.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Maktaba")
                        Text(
                            "OpenITI reading room",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refreshCatalog,
                        enabled = state != CatalogState.Loading,
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh catalog")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                placeholder = { Text("Search titles or authors") },
            )
            Spacer(Modifier.height(4.dp))

            when (val currentState = state) {
                CatalogState.Loading -> {
                    val progress = if (importProgress.totalBytes > 0) {
                        (importProgress.bytesRead.toFloat() / importProgress.totalBytes.toFloat()).coerceIn(0f, 1f)
                    } else {
                        null
                    }
                    if (progress == null) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        text = "Fetched ${importProgress.recordsImported} catalog records",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    if (books.isEmpty()) {
                        LoadingMessage("Importing the OpenITI catalog...")
                    } else {
                        CatalogBookList(books, onBookClick, Modifier.weight(1f))
                    }
                }
                is CatalogState.Error -> {
                    ErrorMessage(currentState.message, viewModel::retryCatalog)
                    if (books.isNotEmpty()) CatalogBookList(books, onBookClick, Modifier.weight(1f))
                }
                CatalogState.Ready -> {
                    if (books.isEmpty()) {
                        LoadingMessage(if (query.isBlank()) "No books found" else "No matching books")
                    } else {
                        CatalogBookList(books, onBookClick, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MaktabaViewModel,
    modifier: Modifier = Modifier,
    onBookClick: (String) -> Unit,
) {
    val books by viewModel.downloadedBooks.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Your library") }) },
    ) { paddingValues ->
        if (books.isEmpty()) {
            LoadingMessage(
                text = "Downloaded books will appear here.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
            ) {
                items(books, key = { it.bookUri }) { book ->
                    BookCard(book, onClick = { onBookClick(book.bookUri) })
                }
            }
        }
    }
}

@Composable
private fun BookCard(book: CatalogBookRow, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(
                text = book.titleArabic.ifBlank { book.titleLatin.ifBlank { book.bookUri } },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (book.titleLatin.isNotBlank() && book.titleLatin != book.titleArabic) {
                Text(
                    book.titleLatin,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                book.authorArabic.ifBlank { book.authorLatin },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${book.versionCount} version${if (book.versionCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    formatCount(book.tokenLength) + " words",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoadingMessage(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (text.contains("Importing")) {
            CircularProgressIndicator(Modifier.size(32.dp))
            Spacer(Modifier.height(12.dp))
        }
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ErrorMessage(message: String, retry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Could not load the catalog", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = retry) { Text("Retry") }
    }
}

fun formatCount(value: Int): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000f)
    value >= 1_000 -> "%.1fK".format(value / 1_000f)
    else -> value.toString()
}

@Composable
private fun CatalogBookList(
    books: List<CatalogBookRow>,
    onBookClick: (String) -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "${books.size} books",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(books, key = { it.bookUri }) { book ->
                BookCard(book, onClick = { onBookClick(book.bookUri) })
            }
        }
    }
}
