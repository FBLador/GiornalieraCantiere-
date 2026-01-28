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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura

@Composable
fun AttrezzaturaStepContent(
    attrezzaturaList: List<Attrezzatura>,
    attrezzaturaSelections: Map<Long, AttrezzaturaFormData>,
    onAttrezzaturaDataChanged: (Long, AttrezzaturaFormData) -> Unit,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
) {
    if (attrezzaturaList.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Nessuna attrezzatura disponibile",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aggiungi attrezzature nelle Impostazioni",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else if (isReadOnly) {
        AttrezzaturaViewMode(
            attrezzaturaList = attrezzaturaList,
            attrezzaturaSelections = attrezzaturaSelections,
            modifier = modifier
        )
    } else {
        AttrezzaturaEditMode(
            attrezzaturaList = attrezzaturaList,
            attrezzaturaSelections = attrezzaturaSelections,
            onAttrezzaturaDataChanged = onAttrezzaturaDataChanged,
            modifier = modifier
        )
    }
}

@Composable
private fun AttrezzaturaViewMode(
    attrezzaturaList: List<Attrezzatura>,
    attrezzaturaSelections: Map<Long, AttrezzaturaFormData>,
    modifier: Modifier = Modifier
) {
    val attrezzatureUtilizzate = attrezzaturaList.filter { attrezzatura ->
        attrezzaturaSelections[attrezzatura.id]?.utilizzata == true
    }
    val attrezzatureNonUtilizzate = attrezzaturaList.filter { attrezzatura ->
        attrezzaturaSelections[attrezzatura.id]?.utilizzata != true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Attrezzature Utilizzate
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ATTREZZATURE UTILIZZATE (${attrezzatureUtilizzate.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )

                if (attrezzatureUtilizzate.isEmpty()) {
                    Text(
                        text = "Nessuna attrezzatura utilizzata",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                    )
                } else {
                    attrezzatureUtilizzate.forEachIndexed { index, attrezzatura ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }

                        Text(
                            text = attrezzatura.nome,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Attrezzature Non Utilizzate
        if (attrezzatureNonUtilizzate.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ATTREZZATURE NON UTILIZZATE (${attrezzatureNonUtilizzate.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )

                    attrezzatureNonUtilizzate.forEachIndexed { index, attrezzatura ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        }

                        Text(
                            text = attrezzatura.nome,
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

@Composable
private fun AttrezzaturaEditMode(
    attrezzaturaList: List<Attrezzatura>,
    attrezzaturaSelections: Map<Long, AttrezzaturaFormData>,
    onAttrezzaturaDataChanged: (Long, AttrezzaturaFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIds = attrezzaturaSelections.filter { it.value.utilizzata }.keys

    var isExpanded by remember {
        mutableStateOf(true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attrezzature utilizzate (${selectedIds.size}/${attrezzaturaList.size})",
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
                        attrezzaturaList.forEach { attrezzatura ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val isCurrentlySelected = attrezzatura.id in selectedIds
                                        onAttrezzaturaDataChanged(
                                            attrezzatura.id,
                                            AttrezzaturaFormData(utilizzata = !isCurrentlySelected)
                                        )
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = attrezzatura.id in selectedIds,
                                    onCheckedChange = { checked ->
                                        onAttrezzaturaDataChanged(
                                            attrezzatura.id,
                                            AttrezzaturaFormData(utilizzata = checked)
                                        )
                                    }
                                )
                                Text(
                                    text = attrezzatura.nome,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
