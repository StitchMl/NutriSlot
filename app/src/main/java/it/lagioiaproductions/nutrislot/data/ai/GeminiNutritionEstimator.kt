package it.lagioiaproductions.nutrislot.data.ai

import it.lagioiaproductions.nutrislot.domain.model.ImportedMealNutrition
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class GeminiNutritionEstimator(
    private val apiKey: String
) {

    fun estimateNutritionForMeal(mealText: String): ImportedMealNutrition? {
        val trimmedApiKey = apiKey.trim()
        if (trimmedApiKey.isBlank()) return null

        val normalizedMealText = mealText.trim()
        if (normalizedMealText.isBlank()) return null

        return runCatching {
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

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseText = when (connection.responseCode) {
                in 200..299 -> connection.inputStream.bufferedReader().use(BufferedReader::readText)
                else -> connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                    ?: return null
            }

            connection.disconnect()

            parseResponse(responseText)
        }.getOrNull()
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
}