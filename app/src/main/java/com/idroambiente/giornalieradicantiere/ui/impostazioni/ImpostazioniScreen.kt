package com.idroambiente.giornalieradicantiere.ui.impostazioni

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.BuildConfig
import com.idroambiente.giornalieradicantiere.data.preferences.ExportBehavior
import com.idroambiente.giornalieradicantiere.data.preferences.StorageMode
import com.idroambiente.giornalieradicantiere.data.preferences.TemaApp
import com.idroambiente.giornalieradicantiere.notifications.ReminderNotificationHelper
import com.idroambiente.giornalieradicantiere.ui.components.AppHeader
import com.idroambiente.giornalieradicantiere.ui.components.AppPanel
import com.idroambiente.giornalieradicantiere.ui.components.AppSheet
import com.idroambiente.giornalieradicantiere.ui.components.AppSwitch
import com.idroambiente.giornalieradicantiere.ui.components.InfoBox
import com.idroambiente.giornalieradicantiere.ui.components.InfoKind
import com.idroambiente.giornalieradicantiere.ui.components.PinSheetContent
import com.idroambiente.giornalieradicantiere.ui.components.SectionLabel
import com.idroambiente.giornalieradicantiere.ui.components.SegItem
import com.idroambiente.giornalieradicantiere.ui.components.SegmentedControl
import com.idroambiente.giornalieradicantiere.ui.components.SettingsRow
import com.idroambiente.giornalieradicantiere.ui.components.SheetButton
import com.idroambiente.giornalieradicantiere.ui.components.SheetButtonKind
import com.idroambiente.giornalieradicantiere.ui.components.SheetTitle
import com.idroambiente.giornalieradicantiere.ui.components.TextLink
import com.idroambiente.giornalieradicantiere.ui.components.ToastHost
import com.idroambiente.giornalieradicantiere.ui.components.ToastState
import com.idroambiente.giornalieradicantiere.ui.components.WheelPickerContent
import com.idroambiente.giornalieradicantiere.ui.components.rememberToastState
import com.idroambiente.giornalieradicantiere.ui.theme.AppText
import com.idroambiente.giornalieradicantiere.ui.theme.AppTheme
import java.util.Calendar

/** Tutto ciò che la schermata mostra. */
data class ImpostazioniUi(
    val versione: String = BuildConfig.VERSION_NAME,
    val promemoriaAttivo: Boolean = false,
    val ora: Int = 18,
    val minuto: Int = 0,
    val giorni: Set<Int> = emptySet(),
    val tema: TemaApp = TemaApp.SISTEMA,
    val pinPredefinito: Boolean = true,
    val storageMode: StorageMode = StorageMode.GOOGLE_DRIVE,
    val driveConnesso: Boolean = false,
    val driveAccount: String? = null,
    val driveCartella: String? = null,
    val driveErrore: String? = null,
    val driveOffline: Boolean = false,
    val driveCaricamento: Boolean = false,
    val cartellaLocale: String? = null,
    val cartellaAccessibile: Boolean = true,
    val comportamento: ExportBehavior = ExportBehavior.SAVE
)

data class ImpostazioniAzioni(
    val indietro: () -> Unit = {},
    val promemoria: (Boolean) -> Unit = {},
    val orario: (Int, Int) -> Unit = { _, _ -> },
    val giorni: (Set<Int>) -> Unit = {},
    val provaNotifica: () -> Unit = {},
    val tema: (TemaApp) -> Unit = {},
    val liste: () -> Unit = {},
    val importaAnagrafiche: () -> Unit = {},
    val importaGiornaliere: () -> Unit = {},
    val cambiaPin: () -> Unit = {},
    val storage: (StorageMode) -> Unit = {},
    val driveCollega: () -> Unit = {},
    val driveEsci: () -> Unit = {},
    val driveCartella: () -> Unit = {},
    val cartellaLocale: () -> Unit = {},
    val comportamento: (ExportBehavior) -> Unit = {},
    val pinCifra: (Char) -> Unit = {},
    val pinCancella: () -> Unit = {},
    val pinAnnulla: () -> Unit = {}
)

@Composable
fun ImpostazioniScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToListeGestione: () -> Unit = {},
    onNavigateToImporta: (anagrafiche: Boolean) -> Unit = {},
    exportSettingsViewModel: ExportSettingsViewModel = hiltViewModel(),
    driveViewModel: GoogleDriveSettingsViewModel = hiltViewModel(),
    viewModel: ImpostazioniViewModel = hiltViewModel()
) {
    val export by exportSettingsViewModel.uiState.collectAsState()
    val drive by driveViewModel.uiState.collectAsState()
    val tema by viewModel.tema.collectAsState()
    val pinPredefinito by viewModel.pinPredefinito.collectAsState()
    val cambioPin by viewModel.cambioPin.collectAsState()
    val toastMsg by viewModel.toast.collectAsState()
    val context = LocalContext.current
    val toast = rememberToastState()
    LaunchedEffect(toastMsg) { toastMsg?.let { toast.show(it); viewModel.toastMostrato() } }

    val notifichePermesso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        exportSettingsViewModel.setDailyReminderEnabled(granted)
        if (!granted) Toast.makeText(context, "Permesso notifiche negato", Toast.LENGTH_SHORT).show()
    }
    val accessoGoogle = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        driveViewModel.handleSignInResult(result.data)
    }
    val cartellaTelefono = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { exportSettingsViewModel.setExportFolder(it) }
    }
    fun servePermesso() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    BackHandler { onNavigateBack() }

    ImpostazioniContent(
        ui = ImpostazioniUi(
            promemoriaAttivo = export.dailyReminderEnabled,
            ora = export.dailyReminderHour,
            minuto = export.dailyReminderMinute,
            giorni = export.dailyReminderDays,
            tema = tema,
            pinPredefinito = pinPredefinito,
            storageMode = export.storageMode,
            driveConnesso = drive.isSignedIn,
            driveAccount = drive.userEmail,
            driveCartella = drive.rootFolderName,
            driveErrore = drive.error,
            driveOffline = drive.isOffline,
            driveCaricamento = drive.isLoading,
            cartellaLocale = export.folderName,
            cartellaAccessibile = export.isFolderAccessible || export.folderUri == null,
            comportamento = export.exportBehavior
        ),
        cambioPin = cambioPin,
        toast = toast,
        azioni = ImpostazioniAzioni(
            indietro = onNavigateBack,
            promemoria = { on ->
                if (!on) exportSettingsViewModel.setDailyReminderEnabled(false)
                else if (servePermesso()) notifichePermesso.launch(Manifest.permission.POST_NOTIFICATIONS)
                else exportSettingsViewModel.setDailyReminderEnabled(true)
            },
            orario = exportSettingsViewModel::setDailyReminderTime,
            giorni = exportSettingsViewModel::setDailyReminderDays,
            provaNotifica = {
                when {
                    servePermesso() -> notifichePermesso.launch(Manifest.permission.POST_NOTIFICATIONS)
                    !ReminderNotificationHelper.areSystemNotificationsEnabled(context) ->
                        Toast.makeText(context, "Notifiche disattivate per questa app nelle impostazioni del telefono", Toast.LENGTH_LONG).show()
                    ReminderNotificationHelper.isReminderChannelBlocked(context) ->
                        Toast.makeText(context, "Canale promemoria bloccato: abilitalo nelle impostazioni notifiche", Toast.LENGTH_LONG).show()
                    else -> {
                        ReminderNotificationHelper.showReminderNotification(context = context, isTest = true)
                        toast.show("Notifica di prova inviata")
                    }
                }
            },
            tema = viewModel::setTema,
            liste = onNavigateToListeGestione,
            importaAnagrafiche = { onNavigateToImporta(true) },
            importaGiornaliere = { onNavigateToImporta(false) },
            cambiaPin = viewModel::apriCambioPin,
            storage = exportSettingsViewModel::setStorageMode,
            driveCollega = { accessoGoogle.launch(driveViewModel.getSignInIntent()) },
            driveEsci = driveViewModel::signOut,
            driveCartella = driveViewModel::openFolderPicker,
            cartellaLocale = { cartellaTelefono.launch(null) },
            comportamento = exportSettingsViewModel::setExportBehavior,
            pinCifra = viewModel::cifra,
            pinCancella = viewModel::cancella,
            pinAnnulla = viewModel::annullaCambioPin
        )
    )

    if (drive.showFolderPicker) {
        GoogleDriveFolderPickerDialog(
            state = drive.folderPickerState,
            onNavigateToFolder = driveViewModel::navigateToFolder,
            onNavigateToBreadcrumb = driveViewModel::navigateToBreadcrumb,
            onSelectFolder = driveViewModel::selectCurrentFolder,
            onCreateFolder = driveViewModel::createFolder,
            onShowCreateFolderDialog = driveViewModel::showCreateFolderDialog,
            onHideCreateFolderDialog = driveViewModel::hideCreateFolderDialog,
            onDismiss = driveViewModel::closeFolderPicker
        )
    }
}

@Composable
fun ImpostazioniContent(
    ui: ImpostazioniUi,
    cambioPin: CambioPinState?,
    toast: ToastState,
    azioni: ImpostazioniAzioni
) {
    val c = AppTheme.colors
    var sceltaOrario by remember { mutableStateOf(false) }
    var modalitaDaConfermare by remember { mutableStateOf<StorageMode?>(null) }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize()) {
            AppHeader(
                title = "Impostazioni",
                onBack = azioni.indietro,
                trailing = { Text("v${ui.versione}", style = AppText.metaStrong, color = c.hdrSub, modifier = Modifier.padding(end = 6.dp)) }
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ── Promemoria ──
                SectionLabel("Promemoria", Modifier.padding(start = 4.dp, top = 4.dp))
                AppPanel(padding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 14.dp)) {
                    SettingsRow(
                        Icons.Default.Notifications, "Promemoria giornaliero",
                        subtitle = if (ui.promemoriaAttivo) "Alle ${"%02d:%02d".format(ui.ora, ui.minuto)} · ${descriviGiorni(ui.giorni)}" else "Disattivato",
                        onClick = { azioni.promemoria(!ui.promemoriaAttivo) },
                        trailing = { AppSwitch(ui.promemoriaAttivo) }
                    )
                    SettingsRow(
                        Icons.Default.Schedule, "Orario",
                        enabled = ui.promemoriaAttivo,
                        onClick = { sceltaOrario = true },
                        trailing = {
                            Row(
                                Modifier.height(48.dp).clip(RoundedCornerShape(10.dp)).background(c.soft).padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("%02d:%02d".format(ui.ora, ui.minuto), style = AppText.sheetTitle.copy(lineHeight = AppText.sheetTitle.fontSize), color = c.ink)
                                Icon(Icons.Default.Edit, null, tint = c.sub)
                            }
                        }
                    )
                    Column(Modifier.alpha(if (ui.promemoriaAttivo) 1f else 0.4f)) {
                        Text("Giorni", style = AppText.buttonSmall, color = c.ink, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
                        val giorni = listOf(
                            Calendar.MONDAY to "L", Calendar.TUESDAY to "M", Calendar.WEDNESDAY to "M", Calendar.THURSDAY to "G",
                            Calendar.FRIDAY to "V", Calendar.SATURDAY to "S", Calendar.SUNDAY to "D"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            giorni.forEach { (g, l) ->
                                val on = g in ui.giorni
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (on) c.pri else c.soft)
                                        .clickable(enabled = ui.promemoriaAttivo) { azioni.giorni(if (on) ui.giorni - g else ui.giorni + g) },
                                    contentAlignment = Alignment.Center
                                ) { Text(l, style = AppText.buttonSmall.copy(fontSize = AppText.body.fontSize), color = if (on) c.onPri else c.sub) }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                        TextLink("Prova notifica", azioni.provaNotifica)
                    }
                }

                // ── Aspetto ──
                SectionLabel("Aspetto", Modifier.padding(start = 4.dp, top = 8.dp))
                SegmentedControl(
                    items = listOf(SegItem("Automatico", Icons.Default.SettingsBrightness), SegItem("Chiaro", Icons.Default.LightMode), SegItem("Scuro", Icons.Default.DarkMode)),
                    selectedIndex = ui.tema.ordinal,
                    onSelect = { azioni.tema(TemaApp.entries[it]) }
                )

                // ── Dati ──
                SectionLabel("Dati", Modifier.padding(start = 4.dp, top = 8.dp))
                AppPanel(padding = PaddingValues(horizontal = 14.dp)) {
                    SettingsRow(Icons.Default.Groups, "Gestione liste", subtitle = "Persone, mezzi, attrezzature", onClick = azioni.liste,
                        trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.sub) })
                    SettingsRow(Icons.Default.UploadFile, "Importa anagrafiche da CSV", subtitle = "Persone, mezzi e attrezzature da un file", onClick = azioni.importaAnagrafiche,
                        trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.sub) })
                    SettingsRow(Icons.Default.Download, "Importa giornaliere", subtitle = "Da una cartella di backup su Drive", onClick = azioni.importaGiornaliere, showDivider = false,
                        trailing = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.sub) })
                }

                // ── Sicurezza ──
                SectionLabel("Sicurezza", Modifier.padding(start = 4.dp, top = 8.dp))
                AppPanel(padding = PaddingValues(horizontal = 14.dp)) {
                    SettingsRow(
                        Icons.Default.Lock, "PIN responsabile",
                        subtitle = if (ui.pinPredefinito) "PIN predefinito dell'azienda · cambialo su questo telefono" else "PIN personalizzato su questo telefono",
                        onClick = azioni.cambiaPin, showDivider = false,
                        trailing = { Text("Cambia", style = AppText.buttonSmall, color = c.pri) }
                    )
                }

                // ── Salvataggio ──
                SectionLabel("Salvataggio PDF e CSV", Modifier.padding(start = 4.dp, top = 8.dp))
                SegmentedControl(
                    items = listOf(SegItem("Google Drive", Icons.Default.Cloud), SegItem("Sul telefono", Icons.Default.Smartphone)),
                    selectedIndex = if (ui.storageMode == StorageMode.GOOGLE_DRIVE) 0 else 1,
                    onSelect = { i ->
                        val nuova = if (i == 0) StorageMode.GOOGLE_DRIVE else StorageMode.LOCAL
                        if (nuova != ui.storageMode) modalitaDaConfermare = nuova
                    }
                )
                if (ui.storageMode == StorageMode.GOOGLE_DRIVE) {
                    if (ui.driveOffline) InfoBox("Nessuna connessione internet", InfoKind.WARN, icon = Icons.Default.WifiOff)
                    ui.driveErrore?.let { InfoBox(it, InfoKind.ERROR, icon = Icons.Default.Error) }
                    AppPanel(padding = PaddingValues(horizontal = 14.dp)) {
                        SettingsRow(
                            Icons.Default.AccountCircle,
                            title = if (ui.driveCaricamento) "Connessione in corso…" else ui.driveAccount ?: "Non collegato",
                            subtitle = "Account", subtitleFirst = true,
                            onClick = if (ui.driveConnesso) azioni.driveEsci else azioni.driveCollega,
                            trailing = { Text(if (ui.driveConnesso) "Esci" else "Collega", style = AppText.buttonSmall, color = c.pri) }
                        )
                        SettingsRow(
                            Icons.Default.Folder,
                            title = ui.driveCartella ?: "Non scelta",
                            subtitle = "Cartella", subtitleFirst = true, showDivider = false,
                            enabled = ui.driveConnesso,
                            onClick = azioni.driveCartella,
                            trailing = { Text("Cambia", style = AppText.buttonSmall, color = c.pri) }
                        )
                    }
                } else {
                    if (!ui.cartellaAccessibile) InfoBox("La cartella non è più accessibile: scegline una nuova.", InfoKind.ERROR, icon = Icons.Default.Error)
                    AppPanel(padding = PaddingValues(horizontal = 14.dp)) {
                        SettingsRow(
                            Icons.Default.Folder, title = ui.cartellaLocale ?: "Non scelta",
                            subtitle = "Cartella", subtitleFirst = true, showDivider = false,
                            onClick = azioni.cartellaLocale,
                            trailing = { Text("Cambia", style = AppText.buttonSmall, color = c.pri) }
                        )
                    }
                    Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Send, null, tint = c.sub)
                        Text("Dopo l'invio", style = AppText.buttonSmall, color = c.ink)
                    }
                    SegmentedControl(
                        items = listOf(SegItem("Salva"), SegItem("Condividi"), SegItem("Entrambi")),
                        selectedIndex = when (ui.comportamento) {
                            ExportBehavior.SAVE -> 0
                            ExportBehavior.SHARE -> 1
                            ExportBehavior.BOTH -> 2
                        },
                        onSelect = { azioni.comportamento(listOf(ExportBehavior.SAVE, ExportBehavior.SHARE, ExportBehavior.BOTH)[it]) }
                    )
                    Text(
                        when (ui.comportamento) {
                            ExportBehavior.SAVE -> "PDF e CSV vengono salvati nella cartella."
                            ExportBehavior.SHARE -> "Si apre la condivisione (WhatsApp, email…). Se modifichi una giornaliera, il PDF nuovo avrà «_rev» nel nome."
                            ExportBehavior.BOTH -> "Salva nella cartella e poi apre la condivisione."
                        },
                        style = AppText.meta, color = c.sub, modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        ToastHost(toast, bottomPadding = 24.dp)

        AppSheet(visible = sceltaOrario, onDismiss = { sceltaOrario = false }) {
            WheelPickerContent(
                label = "Orario promemoria",
                isTime = true,
                initialMinutes = ui.ora * 60 + ui.minuto,
                onConfirm = { azioni.orario(it / 60, it % 60); sceltaOrario = false },
                onCancel = { sceltaOrario = false }
            )
        }
        AppSheet(visible = modalitaDaConfermare != null, onDismiss = { modalitaDaConfermare = null }) {
            val nuova = modalitaDaConfermare ?: return@AppSheet
            SheetTitle(
                if (nuova == StorageMode.GOOGLE_DRIVE) "Salvare su Google Drive?" else "Salvare sul telefono?",
                if (nuova == StorageMode.LOCAL) "La configurazione di Google Drive resta salvata ma non verrà usata."
                else "Su Drive PDF e CSV vengono sempre salvati automaticamente."
            )
            SheetButton("Conferma", { azioni.storage(nuova); modalitaDaConfermare = null })
            SheetButton("Annulla", { modalitaDaConfermare = null }, SheetButtonKind.SOFT)
        }
        AppSheet(visible = cambioPin != null, onDismiss = azioni.pinAnnulla) {
            val p = cambioPin ?: CambioPinState()
            val (titolo, sotto) = when (p.passo) {
                PassoPin.ATTUALE -> "Cambia PIN" to "Inserisci il PIN attuale"
                PassoPin.NUOVO -> "Nuovo PIN" to "Scegli 6 cifre"
                PassoPin.CONFERMA -> "Conferma nuovo PIN" to "Inseriscilo di nuovo"
            }
            PinSheetContent(
                title = titolo,
                subtitle = sotto,
                icon = if (p.passo == PassoPin.ATTUALE) Icons.Default.Lock else Icons.Default.Password,
                digits = p.cifre.length,
                error = p.errore,
                locked = p.bloccato,
                onDigit = azioni.pinCifra,
                onDelete = azioni.pinCancella,
                onCancel = azioni.pinAnnulla
            )
        }
    }
}

private fun descriviGiorni(giorni: Set<Int>): String {
    val feriali = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
    return when {
        giorni == feriali -> "lun–ven"
        giorni.size == 7 -> "tutti i giorni"
        giorni.isEmpty() -> "nessun giorno"
        else -> "${giorni.size} giorni"
    }
}

