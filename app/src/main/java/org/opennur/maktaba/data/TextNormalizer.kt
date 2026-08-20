package org.maktaba.app.data

import java.text.Normalizer

object TextNormalizer {
    private val ArabicMarks = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")

    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFC)
        .replace('\u0640'.toString(), "")
        .replace(ArabicMarks, "")
        .replace(Regex("[أإآٱ]"), "ا")
        .replace('ى', 'ي')
        .lowercase()

    fun toMatchQuery(value: String): String = normalize(value)
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" AND ") { token ->
            "\"${token.replace("\"", "\"\"")}\""
        }
}
