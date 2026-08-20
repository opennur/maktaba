package org.opennur.maktaba.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenItiMarkdownParserTest {
    @Test
    fun parsesMetadataHeadingsParagraphsAndPages() = runBlocking {
        val text = """
            ######OpenITI#
            #META# title
            #META#Header#End#
            ### | First chapter
            # first paragraph
            ~~continued on the next line

            PageV00P001
            ### || A subsection
            # second paragraph
            Milestone300
        """.trimIndent()

        val blocks = OpenItiMarkdownParser.parseToList(text)

        assertEquals(5, blocks.size)
        assertEquals(BlockKinds.HEADING, blocks[0].kind)
        assertEquals(1, blocks[0].depth)
        assertEquals("First chapter", blocks[0].title)
        assertEquals("first paragraph continued on the next line", blocks[1].text)
        assertEquals(BlockKinds.PAGE, blocks[2].kind)
        assertEquals("PageV00P001", blocks[2].pageLabel)
        assertEquals(2, blocks[3].depth)
        assertTrue(blocks[4].text.contains("second paragraph"))
    }

    @Test
    fun ignoresStructuralAnnotationsWithoutDroppingText() = runBlocking {
        val text = """
            ######OpenITI#
            #META#Header#End#
            #~:sermon:
            # actual text
        """.trimIndent()

        val blocks = OpenItiMarkdownParser.parseToList(text)

        assertEquals(1, blocks.size)
        assertEquals("actual text", blocks.single().text)
    }
}
