package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import com.idroambiente.giornalieradicantiere.database.entities.Personale
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class RiepilogoData(
    val totalePersonePresenti: Int = 0,
    val totaleOreLavoro: Float = 0f,
    val totaleOreViaggio: Float = 0f,
    val totalePernotti: Int = 0,
    val totaleMezziUtilizzati: Int = 0,
    val totaleKm: Float = 0f,
    val totaleAttrezzatureUtilizzate: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiepilogoStepContent(
    personaleList: List<Personale>,
    mezzoList: List<Mezzo>,
    attrezzaturaList: List<Attrezzatura>,
    personaleSelections: Map<Long, PersonaleFormData>,
    mezzoSelections: Map<Long, MezzoFormData>,
    attrezzaturaSelections: Map<Long, AttrezzaturaFormData>,
    riepilogo: RiepilogoData,
    note: String,
    onNoteChanged: (String) -> Unit,
    selectedDate: Long?,
    onDateChanged: (Long?) -> Unit,
    isReadOnly: Boolean = false,
    snackbarHostState: SnackbarHostState? = null,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val today = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = today
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val todayStartOfDay = calendar.timeInMillis

    // Initialize to today if null and not read-only
    LaunchedEffect(Unit) {
        if (selectedDate == null && !isReadOnly) {
            onDateChanged(todayStartOfDay)
        }
    }

    // Format date for display
    val formattedDate = selectedDate?.let { dateFormat.format(Date(it)) }
    val isToday = selectedDate != null && selectedDate >= todayStartOfDay && selectedDate < todayStartOfDay + 86_400_000L
    val displayText = when {
        isToday -> "Oggi"
        formattedDate != null -> formattedDate
        else -> "Seleziona"
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Sezione Data
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isReadOnly) {
                    Text(
                        text = formattedDate ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { showDatePicker = true })
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedDate != null) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Sezione Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isReadOnly) {
                    if (note.isBlank()) {
                        Text(
                            text = "Nessuna nota",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = note,
                        onValueChange = onNoteChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        placeholder = {
                            Text("Inserisci eventuali note...")
                        },
                        maxLines = 5
                    )
                }
            }
        }

        // Sezione Personale
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Personale",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val personalePresenti = personaleSelections.filter { it.value.presente }
                if (personalePresenti.isEmpty()) {
                    Text(
                        text = "Nessuna persona presente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    personalePresenti.forEach { (personaleId, data) ->
                        val persona = personaleList.find { it.id == personaleId }
                        if (persona != null) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = "${persona.nome} ${persona.cognome}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Lavoro: ${formatHours(data.oreLavoro)}h",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Viaggio: ${formatHours(data.oreViaggioAndata + data.oreViaggioRitorno)}h",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (data.pernotto) {
                                        Text(
                                            text = "Pernotto",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (data.inizioTurno > 0 && data.fineTurno > 0) {
                                    Text(
                                        text = "Turno: ${formatTimeFromMillis(data.inizioTurno)} - ${formatTimeFromMillis(data.fineTurno)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryItem("Presenti", "${riepilogo.totalePersonePresenti}")
                    SummaryItem("Ore Lavoro", formatHours(riepilogo.totaleOreLavoro))
                    SummaryItem("Ore Viaggio", formatHours(riepilogo.totaleOreViaggio))
                    SummaryItem("Pernotti", "${riepilogo.totalePernotti}")
                }
            }
        }

        // Sezione Mezzi
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Mezzi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val mezziUtilizzati = mezzoSelections.filter { it.value.utilizzato }
                if (mezziUtilizzati.isEmpty()) {
                    Text(
                        text = "Nessun mezzo utilizzato",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    mezziUtilizzati.forEach { (mezzoId, data) ->
                        val mezzo = mezzoList.find { it.id == mezzoId }
                        if (mezzo != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = mezzo.nome,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = mezzo.targa,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${formatKm(data.kmAndata + data.kmRitorno)} km",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SummaryItem("Mezzi", "${riepilogo.totaleMezziUtilizzati}")
                    SummaryItem("Totale KM", formatKm(riepilogo.totaleKm))
                }
            }
        }

        // Sezione Attrezzature
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Attrezzature",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val attrezzatureUtilizzate = attrezzaturaSelections.filter { it.value.utilizzata }
                if (attrezzatureUtilizzate.isEmpty()) {
                    Text(
                        text = "Nessuna attrezzatura utilizzata",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    attrezzatureUtilizzate.forEach { (attrezzaturaId, _) ->
                        val attrezzatura = attrezzaturaList.find { it.id == attrezzaturaId }
                        if (attrezzatura != null) {
                            Text(
                                text = attrezzatura.nome,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                SummaryItem("Totale", "${riepilogo.totaleAttrezzatureUtilizzate}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Date Picker Dialog
    if (showDatePicker && !isReadOnly) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate ?: todayStartOfDay,
            yearRange = IntRange(1900, Calendar.getInstance().get(Calendar.YEAR))
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        // Normalize to start of day
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = selected
                        cal.set(Calendar.HOUR_OF_DAY, 0)
                        cal.set(Calendar.MINUTE, 0)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        val normalizedDate = cal.timeInMillis
                        
                        // Check if date is not in the future
                        if (normalizedDate <= todayStartOfDay) {
                            onDateChanged(normalizedDate)
                        } else {
                            // Show feedback for future date selection
                            snackbarHostState?.let { hostState ->
                                scope.launch {
                                    hostState.showSnackbar(
                                        message = "Non è possibile selezionare una data futura. La data di oggi è stata mantenuta.",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatHours(hours: Float): String {
    return if (hours == hours.toInt().toFloat()) {
        hours.toInt().toString()
    } else {
        String.format(Locale.ITALY, "%.1f", hours)
    }
}

private fun formatKm(km: Float): String {
    return if (km == km.toInt().toFloat()) {
        km.toInt().toString()
    } else {
        String.format(Locale.ITALY, "%.1f", km)
    }
}

