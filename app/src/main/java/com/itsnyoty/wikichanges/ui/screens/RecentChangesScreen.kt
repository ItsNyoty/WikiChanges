package com.itsnyoty.wikichanges.ui.screens

import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itsnyoty.wikichanges.R
import com.itsnyoty.wikichanges.data.model.RecentChange
import com.itsnyoty.wikichanges.data.model.RecentChangesFilters
import com.itsnyoty.wikichanges.data.model.UiState
import com.itsnyoty.wikichanges.data.model.formatTimestamp
import com.itsnyoty.wikichanges.data.model.getByteDifference
import com.itsnyoty.wikichanges.data.model.isPatrolled
import com.itsnyoty.wikichanges.ui.theme.WikiGreen
import com.itsnyoty.wikichanges.ui.theme.WikiRed
import com.itsnyoty.wikichanges.ui.viewmodel.BadEditAction
import com.itsnyoty.wikichanges.ui.viewmodel.RecentChangesViewModel
import com.itsnyoty.wikichanges.ui.viewmodel.WikiChangesViewModelProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecentChangesScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToDeveloperSettings: () -> Unit,
    viewModel: RecentChangesViewModel = viewModel(factory = WikiChangesViewModelProvider.recentChangesFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedWiki by viewModel.selectedWiki.collectAsState()
    val wikis by viewModel.wikis.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val userRights by viewModel.userRights.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val newItemsCount by viewModel.newItemsCount.collectAsState()
    val diffState by viewModel.diffState.collectAsState()
    val activeDiffChange by viewModel.activeDiffChange.collectAsState()

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState()
    var showWikiMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showBadBottomSheet by remember { mutableStateOf<RecentChange?>(null) }
    var logoClicks by remember { mutableStateOf(0) }

    // Auto-refresh loop
    LaunchedEffect(filters.autoRefreshSeconds, selectedWiki) {
        if (filters.autoRefreshSeconds <= 0) return@LaunchedEffect
        while (isActive) {
            delay(filters.autoRefreshSeconds * 1000L)
            viewModel.refresh()
        }
    }

    // Auto-scroll to top when new items arrive if we are already near the top
    val firstVisibleItemIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val firstVisibleItemScrollOffset by remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }
    
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success && (filters.autoShowNewChanges || (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset < 100))) {
            // If we are at the top, or auto-show is enabled, stay/go to the top when new items arrive
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(actionState) {
        when (actionState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar((actionState as UiState.Success<String>).data)
                viewModel.clearActionState()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar((actionState as UiState.Error).message)
                viewModel.clearActionState()
            }
            else -> {}
        }
    }

    LaunchedEffect(newItemsCount) {
        if (newItemsCount > 0) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.new_changes_found, newItemsCount),
                actionLabel = context.getString(R.string.scroll_to_top),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                listState.animateScrollToItem(0)
            }
            viewModel.clearNewItemsCount()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_favicon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    logoClicks++
                                    if (logoClicks >= 10) {
                                        logoClicks = 0
                                        onNavigateToDeveloperSettings()
                                    }
                                }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.app_name))
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { showWikiMenu = true }) {
                            Text(selectedWiki?.code?.uppercase() ?: stringResource(R.string.wiki_selector))
                        }
                        DropdownMenu(
                            expanded = showWikiMenu,
                            onDismissRequest = { showWikiMenu = false }
                        ) {
                            wikis.forEach { wiki ->
                                DropdownMenuItem(
                                    text = { Text("${wiki.name} (${wiki.code})") },
                                    onClick = {
                                        viewModel.selectWiki(wiki)
                                        showWikiMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.filters)
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is UiState.Success) {
                FloatingActionButton(onClick = { viewModel.loadRecentChanges(loadMore = true) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.load_more))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (uiState) {
                    is UiState.Loading -> CircularProgressIndicator()
                    is UiState.Error -> ErrorView(
                        message = (uiState as UiState.Error).message,
                        onRetry = { viewModel.refresh() }
                    )
                    is UiState.Empty -> EmptyView()
                    is UiState.Success -> {
                        RecentChangesList(
                            changes = (uiState as UiState.Success<List<RecentChange>>).data,
                            userRights = userRights,
                            listState = listState,
                            onGood = { viewModel.markAsGood(it) },
                            onBad = { change, _, _ -> showBadBottomSheet = change },
                            onViewDiff = { change -> viewModel.loadDiff(change) },
                            onOpenWiki = { change ->
                                val url = viewModel.getDiffUrl(change)
                                val intent = CustomTabsIntent.Builder().build()
                                intent.launchUrl(context, android.net.Uri.parse(url))
                            }
                        )
                    }
                }
            }
        }
    }

    diffState?.let { state ->
        activeDiffChange?.let { change ->
            DiffDialog(
                state = state,
                change = change,
                userRights = userRights,
                actionState = actionState,
                onPatrol = { viewModel.markAsGood(change, autoNext = true) },
                onBadAction = { showBadBottomSheet = change },
                onNext = { viewModel.navigateToNext() },
                onPrevious = { viewModel.navigateToPrevious() },
                onDismiss = { viewModel.clearDiffState() },
                onClearAction = { viewModel.clearActionState() },
                onOpenWiki = {
                    val url = viewModel.getDiffUrl(change)
                    val intent = CustomTabsIntent.Builder().build()
                    intent.launchUrl(context, android.net.Uri.parse(url))
                }
            )
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState
        ) {
            FilterSheetContent(
                filters = filters,
                canReadPatrolMarks = userRights.contains("patrol"),
                onFilterChange = { viewModel.updateFilters { it } },
                onClose = { showFilterSheet = false }
            )
        }
    }

    showBadBottomSheet?.let { change ->
        BadActionBottomSheet(
            userRights = userRights,
            warningTemplates = selectedWiki?.warningTemplates ?: emptyMap(),
            onAction = { action, reason, rb, expiry, template, message ->
                viewModel.performBadAction(
                    change = change,
                    action = action,
                    reason = reason,
                    rollbackToo = rb,
                    expiry = expiry,
                    warningTemplate = template,
                    customMessage = message
                )
                showBadBottomSheet = null
            },
            onDismiss = { showBadBottomSheet = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheetContent(
    filters: RecentChangesFilters,
    canReadPatrolMarks: Boolean,
    onFilterChange: (RecentChangesFilters) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.filter_title), style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        }

        Spacer(Modifier.height(16.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = filters.onlyAnon,
                onClick = { onFilterChange(filters.copy(onlyAnon = !filters.onlyAnon)) },
                label = { Text(stringResource(R.string.filter_only_anon)) }
            )
            FilterChip(
                selected = filters.hideBots,
                onClick = { onFilterChange(filters.copy(hideBots = !filters.hideBots)) },
                label = { Text(stringResource(R.string.filter_hide_bots)) }
            )
            FilterChip(
                selected = filters.onlyUnpatrolled && canReadPatrolMarks,
                onClick = { onFilterChange(filters.copy(onlyUnpatrolled = !filters.onlyUnpatrolled)) },
                label = { Text(stringResource(R.string.filter_only_unpatrolled)) },
                enabled = canReadPatrolMarks
            )
            FilterChip(
                selected = filters.hideNewPages,
                onClick = { onFilterChange(filters.copy(hideNewPages = !filters.hideNewPages)) },
                label = { Text(stringResource(R.string.filter_hide_new_pages)) }
            )
            FilterChip(
                selected = filters.hideMinor,
                onClick = { onFilterChange(filters.copy(hideMinor = !filters.hideMinor)) },
                label = { Text(stringResource(R.string.filter_hide_minor)) }
            )
            FilterChip(
                selected = filters.hideExtendedConfirmed,
                onClick = { onFilterChange(filters.copy(hideExtendedConfirmed = !filters.hideExtendedConfirmed)) },
                label = { Text(stringResource(R.string.filter_hide_extended_confirmed)) }
            )
            FilterChip(
                selected = filters.autoShowNewChanges,
                onClick = { onFilterChange(filters.copy(autoShowNewChanges = !filters.autoShowNewChanges)) },
                label = { Text(stringResource(R.string.filter_auto_show)) }
            )
        }

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NamespaceDropdown(
                selected = filters.namespace,
                onSelect = { onFilterChange(filters.copy(namespace = it)) },
                modifier = Modifier.fillMaxWidth()
            )
            SortDropdown(
                newestFirst = filters.sortNewestFirst,
                onSelect = { onFilterChange(filters.copy(sortNewestFirst = it)) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberTextField(
                    value = filters.limit,
                    onValueChange = { onFilterChange(filters.copy(limit = it.coerceIn(1, 500))) },
                    label = stringResource(R.string.filter_max),
                    modifier = Modifier.weight(1f)
                )
                NumberTextField(
                    value = filters.autoRefreshSeconds,
                    onValueChange = { onFilterChange(filters.copy(autoRefreshSeconds = it.coerceAtLeast(0))) },
                    label = stringResource(R.string.filter_auto_refresh),
                    supportingText = stringResource(R.string.auto_refresh_off_hint),
                    modifier = Modifier.weight(1.5f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.apply))
        }
    }
}

@Composable
private fun NumberTextField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { it.toIntOrNull()?.let(onValueChange) },
        label = { Text(label, maxLines = 1) },
        supportingText = supportingText?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NamespaceDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "" to stringResource(R.string.ns_all),
        "0" to stringResource(R.string.ns_main),
        "1" to stringResource(R.string.ns_talk),
        "2" to stringResource(R.string.ns_user),
        "3" to stringResource(R.string.ns_user_talk),
        "4" to stringResource(R.string.ns_project),
        "5" to stringResource(R.string.ns_project_talk),
        "6" to stringResource(R.string.ns_file),
        "7" to stringResource(R.string.ns_file_talk),
        "8" to stringResource(R.string.ns_mediawiki),
        "9" to stringResource(R.string.ns_mediawiki_talk),
        "10" to stringResource(R.string.ns_template),
        "11" to stringResource(R.string.ns_template_talk),
        "12" to stringResource(R.string.ns_help),
        "13" to stringResource(R.string.ns_help_talk),
        "14" to stringResource(R.string.ns_category),
        "15" to stringResource(R.string.ns_category_talk),
        "100" to stringResource(R.string.ns_portal),
        "101" to stringResource(R.string.ns_portal_talk)
    )
    val label = options.find { it.first == selected }?.second ?: selected

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.filter_namespace)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortDropdown(
    newestFirst: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        true to stringResource(R.string.filter_sort_newest),
        false to stringResource(R.string.filter_sort_oldest)
    )
    val label = options.find { it.first == newestFirst }?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.filter_sort)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.error_loading), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.no_changes_found), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun RecentChangesList(
    changes: List<RecentChange>,
    userRights: List<String>,
    listState: LazyListState,
    onGood: (RecentChange) -> Unit,
    onBad: (RecentChange, BadEditAction, String) -> Unit,
    onViewDiff: (RecentChange) -> Unit,
    onOpenWiki: (RecentChange) -> Unit
) {
    LazyColumn(state = listState) {
        items(changes, key = { "${it.id}_${it.timestamp}" }) { change ->
            ChangeCard(
                change = change,
                userRights = userRights,
                onGood = onGood,
                onBad = onBad,
                onViewDiff = onViewDiff,
                onOpenWiki = onOpenWiki
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeCard(
    change: RecentChange,
    userRights: List<String>,
    onGood: (RecentChange) -> Unit,
    onBad: (RecentChange, BadEditAction, String) -> Unit,
    onViewDiff: (RecentChange) -> Unit,
    onOpenWiki: (RecentChange) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val canPatrol = userRights.contains("patrol")
    val canRollback = userRights.contains("rollback")
    val canEdit = userRights.contains("edit")

    val byteDiff = change.getByteDifference()
    val diffColor = when {
        byteDiff == null -> MaterialTheme.colorScheme.onSurface
        byteDiff > 0 -> WikiGreen
        byteDiff < 0 -> WikiRed
        else -> MaterialTheme.colorScheme.onSurface
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewDiff(change) }
                ) {
                    Text(
                        text = change.title?.replace("_", " ") ?: stringResource(R.string.unknown_page),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.user_colon, change.user ?: stringResource(R.string.unknown_user)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = change.formatTimestamp(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (change.type == "new") {
                    Surface(
                        color = WikiGreen.copy(alpha = 0.1f),
                        contentColor = WikiGreen,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.tag_new_page),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                if (byteDiff != null) {
                    Text(
                        text = if (byteDiff > 0) "+${byteDiff}" else byteDiff.toString(),
                        color = diffColor,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                IconButton(onClick = { onOpenWiki(change) }) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = stringResource(R.string.action_open_wiki)
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(R.string.filters)
                    )
                }
            }

            if (!change.comment.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = change.comment,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onViewDiff(change) }
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onGood(change) },
                            colors = ButtonDefaults.buttonColors(containerColor = WikiGreen),
                            modifier = Modifier.weight(1f),
                            enabled = canPatrol
                        ) {
                            Text(stringResource(R.string.action_good))
                        }
                        Button(
                            onClick = { onBad(change, BadEditAction.ROLLBACK, "") },
                            colors = ButtonDefaults.buttonColors(containerColor = WikiRed),
                            modifier = Modifier.weight(1f),
                            enabled = canRollback || canEdit
                        ) {
                            Text(stringResource(R.string.action_bad))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffDialog(
    state: UiState<String>,
    change: RecentChange,
    userRights: List<String>,
    actionState: UiState<String>? = null,
    onPatrol: () -> Unit,
    onBadAction: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit,
    onClearAction: () -> Unit = {},
    onOpenWiki: () -> Unit
) {
    val canPatrol = userRights.contains("patrol")
    val canRollback = userRights.contains("rollback")
    val canEdit = userRights.contains("edit")

    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(actionState) {
        when (actionState) {
            is UiState.Success -> {
                snackbarHostState.showSnackbar(actionState.data)
                onClearAction()
            }
            is UiState.Error -> {
                snackbarHostState.showSnackbar(actionState.message)
                onClearAction()
            }
            else -> {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header met acties en navigatie
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onPrevious) {
                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Vorig")
                            }
                            Text(
                                text = change.title?.replace("_", " ") ?: stringResource(R.string.unknown_page),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(onClick = onNext) {
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Volgend")
                            }
                            IconButton(onClick = onOpenWiki) {
                                Icon(Icons.Default.OpenInNew, contentDescription = stringResource(R.string.action_open_wiki))
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                            }
                        }
                        Divider()

                        // Diff Informatie Header (Wikipedia stijl)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                DiffHeaderColumn(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.diff_version_of, change.formatTimestamp()),
                                    user = change.user ?: stringResource(R.string.unknown_user)
                                )
                                Spacer(Modifier.width(16.dp))
                                DiffHeaderColumn(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.diff_current_version, "nu"),
                                    user = change.user ?: stringResource(R.string.unknown_user),
                                    isCurrent = true
                                )
                            }

                            if (!change.comment.isNullOrBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "(${change.comment})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (canPatrol && !change.isPatrolled()) {
                                    Button(
                                        onClick = onPatrol,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WikiGreen)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.action_good))
                                    }
                                }
                                
                                Button(
                                    onClick = onBadAction,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = canRollback || canEdit,
                                    colors = ButtonDefaults.buttonColors(containerColor = WikiRed)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.action_bad))
                                }
                            }
                        }
                        Divider()

                        // De eigenlijke diff (WebView)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            when (state) {
                                is UiState.Loading -> { /* Centered indicator is handled by parent Box */ }
                                is UiState.Error -> Text(
                                    state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                is UiState.Success -> {
                                    val isDark = isSystemInDarkTheme()
                                    val html = wrapDiffHtml(state.data, isDark)
                                    AndroidView(
                                        factory = { context ->
                                            WebView(context).apply {
                                                settings.javaScriptEnabled = false
                                                setBackgroundColor(0)
                                            }
                                        },
                                        update = { webView ->
                                            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                else -> {}
                            }
                        }
                    }

                    if (state is UiState.Loading) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun BadActionBottomSheet(
    userRights: List<String>,
    warningTemplates: Map<String, String>,
    onAction: (BadEditAction, String, Boolean, String, String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAction by remember { mutableStateOf<BadEditAction?>(null) }
    var reason by remember { mutableStateOf("") }
    var rollbackToo by remember { mutableStateOf(true) }
    var expiry by remember { mutableStateOf("1 week") }
    
    // Waarschuwing specifieke velden
    var selectedWarningType by remember { mutableStateOf("vandalism") }
    var customWarningMessage by remember { mutableStateOf("") }
    var expandedWarningMenu by remember { mutableStateOf(false) }

    val warningOptions = listOf(
        "vandalism" to stringResource(R.string.warning_vandalism),
        "nonsense" to stringResource(R.string.warning_nonsense),
        "spam" to stringResource(R.string.warning_spam),
        "blanking" to stringResource(R.string.warning_blanking),
        "bullying" to stringResource(R.string.warning_bullying),
        "editwar" to stringResource(R.string.warning_editwar),
        "blp" to stringResource(R.string.warning_blp),
        "copyvio" to stringResource(R.string.warning_copyvio),
        "disruption" to stringResource(R.string.warning_disruption),
        "attack" to stringResource(R.string.warning_attack),
        "custom" to stringResource(R.string.warning_custom)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Scrim
            Surface(
                modifier = Modifier.fillMaxSize().clickable { onDismiss() },
                color = Color.Black.copy(alpha = 0.4f)
            ) {}

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    if (selectedAction == null) {
                        Text(stringResource(R.string.choose_action), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))
                        
                        ActionItem(
                            title = stringResource(R.string.action_rollback),
                            icon = Icons.Default.History,
                            enabled = userRights.contains("rollback"),
                            onClick = { onAction(BadEditAction.ROLLBACK, "", false, "", null, null) }
                        )
                        ActionItem(
                            title = stringResource(R.string.action_warning),
                            icon = Icons.Default.Warning,
                            enabled = userRights.contains("edit"),
                            onClick = { selectedAction = BadEditAction.WARNING }
                        )
                        ActionItem(
                            title = stringResource(R.string.action_block),
                            icon = Icons.Default.Block,
                            enabled = userRights.contains("block"),
                            onClick = { selectedAction = BadEditAction.BLOCK }
                        )
                        ActionItem(
                            title = stringResource(R.string.action_delete),
                            icon = Icons.Default.Delete,
                            enabled = userRights.contains("delete"),
                            onClick = { selectedAction = BadEditAction.DELETE }
                        )
                    } else {
                        Text(
                            text = when(selectedAction) {
                                BadEditAction.WARNING -> stringResource(R.string.action_warning)
                                BadEditAction.BLOCK -> stringResource(R.string.action_block)
                                BadEditAction.DELETE -> stringResource(R.string.action_delete)
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        if (selectedAction == BadEditAction.WARNING) {
                            // Dropdown voor type waarschuwing
                            @OptIn(ExperimentalMaterial3Api::class)
                            ExposedDropdownMenuBox(
                                expanded = expandedWarningMenu,
                                onExpandedChange = { expandedWarningMenu = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = warningOptions.find { it.first == selectedWarningType }?.second ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.warning_type_label)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWarningMenu) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedWarningMenu,
                                    onDismissRequest = { expandedWarningMenu = false }
                                ) {
                                    warningOptions.forEach { (type, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                selectedWarningType = type
                                                expandedWarningMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = customWarningMessage,
                                onValueChange = { customWarningMessage = it },
                                label = { Text(stringResource(R.string.custom_message_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        } else {
                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                label = { Text(stringResource(R.string.reason_placeholder)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        if (selectedAction == BadEditAction.BLOCK) {
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.block_duration_label), style = MaterialTheme.typography.labelMedium)
                            val periods = listOf(
                                "31 hours", "1 day", "3 days", "1 week", "2 weeks", 
                                "1 month", "3 months", "6 months", "1 year", "infinite"
                            )
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                periods.forEach { p ->
                                    FilterChip(
                                        selected = expiry == p,
                                        onClick = { expiry = p },
                                        label = { Text(p) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }
                        }
                        
                        if (selectedAction != BadEditAction.DELETE) {
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = rollbackToo, onCheckedChange = { rollbackToo = it })
                                Text(stringResource(R.string.rollback_also_label), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { 
                                val template = if (selectedWarningType == "custom") null else warningTemplates[selectedWarningType]
                                onAction(
                                    selectedAction!!, 
                                    reason, 
                                    rollbackToo, 
                                    expiry, 
                                    template, 
                                    customWarningMessage.takeIf { it.isNotBlank() }
                                ) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = WikiRed)
                        ) {
                            Text(stringResource(R.string.execute))
                        }
                        TextButton(
                            onClick = { selectedAction = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.back))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}


@Composable
private fun ActionItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = color)
        if (!enabled) {
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.no_right_suffix), style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

@Composable
private fun DiffHeaderColumn(
    modifier: Modifier = Modifier,
    label: String,
    user: String,
    isCurrent: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = user,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DiffActionText(stringResource(R.string.diff_talk))
            Text("|", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))
            DiffActionText(stringResource(R.string.diff_contribs))
            Text("|", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))
            DiffActionText(stringResource(R.string.diff_block))
        }
    }
}

@Composable
private fun DiffActionText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 10.sp
    )
}

private fun wrapDiffHtml(body: String, isDark: Boolean): String {
    val background = if (isDark) "#1c1b1f" else "#ffffff"
    val foreground = if (isDark) "#e6e1e5" else "#1c1b1f"
    val deletedBg = if (isDark) "#4a3a00" else "#feeec8" // Geelachtig voor verwijderd (Wikipedia stijl)
    val addedBg = if (isDark) "#2a3a4a" else "#d8ecff"   // Blauwachtig voor toegevoegd (Wikipedia stijl)
    val deletedBorder = if (isDark) "#7a6a00" else "#fbdc8a"
    val addedBorder = if (isDark) "#3a5a7a" else "#a3d3ff"

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { font-family: sans-serif; margin: 0; background: $background; color: $foreground; line-height: 1.4; }
                .diff-wrapper { padding: 4px; }
                table.diff { border-collapse: separate; border-spacing: 4px; width: 100%; font-size: 11px; table-layout: fixed; margin: 0 auto; }
                
                td { 
                    padding: 6px; 
                    vertical-align: top; 
                    word-wrap: break-word; 
                    overflow-wrap: anywhere; 
                    border-radius: 4px; 
                    border: 1px solid transparent;
                }
                
                /* Target columns: markers (widths) and content (alignment) */
                .diff-marker { width: 10px; text-align: right; color: #888; font-weight: bold; }
                .diff-content { text-align: left !important; }
                .diff-context { background: transparent; color: #888; text-align: left !important; }
                
                .diff-deletedline { background: $deletedBg; border-color: $deletedBorder; }
                .diff-addedline { background: $addedBg; border-color: $addedBorder; }
                
                .diffchange { 
                    background: rgba(255,255,255,0.3); 
                    font-weight: bold; 
                }
                
                .diff-deletedline .diffchange { background: rgba(255, 100, 100, 0.15); text-decoration: line-through; }
                .diff-addedline .diffchange { background: rgba(100, 100, 255, 0.15); text-decoration: underline; }
                
                .diff-lineno { color: #888; font-size: 10px; padding: 4px 0; text-align: center !important; }
                
                .diff-ntitle, .diff-otitle { display: none; }
            </style>
        </head>
        <body>
            <div class="diff-wrapper">
                <table class="diff">$body</table>
            </div>
        </body>
        </html>
    """.trimIndent()
}
