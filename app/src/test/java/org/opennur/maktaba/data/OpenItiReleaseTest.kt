package org.opennur.maktaba.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenItiReleaseTest {
    @Test
    fun triesOpenItiBarePathBeforeLegacySuffixes() {
        val path = "data/0100Author/0100Author.Book/0100Author.Book.Source001-ara1"

        assertEquals(
            listOf(
                path,
                "$path.mARkdown",
                "$path.completed",
                "$path.inProgress",
            ),
            OpenItiRelease.contentCandidates(path),
        )
    }

    @Test
    fun doesNotAppendSuffixToExplicitExtension() {
        val path = "data/0001Quran/book.mARkdown"

        assertEquals(listOf(path), OpenItiRelease.contentCandidates(path))
    }

    @Test
    fun createsSafeExportNameWithoutExtension() {
        assertEquals(
            "0100Author.Book.Source001-ara1",
            OpenItiRelease.exportFileName("0100Author.Book.Source001-ara1"),
        )
    }
}
