package com.itsnyoty.wikichanges.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.res.stringResource
import com.itsnyoty.wikichanges.R
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
    var hasManuallyEditedUrl by remember { mutableStateOf(false) }
    var hasManuallyEditedName by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                    stringResource(R.string.settings_wiki_projects),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                
                // Language selector
                Text(stringResource(R.string.app_language_label), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                
                var expandedLanguage by remember { mutableStateOf(false) }
                val currentLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()
                val languageOptions = listOf(
                    "" to stringResource(R.string.language_system),
                    "en" to stringResource(R.string.language_en),
                    "nl" to stringResource(R.string.language_nl)
                )
                val currentLabel = languageOptions.find { it.first == currentLocale }?.second ?: languageOptions.first().second

                ExposedDropdownMenuBox(
                    expanded = expandedLanguage,
                    onExpandedChange = { expandedLanguage = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguage) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedLanguage,
                        onDismissRequest = { expandedLanguage = false }
                    ) {
                        languageOptions.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    val appLocale: androidx.core.os.LocaleListCompat = if (code.isEmpty()) {
                                        androidx.core.os.LocaleListCompat.getEmptyLocaleList()
                                    } else {
                                        androidx.core.os.LocaleListCompat.forLanguageTags(code)
                                    }
                                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(appLocale)
                                    expandedLanguage = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                Divider()
                Spacer(Modifier.height(16.dp))
            }

            items(uiState.wikis) { wiki ->
                WikiItem(
                    wiki = wiki,
                    isSelected = wiki.id == uiState.selectedWikiId,
                    roles = uiState.wikiRoles[wiki.id] ?: emptyList(),
                    canDelete = !wiki.isDefault,
                    onDelete = { viewModel.removeWiki(wiki.id) },
                    onUpdateTemplate = { viewModel.updateWikiWarningTemplate(wiki.id, it) },
                    onUpdateTemplates = { viewModel.updateWikiWarningTemplates(wiki.id, it) }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showAddWikiDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_add_wiki))
                }
                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(16.dp))
                var userInfoState by remember { mutableStateOf<UiState<com.itsnyoty.wikichanges.data.auth.UserInfo>?>(null) }
                var isLoadingProfile by remember { mutableStateOf(false) }
                val oAuthManager = com.itsnyoty.wikichanges.data.auth.OAuthManager.getInstance(context)
                val accessToken by oAuthManager.accessToken.collectAsState(initial = null)
                val isLoggedIn = !accessToken.isNullOrBlank()

                Text(stringResource(R.string.settings_your_profile), style = MaterialTheme.typography.titleMedium)
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
                                            stringResource(R.string.groups_colon, user.groups.take(5).joinToString()),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                is UiState.Error -> Text(
                                    state.message,
                                    color = MaterialTheme.colorScheme.error
                                )
                                else -> Text(stringResource(R.string.profile_logged_in))
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
                        Text(stringResource(R.string.settings_logout))
                    }
                } else {
                    Text(
                        stringResource(R.string.settings_not_logged_in),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_back))
                    }
                }
            }
        }
    }

    if (showAddWikiDialog) {
        AlertDialog(
            onDismissRequest = { showAddWikiDialog = false },
            title = { Text(stringResource(R.string.add_wiki_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newWikiId,
                        onValueChange = { input ->
                            val id = input.lowercase().trim()
                            newWikiId = id
                            // Automatische URL-suggestie
                            val code = if (id.endsWith("wiki")) id.removeSuffix("wiki") else id
                            
                            if (code.length in 2..3 || id.endsWith("wiki")) {
                                if (!hasManuallyEditedUrl) {
                                    newWikiUrl = "https://$code.wikipedia.org/w/api.php"
                                }
                                if (!hasManuallyEditedName) {
                                    newWikiName = "${code.uppercase()} Wikipedia"
                                }
                            } else if (id == "commons" || id == "wikidata" || id == "meta") {
                                if (!hasManuallyEditedUrl) {
                                    newWikiUrl = "https://$id.wikimedia.org/w/api.php"
                                }
                                if (!hasManuallyEditedName) {
                                    newWikiName = id.replaceFirstChar { it.uppercase() }
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.settings_wiki_id)) },
                        placeholder = { Text("bijv. de, fr, nl of enwiki") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newWikiName,
                        onValueChange = { 
                            newWikiName = it
                            hasManuallyEditedName = true
                        },
                        label = { Text(stringResource(R.string.settings_wiki_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newWikiUrl,
                        onValueChange = { 
                            newWikiUrl = it
                            hasManuallyEditedUrl = true
                        },
                        label = { Text(stringResource(R.string.settings_wiki_url)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newWikiId.isNotBlank() && newWikiUrl.isNotBlank()) {
                            val id = newWikiId.trim().lowercase()
                            val normalizedId = if (id.length <= 3) "${id}wiki" else id
                            val wikiCode = normalizedId.removeSuffix("wiki")
                            
                            viewModel.addWiki(
                                WikiProject(
                                    id = normalizedId,
                                    name = newWikiName.ifBlank { normalizedId },
                                    code = wikiCode,
                                    baseUrl = newWikiUrl.substringBefore("/w/api.php"),
                                    apiUrl = newWikiUrl,
                                    warningTemplates = com.itsnyoty.wikichanges.data.model.getDefaultTemplatesForCode(wikiCode)
                                )
                            )
                            newWikiId = ""
                            newWikiName = ""
                            newWikiUrl = ""
                            hasManuallyEditedUrl = false
                            hasManuallyEditedName = false
                            showAddWikiDialog = false
                        }
                    }
                ) { Text(stringResource(R.string.settings_add)) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddWikiDialog = false
                    hasManuallyEditedUrl = false
                    hasManuallyEditedName = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun WikiItem(
    wiki: WikiProject,
    isSelected: Boolean,
    roles: List<String>,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onUpdateTemplate: (String) -> Unit,
    onUpdateTemplates: (Map<String, String>) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var tempTemplate by remember { mutableStateOf(wiki.warningTemplate ?: "") }
    var tempTemplates by remember { mutableStateOf(wiki.warningTemplates) }

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
        "attack" to stringResource(R.string.warning_attack)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { showEditDialog = true },
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
                
                if (roles.isNotEmpty()) {
                    Text(
                        stringResource(R.string.roles_label, roles.joinToString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    stringResource(R.string.warnings_count_label, wiki.warningTemplates.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (wiki.isDefault) {
                    Text(
                        stringResource(R.string.standard_wiki),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.wiki_settings_title)) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    item {
                        Text(stringResource(R.string.warning_template_legacy), style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = tempTemplate,
                            onValueChange = { tempTemplate = it },
                            label = { Text(stringResource(R.string.template_name_label)) },
                            placeholder = { Text(stringResource(R.string.template_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.specific_warnings_title), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    items(warningOptions) { (key, label) ->
                        OutlinedTextField(
                            value = tempTemplates[key] ?: "",
                            onValueChange = { 
                                val newMap = tempTemplates.toMutableMap()
                                newMap[key] = it
                                tempTemplates = newMap
                            },
                            label = { Text(label) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateTemplate(tempTemplate)
                    onUpdateTemplates(tempTemplates)
                    showEditDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
