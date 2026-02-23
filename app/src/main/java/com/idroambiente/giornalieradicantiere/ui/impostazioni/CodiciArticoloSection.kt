package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CodiciArticoloSection(
    codiceOreLavoro: String,
    codiceOreViaggio: String,
    onCodiceOreLavoroChange: (String) -> Unit,
    onCodiceOreViaggioChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingField by remember { mutableStateOf<String?>(null) }
    var editValue by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Codici Articolo CSV",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        ListItem(
            headlineContent = { Text("Ore Lavoro") },
            supportingContent = { Text(codiceOreLavoro) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    editingField = "oreLavoro"
                    editValue = codiceOreLavoro
                }
        )
        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Ore Viaggio") },
            supportingContent = { Text(codiceOreViaggio) },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    editingField = "oreViaggio"
                    editValue = codiceOreViaggio
                }
        )
        HorizontalDivider()
    }

    editingField?.let { field ->
        val title = if (field == "oreLavoro") "Codice Articolo Ore Lavoro" else "Codice Articolo Ore Viaggio"
        AlertDialog(
            onDismissRequest = { editingField = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text("Codice Articolo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (field == "oreLavoro") {
                        onCodiceOreLavoroChange(editValue.trim())
                    } else {
                        onCodiceOreViaggioChange(editValue.trim())
                    }
                    editingField = null
                }) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingField = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}
