package com.eversorm.weeklyschedule.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Settings. In Phase 1 only the backup; notification settings come in Phase 2.
 *
 * Export/import use Android's own file picker ("Storage Access Framework"): you choose
 * where to save or what to open, so the app needs no storage permission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val backup = routineApp().backupManager
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var message by remember { mutableStateOf<String?>(null) }
    var fileToImport by remember { mutableStateOf<Uri?>(null) }

    // Opens the system "Save as" screen; the callback receives the chosen file (or null if cancelled).
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            message = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { backup.export(it) }
                }
                "Backup saved."
            } catch (e: Exception) {
                "Export failed: ${e.message}"
            }
        }
    }

    // Opens the system "Open file" screen.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) fileToImport = uri // ask for confirmation first
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Backup", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "Your data is stored only on this phone. Export it to a file regularly " +
                    "(e.g. to Downloads or Google Drive) so you can restore it after a reinstall or on a new phone.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { exportLauncher.launch("weekly-schedule-backup-${LocalDate.now()}.json") }) {
                Text("Export to file")
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }) {
                Text("Import from file")
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        }

        fileToImport?.let { uri ->
            AlertDialog(
                onDismissRequest = { fileToImport = null },
                title = { Text("Replace all data?") },
                text = { Text("Everything currently in the app will be replaced by the content of the backup file.") },
                confirmButton = {
                    TextButton(onClick = {
                        fileToImport = null
                        scope.launch {
                            message = try {
                                withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(uri)!!.use { backup.import(it) }
                                }
                                "Backup restored."
                            } catch (e: Exception) {
                                "Import failed: this file is not a valid backup. Your data was not changed."
                            }
                        }
                    }) { Text("Replace") }
                },
                dismissButton = { TextButton(onClick = { fileToImport = null }) { Text("Cancel") } },
            )
        }
    }
}
