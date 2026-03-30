package it.lagioiaproductions.nutrislot.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WeeklyFrequencyTargetSupportTest {

    @Test
    fun `parseFrequencyTargetRule handles the supported real world notes`() {
        val cases = listOf(
            Triple(
                "Consumare almeno 5 porzioni di frutta e verdura al giorno",
                WeeklyFrequencyTargetSupport.FrequencyTargetRule(
                    period = WeeklyFrequencyTargetSupport.FrequencyTargetPeriod.DAY,
                    measure = WeeklyFrequencyTargetSupport.FrequencyTargetMeasure.PORTIONS,
                    minimumValue = 5,
                    maximumValue = null
                ),
                "frutta e verdura"
            ),
            Triple(
                "Bere almeno 2L di acqua/die",
                WeeklyFrequencyTargetSupport.FrequencyTargetRule(
                    period = WeeklyFrequencyTargetSupport.FrequencyTargetPeriod.DAY,
                    measure = WeeklyFrequencyTargetSupport.FrequencyTargetMeasure.MILLILITERS,
                    minimumValue = 2000,
                    maximumValue = null
                ),
                "acqua"
            ),
            Triple(
                "Consumare massimo N.3 caffe/the al giorno",
                WeeklyFrequencyTargetSupport.FrequencyTargetRule(
                    period = WeeklyFrequencyTargetSupport.FrequencyTargetPeriod.DAY,
                    measure = WeeklyFrequencyTargetSupport.FrequencyTargetMeasure.OCCURRENCES,
                    minimumValue = null,
                    maximumValue = 3
                ),
                "caffe e the"
            ),
            Triple(
                "Carne bianca 2-3 volte a settimana",
                WeeklyFrequencyTargetSupport.FrequencyTargetRule(
                    period = WeeklyFrequencyTargetSupport.FrequencyTargetPeriod.WEEK,
                    measure = WeeklyFrequencyTargetSupport.FrequencyTargetMeasure.OCCURRENCES,
                    minimumValue = 2,
                    maximumValue = 3
                ),
                "carne bianca"
            ),
            Triple(
                "Uova 1 porzione a settimana (N. 2 uova)",
                WeeklyFrequencyTargetSupport.FrequencyTargetRule(
                    period = WeeklyFrequencyTargetSupport.FrequencyTargetPeriod.WEEK,
                    measure = WeeklyFrequencyTargetSupport.FrequencyTargetMeasure.PORTIONS,
                    minimumValue = 1,
                    maximumValue = 1
                ),
                "uova"
            )
        )

        cases.forEach { (input, expectedRule, title) ->
            val actual = WeeklyFrequencyTargetSupport.parseFrequencyTargetRule(input)
            assertNotNull("Regola non rilevata per: $input", actual)
            assertEquals("Regola errata per: $title", expectedRule, actual)
        }
    }
}
