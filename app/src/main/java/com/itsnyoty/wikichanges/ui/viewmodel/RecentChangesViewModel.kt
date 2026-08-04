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

    private val _newItemsCount = MutableStateFlow(0)
    val newItemsCount: StateFlow<Int> = _newItemsCount.asStateFlow()

    private val _diffState = MutableStateFlow<UiState<String>?>(null)
    val diffState: StateFlow<UiState<String>?> = _diffState.asStateFlow()

    private val _activeDiffChange = MutableStateFlow<RecentChange?>(null)
    val activeDiffChange: StateFlow<RecentChange?> = _activeDiffChange.asStateFlow()

    private var currentOffset: String? = null
    private var isLoadingMore = false
    private var currentChanges = listOf<RecentChange>()

    init {
        viewModelScope.launch {
            OAuthManager.getInstance(getApplication()).loadTokenIntoMemory()
            
            // Observeer wikilijst wijzigingen
            repository.getAllWikisFlow().onEach { wikiList ->
                _wikis.value = wikiList
            }.launchIn(this)

            // Observeer geselecteerde wiki wijzigingen
            repository.selectedWiki.onEach { selectedId ->
                val wikis = repository.getAllWikis()
                val wiki = wikis.find { it.id == selectedId } ?: wikis.firstOrNull { it.isDefault } ?: wikis.firstOrNull()
                if (wiki != null && wiki.id != _selectedWiki.value?.id) {
                    _selectedWiki.value = wiki
                    _userRights.value = emptyList()
                    currentOffset = null
                    currentChanges = emptyList()
                    loadRecentChanges()
                }
            }.launchIn(this)

            _filters.value = repository.getFilters()
        }
    }

    fun selectWiki(wiki: WikiProject) {
        viewModelScope.launch {
            repository.setSelectedWiki(wiki.id)
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
        
        isLoadingMore = true
        _isRefreshing.value = isRefresh

        viewModelScope.launch {
            if (!loadMore && !isRefresh && currentChanges.isEmpty()) {
                _uiState.value = UiState.Loading
            }

            try {
                val wiki = selectedWiki.value ?: repository.getSelectedWikiOrDefault()
                
                // Fetch rights only if we don't have them yet or if it's a manual refresh
                if (_userRights.value.isEmpty() || !isRefresh) {
                    val rights = repository.getCurrentUserRights(wiki)
                    _userRights.value = rights
                }

                val changes = repository.getRecentChanges(
                    wiki = wiki,
                    filters = _filters.value,
                    canReadPatrolMarks = _userRights.value.contains("patrol"),
                    start = if (loadMore) currentOffset else null
                )

                val filteredChanges = changes.filter { !it.title.isNullOrBlank() && !it.user.isNullOrBlank() }

                currentChanges = when {
                    loadMore -> {
                        // Append to bottom
                        val existingIds = currentChanges.map { it.id }.toSet()
                        currentChanges + filteredChanges.filter { it.id !in existingIds }
                    }
                    isRefresh -> {
                        // Merge with top, update existing items
                        val newItemsMap = filteredChanges.associateBy { it.id }
                        val trulyNew = filteredChanges.filter { new -> currentChanges.none { it.id == new.id } }
                        
                        if (trulyNew.isNotEmpty()) {
                            _newItemsCount.value = trulyNew.size
                        }

                        // Update existing items if they are in the latest fetch, otherwise keep them
                        val updatedCurrent = currentChanges.map { existing ->
                            newItemsMap[existing.id] ?: existing
                        }
                        
                        trulyNew + updatedCurrent
                    }
                    else -> filteredChanges
                }

                // Update offset for pagination (always tracks the end of the loaded list)
                if (loadMore || !isRefresh || currentOffset == null) {
                    currentOffset = currentChanges.lastOrNull()?.timestamp
                }

                _uiState.value = if (currentChanges.isEmpty()) {
                    UiState.Empty
                } else {
                    // Force a new list instance to ensure StateFlow observers are notified
                    UiState.Success(currentChanges.toList())
                }
            } catch (e: Exception) {
                if (currentChanges.isEmpty()) {
                    _uiState.value = UiState.Error(
                        message = e.localizedMessage ?: getApplication<Application>().getString(R.string.unknown_error),
                        errorCode = e.javaClass.simpleName
                    )
                }
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
            _activeDiffChange.value = change
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
        _activeDiffChange.value = null
    }

    fun getDiffUrl(change: RecentChange): String {
        val wiki = selectedWiki.value ?: DefaultWikiProjects.first()
        return if (change.oldRevid != null && change.oldRevid != 0L) {
            "${wiki.baseUrl}/w/index.php?diff=${change.oldRevid}"
        } else {
            "${wiki.baseUrl}/wiki/${change.title?.replace(" ", "_") ?: ""}"
        }
    }

    fun navigateToNext() {
        val current = _activeDiffChange.value ?: return
        val list = currentChanges
        val index = list.indexOfFirst { it.id == current.id }
        if (index >= 0 && index < list.size - 1) {
            loadDiff(list[index + 1])
        } else if (index == list.size - 1) {
            // Load more if we are at the end
            loadRecentChanges(loadMore = true)
        }
    }

    fun navigateToPrevious() {
        val current = _activeDiffChange.value ?: return
        val list = currentChanges
        val index = list.indexOfFirst { it.id == current.id }
        if (index > 0) {
            loadDiff(list[index - 1])
        }
    }

    fun markAsGood(change: RecentChange, autoNext: Boolean = false) {
        viewModelScope.launch {
            if (com.itsnyoty.wikichanges.data.model.DebugSettings.isDryModeEnabled.value) {
                _actionState.value = UiState.Success("Dry mode: Mark as good simulated for ${change.title}")
                if (autoNext) navigateToNext() else refresh()
                return@launch
            }

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
                    
                    if (autoNext) {
                        navigateToNext()
                    } else {
                        refresh()
                    }
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

    fun performBadAction(
        change: RecentChange,
        action: BadEditAction,
        reason: String,
        rollbackToo: Boolean = false,
        expiry: String = "1 week"
    ) {
        viewModelScope.launch {
            if (com.itsnyoty.wikichanges.data.model.DebugSettings.isDryModeEnabled.value) {
                _actionState.value = UiState.Success("Dry mode: $action simulated for ${change.title}")
                refresh()
                return@launch
            }

            _actionState.value = UiState.Loading
            try {
                val wiki = selectedWiki.value ?: repository.getSelectedWikiOrDefault()
                val tokens = repository.getTokens(wiki)
                val app = getApplication<Application>()

                if ((rollbackToo || action == BadEditAction.ROLLBACK) && action != BadEditAction.DELETE) {
                    val rbToken = tokens["rollbacktoken"]?.takeIf { it.isNotBlank() }
                        ?: throw Exception(app.getString(R.string.rollback_no_token))
                    repository.performRollback(
                        wiki = wiki,
                        title = change.title ?: "",
                        user = change.user ?: "",
                        changeId = change.id,
                        token = rbToken,
                        summary = "Reverted with WikiChanges: ${reason}"
                    )
                }

                val csrfToken = tokens["csrftoken"]?.takeIf { it.isNotBlank() }
                
                when (action) {
                    BadEditAction.WARNING -> {
                        val token = csrfToken ?: throw Exception(app.getString(R.string.csrf_no_token))
                        repository.warnUser(
                            wiki = wiki,
                            user = change.user ?: "",
                            warningTemplate = wiki.warningTemplate ?: "Warning",
                            reason = reason.ifBlank { change.title ?: "" },
                            token = token
                        )
                        _actionState.value = UiState.Success(app.getString(R.string.user_warned, change.user ?: ""))
                    }
                    BadEditAction.BLOCK -> {
                        val token = csrfToken ?: throw Exception(app.getString(R.string.csrf_no_token))
                        repository.blockUser(
                            wiki = wiki,
                            user = change.user ?: "",
                            expiry = expiry,
                            reason = "Vandalism: $reason (WikiChanges)",
                            token = token
                        )
                        _actionState.value = UiState.Success(app.getString(R.string.user_blocked, change.user ?: ""))
                    }
                    BadEditAction.ROLLBACK -> {
                        if (!rollbackToo) { // If already done above, skip
                            _actionState.value = UiState.Success(app.getString(R.string.change_rollbacked, change.title ?: ""))
                        }
                    }
                    BadEditAction.DELETE -> {
                        val token = csrfToken ?: throw Exception(app.getString(R.string.csrf_no_token))
                        repository.deletePage(
                            wiki = wiki,
                            title = change.title ?: "",
                            reason = reason.ifBlank { "Vandalism/unsuitable content (WikiChanges)" },
                            token = token
                        )
                        _actionState.value = UiState.Success(app.getString(R.string.page_deleted, change.title ?: ""))
                    }
                }
                refresh()
            } catch (e: Exception) {
                val app = getApplication<Application>()
                _actionState.value = UiState.Error(
                    app.getString(R.string.action_failed, e.localizedMessage ?: "")
                )
            }
        }
    }

    fun clearActionState() {
        _actionState.value = null
    }

    fun clearNewItemsCount() {
        _newItemsCount.value = 0
    }
}

enum class BadEditAction {
    ROLLBACK,
    WARNING,
    BLOCK,
    DELETE
}
