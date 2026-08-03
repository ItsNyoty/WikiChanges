package com.itsnyoty.wikichanges.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.itsnyoty.wikichanges.data.model.UiState
import com.itsnyoty.wikichanges.data.model.WikiProject
import com.itsnyoty.wikichanges.data.auth.fetchUserInfo
import com.itsnyoty.wikichanges.ui.viewmodel.SettingsViewModel
import com.itsnyoty.wikichanges.ui.viewmodel.WikiChangesViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = WikiChangesViewModelProvider.settingsFactory)
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddWikiDialog by remember { mutableStateOf(false) }
    var newWikiId by remember { mutableStateOf("") }
    var newWikiName by remember { mutableStateOf("") }
    var newWikiUrl by remember { mutableStateOf("") }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instellingen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    "Wiki projecten",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "WikiChanges is niet gelieerd aan de Wikimedia Foundation. " +
                    "Deze app is gemaakt door ItsNyoty.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
            }

            items(uiState.wikis) { wiki ->
                WikiItem(
                    wiki = wiki,
                    isSelected = wiki.id == uiState.selectedWikiId,
                    canDelete = !wiki.isDefault,
                    onDelete = { viewModel.removeWiki(wiki.id) }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showAddWikiDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Wiki toevoegen")
                }
                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))
                var userInfoState by remember { mutableStateOf<UiState<com.itsnyoty.wikichanges.data.auth.UserInfo>?>(null) }
                var isLoadingProfile by remember { mutableStateOf(false) }
                val oAuthManager = com.itsnyoty.wikichanges.data.auth.OAuthManager.getInstance(context)
                val accessToken by oAuthManager.accessToken.collectAsState(initial = null)
                val isLoggedIn = !accessToken.isNullOrBlank()

                Text("Jouw profiel", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (isLoggedIn) {
                    LaunchedEffect(Unit) {
                        isLoadingProfile = true
                        userInfoState = oAuthManager.fetchUserInfo()
                        isLoadingProfile = false
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            when (val state = userInfoState) {
                                is UiState.Loading -> CircularProgressIndicator()
                                is UiState.Success -> {
                                    val user = state.data
                                    Text(
                                        user.username,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (!user.realName.isNullOrBlank()) {
                                        Text(user.realName, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    if (!user.email.isNullOrBlank()) {
                                        Text(user.email, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (user.groups.isNotEmpty()) {
                                        Text(
                                            "Groepen: ${user.groups.take(5).joinToString()}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                is UiState.Error -> Text(
                                    state.message,
                                    color = MaterialTheme.colorScheme.error
                                )
                                else -> Text("Ingelogd bij Wikimedia")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.logout { result ->
                                userInfoState = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Uitloggen")
                    }
                } else {
                    Text(
                        "Je bent niet ingelogd. Sommige acties (zoals patrolleren en rollbacken) vereisen een Wikimedia-account.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Terug naar recente wijzigingen")
                    }
                }
            }
        }
    }

    if (showAddWikiDialog) {
        AlertDialog(
            onDismissRequest = { showAddWikiDialog = false },
            title = { Text("Nieuwe wiki toevoegen") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newWikiId,
                        onValueChange = { newWikiId = it.lowercase() },
                        label = { Text("Wiki ID (bijv. dewiki)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newWikiName,
                        onValueChange = { newWikiName = it },
                        label = { Text("Naam (bijv. Deutsches Wikipedia)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newWikiUrl,
                        onValueChange = { newWikiUrl = it },
                        label = { Text("API URL (bijv. https://de.wikipedia.org/w/api.php)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newWikiId.isNotBlank() && newWikiUrl.isNotBlank()) {
                            viewModel.addWiki(
                                WikiProject(
                                    id = newWikiId,
                                    name = newWikiName.ifBlank { newWikiId },
                                    code = newWikiId.replace("wiki", ""),
                                    baseUrl = newWikiUrl.removeSuffix("/w/api.php"),
                                    apiUrl = newWikiUrl
                                )
                            )
                            newWikiId = ""
                            newWikiName = ""
                            newWikiUrl = ""
                            showAddWikiDialog = false
                        }
                    }
                ) { Text("Toevoegen") }
            },
            dismissButton = {
                TextButton(onClick = { showAddWikiDialog = false }) { Text("Annuleren") }
            }
        )
    }
}

@Composable
private fun WikiItem(
    wiki: WikiProject,
    isSelected: Boolean,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(wiki.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${wiki.code} • ${wiki.apiUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (wiki.isDefault) {
                    Text(
                        "Standaard wiki",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Verwijderen")
                }
            }
        }
    }
}
