package com.itsnyoty.wikichanges.data.model

data class RecentChangesFilters(
    val onlyAnon: Boolean = false,
    val hideBots: Boolean = true,
    val onlyUnpatrolled: Boolean = false,
    val hideNewPages: Boolean = false,
    val hideMinor: Boolean = false,
    val hideExtendedConfirmed: Boolean = false,
    val namespace: String = "",
    val limit: Int = 100,
    val sortNewestFirst: Boolean = true,
    val autoRefreshSeconds: Int = 5,
    val autoShowNewChanges: Boolean = false
)
