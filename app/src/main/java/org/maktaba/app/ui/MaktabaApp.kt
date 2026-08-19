package org.maktaba.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.maktaba.app.MaktabaViewModel

private sealed interface Destination {
    data object Home : Destination
    data class Book(val bookUri: String) : Destination
    data class Reader(val versionUri: String) : Destination
}

private enum class HomeTab {
    CATALOG,
    LIBRARY,
}

@Composable
fun MaktabaApp(viewModel: MaktabaViewModel) {
    var destination by remember { mutableStateOf<Destination>(Destination.Home) }
    var homeTab by remember { mutableStateOf(HomeTab.CATALOG) }

    BackHandler(enabled = destination !is Destination.Home) {
        destination = when (destination) {
            is Destination.Reader -> Destination.Book((destination as Destination.Reader).versionUri.substringBeforeLast('.'))
            is Destination.Book -> Destination.Home
            Destination.Home -> Destination.Home
        }
    }

    when (val current = destination) {
        Destination.Home -> {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = homeTab == HomeTab.CATALOG,
                            onClick = { homeTab = HomeTab.CATALOG },
                            icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Catalog") },
                            label = { Text("Catalog") },
                        )
                        NavigationBarItem(
                            selected = homeTab == HomeTab.LIBRARY,
                            onClick = { homeTab = HomeTab.LIBRARY },
                            icon = { Icon(Icons.Default.Bookmarks, contentDescription = "Library") },
                            label = { Text("Library") },
                        )
                    }
                },
            ) { paddingValues ->
                when (homeTab) {
                    HomeTab.CATALOG -> CatalogScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues),
                        onBookClick = { destination = Destination.Book(it) },
                    )
                    HomeTab.LIBRARY -> LibraryScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(paddingValues),
                        onBookClick = { destination = Destination.Book(it) },
                    )
                }
            }
        }
        is Destination.Book -> BookDetailScreen(
            viewModel = viewModel,
            bookUri = current.bookUri,
            onBack = { destination = Destination.Home },
            onRead = { destination = Destination.Reader(it) },
        )
        is Destination.Reader -> ReaderScreen(
            viewModel = viewModel,
            versionUri = current.versionUri,
            onBack = { destination = Destination.Book(current.versionUri.substringBeforeLast('.')) },
        )
    }
}
