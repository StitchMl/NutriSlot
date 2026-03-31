@file:Suppress("IntroduceWhenSubject")

package it.lagioiaproductions.nutrislot.data.ai

import it.lagioiaproductions.nutrislot.domain.model.ImportedMealNutrition
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log
import java.io.IOException

data class GeminiNutritionEstimateResult(
    val nutrition: ImportedMealNutrition? = null,
    val errorMessage: String? = null,
    val retryable: Boolean = false
)

class GeminiNutritionEstimator(
    private val apiKey: String
) {

    fun estimateNutritionForMealDetailed(mealText: String): GeminiNutritionEstimateResult {
        val trimmedApiKey = apiKey.trim()
        if (trimmedApiKey.isBlank()) {
            Log.e(TAG, "Gemini API key vuota.")
            return GeminiNutritionEstimateResult(
                errorMessage = "Gemini API key vuota.",
                retryable = false
            )
        }

        val normalizedMealText = mealText.trim()
        if (normalizedMealText.isBlank()) {
            Log.w(TAG, "Meal text vuoto, salto la richiesta.")
            return GeminiNutritionEstimateResult(
                errorMessage = "Testo del pasto vuoto.",
                retryable = false
            )
        }

        return try {
            val endpoint = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$trimmedApiKey"
            )

            val connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val requestBody = buildRequestBody(normalizedMealText)

            Log.d(
                TAG,
                "Gemini request start. textLength=${normalizedMealText.length} body=${requestBody.toString().take(1200)}"
            )

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val statusCode = connection.responseCode
            val responseText = when (statusCode) {
                in 200..299 -> {
                    connection.inputStream.bufferedReader().use(BufferedReader::readText)
                }
                else -> {
                    connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                        ?: ""
                }
            }

            connection.disconnect()

            if (statusCode !in 200..299) {
                val errorMessage = buildErrorMessage(
                    statusCode = statusCode,
                    responseText = responseText
                )
                Log.e(
                    TAG,
                    "Gemini HTTP $statusCode. Message=$errorMessage Response=$responseText"
                )
                return GeminiNutritionEstimateResult(
                    errorMessage = errorMessage,
                    retryable = statusCode !in listOf(400, 401, 403)
                )
            }

            Log.d(
                TAG,
                "Gemini success. Response=${responseText.take(1200)}"
            )

            val parsed = parseResponse(responseText)
            Log.d(TAG, "Gemini parsed nutrition=$parsed")

            if (parsed != null) {
                GeminiNutritionEstimateResult(nutrition = parsed)
            } else {
                GeminiNutritionEstimateResult(
                    errorMessage = "Gemini non ha restituito nutrienti validi per questo pasto.",
                    retryable = false
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "Gemini IO error", e)
            GeminiNutritionEstimateResult(
                errorMessage = "Errore di rete durante la chiamata a Gemini.",
                retryable = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gemini unexpected error", e)
            GeminiNutritionEstimateResult(
                errorMessage = "Errore inatteso durante la chiamata a Gemini.",
                retryable = true
            )
        }
    }

    private fun buildRequestBody(mealText: String): JSONObject {
        val prompt = """
            Sei un nutrizionista tecnico.
            Devi stimare i valori nutrizionali TOTALI del pasto descritto.
            
            Regole:
            - Restituisci solo il JSON richiesto dallo schema.
            - I valori devono essere interi.
            - Se un valore non è stimabile con sufficiente affidabilità, restituisci null.
            - Considera il pasto come una porzione standard realistica, salvo quantità esplicite nel testo.
            - Non inventare ingredienti non suggeriti dal testo.
            
            Testo del pasto:
            $mealText
        """.trimIndent()

        val schema = JSONObject()
            .put("type", "object")
            .put(
                "properties",
                JSONObject()
                    .put(
                        "calories",
                        JSONObject()
                            .put("type", org.json.JSONArray().put("integer").put("null"))
                    )
                    .put(
                        "proteinGrams",
                        JSONObject()
                            .put("type", org.json.JSONArray().put("integer").put("null"))
                    )
                    .put(
                        "carbsGrams",
                        JSONObject()
                            .put("type", org.json.JSONArray().put("integer").put("null"))
                    )
                    .put(
                        "fibreGrams",
                        JSONObject()
                            .put("type", org.json.JSONArray().put("integer").put("null"))
                    )
            )
            .put(
                "required",
                org.json.JSONArray()
                    .put("calories")
                    .put("proteinGrams")
                    .put("carbsGrams")
                    .put("fibreGrams")
            )
            .put("additionalProperties", false)

        return JSONObject()
            .put(
                "contents",
                org.json.JSONArray().put(
                    JSONObject().put(
                        "parts",
                        org.json.JSONArray().put(
                            JSONObject().put("text", prompt)
                        )
                    )
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("responseJsonSchema", schema)
            )
    }

    private fun parseResponse(rawResponse: String): ImportedMealNutrition? {
        val root = JSONObject(rawResponse)
        val candidates = root.optJSONArray("candidates") ?: return null
        val firstCandidate = candidates.optJSONObject(0) ?: return null
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        val firstPart = parts.optJSONObject(0) ?: return null
        val jsonText = firstPart.optString("text").trim()

        if (jsonText.isBlank()) return null

        val payload = JSONObject(jsonText)

        fun nullableInt(name: String): Int? {
            if (!payload.has(name) || payload.isNull(name)) return null
            return payload.optInt(name).takeIf { it >= 0 }
        }

        val nutrition = ImportedMealNutrition(
            calories = nullableInt("calories"),
            proteinGrams = nullableInt("proteinGrams"),
            carbsGrams = nullableInt("carbsGrams"),
            fibreGrams = nullableInt("fibreGrams")
        )

        return nutrition.takeIf { it.hasAnyValue }
    }

    private fun buildErrorMessage(
        statusCode: Int,
        responseText: String
    ): String {
        val normalizedResponse = responseText.lowercase()

        return when {
            statusCode == 403 && normalizedResponse.contains("reported as leaked") ->
                "La Gemini API key configurata è stata segnalata come compromessa. Aggiornala in gradle.properties."

            statusCode == 401 || statusCode == 403 ->
                "Gemini ha rifiutato la API key configurata."

            statusCode == 429 ->
                "Gemini è temporaneamente occupato. Riprova tra poco."

            statusCode == 400 ->
                "La richiesta inviata a Gemini non è valida."

            else ->
                "Gemini ha risposto con HTTP $statusCode."
        }
    }

    companion object {
        private const val TAG = "GeminiEstimator"
    }
}
