package com.idroambiente.giornalieradicantiere.ui.giornaliere.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class WizardStep(
    val label: String,
    val index: Int
)

val wizardSteps = listOf(
    WizardStep("Persone", 0),
    WizardStep("Mezzi", 1),
    WizardStep("Attrezz.", 2),
    WizardStep("Generale", 3)
)

@Composable
fun StepProgressIndicator(
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        wizardSteps.forEachIndexed { index, step ->
            if (index > 0) {
                StepConnector(
                    isCompleted = index <= currentStep,
                    modifier = Modifier.weight(1f)
                )
            }
            StepCircle(
                step = step,
                isCompleted = index < currentStep,
                isCurrent = index == currentStep
            )
        }
    }
}

@Composable
private fun StepCircle(
    step: WizardStep,
    isCompleted: Boolean,
    isCurrent: Boolean
) {
    val backgroundColor = when {
        isCompleted -> MaterialTheme.colorScheme.primary
        isCurrent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isCompleted || isCurrent -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = "${step.index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
        Text(
            text = step.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCurrent || isCompleted)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun StepConnector(
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .height(2.dp)
            .background(
                if (isCompleted)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
    )
}
