package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo

private enum class MezzoEditPhase {
    SELECTION,
    DATA_ENTRY
}

@Composable
fun MezzoStepContent(
    mezzoList: List<Mezzo>,
    mezzoSelections: Map<Long, MezzoFormData>,
    onMezzoDataChanged: (Long, MezzoFormData) -> Unit,
    onLoadPreviousData: () -> Unit,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
) {
    if (mezzoList.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Nessun mezzo disponibile",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aggiungi mezzi nelle Impostazioni",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else if (isReadOnly) {
        MezzoViewMode(
            mezzoList = mezzoList,
            mezzoSelections = mezzoSelections,
            modifier = modifier
        )
    } else {
        MezzoEditMode(
            mezzoList = mezzoList,
            mezzoSelections = mezzoSelections,
            onMezzoDataChanged = onMezzoDataChanged,
            onLoadPreviousData = onLoadPreviousData,
            modifier = modifier
        )
    }
}

@Composable
private fun MezzoViewMode(
    mezzoList: List<Mezzo>,
    mezzoSelections: Map<Long, MezzoFormData>,
    modifier: Modifier = Modifier
) {
    val mezziUtilizzati = mezzoList.filter { mezzo ->
        mezzoSelections[mezzo.id]?.utilizzato == true
    }
    val mezziNonUtilizzati = mezzoList.filter { mezzo ->
        mezzoSelections[mezzo.id]?.utilizzato != true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "MEZZI UTILIZZATI (${mezziUtilizzati.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )

                if (mezziUtilizzati.isEmpty()) {
                    Text(
                        text = "Nessun mezzo utilizzato",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                    )
                } else {
                    mezziUtilizzati.forEachIndexed { index, mezzo ->
                        val formData = mezzoSelections[mezzo.id] ?: MezzoFormData()

                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "${mezzo.nome} - ${mezzo.targa}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val kmTotali = (formData.kmAndata ?: 0f) + (formData.kmRitorno ?: 0f)
                            Text(
                                text = "KM: ${formatKm(kmTotali)} (andata: ${formatKm(formData.kmAndata ?: 0f)}, ritorno: ${formatKm(formData.kmRitorno ?: 0f)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (mezziNonUtilizzati.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "MEZZI NON UTILIZZATI (${mezziNonUtilizzati.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )

                    mezziNonUtilizzati.forEachIndexed { index, mezzo ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        }

                        Text(
                            text = "${mezzo.nome} - ${mezzo.targa}",
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

private fun formatKm(km: Float): String {
    return if (km == km.toLong().toFloat()) {
        "${km.toLong()}"
    } else {
        "$km"
    }
}

@Composable
private fun MezzoEditMode(
    mezzoList: List<Mezzo>,
    mezzoSelections: Map<Long, MezzoFormData>,
    onMezzoDataChanged: (Long, MezzoFormData) -> Unit,
    onLoadPreviousData: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine initial phase: if there are already mezzi with KM data, go to DATA_ENTRY
    val hasExistingData = mezzoSelections.any {
        it.value.utilizzato && it.value.kmAndata != null && it.value.kmRitorno != null
    }
    var currentPhase by remember {
        mutableStateOf(if (hasExistingData) MezzoEditPhase.DATA_ENTRY else MezzoEditPhase.SELECTION)
    }

    when (currentPhase) {
        MezzoEditPhase.SELECTION -> {
            MezzoSelectionPhase(
                mezzoList = mezzoList,
                mezzoSelections = mezzoSelections,
                onMezzoDataChanged = onMezzoDataChanged,
                onProceed = { currentPhase = MezzoEditPhase.DATA_ENTRY },
                modifier = modifier
            )
        }
        MezzoEditPhase.DATA_ENTRY -> {
            MezzoDataEntryPhase(
                mezzoList = mezzoList,
                mezzoSelections = mezzoSelections,
                onMezzoDataChanged = onMezzoDataChanged,
                onLoadPreviousData = onLoadPreviousData,
                onBack = { currentPhase = MezzoEditPhase.SELECTION },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun MezzoSelectionPhase(
    mezzoList: List<Mezzo>,
    mezzoSelections: Map<Long, MezzoFormData>,
    onMezzoDataChanged: (Long, MezzoFormData) -> Unit,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCount = mezzoSelections.count { it.value.utilizzato }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Seleziona i mezzi utilizzati",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(mezzoList, key = { it.id }) { mezzo ->
                val formData = mezzoSelections[mezzo.id] ?: MezzoFormData()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onMezzoDataChanged(
                                mezzo.id,
                                formData.copy(utilizzato = !formData.utilizzato)
                            )
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = formData.utilizzato,
                        onCheckedChange = { checked ->
                            onMezzoDataChanged(mezzo.id, formData.copy(utilizzato = checked))
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mezzo.nome,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = mezzo.targa,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Selezionati: $selectedCount mezz${if (selectedCount == 1) "o" else "i"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onProceed,
                enabled = selectedCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Prosegui ai dettagli")
            }
        }
    }
}

@Composable
private fun MezzoDataEntryPhase(
    mezzoList: List<Mezzo>,
    mezzoSelections: Map<Long, MezzoFormData>,
    onMezzoDataChanged: (Long, MezzoFormData) -> Unit,
    onLoadPreviousData: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Form state for batch KM
    var kmAndata by remember { mutableStateOf<Float?>(null) }
    var kmRitorno by remember { mutableStateOf<Float?>(null) }

    // Get list of utilized mezzi
    val utilizzati = mezzoList.filter { mezzoSelections[it.id]?.utilizzato == true }

    // Selected mezzi for batch application (default: those without data yet)
    var selectedIds by remember {
        mutableStateOf(
            utilizzati.filter { m ->
                val data = mezzoSelections[m.id]
                data == null || (data.kmAndata == null || data.kmRitorno == null)
            }.map { it.id }.toSet()
        )
    }

    // Feedback message
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // Header with back button
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Modifica Selezione")
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Compila Dati Mezzi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            // "Usa dati precedenti" button
            OutlinedButton(
                onClick = onLoadPreviousData,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Usa dati precedenti")
            }

            // Feedback message
            feedbackMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(16.dp))

            // Batch KM form
            Text(
                text = "Chilometri",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DecimalTextField(
                    value = kmAndata,
                    onValueChange = { kmAndata = it },
                    label = "KM Andata",
                    modifier = Modifier.weight(1f),
                    maxValue = 9999f
                )
                DecimalTextField(
                    value = kmRitorno,
                    onValueChange = { kmRitorno = it },
                    label = "KM Ritorno",
                    modifier = Modifier.weight(1f),
                    maxValue = 9999f
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // "Applica a" section with Tutti/Nessuno
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
                    TextButton(onClick = { selectedIds = utilizzati.map { it.id }.toSet() }) {
                        Text("Tutti")
                    }
                    TextButton(onClick = { selectedIds = emptySet() }) {
                        Text("Nessuno")
                    }
                }
            }

            // Checkbox list with status indicators
            utilizzati.forEach { mezzo ->
                val isSelected = selectedIds.contains(mezzo.id)
                val data = mezzoSelections[mezzo.id]
                val hasData = data != null && data.kmAndata != null && data.kmRitorno != null

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedIds = if (isSelected) selectedIds - mezzo.id
                            else selectedIds + mezzo.id
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            selectedIds = if (checked) selectedIds + mezzo.id
                            else selectedIds - mezzo.id
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${mezzo.nome} - ${mezzo.targa}",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hasData) {
                            Text(
                                text = "A: ${formatKm(data!!.kmAndata!!)} km | R: ${formatKm(data.kmRitorno!!)} km",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = if (hasData) "\u2713 compilato" else "\u25CB da compilare",
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
                selectedIds.forEach { mezzoId ->
                    val currentData = mezzoSelections[mezzoId] ?: MezzoFormData()
                    onMezzoDataChanged(
                        mezzoId,
                        currentData.copy(
                            utilizzato = true,
                            kmAndata = kmAndata,
                            kmRitorno = kmRitorno
                        )
                    )
                }
                feedbackMessage = "KM applicati a ${selectedIds.size} mezz${if (selectedIds.size == 1) "o" else "i"}"
                selectedIds = emptySet()
            },
            enabled = selectedIds.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Applica a ${selectedIds.size} mezz${if (selectedIds.size == 1) "o" else "i"}")
        }
    }
}

@Composable
private fun DecimalTextField(
    value: Float?,
    onValueChange: (Float?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxValue: Float = Float.MAX_VALUE
) {
    var textValue by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (value != null && value > 0f && textValue.isEmpty()) {
            textValue = value.toString().removeSuffix(".0")
        }
    }

    val isOverMax = value != null && value > maxValue

    OutlinedTextField(
        value = textValue,
        onValueChange = { newText ->
            val filtered = newText.filterIndexed { index, c ->
                c.isDigit() || ((c == '.' || c == ',') &&
                    newText.take(index).none { it == '.' || it == ',' })
            }
            textValue = filtered

            val parsed = filtered.replace(",", ".").toFloatOrNull()
            if (parsed != null) {
                onValueChange(maxOf(0f, parsed).coerceAtMost(maxValue))
            } else if (filtered.isEmpty()) {
                onValueChange(null)
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
