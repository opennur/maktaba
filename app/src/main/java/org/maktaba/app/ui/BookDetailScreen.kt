package org.maktaba.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maktaba.app.DownloadState
import org.maktaba.app.MaktabaViewModel
import org.maktaba.app.data.BookVersionEntity
import org.maktaba.app.data.OpenItiRelease

private const val SECRET_EXPORT_TAPS = 20
private const val SECRET_TAP_TIMEOUT_MS = 600L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    viewModel: MaktabaViewModel,
    bookUri: String,
    onBack: () -> Unit,
    onRead: (String) -> Unit,
) {
    val versionsFlow = remember(bookUri) { viewModel.versions(bookUri) }
    val versions by versionsFlow.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var pendingExportVersionUri by remember { mutableStateOf<String?>(null) }
    var deleteVersionUri by remember { mutableStateOf<String?>(null) }
    var deleteErrorMessage by remember { mutableStateOf<String?>(null) }
    var tapVersionUri by remember { mutableStateOf<String?>(null) }
    var tapCount by remember { mutableIntStateOf(0) }
    var tapResetJob by remember { mutableStateOf<Job?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { destination ->
        val versionUri = pendingExportVersionUri
        pendingExportVersionUri = null
        if (destination != null && versionUri != null) {
            viewModel.exportBook(versionUri, destination) { }
        }
    }

    fun handleVersionTap(versionUri: String) {
        tapResetJob?.cancel()
        if (tapVersionUri != versionUri) {
            tapVersionUri = versionUri
            tapCount = 0
        }
        tapCount += 1
        if (tapCount >= SECRET_EXPORT_TAPS) {
            tapVersionUri = null
            tapCount = 0
            pendingExportVersionUri = versionUri
            exportLauncher.launch(OpenItiRelease.exportFileName(versionUri))
            return
        }
        tapResetJob = scope.launch {
            delay(SECRET_TAP_TIMEOUT_MS)
            if (tapVersionUri == versionUri) {
                tapVersionUri = null
                tapCount = 0
            }
        }
    }
    val representative = versions.firstOrNull()

    deleteVersionUri?.let { versionUriToDelete ->
        AlertDialog(
            onDismissRequest = { deleteVersionUri = null },
            title = { Text("Delete downloaded book?") },
            text = {
                Text("The downloaded text and reader index will be removed. Bookmarks and reading progress will be kept.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteVersionUri = null
                        viewModel.deleteBook(versionUriToDelete) { error ->
                            deleteErrorMessage = error?.message
                        }
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteVersionUri = null }) { Text("Cancel") }
            },
        )
    }

    deleteErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteErrorMessage = null },
            title = { Text("Could not delete book") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { deleteErrorMessage = null }) { Text("OK") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        if (representative == null) {
            Text(
                "This book is not available in the catalog.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                BookHeader(representative, versions.size)
            }
            item {
                Text(
                    "Available versions",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            items(versions, key = { it.versionUri }) { version ->
                VersionCard(
                    version = version,
                    state = downloadStates[version.versionUri] ?: DownloadState.Idle,
                    onDownload = { viewModel.download(version) },
                    onRead = { onRead(version.versionUri) },
                    onDelete = { deleteVersionUri = version.versionUri },
                    onSecretTap = { handleVersionTap(version.versionUri) },
                )
            }
        }
    }
}

@Composable
private fun BookHeader(version: BookVersionEntity, versionCount: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                version.titleArabic.ifBlank { version.titleLatin.ifBlank { version.bookUri } },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                fontFamily = FontFamily.Serif,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (version.titleLatin.isNotBlank()) {
                Text(
                    version.titleLatin,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                version.authorArabic.ifBlank { version.authorLatin },
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            MetadataLine("OpenITI URI", version.bookUri)
            MetadataLine("Versions", versionCount.toString())
            MetadataLine("Release", "v2025.1.9")
        }
    }
}

@Composable
private fun MetadataLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VersionCard(
    version: BookVersionEntity,
    state: DownloadState,
    onDownload: () -> Unit,
    onRead: () -> Unit,
    onDelete: () -> Unit,
    onSecretTap: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = version.downloaded || state == DownloadState.Ready,
                onClick = onSecretTap,
            ),
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (version.downloaded) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = if (version.downloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(Modifier.padding(start = 6.dp).weight(1f)) {
                    Text(version.versionUri, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOf(version.language, version.sourceId, version.date)
                            .filter { it.isNotBlank() }
                            .joinToString("  |  "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (version.editionInfo.isNotBlank()) {
                Text(
                    version.editionInfo,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${formatCount(version.tokenLength)} words",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when (state) {
                    is DownloadState.Downloading -> {
                        val progress = if (state.totalBytes > 0) {
                            state.bytesRead.toFloat() / state.totalBytes.toFloat()
                        } else {
                            null
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (progress == null) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            } else {
                                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(0.35f))
                            }
                        }
                    }
                    is DownloadState.Error -> {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2,
                            )
                            TextButton(onClick = onDownload) { Text("Retry") }
                        }
                    }
                    DownloadState.Idle, DownloadState.Ready -> {
                        if (version.downloaded || state == DownloadState.Ready) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (version.downloaded) {
                                    TextButton(onClick = onDelete) { Text("Delete") }
                                }
                                Button(onClick = onRead) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Read")
                                }
                            }
                        } else {
                            OutlinedButton(onClick = onDownload) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Download")
                            }
                        }
                    }
                }
            }
        }
    }
}
