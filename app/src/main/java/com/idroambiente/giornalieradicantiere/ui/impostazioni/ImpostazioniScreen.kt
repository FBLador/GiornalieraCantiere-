package com.idroambiente.giornalieradicantiere.ui.impostazioni

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.BuildConfig
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import com.idroambiente.giornalieradicantiere.notifications.ReminderNotificationHelper
import com.idroambiente.giornalieradicantiere.ui.components.AppTopBar

private enum class TestNotificationMode {
    IMMEDIATE,
    SCHEDULED
}

@Composable
fun ImpostazioniScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToListeGestione: () -> Unit = {},
    exportSettingsViewModel: ExportSettingsViewModel = hiltViewModel()
) {
    val exportSettingsState by exportSettingsViewModel.uiState.collectAsState()
    var pendingModeChange by remember { mutableStateOf<StorageMode?>(null) }
    var pendingTestNotificationMode by remember { mutableStateOf<TestNotificationMode?>(null) }
    val context = LocalContext.current

    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        exportSettingsViewModel.setDailyReminderEnabled(granted)
    }

    val testNotificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val mode = pendingTestNotificationMode
        pendingTestNotificationMode = null

        if (!granted) {
            Toast.makeText(context, "Permesso notifiche negato", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        if (!ReminderNotificationHelper.areSystemNotificationsEnabled(context)) {
            Toast.makeText(
                context,
                "Notifiche disattivate a livello di sistema per questa app",
                Toast.LENGTH_LONG
            ).show()
            return@rememberLauncherForActivityResult
        }

        if (ReminderNotificationHelper.isReminderChannelBlocked(context)) {
            Toast.makeText(
                context,
                "Canale promemoria bloccato: abilitalo nelle impostazioni notifiche",
                Toast.LENGTH_LONG
            ).show()
            return@rememberLauncherForActivityResult
        }

        when (mode) {
            TestNotificationMode.IMMEDIATE -> {
                ReminderNotificationHelper.showReminderNotification(
                    context = context,
                    isTest = true
                )
                Toast.makeText(context, "Notifica inviata", Toast.LENGTH_SHORT).show()
            }

            TestNotificationMode.SCHEDULED -> {
                exportSettingsViewModel.scheduleTestReminder()
                Toast.makeText(context, "Notifica di test programmata tra 20 secondi", Toast.LENGTH_SHORT).show()
            }

            null -> {
                // no-op
            }
        }
    }

    // Confirmation dialog for mode change
    pendingModeChange?.let { newMode ->
        AlertDialog(
            onDismissRequest = { pendingModeChange = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Cambiare modalita?") },
            text = {
                Text(
                    if (newMode == StorageMode.LOCAL) {
                        "Passando alla modalita locale, la configurazione Google Drive restera salvata ma non verra utilizzata."
                    } else {
                        "Passando a Google Drive, il comportamento all'invio verra impostato su \"Salva automaticamente\"."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    exportSettingsViewModel.setStorageMode(newMode)
                    pendingModeChange = null
                }) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingModeChange = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    BackHandler { onNavigateBack() }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Impostazioni",
                showBack = true,
                onBackClick = onNavigateBack,
                actions = {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            NotificationSettingsSection(
                isEnabled = exportSettingsState.dailyReminderEnabled,
                hour = exportSettingsState.dailyReminderHour,
                minute = exportSettingsState.dailyReminderMinute,
                enabledDays = exportSettingsState.dailyReminderDays,
                onEnabledChange = { enabled ->
                    if (!enabled) {
                        exportSettingsViewModel.setDailyReminderEnabled(false)
                    } else if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        exportSettingsViewModel.setDailyReminderEnabled(true)
                    }
                },
                onTimeChange = { hour, minute ->
                    exportSettingsViewModel.setDailyReminderTime(hour, minute)
                },
                onDaysChange = { days ->
                    exportSettingsViewModel.setDailyReminderDays(days)
                },
                onSendImmediateTestNotification = {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        pendingTestNotificationMode = TestNotificationMode.IMMEDIATE
                        testNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (!ReminderNotificationHelper.areSystemNotificationsEnabled(context)) {
                        Toast.makeText(
                            context,
                            "Notifiche disattivate a livello di sistema per questa app",
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (ReminderNotificationHelper.isReminderChannelBlocked(context)) {
                        Toast.makeText(
                            context,
                            "Canale promemoria bloccato: abilitalo nelle impostazioni notifiche",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        ReminderNotificationHelper.showReminderNotification(
                            context = context,
                            isTest = true
                        )
                        Toast.makeText(context, "Notifica inviata", Toast.LENGTH_SHORT).show()
                    }
                },
                onScheduleTestNotification = {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        pendingTestNotificationMode = TestNotificationMode.SCHEDULED
                        testNotificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (!ReminderNotificationHelper.areSystemNotificationsEnabled(context)) {
                        Toast.makeText(
                            context,
                            "Notifiche disattivate a livello di sistema per questa app",
                            Toast.LENGTH_LONG
                        ).show()
                    } else if (ReminderNotificationHelper.isReminderChannelBlocked(context)) {
                        Toast.makeText(
                            context,
                            "Canale promemoria bloccato: abilitalo nelle impostazioni notifiche",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        exportSettingsViewModel.scheduleTestReminder()
                        Toast.makeText(context, "Notifica di test programmata tra 20 secondi", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sezione Dati
            Text(
                text = "Gestione Dati",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("Gestione liste") },
                supportingContent = { Text("Persone, mezzi, attrezzature") },
                leadingContent = {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToListeGestione() }
            )
            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            CsvImportSection()

            Spacer(modifier = Modifier.height(24.dp))

            /*CodiciArticoloSection(
                codiceOreLavoro = exportSettingsState.codiceArticoloOreLavoro,
                codiceOreViaggio = exportSettingsState.codiceArticoloOreViaggio,
                onCodiceOreLavoroChange = { exportSettingsViewModel.setCodiceArticoloOreLavoro(it) },
                onCodiceOreViaggioChange = { exportSettingsViewModel.setCodiceArticoloOreViaggio(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))*/

            // Sezione configurazione specifica per modalita
            when (exportSettingsState.storageMode) {
                StorageMode.GOOGLE_DRIVE -> {
                    // Google Drive settings
                    GoogleDriveSettingsSection()
                }

                StorageMode.LOCAL -> {
                    // Local folder settings (SAF)
                    ExportSettingsSection(
                        folderUri = exportSettingsState.folderUri,
                        folderName = exportSettingsState.folderName,
                        exportBehavior = exportSettingsState.exportBehavior,
                        isFolderAccessible = exportSettingsState.isFolderAccessible,
                        onFolderSelected = { uri ->
                            exportSettingsViewModel.setExportFolder(uri)
                        },
                        onClearFolder = {
                            exportSettingsViewModel.clearExportFolder()
                        },
                        onBehaviorChanged = { behavior ->
                            exportSettingsViewModel.setExportBehavior(behavior)
                        }
                    )
                }
            }
        }
    }
}
