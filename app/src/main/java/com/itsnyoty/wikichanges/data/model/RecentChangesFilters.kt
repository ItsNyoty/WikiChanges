package com.itsnyoty.wikichanges.data.model

data class RecentChangesFilters(
    val onlyAnon: Boolean = false,
    val hideBots: Boolean = true,
    val onlyUnpatrolled: Boolean = true,
    val hideNewPages: Boolean = false,
    val hideMinor: Boolean = false,
    val namespace: String = "0",
    val limit: Int = 100,
    val sortNewestFirst: Boolean = true,
    val autoRefreshSeconds: Int = 5
)

val NamespaceOptions = listOf(
    "0" to "Hoofdnaamruimte (0)",
    "1" to "Overleg (1)",
    "2" to "Gebruiker (2)",
    "3" to "Gebruikersoverleg (3)",
    "4" to "Wikipedia/Project (4)",
    "5" to "Overleg Wikipedia (5)",
    "6" to "Bestand (6)",
    "7" to "Bestandsoverleg (7)"
)
