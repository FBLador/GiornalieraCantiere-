package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiornalieraWizardScreen(
    viewModel: GiornalieraWizardViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val personaleList by viewModel.personaleList.collectAsState()
    val mezzoList by viewModel.mezzoList.collectAsState()
    val attrezzaturaList by viewModel.attrezzaturaList.collectAsState()

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuova Giornaliera") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Indietro"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            StepProgressIndicator(
                currentStep = uiState.currentStep
            )

            HorizontalDivider()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.currentStep) {
                    0 -> PersonaleStepContent(
                        personaleList = personaleList,
                        personaleSelections = uiState.personaleSelections,
                        onPersonaleDataChanged = { personaleId, data ->
                            viewModel.updatePersonaleData(personaleId, data)
                        }
                    )
                    1 -> MezzoStepContent(
                        mezzoList = mezzoList,
                        mezzoSelections = uiState.mezzoSelections,
                        onMezzoDataChanged = { mezzoId, data ->
                            viewModel.updateMezzoData(mezzoId, data)
                        }
                    )
                    2 -> AttrezzaturaStepContent(
                        attrezzaturaList = attrezzaturaList,
                        attrezzaturaSelections = uiState.attrezzaturaSelections,
                        onAttrezzaturaDataChanged = { attrezzaturaId, data ->
                            viewModel.updateAttrezzaturaData(attrezzaturaId, data)
                        }
                    )
                    3 -> PlaceholderStepContent(
                        stepIndex = uiState.currentStep
                    )
                }

                if (uiState.isSaving) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            HorizontalDivider()

            WizardNavigationButtons(
                currentStep = uiState.currentStep,
                onPreviousClick = { viewModel.previousStep() },
                onNextClick = { viewModel.nextStep() },
                onSaveClick = { viewModel.saveGiornaliera() },
                isSaving = uiState.isSaving
            )
        }
    }
}

@Composable
private fun WizardNavigationButtons(
    currentStep: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSaveClick: () -> Unit,
    isSaving: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (currentStep > 0) {
            OutlinedButton(
                onClick = onPreviousClick,
                enabled = !isSaving
            ) {
                Text("Indietro")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (currentStep < 3) {
            Button(
                onClick = onNextClick,
                enabled = !isSaving
            ) {
                Text("Avanti")
            }
        } else {
            Button(
                onClick = onSaveClick,
                enabled = !isSaving
            ) {
                Text("Salva")
            }
        }
    }
}
