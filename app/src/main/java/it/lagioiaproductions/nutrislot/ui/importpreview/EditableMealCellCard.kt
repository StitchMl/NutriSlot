package it.lagioiaproductions.nutrislot.ui.importpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.lagioiaproductions.nutrislot.domain.model.CellRecognitionState
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.ui.importfile.EditableImportedMealCellUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.ParsedMealSectionUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.parseMealSectionVisuals

@Composable
internal fun EditableMealCellCard(
    modifier: Modifier = Modifier,
    cell: EditableImportedMealCellUi?,
    slotType: MealSlotType,
    onClick: () -> Unit
) {
    val parsedSections = remember(cell?.mealText) {
        cell?.mealText
            ?.let(::stripNutritionForPreview)
            ?.let(::parseMealSectionVisuals)
            .orEmpty()
    }

    val preview = remember(parsedSections, slotType) {
        buildPreviewCellContent(
            parsedSections = parsedSections,
            fallbackTitle = slotType.displayName,
            fallbackEmoji = fallbackEmojiForSlot(slotType)
        )
    }

    val style = remember(
        cell?.mealSlotType ?: slotType,
        cell?.originalRecognitionState,
        cell?.wasManuallyEdited,
        parsedSections.firstOrNull()?.visualInfo?.semanticKey
    ) {
        previewVisualStyleForSlot(
            slotType = cell?.mealSlotType ?: slotType,
            recognitionState = cell?.originalRecognitionState ?: CellRecognitionState.EMPTY,
            wasManuallyEdited = cell?.wasManuallyEdited ?: false,
            semanticKey = parsedSections.firstOrNull()?.visualInfo?.semanticKey
        )
    }

    ElevatedCard(
        modifier = modifier
            .heightIn(min = 156.dp)
            .clickable(enabled = cell != null, onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = style.container
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(style.accent)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = style.badgeContainer
                    ) {
                        Text(
                            text = recognitionLabel(
                                state = cell?.originalRecognitionState ?: CellRecognitionState.EMPTY,
                                wasManuallyEdited = cell?.wasManuallyEdited ?: false
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = style.badgeContent
                        )
                    }

                    Text(
                        text = "${preview.primaryEmoji} ${preview.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = style.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (cell == null || cell.mealText.isBlank()) {
                    Text(
                        text = "Tocca per aggiungere o correggere",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    preview.sectionChips.forEach { chip ->
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = style.accent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${chip.emoji} ${chip.label}",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = style.meta,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    preview.supportingLines.forEachIndexed { index, line ->
                        Text(
                            text = line,
                            style = if (index == 0) {
                                MaterialTheme.typography.bodyMedium
                            } else {
                                MaterialTheme.typography.bodySmall
                            },
                            fontWeight = if (index == 0) FontWeight.Medium else FontWeight.Normal,
                            color = if (index == 0) style.body else style.meta,
                            maxLines = if (index == 0) 2 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = if (cell == null) "Vuoto" else "Tocca per modificare",
                    style = MaterialTheme.typography.labelMedium,
                    color = style.accent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun recognitionLabel(
    state: CellRecognitionState,
    wasManuallyEdited: Boolean
): String {
    if (wasManuallyEdited) return "Modificato"

    return when (state) {
        CellRecognitionState.RECOGNIZED -> "OK"
        CellRecognitionState.SUSPECTED -> "Dubbio"
        CellRecognitionState.MISSING_DAY -> "Giorno?"
        CellRecognitionState.MISSING_MEAL_SLOT -> "Slot?"
        CellRecognitionState.EMPTY -> "Vuoto"
    }
}

private data class PreviewCellVisualStyle(
    val container: Color,
    val accent: Color,
    val title: Color,
    val body: Color,
    val meta: Color,
    val badgeContainer: Color,
    val badgeContent: Color
)

private fun previewVisualStyleForSlot(
    slotType: MealSlotType,
    recognitionState: CellRecognitionState,
    wasManuallyEdited: Boolean,
    semanticKey: String?
): PreviewCellVisualStyle {
    val base = semanticPreviewStyle(semanticKey) ?: fallbackPreviewStyle(slotType)

    return when {
        wasManuallyEdited -> base.copy(
            badgeContainer = Color(0xFFDFF0E0),
            badgeContent = Color(0xFF1F5A28)
        )
        recognitionState == CellRecognitionState.RECOGNIZED -> base.copy(
            badgeContainer = Color(0xFFDDEAFE),
            badgeContent = Color(0xFF1E4E8C)
        )
        recognitionState == CellRecognitionState.EMPTY -> base.copy(
            badgeContainer = Color(0xFFE7E7E7),
            badgeContent = Color(0xFF4A4A4A)
        )
        else -> base.copy(
            badgeContainer = Color(0xFFFFE1DE),
            badgeContent = Color(0xFF8C2F27)
        )
    }
}

private fun semanticPreviewStyle(
    semanticKey: String?
): PreviewCellVisualStyle? {
    return when (semanticKey) {
        "panino" -> basePreviewStyle(Color(0xFFF5E5D0), Color(0xFFC78A48))
        "piadina" -> basePreviewStyle(Color(0xFFFFE3D1), Color(0xFFE58D5E))
        "frisella" -> basePreviewStyle(Color(0xFFF2E7D6), Color(0xFFB98A52))
        "insalata", "verdura", "avocado" -> basePreviewStyle(Color(0xFFE3F6E4), Color(0xFF59B86D))
        "cereale_primo" -> basePreviewStyle(Color(0xFFFFE3C2), Color(0xFFF08A24))
        "carne" -> basePreviewStyle(Color(0xFFFFD8D3), Color(0xFFE96A5F))
        "pesce" -> basePreviewStyle(Color(0xFFD9EEFF), Color(0xFF4DA3FF))
        "uova" -> basePreviewStyle(Color(0xFFFFF3C8), Color(0xFFE0B400))
        "latticino", "formaggio" -> basePreviewStyle(Color(0xFFE8F3FF), Color(0xFF6BA4FF))
        "frutta", "banana", "mela", "pera" -> basePreviewStyle(Color(0xFFFFDCE8), Color(0xFFFF5C8A))
        "pane" -> basePreviewStyle(Color(0xFFF2E2CC), Color(0xFFC78A48))
        "colazione_secca", "pancake", "dolce_spalmabile", "caffe" -> basePreviewStyle(Color(0xFFFFEBD9), Color(0xFFFFA36C))
        "olio" -> basePreviewStyle(Color(0xFFEAF4CF), Color(0xFF97B63E))
        else -> null
    }
}

private fun fallbackPreviewStyle(
    slotType: MealSlotType
): PreviewCellVisualStyle {
    return when (slotType) {
        MealSlotType.BREAKFAST -> basePreviewStyle(
            container = Color(0xFFFFF4EC),
            accent = Color(0xFFFFA36C)
        )
        MealSlotType.MORNING_SNACK -> basePreviewStyle(
            container = Color(0xFFF1FAF1),
            accent = Color(0xFF73C27C)
        )
        MealSlotType.LUNCH -> basePreviewStyle(
            container = Color(0xFFEDF5FF),
            accent = Color(0xFF5AA9FF)
        )
        MealSlotType.AFTERNOON_SNACK -> basePreviewStyle(
            container = Color(0xFFFFF4E3),
            accent = Color(0xFFFFC15A)
        )
        MealSlotType.DINNER -> basePreviewStyle(
            container = Color(0xFFF4F0FF),
            accent = Color(0xFF9A89FF)
        )
    }
}

private fun basePreviewStyle(
    container: Color,
    accent: Color
): PreviewCellVisualStyle {
    return PreviewCellVisualStyle(
        container = container,
        accent = accent,
        title = Color(0xFF1F1A17),
        body = Color(0xFF2F2925),
        meta = Color(0xFF665E57),
        badgeContainer = Color(0xFFE7E7E7),
        badgeContent = Color(0xFF4A4A4A)
    )
}

private data class PreviewCellContentUi(
    val primaryEmoji: String,
    val title: String,
    val supportingLines: List<String>,
    val sectionChips: List<PreviewSectionChipUi>
)

private data class PreviewSectionChipUi(
    val emoji: String,
    val label: String
)

private fun buildPreviewCellContent(
    parsedSections: List<ParsedMealSectionUi>,
    fallbackTitle: String,
    fallbackEmoji: String
): PreviewCellContentUi {
    if (parsedSections.isEmpty()) {
        return PreviewCellContentUi(
            primaryEmoji = fallbackEmoji,
            title = fallbackTitle,
            supportingLines = emptyList(),
            sectionChips = emptyList()
        )
    }

    val primarySection = parsedSections.first()
    val firstLine = normalizePreviewLine(primarySection.lines.firstOrNull().orEmpty())
    val title = firstLine.ifBlank { fallbackTitle }

    val supportingLines = buildList {
        addAll(primarySection.lines.drop(1))
    }
        .map(::normalizePreviewLine)
        .filter { it.isNotBlank() }
        .distinctBy { it.lowercase() }
        .take(2)

    val chips = parsedSections
        .take(2)
        .map { section ->
            PreviewSectionChipUi(
                emoji = section.visualInfo.emoji,
                label = ellipsize(
                    text = section.lines.firstOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: previewSemanticLabel(section.visualInfo.semanticKey),
                    maxLength = 24
                )
            )
        }
        .distinctBy { "${it.emoji}|${it.label}" }

    return PreviewCellContentUi(
        primaryEmoji = primarySection.visualInfo.emoji,
        title = ellipsize(title, 56),
        supportingLines = supportingLines,
        sectionChips = chips
    )
}

private fun normalizePreviewLine(line: String): String {
    return stripNutritionForPreview(line)
        .removePrefix("•")
        .removePrefix("-")
        .removePrefix("–")
        .removePrefix("—")
        .trim()
        .replace(Regex("\\s+"), " ")
        .removeSuffix(".")
        .trim()
}

private fun ellipsize(text: String, maxLength: Int): String {
    if (text.length <= maxLength) return text
    return text.take(maxLength - 1).trimEnd() + "…"
}

private fun fallbackEmojiForSlot(slotType: MealSlotType): String {
    return when (slotType) {
        MealSlotType.BREAKFAST -> "🥣"
        MealSlotType.MORNING_SNACK -> "🍏"
        MealSlotType.LUNCH -> "🍽️"
        MealSlotType.AFTERNOON_SNACK -> "🥜"
        MealSlotType.DINNER -> "🍽️"
    }
}

private fun previewSemanticLabel(semanticKey: String): String {
    return when (semanticKey) {
        "panino" -> "Panino"
        "piadina" -> "Piadina"
        "frisella" -> "Frisella"
        "insalata" -> "Insalata"
        "cereale_primo" -> "Primo"
        "pancake" -> "Pancake"
        "latticino" -> "Latticino"
        "carne" -> "Carne"
        "pesce" -> "Pesce"
        "uova" -> "Uova"
        "pane" -> "Pane"
        "colazione_secca" -> "Colazione"
        "banana" -> "Banana"
        "mela" -> "Mela"
        "pera" -> "Pera"
        "frutta" -> "Frutta"
        "frutta_secca" -> "Frutta secca"
        "avocado" -> "Avocado"
        "formaggio" -> "Formaggio"
        "pomodoro" -> "Pomodoro"
        "carota" -> "Carota"
        "verdura" -> "Verdura"
        "olio" -> "Olio"
        "dolce_spalmabile" -> "Dolce"
        "caffe" -> "Caffè"
        else -> "Pasto"
    }
}

private fun stripNutritionForPreview(text: String): String {
    val normalized = text
        .replace("\r\n", "\n")
        .replace("\r", "\n")

    val lines = normalized.lines()
    if (lines.size > 1) {
        val keptLines = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) {
                keptLines += line
                continue
            }
            if (isNutritionLineLocal(trimmed)) break
            keptLines += line
        }

        val joined = keptLines.joinToString(separator = "\n").trim()
        if (joined.isNotBlank()) return joined
    }

    return normalized
        .replace(Regex("(?is)\\bNutrienti\\s*:.*$"), "")
        .replace(
            Regex("(?is)\\bTot\\.?\\s*(?:kcal|g\\s+proteine|g\\s+carboidrati|g\\s+fibre|g\\s+grassi|g\\s+lipidi)\\b.*$"),
            ""
        )
        .trim()
}

private fun isNutritionLineLocal(line: String): Boolean {
    val normalized = line
        .lowercase()
        .replace("’", "'")
        .replace(Regex("\\s+"), " ")
        .trim()

    return normalized.startsWith("nutrienti") ||
            normalized.startsWith("tot kcal") ||
            normalized.startsWith("tot. kcal") ||
            normalized.startsWith("tot g proteine") ||
            normalized.startsWith("tot. g proteine") ||
            normalized.startsWith("tot g carboidrati") ||
            normalized.startsWith("tot. g carboidrati") ||
            normalized.startsWith("tot g fibre") ||
            normalized.startsWith("tot. g fibre") ||
            normalized.startsWith("tot g grassi") ||
            normalized.startsWith("tot. g grassi") ||
            normalized.startsWith("tot g lipidi") ||
            normalized.startsWith("tot. g lipidi")
}