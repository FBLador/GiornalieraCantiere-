package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.idroambiente.giornalieradicantiere.ui.components.AppTopBar
import com.idroambiente.giornalieradicantiere.ui.components.PrimaryButton
import com.idroambiente.giornalieradicantiere.ui.components.SecondaryButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.idroambiente.giornalieradicantiere.database.entities.StatoGiornaliera
import kotlinx.coroutines.launch

@Composable
fun GiornalieraWizardScreen(
    viewModel: GiornalieraWizardViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val personaleList by viewModel.personaleList.collectAsState()
    val mezzoList by viewModel.mezzoList.collectAsState()
    val attrezzaturaList by viewModel.attrezzaturaList.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successInfo by remember { mutableStateOf<Triple<String, String?, String?>>(Triple("", null, null)) }
    var showAnnullaConfirmDialog by remember { mutableStateOf(false) }

    val isDetailMode = viewModel.isDetailMode
    val isEditMode = uiState.isEditMode
    val isReadOnly = isDetailMode && !isEditMode

    // Intercetta back gesture in modalità creazione
    BackHandler(enabled = !isDetailMode || isEditMode) {
        showAnnullaConfirmDialog = true
    }

    // Determine title based on mode
    val title = when {
        !isDetailMode -> "Nuova Giornaliera"
        isEditMode -> "Modifica Giornaliera"
        else -> "Dettaglio Giornaliera"
    }

    // Check if giornaliera is already sent
    val isGiornalieraInviata = uiState.giornaliera?.stato == StatoGiornaliera.INVIATA

    // Handle PDF status changes
    LaunchedEffect(uiState.pdfStatus) {
        when (val status = uiState.pdfStatus) {
            is PdfStatus.Success -> {
                successInfo = Triple(status.fileName, status.folderName, status.webViewLink)
                showSuccessDialog = true
            }
            is PdfStatus.ReadyToShare -> {
                try {
                    context.startActivity(status.shareIntent)
                    viewModel.confirmCompletion()
                } catch (e: Exception) {
                    errorMessage = "Impossibile aprire le opzioni di condivisione"
                    showErrorDialog = true
                }
            }
            is PdfStatus.SavedAndReadyToShare -> {
                // Show snackbar for save success, then launch share
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "PDF salvato in ${status.folderName ?: "cartella"}",
                        actionLabel = "Condividi",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        try {
                            context.startActivity(status.shareIntent)
                        } catch (e: Exception) {
                            // Ignore share error
                        }
                    }
                    viewModel.confirmCompletion()
                }
            }
            is PdfStatus.Error -> {
                errorMessage = status.message ?: "Errore sconosciuto"
                showErrorDialog = true
            }
            else -> { /* Idle, Generating, Saving - handled by UI */ }
        }
    }

    // Handle previous mezzo data result feedback
    LaunchedEffect(uiState.previousMezzoDataResult) {
        uiState.previousMezzoDataResult?.let { result ->
            snackbarHostState.showSnackbar(
                message = result.message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearPreviousMezzoDataResult()
        }
    }

    // Handle duplicate error
    LaunchedEffect(uiState.duplicateError) {
        uiState.duplicateError?.let { error ->
            errorMessage = error
            showErrorDialog = true
        }
    }

    // Navigate back when completed (but not for PDF states that need user interaction)
    LaunchedEffect(uiState.saveCompleted, uiState.pdfStatus) {
        if (uiState.saveCompleted && uiState.pdfStatus is PdfStatus.Idle) {
            onNavigateBack()
        }
    }

    // Error Dialog
    if (showErrorDialog && errorMessage != null) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                errorMessage = null
                viewModel.resetPdfStatus()
                viewModel.clearDuplicateError()
            },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Errore") },
            text = { Text(errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { 
                    showErrorDialog = false
                    errorMessage = null
                    viewModel.resetPdfStatus()
                    viewModel.clearDuplicateError()
                    if (uiState.duplicateError != null) {
                        // Don't navigate back for duplicate errors, just close dialog
                    } else {
                        onNavigateBack()
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                viewModel.resetPdfStatus()
                onNavigateBack()
            },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Giornaliera Inviata") },
            text = {
                Column {
                    Text("Il PDF è stato generato e salvato con successo.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "File: ${successInfo.first}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    successInfo.second?.let { folder ->
                        Text(
                            text = "Cartella: $folder",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                PrimaryButton(
                    onClick = {
                        showSuccessDialog = false
                        viewModel.resetPdfStatus()
                        onNavigateBack()
                    },
                    content = { Text("OK") }
                )
            },
            dismissButton = {
                successInfo.third?.let { link ->
                    TextButton(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                        showSuccessDialog = false
                        viewModel.resetPdfStatus()
                        onNavigateBack()
                    }) {
                        Text("Apri su Drive")
                    }
                }
            }
        )
    }

    // Configuration error dialog
    if (uiState.showConfigErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfigErrorDialog() },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Configurazione incompleta") },
            text = { Text(uiState.configErrorMessage ?: "") },
            confirmButton = {
                PrimaryButton(
                    onClick = { viewModel.dismissConfigErrorDialog() },
                    content = { Text("OK") }
                )
            }
        )
    }

    // Offline dialog
    if (uiState.showOfflineDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissOfflineDialog() },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Connessione non disponibile") },
            text = { Text("Impossibile caricare su Google Drive senza connessione internet. Riprova quando sei online.") },
            confirmButton = {
                PrimaryButton(
                    onClick = { viewModel.dismissOfflineDialog() },
                    content = { Text("OK") }
                )
            }
        )
    }

    // Uscita dalla creazione: salva bozza / scarta / annulla
    if (showAnnullaConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showAnnullaConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Uscire dalla giornaliera?") },
            text = { Text("Puoi salvare come bozza e continuare in un secondo momento.") },
            confirmButton = {
                PrimaryButton(
                    onClick = {
                        showAnnullaConfirmDialog = false
                        viewModel.saveGiornaliera()
                    },
                    content = { Text("Salva bozza") }
                )
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showAnnullaConfirmDialog = false }) {
                        Text("Annulla")
                    }
                    TextButton(
                        onClick = {
                            showAnnullaConfirmDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text("Scarta", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (isDetailMode) {
                AppTopBar(
                    title = title,
                    showBack = true,
                    onBackClick = onNavigateBack,
                    actions = {
                        if (!isGiornalieraInviata) {
                            IconButton(onClick = { viewModel.toggleEditMode() }) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Close else Icons.Default.Edit,
                                    contentDescription = if (isEditMode) "Annulla modifica" else "Modifica",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                )
            }
        },
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            StepProgressIndicator(
                currentStep = uiState.currentStep
            )

            HorizontalDivider()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    when (uiState.currentStep) {
                        0 -> PersonaleStepContent(
                            personaleList = personaleList,
                            personaleSelections = uiState.personaleSelections,
                            onPersonaleDataChanged = { personaleId, data ->
                                viewModel.updatePersonaleData(personaleId, data)
                            },
                            isReadOnly = isReadOnly
                        )
                        1 -> MezzoStepContent(
                            mezzoList = mezzoList,
                            mezzoSelections = uiState.mezzoSelections,
                            onMezzoDataChanged = { mezzoId, data ->
                                viewModel.updateMezzoData(mezzoId, data)
                            },
                            onLoadPreviousData = { viewModel.loadPreviousMezzoData() },
                            isReadOnly = isReadOnly
                        )
                        2 -> AttrezzaturaStepContent(
                            attrezzaturaList = attrezzaturaList,
                            attrezzaturaSelections = uiState.attrezzaturaSelections,
                            onAttrezzaturaDataChanged = { attrezzaturaId, data ->
                                viewModel.updateAttrezzaturaData(attrezzaturaId, data)
                            },
                            isReadOnly = isReadOnly
                        )
                        3 -> RiepilogoStepContent(
                            personaleList = personaleList,
                            mezzoList = mezzoList,
                            attrezzaturaList = attrezzaturaList,
                            personaleSelections = uiState.personaleSelections,
                            mezzoSelections = uiState.mezzoSelections,
                            attrezzaturaSelections = uiState.attrezzaturaSelections,
                            riepilogo = uiState.riepilogo,
                            note = uiState.note,
                            onNoteChanged = { viewModel.updateNote(it) },
                            selectedDate = uiState.selectedDate,
                            onDateChanged = { viewModel.updateSelectedDate(it) },
                            isReadOnly = isReadOnly,
                            snackbarHostState = snackbarHostState
                        )
                    }
                }

                // Show loading overlay for saving/PDF generation
                if (uiState.isOperationInProgress) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (uiState.pdfStatus) {
                                    is PdfStatus.Generating -> "Generazione PDF..."
                                    is PdfStatus.Saving -> "Salvataggio in corso..."
                                    else -> "Salvataggio..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            WizardNavigationButtons(
                currentStep = uiState.currentStep,
                onPreviousClick = { viewModel.previousStep() },
                onCancelClick = { showAnnullaConfirmDialog = true },
                onNextClick = { viewModel.nextStep() },
                onSaveClick = { viewModel.saveGiornaliera() },
                onSendClick = { viewModel.sendGiornaliera() },
                onSaveAndSendClick = { viewModel.saveAndSendGiornaliera() },
                isSaving = uiState.isOperationInProgress,
                isDetailMode = isDetailMode,
                isEditMode = isEditMode,
                isGiornalieraInviata = isGiornalieraInviata
            )
        }
    }
}

@Composable
private fun WizardNavigationButtons(
    currentStep: Int,
    onPreviousClick: () -> Unit,
    onCancelClick: () -> Unit,
    onNextClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSendClick: () -> Unit,
    onSaveAndSendClick: () -> Unit,
    isSaving: Boolean,
    isDetailMode: Boolean,
    isEditMode: Boolean,
    isGiornalieraInviata: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when {
            currentStep == 0 && (!isDetailMode || isEditMode) -> {
                SecondaryButton(
                    onClick = onCancelClick,
                    enabled = !isSaving,
                    content = { Text("Annulla") }
                )
            }
            currentStep > 0 -> {
                SecondaryButton(
                    onClick = onPreviousClick,
                    enabled = !isSaving,
                    content = { Text("Indietro") }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (currentStep < 3) {
            PrimaryButton(
                onClick = onNextClick,
                enabled = !isSaving,
                content = { Text("Avanti") }
            )
        } else {
            when {
                !isDetailMode -> {
                    PrimaryButton(
                        onClick = onSaveAndSendClick,
                        enabled = !isSaving,
                        content = { Text("Invia") }
                    )
                }
                isEditMode -> {
                    PrimaryButton(
                        onClick = onSendClick,
                        enabled = !isSaving,
                        content = { Text("Invia") }
                    )
                }
                !isGiornalieraInviata -> {
                    PrimaryButton(
                        onClick = onSendClick,
                        enabled = !isSaving,
                        content = { Text("Invia") }
                    )
                }
                else -> { }
            }
        }
    }
}
