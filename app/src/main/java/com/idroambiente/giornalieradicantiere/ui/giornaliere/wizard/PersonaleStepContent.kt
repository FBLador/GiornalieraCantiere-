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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALY)

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
                            val inizioTurnoStr = if (formData.inizioTurno > 0) timeFormat.format(Date(formData.inizioTurno)) else "--:--"
                            val fineTurnoStr = if (formData.fineTurno > 0) timeFormat.format(Date(formData.fineTurno)) else "--:--"
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

@Composable
private fun PersonaleEditMode(
    personaleList: List<Personale>,
    personaleSelections: Map<Long, PersonaleFormData>,
    onPersonaleDataChanged: (Long, PersonaleFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate the list of present personale for copy functionality
    val presentPersonale = personaleList.filter {
        personaleSelections[it.id]?.presente == true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        items(personaleList, key = { it.id }) { personale ->
            val formData = personaleSelections[personale.id] ?: PersonaleFormData()

            // Find the index in the present list and the person above
            val indexInPresent = presentPersonale.indexOfFirst { it.id == personale.id }
            val personaleAbove = if (indexInPresent > 0) presentPersonale[indexInPresent - 1] else null
            val personaleAboveData = personaleAbove?.let { personaleSelections[it.id] }

            // Other present personale (excluding current)
            val otherPresenti = presentPersonale.filter { it.id != personale.id }

            PersonaleCard(
                personale = personale,
                formData = formData,
                onFormDataChanged = { newData ->
                    onPersonaleDataChanged(personale.id, newData)
                },
                personaleAbove = personaleAbove,
                personaleAboveData = personaleAboveData,
                otherPresenti = otherPresenti,
                allSelections = personaleSelections,
                onCopyFrom = { sourceId ->
                    personaleSelections[sourceId]?.let { sourceData ->
                        onPersonaleDataChanged(personale.id, formData.copy(
                            oreViaggioAndata = sourceData.oreViaggioAndata,
                            oreViaggioRitorno = sourceData.oreViaggioRitorno,
                            inizioTurno = sourceData.inizioTurno,
                            fineTurno = sourceData.fineTurno,
                            oreLavoro = sourceData.oreLavoro,
                            pernotto = sourceData.pernotto
                        ))
                    }
                }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun PersonaleCard(
    personale: Personale,
    formData: PersonaleFormData,
    onFormDataChanged: (PersonaleFormData) -> Unit,
    personaleAbove: Personale? = null,
    personaleAboveData: PersonaleFormData? = null,
    otherPresenti: List<Personale> = emptyList(),
    allSelections: Map<Long, PersonaleFormData> = emptyMap(),
    onCopyFrom: (Long) -> Unit = {}
) {
    val alpha = if (formData.presente) 1f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (formData.presente) {
            CardDefaults.cardColors()
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${personale.nome} ${personale.cognome}",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Presente",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Checkbox(
                        checked = formData.presente,
                        onCheckedChange = { checked ->
                            onFormDataChanged(formData.copy(presente = checked))
                        }
                    )
                }
            }

            if (formData.presente) {
                // Show "Copy from..." button if there are other people to copy from
                if (personaleAbove != null || otherPresenti.isNotEmpty()) {
                    CopyFromButton(
                        personaleAbove = personaleAbove,
                        personaleAboveData = personaleAboveData,
                        otherPresenti = otherPresenti,
                        allSelections = allSelections,
                        onCopyFrom = onCopyFrom
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Ore Viaggio",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DecimalTextField(
                        value = formData.oreViaggioAndata,
                        onValueChange = { value ->
                            onFormDataChanged(formData.copy(oreViaggioAndata = value))
                        },
                        label = "Andata",
                        modifier = Modifier.weight(1f)
                    )
                    DecimalTextField(
                        value = formData.oreViaggioRitorno,
                        onValueChange = { value ->
                            onFormDataChanged(formData.copy(oreViaggioRitorno = value))
                        },
                        label = "Ritorno",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                        value = formData.inizioTurno,
                        onValueChange = { time ->
                            onFormDataChanged(formData.copy(inizioTurno = time))
                        },
                        label = "Inizio",
                        modifier = Modifier.weight(1f)
                    )
                    Text("-")
                    TimePickerField(
                        value = formData.fineTurno,
                        onValueChange = { time ->
                            onFormDataChanged(formData.copy(fineTurno = time))
                        },
                        label = "Fine",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                DecimalTextField(
                    value = formData.oreLavoro,
                    onValueChange = { value ->
                        onFormDataChanged(formData.copy(oreLavoro = value))
                    },
                    label = "Ore Lavoro (esclusa pausa pranzo)",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                        checked = formData.pernotto,
                        onCheckedChange = { checked ->
                            onFormDataChanged(formData.copy(pernotto = checked))
                        }
                    )
                }
            }
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
    val timeFormat = SimpleDateFormat("HH:mm", Locale.ITALY)
    val displayValue = if (value > 0) timeFormat.format(Date(value)) else ""

    // Extract hours and minutes from current value
    val currentHour = if (value > 0) ((value / 1000 / 60 / 60) % 24).toInt() else 8
    val currentMinute = if (value > 0) ((value / 1000 / 60) % 60).toInt() else 0

    Box(modifier = modifier.clickable { showDialog = true }) {
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
            title = { Text(label) },
            text = {
                TimePicker(state = timePickerState)
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
    modifier: Modifier = Modifier
) {
    var textValue by remember { mutableStateOf("") }

    // Sincronizza solo all'init se c'è un valore pre-esistente
    LaunchedEffect(Unit) {
        if (value > 0 && textValue.isEmpty()) {
            textValue = value.toString().removeSuffix(".0")
        }
    }

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
                onValueChange(parsed)
            } else if (filtered.isEmpty()) {
                onValueChange(0f)
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true
    )
}

@Composable
private fun CopyFromButton(
    personaleAbove: Personale?,
    personaleAboveData: PersonaleFormData?,
    otherPresenti: List<Personale>,
    allSelections: Map<Long, PersonaleFormData>,
    onCopyFrom: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Filter other present people who have some data filled in
    val altriConDati = otherPresenti.filter { p ->
        val data = allSelections[p.id]
        data != null && (data.oreLavoro > 0 || data.inizioTurno > 0)
    }

    // Only show if there's someone above or others with data
    val hasPersonaleAboveWithData = personaleAbove != null && personaleAboveData != null &&
            (personaleAboveData.oreLavoro > 0 || personaleAboveData.inizioTurno > 0)

    if (!hasPersonaleAboveWithData && altriConDati.isEmpty()) {
        return
    }

    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                modifier = Modifier.height(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Copia da...")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Quick option "Copy from above"
            if (hasPersonaleAboveWithData && personaleAbove != null) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Copia da sopra (${personaleAbove.nome})")
                        }
                    },
                    onClick = {
                        onCopyFrom(personaleAbove.id)
                        expanded = false
                    }
                )

                if (altriConDati.isNotEmpty()) {
                    HorizontalDivider()
                }
            }

            // List of other present people with filled data
            altriConDati.forEach { p ->
                DropdownMenuItem(
                    text = { Text("${p.nome} ${p.cognome}") },
                    onClick = {
                        onCopyFrom(p.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
