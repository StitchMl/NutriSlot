package it.lagioiaproductions.nutrislot.ui.weeklyplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun WeeklySlotCard(
    modifier: Modifier = Modifier,
    slotUi: WeeklySlotUi,
    onManageClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleCompletedClick: () -> Unit,
    onAddToShoppingClick: () -> Unit
) {
    val presentation = rememberWeeklySlotCardPresentation(slotUi)
    val cardShape = MaterialTheme.shapes.medium

    ElevatedCard(
        onClick = onManageClick,
        modifier = modifier.border(
            width = 1.dp,
            color = presentation.visualStyle.border,
            shape = cardShape
        ),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = presentation.visualStyle.container
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(7.dp)
                    .background(presentation.visualStyle.accent)
            )

            WeeklySlotCardBody(
                presentation = presentation,
                slotUi = slotUi,
                onEditClick = onEditClick,
                onToggleCompletedClick = onToggleCompletedClick,
                onAddToShoppingClick = onAddToShoppingClick
            )
        }
    }
}
