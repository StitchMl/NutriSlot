package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun WeeklySlotCardBody(
    presentation: WeeklySlotCardPresentation,
    slotUi: WeeklySlotUi,
    onEditClick: () -> Unit,
    onToggleCompletedClick: () -> Unit,
    onAddToShoppingClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        WeeklySlotCardHeader(
            presentation = presentation,
            onEditClick = onEditClick,
            onToggleCompletedClick = onToggleCompletedClick,
            onAddToShoppingClick = onAddToShoppingClick
        )

        Text(
            text = presentation.content.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = presentation.visualStyle.title
        )

        presentation.content.detailLines.forEachIndexed { index, line ->
            Text(
                text = line,
                style = if (index == 0) {
                    MaterialTheme.typography.bodySmall
                } else {
                    MaterialTheme.typography.labelSmall
                },
                fontWeight = if (index == 0) FontWeight.Medium else FontWeight.Normal,
                color = if (index == 0) {
                    presentation.visualStyle.body
                } else {
                    presentation.visualStyle.meta
                }
            )
        }

        presentation.content.alternativeLines.forEach { alternative ->
            CompactBadge(
                text = alternative,
                background = presentation.visualStyle.accent.copy(alpha = 0.10f),
                content = presentation.visualStyle.meta
            )
        }

        if (presentation.showNutritionInline) {
            slotUi.nutritionSummary?.let { nutritionSummary ->
                CompactBadge(
                    text = "Nutrienti: $nutritionSummary",
                    background = presentation.visualStyle.accent.copy(alpha = 0.10f),
                    content = presentation.visualStyle.meta
                )
            }
        }

        presentation.footerNote?.let { footerNote ->
            Text(
                text = footerNote,
                style = MaterialTheme.typography.labelSmall,
                color = presentation.visualStyle.meta
            )
        }

        if (slotUi.hasCustomizations) {
            Text(
                text = "Personalizzato",
                style = MaterialTheme.typography.labelSmall,
                color = presentation.visualStyle.meta,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WeeklySlotCardHeader(
    presentation: WeeklySlotCardPresentation,
    onEditClick: () -> Unit,
    onToggleCompletedClick: () -> Unit,
    onAddToShoppingClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = buildString {
                presentation.content.primaryEmoji?.let { emoji ->
                    append(emoji)
                    append("  ")
                }
                append(presentation.timeLabel)
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = presentation.visualStyle.meta
        )

        WeeklySlotCardActions(
            presentation = presentation,
            onEditClick = onEditClick,
            onToggleCompletedClick = onToggleCompletedClick,
            onAddToShoppingClick = onAddToShoppingClick
        )
    }
}

@Composable
private fun WeeklySlotCardActions(
    presentation: WeeklySlotCardPresentation,
    onEditClick: () -> Unit,
    onToggleCompletedClick: () -> Unit,
    onAddToShoppingClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            onClick = onEditClick,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Modifica slot",
                modifier = Modifier.size(18.dp)
            )
        }

        if (presentation.canToggleCompleted) {
            FilledIconButton(
                onClick = onToggleCompletedClick,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = presentation.completionContainer,
                    contentColor = presentation.completionContent
                )
            ) {
                Icon(
                    imageVector = if (presentation.isCompletedState) {
                        Icons.Default.RemoveCircle
                    } else {
                        Icons.Default.CheckCircle
                    },
                    contentDescription = if (presentation.isCompletedState) {
                        "Annulla completamento"
                    } else {
                        "Segna completato"
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        FilledIconButton(
            onClick = onAddToShoppingClick,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Aggiungi alla spesa",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CompactBadge(
    text: String,
    background: Color,
    content: Color
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = background
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            fontWeight = FontWeight.Medium
        )
    }
}
