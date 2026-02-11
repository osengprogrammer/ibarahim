package com.example.crashcourse.utils

/**
 * 📊 Standardized result for Bulk & Single processing.
 */
data class ProcessResult(
    val studentId: String,
    val name: String,
    val className: String = "",
    val status: String,
    val isSuccess: Boolean = false,
    val photoSize: Long = 0,
    val error: String? = null
)