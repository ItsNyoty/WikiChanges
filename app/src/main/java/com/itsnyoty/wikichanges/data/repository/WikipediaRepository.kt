package com.itsnyoty.wikichanges.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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

    private val apiService = RetrofitClient.createService()

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
    }

    private val gson = com.google.gson.Gson()

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
        return if (savedJson.isNullOrBlank()) {
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

    private suspend fun saveWikiList(wikis: List<WikiProject>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIKI_LIST] = com.google.gson.Gson().toJson(wikis)
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
            rcnamespace = filters.namespace,
            rcshow = rcshow.ifBlank { null },
            rctype = rctype,
            rcdir = rcdir,
            rcstart = start
        )

        return response.query.recentChanges
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
        apiService.rollback(
            url = wiki.apiUrl,
            title = title,
            user = user,
            id = changeId,
            token = token,
            summary = summary
        )
    }

    suspend fun warnUser(
        wiki: WikiProject,
        user: String,
        warningTemplate: String,
        reason: String,
        token: String
    ) {
        val userTalkPage = when (wiki.id) {
            "nlwiki" -> "Gebruikersoverleg:${user.replace(" ", "_")}"
            else -> "User_talk:${user.replace(" ", "_")}"
        }
        val template = when (wiki.id) {
            "nlwiki" -> "Waarschuwing"
            "enwiki" -> "uw-vandalism"
            else -> warningTemplate
        }
        val text = "{{subst:${template}|${titleToSubject(wiki, reason)}}} ~~~~"

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
        apiService.blockUser(
            url = wiki.apiUrl,
            user = user,
            expiry = expiry,
            reason = reason,
            token = token,
            anonOnly = anonOnly,
            createAccount = noCreateAccount,
            enableAutoBlock = autoBlock
        )
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

    private fun titleToSubject(wiki: WikiProject, title: String): String {
        return title.replace(" ", "_").take(100)
    }
}
