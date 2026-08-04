package com.itsnyoty.wikichanges.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.itsnyoty.wikichanges.data.model.UiState
import com.itsnyoty.wikichanges.data.model.WikiProject
import com.itsnyoty.wikichanges.data.repository.WikipediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WikiSettingsUiState(
    val wikis: List<WikiProject> = emptyList(),
    val selectedWikiId: String = "",
    val username: String = "",
    val wikiRoles: Map<String, List<String>> = emptyMap()
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WikipediaRepository.getInstance(application)

    private val _uiState = MutableStateFlow(WikiSettingsUiState())
    val uiState: StateFlow<WikiSettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            loadSettings()
        }
    }

    suspend fun loadSettings() {
        val wikis = repository.getAllWikis()
        val selectedId = repository.selectedWiki.first()
        val user = repository.username.first()
        
        _uiState.value = WikiSettingsUiState(
            wikis = wikis,
            selectedWikiId = selectedId ?: wikis.firstOrNull { it.isDefault }?.id ?: "enwiki",
            username = user ?: ""
        )
        
        fetchRolesForAllWikis(wikis)
    }

    private fun fetchRolesForAllWikis(wikis: List<WikiProject>) {
        viewModelScope.launch {
            val rolesMap = mutableMapOf<String, List<String>>()
            wikis.forEach { wiki ->
                val roles = repository.getCurrentUserGroups(wiki)
                if (roles.isNotEmpty()) {
                    rolesMap[wiki.id] = roles
                }
            }
            _uiState.value = _uiState.value.copy(wikiRoles = rolesMap)
        }
    }

    fun setUsername(username: String) {
        viewModelScope.launch {
            repository.setUsername(username)
            _uiState.value = _uiState.value.copy(username = username)
        }
    }

    fun addWiki(wiki: WikiProject, selectImmediately: Boolean = true) {
        viewModelScope.launch {
            repository.addWiki(wiki)
            if (selectImmediately) {
                repository.setSelectedWiki(wiki.id)
            }
            loadSettings()
        }
    }

    fun removeWiki(wikiId: String) {
        viewModelScope.launch {
            repository.removeWiki(wikiId)
            loadSettings()
        }
    }

    fun updateWikiWarningTemplate(wikiId: String, template: String) {
        viewModelScope.launch {
            val wikis = repository.getAllWikis().map {
                if (it.id == wikiId) it.copy(warningTemplate = template) else it
            }
            repository.saveWikiList(wikis)
            loadSettings()
        }
    }

    fun logout(onResult: (UiState<String>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                com.itsnyoty.wikichanges.data.auth.OAuthManager.getInstance(getApplication()).clear()
                repository.logout()
                onResult(UiState.Success("Succesvol uitgelogd"))
            } catch (e: Exception) {
                onResult(UiState.Error("Uitloggen mislukt: ${e.localizedMessage}"))
            }
        }
    }
}
