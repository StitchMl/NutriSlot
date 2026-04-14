@file:Suppress("ConstPropertyName", "ConstPropertyName")

package it.lagioiaproductions.nutrislot.widget

import android.content.Context
import it.lagioiaproductions.nutrislot.data.local.room.NutriSlotDatabase
import it.lagioiaproductions.nutrislot.data.repository.WeeklyPlanRepository
import it.lagioiaproductions.nutrislot.domain.model.CurrentWeekWindow
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState
import it.lagioiaproductions.nutrislot.domain.model.currentWeekWindow
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.WeeklyPlanCustomizationManager
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.WeeklyPlanPreferences
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.WeeklySlotUi
import it.lagioiaproductions.nutrislot.ui.weeklyplan.slot.buildWeeklySlotUis
import it.lagioiaproductions.nutrislot.ui.weeklyplan.edit.stripStoredMealNutrition
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.runBlocking

internal data class MealCalendarWidgetItem(
    val slotId: String,
    val stableId: Long,
    val dayLabel: String,
    val dateLabel: String,
    val metaText: String,
    val mealText: String,
    val mealSlotType: MealSlotType,
    val isToday: Boolean,
    val isDimmed: Boolean
)

@Suppress("ConstPropertyName")
internal object MealCalendarWidgetSupport {
    private const val CompletedLabel = "Completato"
    private const val CompletedWithSwapLabel = "Completato con scambio"
    private const val MovedElsewhereLabel = "Spostato altrove"

    fun loadItems(
        context: Context,
        weekWindow: CurrentWeekWindow = currentWeekWindow(),
        locale: Locale = Locale.getDefault()
    ): List<MealCalendarWidgetItem> {
        val appContext = context.applicationContext
        val database = NutriSlotDatabase.getInstance(appContext)
        val repository = WeeklyPlanRepository(
            database = database
        )

        val snapshot = runBlocking {
            repository.getLatestWeeklyPlanSnapshot()
        } ?: return emptyList()

        val customizationManager = WeeklyPlanCustomizationManager(
            preferences = appContext.getSharedPreferences(
                WeeklyPlanPreferences.PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
        )

        val decoratedSlots = customizationManager.decorateSlots(
            snapshot = snapshot,
            slots = buildWeeklySlotUis(snapshot, customizationManager)
        )

        return buildItems(
            slots = decoratedSlots,
            weekWindow = weekWindow,
            locale = locale
        )
    }

    fun buildItems(
        slots: List<WeeklySlotUi>,
        weekWindow: CurrentWeekWindow = currentWeekWindow(),
        locale: Locale = Locale.getDefault()
    ): List<MealCalendarWidgetItem> {
        val currentDayOrder = weekWindow.currentWeekDay.sortOrder

        return slots.asSequence()
            .sortedWith(
                compareBy(
                    { it.dayOfWeek.sortOrder },
                    { it.mealSlotType.sortOrder }
                )
            )
            .filter { slot -> slot.dayOfWeek.sortOrder >= currentDayOrder }
            .mapNotNull { slot ->
                val mealText = stripStoredMealNutrition(slot.displayedMealText)
                if (mealText.isBlank()) {
                    return@mapNotNull null
                }

                val date = weekWindow.dateFor(slot.dayOfWeek)
                val statusLabel = statusLabelFor(slot)

                MealCalendarWidgetItem(
                    slotId = slot.slotId,
                    stableId = slot.slotId.hashCode().toLong(),
                    dayLabel = dayLabelFor(date, locale),
                    dateLabel = dateLabelFor(date, locale),
                    metaText = listOfNotNull(slot.mealSlotType.displayName, statusLabel)
                        .joinToString(separator = " | "),
                    mealText = mealText,
                    mealSlotType = slot.mealSlotType,
                    isToday = slot.dayOfWeek == weekWindow.currentWeekDay,
                    isDimmed = slot.displayState != SlotDisplayState.PlannedAvailable
                )
            }
            .toList()
    }

    fun weekRangeLabel(
        weekWindow: CurrentWeekWindow = currentWeekWindow(),
        locale: Locale = Locale.getDefault()
    ): String {
        val weekStart = weekWindow.weekStart
        val weekEnd = weekWindow.nextWeekStart.minusDays(1)

        return if (weekStart.month == weekEnd.month) {
            "${weekStart.dayOfMonth}-${weekEnd.dayOfMonth} ${shortMonthLabel(weekEnd, locale)}"
        } else {
            "${weekStart.dayOfMonth} ${shortMonthLabel(weekStart, locale)} - " +
                "${weekEnd.dayOfMonth} ${shortMonthLabel(weekEnd, locale)}"
        }
    }

    private fun statusLabelFor(slot: WeeklySlotUi): String? {
        return when (slot.displayState) {
            SlotDisplayState.Empty -> null
            SlotDisplayState.PlannedAvailable -> null
            SlotDisplayState.ConsumedAsPlanned -> CompletedLabel
            is SlotDisplayState.ConsumedWithReplacement -> CompletedWithSwapLabel
            SlotDisplayState.OriginalMealAlreadyUsedElsewhere -> MovedElsewhereLabel
        }
    }

    private fun dayLabelFor(
        date: LocalDate,
        locale: Locale
    ): String {
        return date.dayOfWeek
            .getDisplayName(TextStyle.SHORT, locale)
            .replace(".", "")
            .uppercase(locale)
    }

    private fun dateLabelFor(
        date: LocalDate,
        locale: Locale
    ): String {
        return "${date.dayOfMonth} ${shortMonthLabel(date, locale).uppercase(locale)}"
    }

    private fun shortMonthLabel(
        date: LocalDate,
        locale: Locale
    ): String {
        return date.month
            .getDisplayName(TextStyle.SHORT, locale)
            .replace(".", "")
            .lowercase(locale)
    }
}
