package com.docforge.core.domain.model

data class ConversionRecord(
    val id: Long = 0,
    val sourceLabel: String,
    val outputPath: String,
    val outputUri: String? = null,
    val displayName: String? = null,
    val operation: String,
    val createdAtMillis: Long,
    val inputCount: Int,
    val outputSizeBytes: Long
)
