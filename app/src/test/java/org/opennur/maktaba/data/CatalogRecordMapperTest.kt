package org.opennur.maktaba.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogRecordMapperTest {
    @Test
    fun mapsEveryCatalogFieldToBookVersionEntity() {
        val record = CatalogRecord(
            versionUri = "0100Author.Book.Source001-ara1",
            language = "ara",
            subcorpus = "ara",
            uncorrectedOcr = true,
            date = "0100",
            authorArabic = "المؤلف",
            authorLatin = "Author",
            bookUri = "0100Author.Book",
            titleArabic = "عنوان الكتاب",
            titleLatin = "Book Title",
            editionInfo = "edition",
            sourceId = "Source001",
            status = "pri",
            tokenLength = 1200,
            characterLength = 5000,
            localPath = "data/book",
            tags = "TAG",
            authorFromUri = "Author",
            parts = "part1",
        )

        val entity = record.toEntity()

        assertEquals(record.versionUri, entity.versionUri)
        assertEquals(record.bookUri, entity.bookUri)
        assertEquals(record.language, entity.language)
        assertEquals(record.uncorrectedOcr, entity.uncorrectedOcr)
        assertEquals(record.titleArabic, entity.titleArabic)
        assertEquals(record.titleLatin, entity.titleLatin)
        assertEquals(record.editionInfo, entity.editionInfo)
        assertEquals(record.tokenLength, entity.tokenLength)
        assertEquals(record.characterLength, entity.characterLength)
        assertEquals(record.localPath, entity.localPath)
        assertEquals(record.parts, entity.parts)
    }
}
