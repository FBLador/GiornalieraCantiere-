package com.idroambiente.giornalieradicantiere.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = modifier.padding(vertical = 4.dp)
    val shape = MaterialTheme.shapes.medium
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        disabledContainerColor = MaterialTheme.colorScheme.surface
    )
    val elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))

    val cardContent: @Composable ColumnScope.() -> Unit = { content() }
    if (onClick != null) {
        Card(
            modifier = cardModifier,
            onClick = onClick,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = cardContent
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = cardContent
        )
    }
}
