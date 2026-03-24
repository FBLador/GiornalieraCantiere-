package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import com.idroambiente.giornalieradicantiere.ui.components.DurationWheelPickerField

/**
 * Enum for the two phases of personnel data entry
 */
private enum class PersonaleEditPhase {
    SELECTION,  // Phase 1: Select present personnel
    DATA_ENTRY  // Phase 2: Enter data using the unified form
}

@Composable
fun PersonaleStepContent(
    personaleList: List<Personale>,
    personaleSelections: Map<Long, PersonaleFormData>,
    onPersonaleDataChanged: (Long, PersonaleFormData) -> Unit,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
) {
    if (personaleList.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Nessuna persona disponibile",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aggiungi persone nelle Impostazioni",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else if (isReadOnly) {
        PersonaleViewMode(
            personaleList = personaleList,
            personaleSelections = personaleSelections,
            modifier = modifier
        )
    } else {
        PersonaleEditMode(
            personaleList = personaleList,
            personaleSelections = personaleSelections,
            onPersonaleDataChanged = onPersonaleDataChanged,
            modifier = modifier
        )
    }
}

@Composable
private fun PersonaleViewMode(
    personaleList: List<Personale>,
    personaleSelections: Map<Long, PersonaleFormData>,
    modifier: Modifier = Modifier
) {
    val personalePresente = personaleList.filter { personale ->
        personaleSelections[personale.id]?.presente == true
    }
    val personaleAssente = personaleList.filter { personale ->
        personaleSelections[personale.id]?.presente != true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Personale Presente
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PERSONALE PRESENTE (${personalePresente.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )

                if (personalePresente.isEmpty()) {
                    Text(
                        text = "Nessun personale presente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                    )
                } else {
                    personalePresente.forEachIndexed { index, personale ->
                        val formData = personaleSelections[personale.id] ?: PersonaleFormData()

                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "${personale.nome} ${personale.cognome}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Ore lavoro e viaggio
                            val oreViaggioTotali = formData.oreViaggioAndata + formData.oreViaggioRitorno
                            Text(
                                text = "Lavoro: ${formatOre(formData.oreLavoro)} | Viaggio: ${formatOre(oreViaggioTotali)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Turno e pernotto
                            val inizioTurnoStr = if (formData.inizioTurno > 0) formatTimeFromMillis(formData.inizioTurno) else "--:--"
                            val fineTurnoStr = if (formData.fineTurno > 0) formatTimeFromMillis(formData.fineTurno) else "--:--"
                            val pernottoStr = if (formData.pernotto) " | Pernotto" else ""

                            Text(
                                text = "Turno: $inizioTurnoStr - $fineTurnoStr$pernottoStr",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Personale Assente
        if (personaleAssente.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PERSONALE ASSENTE (${personaleAssente.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )

                    personaleAssente.forEachIndexed { index, personale ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        }

                        Text(
                            text = "${personale.nome} ${personale.cognome}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatOre(ore: Float): String {
    return if (ore == ore.toLong().toFloat()) {
        "${ore.toLong()}h"
    } else {
        "${ore}h"
    }
}

private fun calculateOreLavoro(inizioTurno: Long, fineTurno: Long, orePausa: Float): Float {
    if (inizioTurno <= 0 || fineTurno <= 0) return 0f
    val durataMs = fineTurno - inizioTurno
    val durataOre = durataMs / 1000f / 60f / 60f
    return maxOf(0f, durataOre - orePausa)
}


@Composable
private fun PersonaleEditMode(
    personaleList: List<Personale>,
    personaleSelections: Map<Long, PersonaleFormData>,
    onPersonaleDataChanged: (Long, PersonaleFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine initial phase: if there are already present people with data, go to DATA_ENTRY
    val hasExistingData = personaleSelections.any {
        it.value.presente && (it.value.oreLavoro > 0 || (it.value.inizioTurno > 0 && it.value.fineTurno > 0))
    }
    var currentPhase by remember {
        mutableStateOf(if (hasExistingData) PersonaleEditPhase.DATA_ENTRY else PersonaleEditPhase.SELECTION)
    }

    when (currentPhase) {
        PersonaleEditPhase.SELECTION -> {
            PersonaleSelectionPhase(
                personaleList = personaleList,
                personaleSelections = personaleSelections,
                onPersonaleDataChanged = onPersonaleDataChanged,
                onProceed = { currentPhase = PersonaleEditPhase.DATA_ENTRY },
                modifier = modifier
            )
        }
        PersonaleEditPhase.DATA_ENTRY -> {
            PersonaleDataEntryPhase(
                personaleList = personaleList,
                personaleSelections = personaleSelections,
                onPersonaleDataChanged = onPersonaleDataChanged,
                onBack = { currentPhase = PersonaleEditPhase.SELECTION },
                modifier = modifier
            )
        }
    }
}

/**
 * Phase 1: Selection of present personnel (checkbox only)
 */
@Composable
private fun PersonaleSelectionPhase(
    personaleList: List<Personale>,
    personaleSelections: Map<Long, PersonaleFormData>,
    onPersonaleDataChanged: (Long, PersonaleFormData) -> Unit,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presentCount = personaleSelections.count { it.value.presente }

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Text(
            text = "Seleziona i presenti",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // People list (checkbox + name)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(personaleList, key = { it.id }) { personale ->
                val formData = personaleSelections[personale.id] ?: PersonaleFormData()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onPersonaleDataChanged(
                                personale.id,
                                formData.copy(presente = !formData.presente)
                            )
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = formData.presente,
                        onCheckedChange = { checked ->
                            onPersonaleDataChanged(personale.id, formData.copy(presente = checked))
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${personale.nome} ${personale.cognome}",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Fixed footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Selezionati: $presentCount person${if (presentCount == 1) "a" else "e"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onProceed,
                enabled = presentCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Prosegui ai dettagli")
            }
        }
    }
}

/**
 * Phase 2: Data entry using the unified form
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonaleDataEntryPhase(
    personaleList: List<Personale>,
    personaleSelections: Map<Long, PersonaleFormData>,
    onPersonaleDataChanged: (Long, PersonaleFormData) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Form state
    var oreViaggioAndata by remember { mutableStateOf(0f) }
    var oreViaggioRitorno by remember { mutableStateOf(0f) }
    var inizioTurno by remember { mutableStateOf(0L) }
    var fineTurno by remember { mutableStateOf(0L) }
    var orePausa by remember { mutableStateOf(0f) }
    var pernotto by remember { mutableStateOf(false) }

    val isTurnoInvalido = inizioTurno > 0 && fineTurno > 0 && fineTurno <= inizioTurno

    val isPausaEccessiva = inizioTurno > 0 && fineTurno > inizioTurno && orePausa > 0f && run {
        val durataTurnoOre = (fineTurno - inizioTurno) / 1000f / 60f / 60f
        orePausa * 2 >= durataTurnoOre
    }

    // Get list of present people
    val presenti = personaleList.filter { personaleSelections[it.id]?.presente == true }

    // Selected people for this application (default: those without data yet)
    var selectedIds by remember {
        mutableStateOf(
            presenti.filter { p ->
                val data = personaleSelections[p.id]
                data == null || (data.oreLavoro == 0f && data.inizioTurno <= 0 && data.fineTurno <= 0)
            }.map { it.id }.toSet()
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header with back button
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Modifica Presenti")
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Compila Dati Personale",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            // Ore Viaggio
            Text(
                text = "Ore Viaggio",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DurationWheelPickerField(
                    value = oreViaggioAndata,
                    onValueChange = { oreViaggioAndata = it },
                    label = "Andata",
                    modifier = Modifier.weight(1f),
                    maxTotalHours = 24f
                )
                DurationWheelPickerField(
                    value = oreViaggioRitorno,
                    onValueChange = { oreViaggioRitorno = it },
                    label = "Ritorno",
                    modifier = Modifier.weight(1f),
                    maxTotalHours = 24f
                )
            }

            Spacer(Modifier.height(8.dp))

            // Turno
            Text(
                text = "Turno",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimePickerField(
                    value = inizioTurno,
                    onValueChange = { inizioTurno = it },
                    label = "Inizio",
                    modifier = Modifier.weight(1f)
                )
                Text("-")
                TimePickerField(
                    value = fineTurno,
                    onValueChange = { fineTurno = it },
                    label = "Fine",
                    modifier = Modifier.weight(1f)
                )
            }

            if (isTurnoInvalido) {
                Text(
                    text = "L'orario di fine deve essere successivo all'inizio",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Ore Pausa
            DecimalTextField(
                value = orePausa,
                onValueChange = { orePausa = it },
                label = "Ore Pausa",
                modifier = Modifier.fillMaxWidth(),
                maxValue = 24f
            )

            if (isPausaEccessiva) {
                Text(
                    text = "Le ore di pausa devono essere inferiori alle ore di lavoro",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Pernotto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pernotto",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = pernotto,
                    onCheckedChange = { pernotto = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // People selection header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Applica a",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Row {
                    TextButton(onClick = { selectedIds = presenti.map { it.id }.toSet() }) {
                        Text("Tutti")
                    }
                    TextButton(onClick = { selectedIds = emptySet() }) {
                        Text("Nessuno")
                    }
                }
            }

            // Checkbox list with status indicators
            presenti.forEach { personale ->
                val isSelected = selectedIds.contains(personale.id)
                val data = personaleSelections[personale.id]
                val hasData = data != null && (data.oreLavoro > 0 || (data.inizioTurno > 0 && data.fineTurno > 0))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedIds = if (isSelected) selectedIds - personale.id
                            else selectedIds + personale.id
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            selectedIds = if (checked) selectedIds + personale.id
                            else selectedIds - personale.id
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${personale.nome} ${personale.cognome}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (hasData) "✓ compilato" else "○ da compilare",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Apply button
        Button(
            onClick = {
                val oreLavoroCalcolate = calculateOreLavoro(inizioTurno, fineTurno, orePausa)
                val formData = PersonaleFormData(
                    presente = true,
                    oreViaggioAndata = oreViaggioAndata,
                    oreViaggioRitorno = oreViaggioRitorno,
                    inizioTurno = inizioTurno,
                    fineTurno = fineTurno,
                    oreLavoro = oreLavoroCalcolate,
                    orePausa = orePausa,
                    pernotto = pernotto
                )
                selectedIds.forEach { personId ->
                    onPersonaleDataChanged(personId, formData)
                }
                // Deselect those just compiled
                selectedIds = emptySet()
            },
            enabled = selectedIds.isNotEmpty() && !isTurnoInvalido && !isPausaEccessiva,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Applica a ${selectedIds.size} person${if (selectedIds.size == 1) "a" else "e"}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(
    value: Long,
    onValueChange: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val displayValue = if (value > 0) formatTimeFromMillis(value) else ""

    // Extract hours and minutes from current value
    val currentHour = if (value > 0) ((value / 1000 / 60 / 60) % 24).toInt() else 8
    val currentMinute = if (value > 0) ((value / 1000 / 60) % 60).toInt() else 0

    Box(modifier = modifier.clickable { focusManager.clearFocus(); showDialog = true }) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = { },
            label = { Text(label) },
            placeholder = { Text("HH:mm") },
            readOnly = true,
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }

    if (showDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = currentHour,
            initialMinute = currentMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(label) },
            text = {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        selectorColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = (timePickerState.hour * 60L + timePickerState.minute) * 60L * 1000L
                        onValueChange(millis)
                        showDialog = false
                    }
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun DecimalTextField(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxValue: Float = Float.MAX_VALUE
) {
    var textValue by remember { mutableStateOf("") }

    // Sincronizza solo all'init se c'è un valore pre-esistente
    LaunchedEffect(Unit) {
        if (value > 0 && textValue.isEmpty()) {
            textValue = value.toString().removeSuffix(".0")
        }
    }

    val isOverMax = value > maxValue

    OutlinedTextField(
        value = textValue,
        onValueChange = { newText ->
            // Filtra: solo cifre, un punto o una virgola
            val filtered = newText.filterIndexed { index, c ->
                c.isDigit() || ((c == '.' || c == ',') &&
                    newText.take(index).none { it == '.' || it == ',' })
            }
            textValue = filtered

            val parsed = filtered.replace(",", ".").toFloatOrNull()
            if (parsed != null) {
                onValueChange(maxOf(0f, parsed).coerceAtMost(maxValue))
            } else if (filtered.isEmpty()) {
                onValueChange(0f)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
        isError = isOverMax,
        supportingText = if (isOverMax) {{ Text("Max ${maxValue.toInt()}") }} else null
    )
}

