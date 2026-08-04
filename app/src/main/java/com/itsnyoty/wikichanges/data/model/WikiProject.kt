package com.itsnyoty.wikichanges.data.model

data class WikiProject(
    val id: String,
    val name: String,
    val code: String,
    val baseUrl: String,
    val apiUrl: String,
    val isDefault: Boolean = false,
    val warningTemplate: String? = null, // Voor achterwaartse compatibiliteit
    val warningTemplates: Map<String, String> = emptyMap()
)

val DefaultNlWikiTemplates = mapOf(
    "vandalism" to "ws-vandalisme",
    "nonsense" to "ws-nonsense",
    "spam" to "ws-reclame",
    "blanking" to "ws-leeghalen",
    "bullying" to "ws-cyberpesten",
    "editwar" to "ws-bwo",
    "blp" to "ws-blp",
    "copyvio" to "ws-copyvio",
    "disruption" to "ws-verstoring",
    "attack" to "ws-aanval"
)

val DefaultEnWikiTemplates = mapOf(
    "vandalism" to "uw-vandalism1",
    "nonsense" to "uw-nonsense1",
    "spam" to "uw-spam1",
    "blanking" to "uw-delete1",
    "bullying" to "uw-harass1",
    "editwar" to "uw-ew1",
    "blp" to "uw-biog1",
    "copyvio" to "uw-copyright1",
    "disruption" to "uw-disruptive1",
    "attack" to "uw-npa1"
)

fun getDefaultTemplatesForCode(code: String): Map<String, String> {
    return when (code.lowercase()) {
        "nl" -> DefaultNlWikiTemplates
        "en" -> DefaultEnWikiTemplates
        else -> emptyMap()
    }
}

val DefaultWikiProjects = listOf(
    WikiProject(
        id = "nlwiki",
        name = "Nederlandstalige Wikipedia",
        code = "nl",
        baseUrl = "https://nl.wikipedia.org",
        apiUrl = "https://nl.wikipedia.org/w/api.php",
        isDefault = true,
        warningTemplate = "Waarschuwing",
        warningTemplates = DefaultNlWikiTemplates
    ),
    WikiProject(
        id = "enwiki",
        name = "English Wikipedia",
        code = "en",
        baseUrl = "https://en.wikipedia.org",
        apiUrl = "https://en.wikipedia.org/w/api.php",
        isDefault = true,
        warningTemplate = "uw-vandalism",
        warningTemplates = DefaultEnWikiTemplates
    )
)
