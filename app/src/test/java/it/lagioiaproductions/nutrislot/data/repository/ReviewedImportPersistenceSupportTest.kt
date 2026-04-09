package it.lagioiaproductions.nutrislot.data.repository

import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealCell
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealOption
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedMealRule
import it.lagioiaproductions.nutrislot.data.repository.model.ReviewedImportedWeeklyFrequencyTarget
import it.lagioiaproductions.nutrislot.domain.model.MealOptionSourceType
import it.lagioiaproductions.nutrislot.domain.model.MealSlotType
import it.lagioiaproductions.nutrislot.domain.model.WeekDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewedImportPersistenceSupportTest {

    @Test
    fun buildImportedPlanPersistencePayload_reusesLatestPlanIdDuringReimport() {
        val payload = buildImportedPlanPersistencePayload(
            existingPlanId = ExistingPlanId,
            sourceFileName = "Piano aprile.pdf",
            cells = listOf(
                ReviewedImportedMealCell(
                    dayOfWeek = WeekDay.MONDAY,
                    mealSlotType = MealSlotType.LUNCH,
                    mealText = "  Pasta \r\n al pomodoro  "
                )
            ),
            extraOptions = listOf(
                ReviewedImportedMealOption(
                    mealSlotType = MealSlotType.DINNER,
                    title = "Alternativa",
                    mealText = "Salmone",
                    sourceType = MealOptionSourceType.OTHER,
                    tags = listOf("omega 3"),
                    pageNumber = null
                )
            ),
            mealRules = listOf(
                ReviewedImportedMealRule(
                    mealSlotType = MealSlotType.LUNCH,
                    label = "Composizione",
                    requiredComponents = listOf("Proteine", "Verdure"),
                    pageNumber = null
                )
            ),
            weeklyTargets = listOf(
                ReviewedImportedWeeklyFrequencyTarget(
                    title = "Pesce",
                    canonicalKey = "pesce",
                    portionText = "150 g",
                    minimumTimesPerWeek = 2,
                    maximumTimesPerWeek = 4,
                    matchTerms = listOf("pesce", "salmone"),
                    pageNumber = null,
                    sourceText = null
                )
            ),
            createdAtEpochMillis = 1234L
        )

        assertTrue(payload.reusedExistingPlanId)
        assertEquals(ExistingPlanId, payload.plan.id)
        assertEquals("Piano aprile", payload.plan.title)
        assertEquals(1234L, payload.plan.createdAtEpochMillis)
        assertEquals(
            "Pasta\nal pomodoro",
            payload.slots.first { it.id == "${ExistingPlanId}_MONDAY_LUNCH" }.plannedMealText
        )
        assertEquals("${ExistingPlanId}_OPTION_0", payload.options.single().id)
        assertEquals("${ExistingPlanId}_RULE_0", payload.rules.single().id)
        assertEquals("${ExistingPlanId}_TARGET_0", payload.weeklyTargets.single().id)
    }

    @Test
    fun buildImportedPlanPersistencePayload_createsNewPlanIdWhenNothingExistsYet() {
        val payload = buildImportedPlanPersistencePayload(
            existingPlanId = null,
            sourceFileName = "nuovo_piano.pdf",
            cells = emptyList()
        )

        assertFalse(payload.reusedExistingPlanId)
        assertTrue(payload.plan.id.isNotBlank())
        assertEquals(payload.plan.id, payload.slots.first().planId)
        assertEquals(35, payload.slots.size)
    }

    private companion object {
        const val ExistingPlanId = "plan-attivo"
    }
}
