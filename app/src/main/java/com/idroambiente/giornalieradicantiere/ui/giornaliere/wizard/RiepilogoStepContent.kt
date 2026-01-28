package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.database.entities.Attrezzatura
import com.idroambiente.giornalieradicantiere.database.entities.Mezzo
import com.idroambiente.giornalieradicantiere.database.entities.Personale
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

@Composable
fun RiepilogoStepContent(
    personaleList: List<Personale>,
    mezzoList: List<Mezzo>,
    attrezzaturaList: List<Attrezzatura>,
    personaleSelections: Map<Long, PersonaleFormData>,
    mezzoSelections: Map<Long, MezzoFormData>,
    attrezzaturaSelections: Map<Long, AttrezzaturaFormData>,
    riepilogo: RiepilogoData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

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

