package com.itsnyoty.wikichanges.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.itsnyoty.wikichanges.data.api.RetrofitClient
import com.itsnyoty.wikichanges.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class WikipediaRepository private constructor(private val context: Context) {

    private val apiService = RetrofitClient.createService(context)

    companion object {
        @Volatile
        private var INSTANCE: WikipediaRepository? = null

        fun getInstance(context: Context): WikipediaRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WikipediaRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private object PreferencesKeys {
        val SELECTED_WIKI = stringPreferencesKey("selected_wiki")
        val WIKI_LIST = stringPreferencesKey("wiki_list")
        val USERNAME = stringPreferencesKey("username")
        val FILTERS = stringPreferencesKey("recent_changes_filters")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
    }

    private val gson = com.google.gson.Gson()

    val isDisclaimerAccepted: Flow<Boolean> = context.dataStore.data
        .map { it[PreferencesKeys.DISCLAIMER_ACCEPTED] ?: false }

    suspend fun setDisclaimerAccepted() {
        context.dataStore.edit { it[PreferencesKeys.DISCLAIMER_ACCEPTED] = true }
    }

    val selectedWiki: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_WIKI]
        }

    val wikiList: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WIKI_LIST]
        }

    val username: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USERNAME]
        }

    fun getAllWikisFlow(): Flow<List<WikiProject>> = context.dataStore.data.map { 
        val savedJson = it[PreferencesKeys.WIKI_LIST]
        if (savedJson.isNullOrBlank()) {
            DefaultWikiProjects
        } else {
            try {
                gson.fromJson(savedJson, Array<WikiProject>::class.java).toList()
            } catch (e: Exception) {
                DefaultWikiProjects
            }
        }
    }

    private val filtersJson: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.FILTERS]
        }

    suspend fun getFilters(): RecentChangesFilters {
        val json = filtersJson.first()
        return if (!json.isNullOrBlank()) {
            try {
                gson.fromJson(json, RecentChangesFilters::class.java)
            } catch (e: Exception) {
                RecentChangesFilters()
            }
        } else {
            RecentChangesFilters()
        }
    }

    suspend fun saveFilters(filters: RecentChangesFilters) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FILTERS] = gson.toJson(filters)
        }
    }

    suspend fun setSelectedWiki(wikiId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_WIKI] = wikiId
        }
    }

    suspend fun getSelectedWikiOrDefault(): WikiProject {
        val selectedId = selectedWiki.first() ?: "enwiki"
        return getAllWikis().find { it.id == selectedId } ?: getAllWikis().first()
    }

    suspend fun getAllWikis(): List<WikiProject> {
        val savedJson = wikiList.first()
        val loadedWikis = if (savedJson.isNullOrBlank()) {
            DefaultWikiProjects
        } else {
            try {
                val wikis = com.google.gson.Gson().fromJson(
                    savedJson,
                    Array<WikiProject>::class.java
                ).toList()
                if (wikis.isEmpty()) DefaultWikiProjects else wikis
            } catch (e: Exception) {
                DefaultWikiProjects
            }
        }

        // Migration: Update default wiki names/URLs if they changed in code
        return loadedWikis.map { loaded ->
            DefaultWikiProjects.find { it.id == loaded.id }?.let { default ->
                // If it's a default wiki, update its name/URL but keep custom fields if any (though currently there aren't many)
                loaded.copy(name = default.name, apiUrl = default.apiUrl, baseUrl = default.baseUrl)
            } ?: loaded
        }
    }

    suspend fun addWiki(wiki: WikiProject) {
        val current = getAllWikis().toMutableList()
        if (current.none { it.id == wiki.id }) {
            current.add(wiki)
            saveWikiList(current)
        }
    }

    suspend fun removeWiki(wikiId: String) {
        val current = getAllWikis().toMutableList()
        current.removeAll { it.id == wikiId }
        saveWikiList(current)
    }

    suspend fun saveWikiList(wikis: List<WikiProject>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIKI_LIST] = gson.toJson(wikis)
        }
    }

    suspend fun setUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USERNAME] = username
        }
    }

    suspend fun getRecentChanges(
        wiki: WikiProject,
        filters: RecentChangesFilters = RecentChangesFilters(),
        canReadPatrolMarks: Boolean = false,
        start: String? = null
    ): List<RecentChange> {
        val rcprop = buildString {
            append("user|title|timestamp|flags|comment|sizes|ids|parsedcomment")
            if (canReadPatrolMarks) append("|patrolled")
        }

        val rcshow = buildString {
            // Laat alleen anonieme bewerkingen zien
            if (filters.onlyAnon) {
                append("anon")
            }
            // Verberg bots
            if (filters.hideBots) {
                if (isNotEmpty()) append("|")
                append("!bot")
            }
            // Laat alleen ongecontroleerde bewerkingen zien (vereist patrolmarks-rechten)
            if (canReadPatrolMarks && filters.onlyUnpatrolled) {
                if (isNotEmpty()) append("|")
                append("unpatrolled")
            }
            // Verberg kleine bewerkingen
            if (filters.hideMinor) {
                if (isNotEmpty()) append("|")
                append("!minor")
            }
        }

        val rctype = if (filters.hideNewPages) "edit" else "edit|new"
        val rcdir = if (filters.sortNewestFirst) "older" else "newer"

        val response = apiService.getRecentChanges(
            url = wiki.apiUrl,
            rcprop = rcprop,
            rclimit = filters.limit,
            rcnamespace = filters.namespace.ifBlank { null },
            rcshow = rcshow.ifBlank { null },
            rctype = rctype,
            rcdir = rcdir,
            rcstart = start,
            curtimestamp = if (start == null) "1" else "0"
        )

        val changes = response.query.recentChanges
        
        // Optioneel: filter op user groups (zoals extendedconfirmed)
        if (filters.hideExtendedConfirmed && changes.isNotEmpty()) {
            val users = changes.mapNotNull { it.user }.distinct()
            // Batch users (MediaWiki limit is 50)
            val groupsMap = mutableMapOf<String, List<String>>()
            
            users.chunked(50).forEach { batch ->
                try {
                    val usersString = batch.joinToString("|")
                    val groupsResponse = apiService.getUsersGroups(url = wiki.apiUrl, users = usersString)
                    groupsResponse.query?.users?.forEach { userDetail ->
                        if (userDetail.name != null && userDetail.groups != null) {
                            groupsMap[userDetail.name] = userDetail.groups
                        }
                    }
                } catch (e: Exception) {
                    // Fout negeren
                }
            }
            
            return changes.filter { change ->
                val groups = groupsMap[change.user] ?: emptyList()
                !groups.contains("extendedconfirmed")
            }.map { it.copy(userGroups = groupsMap[it.user]) }
        }

        return changes
    }

    suspend fun getUsersGroups(wiki: WikiProject, users: List<String>): Map<String, List<String>> {
        val groupsMap = mutableMapOf<String, List<String>>()
        users.chunked(50).forEach { batch ->
            try {
                val usersString = batch.joinToString("|")
                val response = apiService.getUsersGroups(url = wiki.apiUrl, users = usersString)
                response.query?.users?.forEach { userDetail ->
                    if (userDetail.name != null && userDetail.groups != null) {
                        groupsMap[userDetail.name] = userDetail.groups
                    }
                }
            } catch (e: Exception) {
                // Fout negeren
            }
        }
        return groupsMap
    }

    suspend fun getTokens(wiki: WikiProject): Map<String, String> {
        val response = apiService.getTokens(wiki.apiUrl)
        return response.query?.tokens ?: emptyMap()
    }

    suspend fun getCurrentUserRights(wiki: WikiProject): List<String> {
        return try {
            val response = apiService.getUserRights(wiki.apiUrl)
            response.query?.userInfo?.rights ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDiffHtml(wiki: WikiProject, fromRevId: Long, toRevId: Long): String? {
        if (toRevId == 0L) return null
        if (fromRevId == 0L) {
            return getRevisionContentHtml(wiki, toRevId)
        }
        return try {
            val response = apiService.compareRevisions(
                url = wiki.apiUrl,
                fromRev = fromRevId,
                toRev = toRevId
            )
            response.compare?.body
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getRevisionContentHtml(wiki: WikiProject, revId: Long): String? {
        return try {
            val response = apiService.getRevisionContent(url = wiki.apiUrl, revIds = revId)
            val content = response.query?.pages?.values?.firstOrNull()?.revisions?.firstOrNull()?.slots?.get("main")?.content
            if (content != null) {
                // Simuleer een diff waarbij alles is toegevoegd (4-koloms structuur)
                val escapedContent = content
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                
                buildString {
                    append("<tr><td colspan=\"2\" class=\"diff-lineno\"></td><td colspan=\"2\" class=\"diff-lineno\">Regel 1:</td></tr>")
                    escapedContent.lines().forEach { line ->
                        append("<tr>")
                        // Linkerkolom (oud/leeg)
                        append("<td class=\"diff-marker\"></td><td class=\"diff-context\"></td>")
                        // Rechterkolom (nieuw/toegevoegd)
                        append("<td class=\"diff-marker\">+</td><td class=\"diff-addedline\"><div class=\"diff-content\">${line.ifBlank { "&nbsp;" }}</div></td>")
                        append("</tr>")
                    }
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCurrentUserGroups(wiki: WikiProject): List<String> {
        return try {
            val response = apiService.getUserRights(wiki.apiUrl)
            response.query?.userInfo?.groups ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun performRollback(
        wiki: WikiProject,
        title: String,
        user: String,
        changeId: Long,
        token: String,
        summary: String? = null
    ) {
        val response = apiService.rollback(
            url = wiki.apiUrl,
            title = title,
            user = user,
            id = changeId,
            token = token,
            summary = summary
        )
        if (response.error != null) {
            throw Exception(response.error.info ?: response.error.code ?: "Unknown rollback error")
        }
    }

    suspend fun warnUser(
        wiki: WikiProject,
        user: String,
        templateName: String?,
        customMessage: String?,
        reason: String,
        token: String
    ) {
        val userTalkPage = "User_talk:${(user ?: "").replace(" ", "_")}"
        
        val text = buildString {
            if (!templateName.isNullOrBlank()) {
                append("{{subst:${templateName}|${titleToSubject(wiki, reason)}}}")
            }
            if (!customMessage.isNullOrBlank()) {
                if (isNotEmpty()) append("\n\n")
                append(customMessage)
            }
            append(" ~~~~")
        }

        apiService.warnUser(
            url = wiki.apiUrl,
            title = userTalkPage,
            sectionTitle = "Waarschuwing / Warning",
            text = text,
            summary = "Waarschuwing geplaatst met WikiChanges",
            token = token
        )
    }

    suspend fun patrolChange(
        wiki: WikiProject,
        rcid: Long,
        token: String
    ) {
        apiService.patrol(wiki.apiUrl, rcid = rcid, token = token)
    }

    suspend fun thankUser(
        wiki: WikiProject,
        revId: Long,
        token: String
    ) {
        apiService.thankUser(wiki.apiUrl, id = revId, token = token)
    }

    suspend fun blockUser(
        wiki: WikiProject,
        user: String,
        expiry: String,
        reason: String,
        token: String,
        anonOnly: Boolean = false,
        noCreateAccount: Boolean = true,
        autoBlock: Boolean = true
    ) {
        val response = apiService.blockUser(
            url = wiki.apiUrl,
            user = user,
            expiry = expiry,
            reason = reason,
            token = token,
            anonOnly = anonOnly,
            createAccount = noCreateAccount,
            enableAutoBlock = autoBlock
        )
        if (response.error != null) {
            throw Exception(response.error.info ?: response.error.code ?: "Unknown block error")
        }
    }

    suspend fun deletePage(
        wiki: WikiProject,
        title: String,
        reason: String,
        token: String
    ) {
        val response = apiService.deletePage(
            url = wiki.apiUrl,
            title = title,
            reason = reason,
            token = token
        )
        if (response.error != null) {
            throw Exception(response.error.info ?: response.error.code ?: "Unknown delete error")
        }
    }

    suspend fun login(wiki: WikiProject, username: String, password: String): UiState<String> {
        return try {
            val tokenResponse = apiService.getLoginToken(wiki.apiUrl)
            val loginToken = tokenResponse.query?.tokens?.get("login")
                ?: return UiState.Error("Geen login token ontvangen")

            val result = apiService.login(
                url = wiki.apiUrl,
                username = username,
                password = password,
                loginToken = loginToken.ifBlank { "" }
            )

            if (result.login.result == "Success" || result.login.lgUsername?.isNotBlank() == true) {
                UiState.Success("Succesvol ingelogd als ${result.login.lgUsername ?: result.login.username}")
            } else {
                val reason = result.login.reason ?: result.login.result ?: "onbekend"
                UiState.Error("Inloggen mislukt: $reason")
            }
        } catch (e: Exception) {
            UiState.Error("Inloggen mislukt: ${e.localizedMessage}")
        }
    }

    fun logout() {
        RetrofitClient.clearCookies()
    }

    private fun titleToSubject(wiki: WikiProject, title: String?): String {
        return (title ?: "").replace(" ", "_").take(100)
    }
}
