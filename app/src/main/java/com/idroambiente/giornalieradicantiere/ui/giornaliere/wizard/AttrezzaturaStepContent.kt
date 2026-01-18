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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura

@Composable
fun AttrezzaturaStepContent(
    attrezzaturaList: List<Attrezzatura>,
    attrezzaturaSelections: Map<Long, AttrezzaturaFormData>,
    onAttrezzaturaDataChanged: (Long, AttrezzaturaFormData) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIds = attrezzaturaSelections.filter { it.value.utilizzata }.keys

    var isExpanded by remember {
        mutableStateOf(true)
    }

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
    } else {
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
}
