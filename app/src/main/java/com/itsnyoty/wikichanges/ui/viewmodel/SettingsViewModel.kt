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
    val username: String = ""
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
    }

    fun setUsername(username: String) {
        viewModelScope.launch {
            repository.setUsername(username)
            _uiState.value = _uiState.value.copy(username = username)
        }
    }

    fun addWiki(wiki: WikiProject) {
        viewModelScope.launch {
            repository.addWiki(wiki)
            loadSettings()
        }
    }

    fun removeWiki(wikiId: String) {
        viewModelScope.launch {
            repository.removeWiki(wikiId)
            loadSettings()
        }
    }

    fun logout(
        onResult: (UiState<String>) -> Unit = {}
    ) {
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
