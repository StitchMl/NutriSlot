package it.lagioiaproductions.nutrislot.ui.weeklyplan.checklist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Returns the leading icon used by a checklist card. */
internal fun checklistStatusIcon(
    item: WeeklyQuantityChecklistItemUi
): ImageVector {
    return when {
        item.hasLinkedWaterTracking -> Icons.Filled.Opacity
        item.status == WeeklyQuantityChecklistStatusUi.UNDER_TARGET ||
            item.status == WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> Icons.Filled.ErrorOutline
        else -> Icons.Filled.CheckCircle
    }
}

/** Computes the checklist card container color from the current status. */
@Composable
internal fun checklistContainerColor(
    status: WeeklyQuantityChecklistStatusUi
): Color {
    return when (status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f)
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        WeeklyQuantityChecklistStatusUi.COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.48f)
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.62f)
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.64f)
    }
}

/** Computes the foreground accent color from the current status. */
@Composable
internal fun checklistContentColor(
    status: WeeklyQuantityChecklistStatusUi
): Color {
    return when (status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> MaterialTheme.colorScheme.primary
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> MaterialTheme.colorScheme.secondary
        WeeklyQuantityChecklistStatusUi.COMPLETED -> MaterialTheme.colorScheme.tertiary
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> MaterialTheme.colorScheme.tertiary
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> MaterialTheme.colorScheme.error
    }
}

/** Computes the progress bar color from the current status. */
@Composable
internal fun checklistProgressColor(
    status: WeeklyQuantityChecklistStatusUi
): Color {
    return when (status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> MaterialTheme.colorScheme.primary
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> MaterialTheme.colorScheme.secondary
        WeeklyQuantityChecklistStatusUi.COMPLETED -> MaterialTheme.colorScheme.tertiary
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> MaterialTheme.colorScheme.tertiary
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> MaterialTheme.colorScheme.error
    }
}

/** Produces the compact progress hint shown below the checklist status. */
internal fun checklistCompactHint(
    item: WeeklyQuantityChecklistItemUi
): String {
    return when (item.status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> {
            when (item.metric) {
                WeeklyQuantityChecklistMetricUi.MILLILITERS -> "Ancora ${shortMlLabel(item.remainingMinimumValue)}"
                WeeklyQuantityChecklistMetricUi.PORTIONS -> "Ancora ${item.remainingMinimumValue} porz."
                WeeklyQuantityChecklistMetricUi.OCCURRENCES -> "Ancora ${item.remainingMinimumValue} volte"
            }
        }
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> "Sei nel range"
        WeeklyQuantityChecklistStatusUi.COMPLETED -> "Fatto"
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> "Perfetto"
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> "Oltre il limite"
    }
}

/** Shortens milliliter values for compact checklist labels. */
private fun shortMlLabel(
    value: Int
): String {
    return if (value >= 1000 && value % 1000 == 0) {
        "${value / 1000}L"
    } else {
        "$value ml"
    }
}

/** Maps the checklist item to an emoji shortcut used in the title. */
internal fun targetEmoji(
    item: WeeklyQuantityChecklistItemUi
): String {
    val normalizedKey = "${item.id} ${item.title}".lowercase()
    return when {
        normalizedKey.contains("acqua") -> "\uD83D\uDCA7"
        normalizedKey.contains("frutta") || normalizedKey.contains("verdura") -> "\uD83E\uDD66"
        normalizedKey.contains("caffe") || normalizedKey.contains("th") -> "\u2615"
        normalizedKey.contains("carne bianca") -> "\uD83C\uDF57"
        normalizedKey.contains("carne rossa") -> "\uD83E\uDD69"
        normalizedKey.contains("affettati") -> "\uD83E\uDD53"
        normalizedKey.contains("uova") -> "\uD83E\uDD5A"
        normalizedKey.contains("formaggi") -> "\uD83E\uDDC0"
        normalizedKey.contains("patate") -> "\uD83E\uDD54"
        normalizedKey.contains("piatto unico") || normalizedKey.contains("legumi") -> "\uD83C\uDF72"
        normalizedKey.contains("pesce") -> "\uD83D\uDC1F"
        item.period == WeeklyQuantityChecklistPeriodUi.DAILY -> "\uD83C\uDF1E"
        else -> "\uD83D\uDCCC"
    }
}

/** Emoji shortcut for the target period. */
internal fun periodEmoji(
    item: WeeklyQuantityChecklistItemUi
): String {
    return if (item.period == WeeklyQuantityChecklistPeriodUi.DAILY) "\uD83C\uDF1E" else "\uD83D\uDCC5"
}

/** Emoji shortcut for the current target status. */
internal fun statusEmoji(
    status: WeeklyQuantityChecklistStatusUi
): String {
    return when (status) {
        WeeklyQuantityChecklistStatusUi.UNDER_TARGET -> "\u26A0\uFE0F"
        WeeklyQuantityChecklistStatusUi.ON_TRACK -> "\uD83D\uDFE2"
        WeeklyQuantityChecklistStatusUi.COMPLETED -> "\u2705"
        WeeklyQuantityChecklistStatusUi.LIMIT_REACHED -> "\uD83C\uDFAF"
        WeeklyQuantityChecklistStatusUi.OVER_LIMIT -> "\uD83D\uDEAB"
    }
}
