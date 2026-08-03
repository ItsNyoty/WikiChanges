package com.itsnyoty.wikichanges.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String, val errorCode: String? = null) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}

fun RecentChange.getByteDifference(): Int? {
    return sizeDiff ?: (newLength?.minus(oldLength ?: 0))
}

fun RecentChange.formatTimestamp(): String {
    if (timestamp.isBlank()) return ""
    
    val outputFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
    
    // We proberen de UTC timestamp te parsen en te converteren naar de lokale tijdzone
    val inputFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS"
    )

    for (format in inputFormats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            sdf.parse(timestamp)?.let { date ->
                return outputFormat.format(date)
            }
        } catch (e: Exception) {
            continue
        }
    }
    
    return timestamp
}

fun RecentChange.isBot(): Boolean = bot != null
fun RecentChange.isMinor(): Boolean = minor != null
fun RecentChange.isPatrolled(): Boolean = patrolled != null && unpatrolled == null
