package org.opennur.maktaba.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class OpenItiCatalogParserTest {
    @Test
    fun parsesCatalogRowsAndPreservesArabicFields() {
        val tsv = """
            version_uri	language	subcorpus	uncorrected_OCR	date	author_ar	author_lat	book	title_ar	title_lat	ed_info	id	status	tok_length	char_length	local_path	tags	author_from_uri	parts
            0100Author.Book.Source001-ara1	ara	ara	False	0100	المؤلف	Author	0100Author.Book	عنوان الكتاب	Book Title	edition	Source001	pri	1200	5000	data/0100Author/0100Author.Book/0100Author.Book.Source001-ara1	TAG	Author	
        """.trimIndent()

        val records = OpenItiCatalogParser.parse(StringReader(tsv)).toList()

        assertEquals(1, records.size)
        assertEquals("0100Author.Book.Source001-ara1", records.single().versionUri)
        assertEquals("عنوان الكتاب", records.single().titleArabic)
        assertEquals("0100Author.Book", records.single().bookUri)
        assertEquals(1200, records.single().tokenLength)
        assertTrue(!records.single().uncorrectedOcr)
    }

    @Test
    fun convertsMissingBookUriToVersionParent() {
        val tsv = "version_uri\tlanguage\tbook\n1000A.Title.Source-ara1\tara\n"

        val record = OpenItiCatalogParser.parse(StringReader(tsv)).single()

        assertEquals("1000A.Title", record.bookUri)
    }
}
