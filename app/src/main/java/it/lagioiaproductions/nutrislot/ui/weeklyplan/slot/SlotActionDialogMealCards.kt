package it.lagioiaproductions.nutrislot.ui.weeklyplan.slot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.ParsedMealSectionUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.mealSemanticLabel
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parser.parseMealSectionVisuals

@Composable
internal fun rememberDialogMealSections(mealText: String): List<ParsedMealSectionUi> {
    return remember(mealText) {
        parseMealSectionVisuals(mealText)
    }
}

@Composable
internal fun MealSectionsBlock(
    sections: List<ParsedMealSectionUi>
) {
    if (sections.isEmpty()) {
        Text(
            text = "Nessun contenuto disponibile.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            sections.forEachIndexed { index, section ->
                if (index > 0) {
                    SectionSeparatorBadge(text = "Alternativa ${index + 1}")
                }

                MealSectionCard(section = section)
            }
        }
    }
}

@Composable
private fun MealSectionCard(
    section: ParsedMealSectionUi
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val headline = section.lines.firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: mealSemanticLabel(section.visualInfo.semanticKey)

            Text(
                text = "${section.visualInfo.emoji} $headline",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            section.lines.drop(1).forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionSeparatorBadge(text: String) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
internal fun InlineTag(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}
