package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo

@Composable
fun MezzoStepContent(
    mezzoList: List<Mezzo>,
    mezzoSelections: Map<Long, MezzoFormData>,
    onMezzoDataChanged: (Long, MezzoFormData) -> Unit,
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

        // Mezzi Utilizzati
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

                            val kmTotali = formData.kmAndata + formData.kmRitorno
                            Text(
                                text = "KM: ${formatKm(kmTotali)} (andata: ${formatKm(formData.kmAndata)}, ritorno: ${formatKm(formData.kmRitorno)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Mezzi Non Utilizzati
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
    modifier: Modifier = Modifier
) {
    val selectedIds = mezzoSelections.filter { it.value.utilizzato }.keys

    var isSelectionExpanded by remember {
        mutableStateOf(selectedIds.isEmpty())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            MezzoSelectionSection(
                mezzoList = mezzoList,
                selectedIds = selectedIds,
                isExpanded = isSelectionExpanded,
                onToggleExpand = { isSelectionExpanded = !isSelectionExpanded },
                onSelectionChanged = { id, selected ->
                    val currentData = mezzoSelections[id] ?: MezzoFormData()
                    onMezzoDataChanged(id, currentData.copy(utilizzato = selected))
                }
            )
        }

        val selectedMezzi = mezzoList.filter { it.id in selectedIds }
        items(selectedMezzi, key = { it.id }) { mezzo ->
            val formData = mezzoSelections[mezzo.id] ?: MezzoFormData()
            MezzoDetailCard(
                mezzo = mezzo,
                formData = formData,
                onFormDataChanged = { newData ->
                    onMezzoDataChanged(mezzo.id, newData)
                }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun MezzoSelectionSection(
    mezzoList: List<Mezzo>,
    selectedIds: Set<Long>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectionChanged: (Long, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mezzi utilizzati (${selectedIds.size}/${mezzoList.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Comprimi" else "Espandi"
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    mezzoList.forEach { mezzo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectionChanged(mezzo.id, mezzo.id !in selectedIds)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = mezzo.id in selectedIds,
                                onCheckedChange = { checked ->
                                    onSelectionChanged(mezzo.id, checked)
                                }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = mezzo.nome,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = mezzo.targa,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MezzoDetailCard(
    mezzo: Mezzo,
    formData: MezzoFormData,
    onFormDataChanged: (MezzoFormData) -> Unit
) {
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
                text = mezzo.nome,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = mezzo.targa,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                    value = formData.kmAndata,
                    onValueChange = { value ->
                        onFormDataChanged(formData.copy(kmAndata = value))
                    },
                    label = "KM Andata",
                    modifier = Modifier.weight(1f)
                )
                DecimalTextField(
                    value = formData.kmRitorno,
                    onValueChange = { value ->
                        onFormDataChanged(formData.copy(kmRitorno = value))
                    },
                    label = "KM Ritorno",
                    modifier = Modifier.weight(1f)
                )
            }
        }
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

    LaunchedEffect(Unit) {
        if (value > 0 && textValue.isEmpty()) {
            textValue = value.toString().removeSuffix(".0")
        }
    }

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
                // Blocca valori negativi, imposta a 0 invece
                onValueChange(maxOf(0f, parsed))
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
