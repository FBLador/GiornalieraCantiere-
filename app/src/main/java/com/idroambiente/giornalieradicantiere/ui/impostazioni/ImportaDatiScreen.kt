package com.idroambiente.giornalieradicantiere.ui.impostazioni

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.idroambiente.giornalieradicantiere.ui.components.AppHeader
import com.idroambiente.giornalieradicantiere.ui.components.AppPanel
import com.idroambiente.giornalieradicantiere.ui.theme.AppTheme

/** Importazione di anagrafiche (CSV) o giornaliere (backup su Drive): riusa le sezioni esistenti. */
@Composable
fun ImportaDatiScreen(anagrafiche: Boolean, onNavigateBack: () -> Unit) {
    val c = AppTheme.colors
    Column(Modifier.fillMaxSize().background(c.bg)) {
        AppHeader(
            title = if (anagrafiche) "Importa anagrafiche" else "Importa giornaliere",
            subtitle = if (anagrafiche) "Persone, mezzi e attrezzature da CSV" else "Da una cartella di backup su Drive",
            onBack = onNavigateBack
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppPanel {
                if (anagrafiche) CsvImportSection() else GiornalieraImportSection()
            }
        }
    }
}
