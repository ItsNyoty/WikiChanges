package com.itsnyoty.wikichanges.data.model

data class WikiProject(
    val id: String,
    val name: String,
    val code: String,
    val baseUrl: String,
    val apiUrl: String,
    val isDefault: Boolean = false,
    val warningTemplate: String? = null
)

val DefaultWikiProjects = listOf(
    WikiProject(
        id = "nlwiki",
        name = "Nederlandstalige Wikipedia",
        code = "nl",
        baseUrl = "https://nl.wikipedia.org",
        apiUrl = "https://nl.wikipedia.org/w/api.php",
        isDefault = true,
        warningTemplate = "Waarschuwing"
    ),
    WikiProject(
        id = "enwiki",
        name = "English Wikipedia",
        code = "en",
        baseUrl = "https://en.wikipedia.org",
        apiUrl = "https://en.wikipedia.org/w/api.php",
        isDefault = true,
        warningTemplate = "uw-vandalism"
    )
)
