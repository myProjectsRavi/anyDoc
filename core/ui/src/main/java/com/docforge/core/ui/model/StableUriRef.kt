package com.docforge.core.ui.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
data class StableUriRef(
    val encoded: String
) {
    fun toUri(): Uri = Uri.parse(encoded)

    companion object {
        fun from(uri: Uri): StableUriRef = StableUriRef(uri.toString())
    }
}

fun Uri.toStableUriRef(): StableUriRef = StableUriRef.from(this)

fun List<Uri>.toStableUriRefList(): ImmutableList<StableUriRef> =
    if (isEmpty()) {
        persistentListOf()
    } else {
        map { it.toStableUriRef() }.toPersistentList()
    }

fun Iterable<StableUriRef>.toUriList(): List<Uri> = map { it.toUri() }

