package com.idroambiente.giornalieradicantiere.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.idroambiente.giornalieradicantiere.database.entities.StatoCommessa
import com.idroambiente.giornalieradicantiere.ui.theme.AppTheme

data class StatoCommessaUi(val label: String, val icon: ImageVector, val color: Color)

@Composable
fun StatoCommessa.ui(): StatoCommessaUi {
    val c = AppTheme.colors
    return when (this) {
        StatoCommessa.APERTA -> StatoCommessaUi("Aperta", Icons.Default.PlayCircle, c.pri)
        StatoCommessa.SOSPESA -> StatoCommessaUi("Sospesa", Icons.Default.PauseCircle, c.warnInk)
        StatoCommessa.CHIUSA -> StatoCommessaUi("Chiusa", Icons.Default.Inventory2, c.sub)
    }
}
