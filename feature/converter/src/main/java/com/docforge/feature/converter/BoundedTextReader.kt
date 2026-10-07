package com.docforge.feature.converter

import java.io.Reader

internal const val MAX_TEXT_DOCUMENT_CHARS = 4 * 1024 * 1024

internal fun Reader.readTextBounded(maxChars: Int = MAX_TEXT_DOCUMENT_CHARS): String {
    require(maxChars > 0) { "Text document character limit must be positive." }
    val result = StringBuilder(minOf(maxChars, 8192))
    val buffer = CharArray(8192)
    var total = 0
    while (true) {
        val read = read(buffer, 0, minOf(buffer.size, maxChars - total + 1))
        if (read < 0) break
        if (read == 0) continue
        total += read
        require(total <= maxChars) { "Text document exceeds the $maxChars character safety limit." }
        result.append(buffer, 0, read)
    }
    return result.toString()
}

internal fun Reader.readLinesBounded(maxChars: Int = MAX_TEXT_DOCUMENT_CHARS): List<String> =
    readTextBounded(maxChars).reader().readLines()
