package com.itsnyoty.wikichanges.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itsnyoty.wikichanges.R
import com.itsnyoty.wikichanges.data.model.*
import com.itsnyoty.wikichanges.ui.viewmodel.BadEditAction
import com.itsnyoty.wikichanges.ui.viewmodel.RecentChangesViewModel
import com.itsnyoty.wikichanges.ui.viewmodel.WikiChangesViewModelProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecentChangesScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: RecentChangesViewModel = viewModel(factory = WikiChangesViewModelProvider.recentChangesFactory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedWiki by viewModel.selectedWiki.collectAsState()
    val wikis by viewModel.wikis.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val userRights by viewModel.userRights.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showWikiMenu by remember { mutableStateOf(false) }

    // Auto-refresh loop
    LaunchedEffect(filters.autoRefreshSeconds) {
        if (filters.autoRefreshSeconds <= 0) return@LaunchedEffect
        while (isActive) {
            delay(filters.autoRefreshSeconds * 1000L)
            viewModel.refresh()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_favicon),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("WikiChanges")
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { showWikiMenu = true }) {
                            Text(selectedWiki?.code?.uppercase() ?: "WIKI")
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
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Vernieuwen")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Instellingen")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is UiState.Success) {
                FloatingActionButton(onClick = { viewModel.loadRecentChanges(loadMore = true) }) {
                    Icon(Icons.Default.Add, contentDescription = "Meer laden")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val canPatrol = userRights.contains("patrol")

            FilterPanel(
                filters = filters,
                canReadPatrolMarks = canPatrol,
                onFilterChange = { newFilters ->
                    viewModel.updateFilters { newFilters }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Box(modifier = Modifier.weight(1f)) {
                when (uiState) {
                    is UiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    is UiState.Error -> ErrorView(
                        message = (uiState as UiState.Error).message,
                        onRetry = { viewModel.refresh() }
                    )
                    is UiState.Empty -> EmptyView()
                    is UiState.Success -> {
                        RecentChangesList(
                            changes = (uiState as UiState.Success<List<RecentChange>>).data,
                            userRights = userRights,
                            onGood = { viewModel.markAsGood(it) },
                            onBad = { change, action, reason ->
                                viewModel.markAsBad(change, action, reason)
                            },
                            onViewDiff = { change ->
                                val wiki = selectedWiki
                                val revId = change.oldRevid
                                if (wiki != null && revId != null) {
                                    val url = "${wiki.baseUrl}/w/index.php?diff=prev&oldid=$revId"
                                    CustomTabsIntent.Builder().build()
                                        .launchUrl(context, Uri.parse(url))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(
    filters: RecentChangesFilters,
    canReadPatrolMarks: Boolean,
    onFilterChange: (RecentChangesFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = filters.onlyAnon,
                onClick = { onFilterChange(filters.copy(onlyAnon = !filters.onlyAnon)) },
                label = { Text("Alleen anoniemen") }
            )
            FilterChip(
                selected = filters.hideBots,
                onClick = { onFilterChange(filters.copy(hideBots = !filters.hideBots)) },
                label = { Text("Verberg bots") }
            )
            FilterChip(
                selected = filters.onlyUnpatrolled && canReadPatrolMarks,
                onClick = { onFilterChange(filters.copy(onlyUnpatrolled = !filters.onlyUnpatrolled)) },
                label = { Text("Alleen ongecontroleerd") },
                enabled = canReadPatrolMarks
            )
            FilterChip(
                selected = filters.hideNewPages,
                onClick = { onFilterChange(filters.copy(hideNewPages = !filters.hideNewPages)) },
                label = { Text("Verberg nieuwe pagina's") }
            )
            FilterChip(
                selected = filters.hideMinor,
                onClick = { onFilterChange(filters.copy(hideMinor = !filters.hideMinor)) },
                label = { Text("Verberg kleine bewerkingen") }
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NamespaceDropdown(
                selected = filters.namespace,
                onSelect = { onFilterChange(filters.copy(namespace = it)) },
                modifier = Modifier.weight(1.5f)
            )
            SortDropdown(
                newestFirst = filters.sortNewestFirst,
                onSelect = { onFilterChange(filters.copy(sortNewestFirst = it)) },
                modifier = Modifier.weight(1.5f)
            )
            OutlinedTextField(
                value = filters.limit.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { newLimit ->
                        onFilterChange(filters.copy(limit = newLimit.coerceIn(1, 500)))
                    }
                },
                label = { Text("Max") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = filters.autoRefreshSeconds.toString(),
            onValueChange = { value ->
                value.toIntOrNull()?.let { seconds ->
                    onFilterChange(filters.copy(autoRefreshSeconds = seconds.coerceAtLeast(0)))
                }
            },
            label = { Text("Auto-refresh (sec)") },
            supportingText = { Text("0 = uit") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NamespaceDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = NamespaceOptions.find { it.first == selected }?.second ?: selected

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Naamruimte") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            NamespaceOptions.forEach { (value, text) ->
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SortDropdown(
    newestFirst: Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        true to "Nieuwste eerst",
        false to "Oudste eerst"
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
            label = { Text("Sorteer") },
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
        Text("Fout bij ophalen", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Opnieuw proberen") }
    }
}

@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Geen recente wijzigingen gevonden", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun RecentChangesList(
    changes: List<RecentChange>,
    userRights: List<String>,
    onGood: (RecentChange) -> Unit,
    onBad: (RecentChange, BadEditAction, String) -> Unit,
    onViewDiff: (RecentChange) -> Unit
) {
    LazyColumn {
        items(changes, key = { it.id }) { change ->
            ChangeCard(
                change = change,
                userRights = userRights,
                onGood = onGood,
                onBad = onBad,
                onViewDiff = onViewDiff
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ChangeCard(
    change: RecentChange,
    userRights: List<String>,
    onGood: (RecentChange) -> Unit,
    onBad: (RecentChange, BadEditAction, String) -> Unit,
    onViewDiff: (RecentChange) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showBadDialog by remember { mutableStateOf(false) }
    var selectedBadAction by remember { mutableStateOf(BadEditAction.ROLLBACK) }
    var reasonText by remember { mutableStateOf("") }
    var showReasonDialog by remember { mutableStateOf(false) }

    val canPatrol = userRights.contains("patrol")
    val canRollback = userRights.contains("rollback")
    val canBlock = userRights.contains("block")
    val canEdit = userRights.contains("edit")

    val byteDiff = change.getByteDifference()
    val diffColor = when {
        byteDiff == null -> MaterialTheme.colorScheme.onSurface
        byteDiff > 0 -> com.itsnyoty.wikichanges.ui.theme.WikiGreen
        byteDiff < 0 -> com.itsnyoty.wikichanges.ui.theme.WikiRed
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = change.title?.replace("_", " ") ?: "Onbekende pagina",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (byteDiff != null) {
                    Text(
                        text = if (byteDiff > 0) "+${byteDiff}" else byteDiff.toString(),
                        color = diffColor,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Gebruiker: ${change.user ?: "onbekend"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = change.formatTimestamp(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!change.comment.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = change.comment,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onGood(change) },
                        colors = ButtonDefaults.buttonColors(containerColor = com.itsnyoty.wikichanges.ui.theme.WikiGreen),
                        modifier = Modifier.weight(1f),
                        enabled = canPatrol
                    ) {
                        Text("Goed")
                    }
                    Button(
                        onClick = {
                            selectedBadAction = BadEditAction.ROLLBACK
                            showBadDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = com.itsnyoty.wikichanges.ui.theme.WikiRed),
                        modifier = Modifier.weight(1f),
                        enabled = canRollback
                    ) {
                        Text("Slecht")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        selectedBadAction = BadEditAction.WARNING
                        showBadDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canEdit
                ) {
                    Text("Waarschuwing sturen")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onViewDiff(change) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = change.oldRevid != null
                ) {
                    Text("Verschil bekijken")
                }
            }
        }
    }

    if (showBadDialog) {
        AlertDialog(
            onDismissRequest = { showBadDialog = false },
            title = { Text("Actie kiezen") },
            text = {
                Column {
                    Text("Welke actie wil je uitvoeren?")
                    Spacer(modifier = Modifier.height(8.dp))
                    BadEditAction.values().forEach { action ->
                        val enabled = when (action) {
                            BadEditAction.ROLLBACK -> canRollback
                            BadEditAction.WARNING -> canEdit
                            BadEditAction.BLOCK -> canBlock
                        }
                        val suffix = if (enabled) "" else " (geen recht)"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = enabled) {
                                    selectedBadAction = action
                                    showBadDialog = false
                                    showReasonDialog = true
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedBadAction == action,
                                enabled = enabled,
                                onClick = {
                                    selectedBadAction = action
                                    showBadDialog = false
                                    showReasonDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (action) {
                                    BadEditAction.ROLLBACK -> "Terugdraaien (rollback)$suffix"
                                    BadEditAction.WARNING -> "Waarschuwing sturen$suffix"
                                    BadEditAction.BLOCK -> "Gebruiker blokkeren$suffix"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBadDialog = false }) { Text("Annuleren") }
            }
        )
    }

    if (showReasonDialog) {
        AlertDialog(
            onDismissRequest = { showReasonDialog = false },
            title = {
                Text(
                    when (selectedBadAction) {
                        BadEditAction.ROLLBACK -> "Reden voor terugdraaien"
                        BadEditAction.WARNING -> "Waarschuwingstemplate kiezen"
                        BadEditAction.BLOCK -> "Reden voor blokkade"
                    }
                )
            },
            text = {
                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("Reden / opmerking") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReasonDialog = false
                        onBad(change, selectedBadAction, reasonText)
                        reasonText = ""
                    }
                ) { Text("Uitvoeren") }
            },
            dismissButton = {
                TextButton(onClick = { showReasonDialog = false }) { Text("Annuleren") }
            }
        )
    }
}
