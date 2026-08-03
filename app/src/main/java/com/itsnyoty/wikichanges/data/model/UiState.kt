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
    val cleanTimestamp = timestamp.replace("Z", "")
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    val outputFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    for (fmt in formats) {
        try {
            val inputFormat = SimpleDateFormat(fmt, Locale.US)
            inputFormat.parse(cleanTimestamp)?.let { date ->
                return outputFormat.format(date)
            }
        } catch (e: Exception) {
            // probeer volgend formaat
        }
    }
    return timestamp
}

fun RecentChange.isBot(): Boolean = bot != null
fun RecentChange.isMinor(): Boolean = minor != null
fun RecentChange.isPatrolled(): Boolean = patrolled != null && unpatrolled == null
