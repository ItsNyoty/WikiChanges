package com.itsnyoty.wikichanges.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.itsnyoty.wikichanges.R
import com.itsnyoty.wikichanges.data.auth.OAuthManager
import com.itsnyoty.wikichanges.data.model.*
import com.itsnyoty.wikichanges.data.repository.WikipediaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecentChangesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WikipediaRepository.getInstance(application)

    private val _uiState = MutableStateFlow<UiState<List<RecentChange>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<RecentChange>>> = _uiState.asStateFlow()

    private val _selectedWiki = MutableStateFlow<WikiProject?>(null)
    val selectedWiki: StateFlow<WikiProject?> = _selectedWiki.asStateFlow()

    private val _wikis = MutableStateFlow<List<WikiProject>>(emptyList())
    val wikis: StateFlow<List<WikiProject>> = _wikis.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<String>?>(null)
    val actionState: StateFlow<UiState<String>?> = _actionState.asStateFlow()

    private val _userRights = MutableStateFlow<List<String>>(emptyList())
    val userRights: StateFlow<List<String>> = _userRights.asStateFlow()

    private val _filters = MutableStateFlow(RecentChangesFilters())
    val filters: StateFlow<RecentChangesFilters> = _filters.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _diffState = MutableStateFlow<UiState<String>?>(null)
    val diffState: StateFlow<UiState<String>?> = _diffState.asStateFlow()

    private var currentOffset: String? = null
    private var isLoadingMore = false
    private var currentChanges = listOf<RecentChange>()

    init {
        viewModelScope.launch {
            // Zorg dat een opgeslagen access token uit DataStore in het geheugen staat
            // zodat alle API-aanroepen (vooral user rights) geautoriseerd zijn.
            OAuthManager.getInstance(getApplication()).loadTokenIntoMemory()

            _wikis.value = repository.getAllWikis()
            val wiki = repository.getSelectedWikiOrDefault()
            _selectedWiki.value = wiki
            _filters.value = repository.getFilters()
            loadRecentChanges()
        }
    }

    fun selectWiki(wiki: WikiProject) {
        viewModelScope.launch {
            repository.setSelectedWiki(wiki.id)
            _selectedWiki.value = wiki
            _userRights.value = emptyList()
            currentOffset = null
            currentChanges = emptyList()
            loadRecentChanges()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loadRecentChanges(isRefresh = true)
        }
    }

    fun updateFilters(update: RecentChangesFilters.() -> RecentChangesFilters) {
        val newFilters = _filters.value.update()
        _filters.value = newFilters
        viewModelScope.launch {
            repository.saveFilters(newFilters)
            currentOffset = null
            currentChanges = emptyList()
            loadRecentChanges()
        }
    }

    fun loadRecentChanges(loadMore: Boolean = false, isRefresh: Boolean = false) {
        if (isLoadingMore || selectedWiki.value == null) return

        viewModelScope.launch {
            isLoadingMore = true
            _isRefreshing.value = isRefresh

            // Bij een refresh wil je de bestaande lijst NIET vervangen door een spinner
            // maar wel een keer een spinner tonen bij eerste laad.
            if (!loadMore && !isRefresh && currentChanges.isEmpty()) {
                _uiState.value = UiState.Loading
            }

            try {
                val wiki = selectedWiki.value ?: repository.getSelectedWikiOrDefault()
                val rights = repository.getCurrentUserRights(wiki)
                _userRights.value = rights

                val changes = repository.getRecentChanges(
                    wiki = wiki,
                    filters = _filters.value,
                    canReadPatrolMarks = rights.contains("patrol"),
                    start = if (loadMore) currentOffset else null
                )

                val filteredChanges = changes.filter { !it.title.isNullOrBlank() && !it.user.isNullOrBlank() }

                currentChanges = when {
                    loadMore -> currentChanges + filteredChanges
                    isRefresh -> {
                        val existingIds = currentChanges.map { it.id }.toSet()
                        val newChanges = filteredChanges.filter { it.id !in existingIds }
                        if (newChanges.isNotEmpty()) newChanges + currentChanges else currentChanges
                    }
                    else -> filteredChanges
                }

                if (loadMore || !isRefresh) {
                    currentOffset = filteredChanges.lastOrNull()?.timestamp
                }

                _uiState.value = if (currentChanges.isEmpty()) {
                    UiState.Empty
                } else {
                    UiState.Success(currentChanges)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    message = e.localizedMessage ?: getApplication<Application>().getString(R.string.unknown_error),
                    errorCode = e.javaClass.simpleName
                )
            } finally {
                isLoadingMore = false
                _isRefreshing.value = false
            }
        }
    }

    fun canPatrol(): Boolean = _userRights.value.contains("patrol")
    fun canRollback(): Boolean = _userRights.value.contains("rollback")
    fun canBlock(): Boolean = _userRights.value.contains("block")
    fun canEdit(): Boolean = _userRights.value.contains("edit")

    fun loadDiff(change: RecentChange) {
        viewModelScope.launch {
            _diffState.value = UiState.Loading
            try {
                val wiki = selectedWiki.value ?: repository.getSelectedWikiOrDefault()
                val html = repository.getDiffHtml(
                    wiki = wiki,
                    fromRevId = change.lastRevid ?: 0L,
                    toRevId = change.oldRevid ?: 0L
                )
                _diffState.value = if (!html.isNullOrBlank()) {
                    UiState.Success(html)
                } else {
                    UiState.Error(getApplication<Application>().getString(R.string.diff_load_error, change.title ?: ""))
                }
            } catch (e: Exception) {
                _diffState.value = UiState.Error(
                    e.localizedMessage ?: getApplication<Application>().getString(R.string.unknown_error)
                )
            }
        }
    }

    fun clearDiffState() {
        _diffState.value = null
    }

    fun markAsGood(change: RecentChange) {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                val wiki = selectedWiki.value ?: repository.getSelectedWikiOrDefault()
                val tokens = repository.getTokens(wiki)
                val token = tokens["patroltoken"]?.takeIf { it.isNotBlank() }
                    ?: tokens["csrftoken"]?.takeIf { it.isNotBlank() }

                if (token != null && !change.isPatrolled()) {
                    repository.patrolChange(wiki, change.id, token)
                    _actionState.value = UiState.Success(
                        getApplication<Application>().getString(R.string.change_patrolled, change.title ?: "")
                    )
                    refresh()
                } else {
                    _actionState.value = UiState.Error(
                        if (change.isPatrolled()) {
                            getApplication<Application>().getString(R.string.already_patrolled)
                        } else {
                            getApplication<Application>().getString(R.string.patrol_no_token)
                        }
                    )
                }
            } catch (e: Exception) {
                _actionState.value = UiState.Error(
                    getApplication<Application>().getString(R.string.patrol_failed, e.localizedMessage ?: "")
                )
            }
        }
    }

    fun markAsBad(change: RecentChange, action: BadEditAction, reason: String = "") {
        viewModelScope.launch {
            _actionState.value = UiState.Loading
            try {
                val wiki = selectedWiki.value ?: repository.getSelectedWikiOrDefault()
                val tokens = repository.getTokens(wiki)

                val app = getApplication<Application>()
                val token = when (action) {
                    BadEditAction.ROLLBACK -> tokens["rollbacktoken"]?.takeIf { it.isNotBlank() }
                        ?: throw Exception(app.getString(R.string.rollback_no_token))
                    BadEditAction.BLOCK -> tokens["blocktoken"]?.takeIf { it.isNotBlank() }
                        ?: throw Exception(app.getString(R.string.block_no_token))
                    BadEditAction.WARNING -> tokens["csrftoken"]?.takeIf { it.isNotBlank() }
                        ?: throw Exception(app.getString(R.string.csrf_no_token))
                }

                when (action) {
                    BadEditAction.ROLLBACK -> {
                        repository.performRollback(
                            wiki = wiki,
                            title = change.title ?: "",
                            user = change.user ?: "",
                            changeId = change.id,
                            token = token,
                            summary = "Reverted with WikiChanges: ${reason}"
                        )
                        _actionState.value = UiState.Success(
                            app.getString(R.string.change_rollbacked, change.title ?: "")
                        )
                    }
                    BadEditAction.WARNING -> {
                        val template = when (wiki.id) {
                            "nlwiki" -> "Waarschuwing"
                            "enwiki" -> "uw-vandalism"
                            else -> "Warning"
                        }
                        repository.warnUser(
                            wiki = wiki,
                            user = change.user ?: "",
                            warningTemplate = template,
                            reason = reason.ifBlank { change.title ?: "" },
                            token = token
                        )
                        _actionState.value = UiState.Success(
                            app.getString(R.string.user_warned, change.user ?: "")
                        )
                    }
                    BadEditAction.BLOCK -> {
                        repository.blockUser(
                            wiki = wiki,
                            user = change.user ?: "",
                            expiry = "1 week",
                            reason = "Vandalism: $reason (WikiChanges)",
                            token = token
                        )
                        _actionState.value = UiState.Success(
                            app.getString(R.string.user_blocked, change.user ?: "")
                        )
                    }
                }
                refresh()
            } catch (e: Exception) {
                _actionState.value = UiState.Error(
                    getApplication<Application>().getString(R.string.action_failed, e.localizedMessage ?: "")
                )
            }
        }
    }

    fun clearActionState() {
        _actionState.value = null
    }
}

enum class BadEditAction {
    ROLLBACK,
    WARNING,
    BLOCK
}
