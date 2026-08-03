package com.itsnyoty.wikichanges.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itsnyoty.wikichanges.BuildConfig
import com.itsnyoty.wikichanges.data.model.DebugSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    onBack: () -> Unit
) {
    val isDryModeEnabled by DebugSettings.isDryModeEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Debug Info",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    Text("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    Text("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Features",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dry Mode", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Action executes visually but no API requests are sent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isDryModeEnabled,
                    onCheckedChange = { DebugSettings.toggleDryMode() }
                )
            }
        }
    }
}
