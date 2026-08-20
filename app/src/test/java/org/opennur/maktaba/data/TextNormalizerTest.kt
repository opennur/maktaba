package org.opennur.maktaba.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    @Test
    fun normalizesArabicDiacriticsAndAlefVariants() {
        assertEquals("الكتاب", TextNormalizer.normalize("ٱلْكِتَاب"))
    }

    @Test
    fun createsAnAndQueryForMultipleTerms() {
        assertEquals("\"كتاب\" AND \"علم\"", TextNormalizer.toMatchQuery("كتاب علم"))
    }
}
