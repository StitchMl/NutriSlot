package it.lagioiaproductions.nutrislot.widget

import it.lagioiaproductions.nutrislot.domain.model.CurrentWeekWindow
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.SlotDisplayState
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import it.lagioiaproductions.nutrislot.ui.weeklyplan.WeeklySlotUi
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MealCalendarWidgetSupportTest {

    @Test
    fun buildItems_keepsOnlyRowsFromTodayAndStripsStoredNutrition() {
        val items = MealCalendarWidgetSupport.buildItems(
            slots = listOf(
                widgetSlot(
                    slotId = "plan_MONDAY_LUNCH",
                    day = WeekDay.MONDAY,
                    mealSlotType = MealSlotType.LUNCH,
                    displayedMealText = "Pasta al pomodoro"
                ),
                widgetSlot(
                    slotId = "plan_WEDNESDAY_BREAKFAST",
                    day = WeekDay.WEDNESDAY,
                    mealSlotType = MealSlotType.BREAKFAST,
                    displayedMealText = "Yogurt greco\n\nNutrienti: 220 kcal"
                ),
                widgetSlot(
                    slotId = "plan_THURSDAY_DINNER",
                    day = WeekDay.THURSDAY,
                    mealSlotType = MealSlotType.DINNER,
                    displayedMealText = ""
                ),
                widgetSlot(
                    slotId = "plan_FRIDAY_LUNCH",
                    day = WeekDay.FRIDAY,
                    mealSlotType = MealSlotType.LUNCH,
                    displayedMealText = "Riso basmati"
                )
            ),
            weekWindow = fixedWeekWindow(today = LocalDate.of(2026, 4, 8)),
            locale = Locale.ENGLISH
        )

        assertEquals(
            listOf("plan_WEDNESDAY_BREAKFAST", "plan_FRIDAY_LUNCH"),
            items.map { it.slotId }
        )
        assertEquals("Yogurt greco", items.first().mealText)
        assertTrue(items.first().isToday)
    }

    @Test
    fun buildItems_addsStatusTextForCompletedAndMovedMeals() {
        val items = MealCalendarWidgetSupport.buildItems(
            slots = listOf(
                widgetSlot(
                    slotId = "plan_MONDAY_DINNER",
                    day = WeekDay.MONDAY,
                    mealSlotType = MealSlotType.DINNER,
                    displayedMealText = "Salmone",
                    displayState = SlotDisplayState.ConsumedAsPlanned,
                    isActuallyCompletedThisWeek = true
                ),
                widgetSlot(
                    slotId = "plan_TUESDAY_LUNCH",
                    day = WeekDay.TUESDAY,
                    mealSlotType = MealSlotType.LUNCH,
                    displayedMealText = "Insalata di farro",
                    displayState = SlotDisplayState.OriginalMealAlreadyUsedElsewhere
                )
            ),
            weekWindow = fixedWeekWindow(today = LocalDate.of(2026, 4, 6)),
            locale = Locale.ENGLISH
        )

        assertEquals("Cena | Completato", items[0].metaText)
        assertEquals("Pranzo | Spostato altrove", items[1].metaText)
        assertTrue(items.all { it.isDimmed })
    }

    private fun fixedWeekWindow(today: LocalDate): CurrentWeekWindow {
        val weekStart = LocalDate.of(2026, 4, 6)
        return CurrentWeekWindow(
            zoneId = ZoneId.of("Europe/Rome"),
            today = today,
            weekStart = weekStart,
            nextWeekStart = weekStart.plusWeeks(1)
        )
    }

    private fun widgetSlot(
        slotId: String,
        day: WeekDay,
        mealSlotType: MealSlotType,
        displayedMealText: String,
        displayState: SlotDisplayState = SlotDisplayState.PlannedAvailable,
        isActuallyCompletedThisWeek: Boolean = false
    ): WeeklySlotUi {
        return WeeklySlotUi(
            slotId = slotId,
            dayOfWeek = day,
            mealSlotType = mealSlotType,
            originalMealText = displayedMealText,
            displayedMealText = displayedMealText,
            displayState = displayState,
            isActuallyCompletedThisWeek = isActuallyCompletedThisWeek
        )
    }
}
