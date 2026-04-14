package it.lagioiaproductions.nutrislot.data.ai

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class MealTargetCatalogCandidate(
    val canonicalKey: String,
    val title: String,
    val matchTerms: List<String> = emptyList(),
    val ruleDescription: String? = null
)

data class GeminiMealTargetCatalogResult(
    val canonicalKeys: List<String> = emptyList(),
    val errorMessage: String? = null,
    val retryable: Boolean = false
)

class GeminiMealTargetCataloger(
    private val apiKey: String
) {

    fun catalogMealTargets(
        mealText: String,
        candidates: List<MealTargetCatalogCandidate>
    ): GeminiMealTargetCatalogResult {
        val trimmedApiKey = apiKey.trim()
        if (trimmedApiKey.isBlank()) {
            return GeminiMealTargetCatalogResult(
                errorMessage = "Gemini API key vuota.",
                retryable = false
            )
        }

        val normalizedMealText = mealText.trim()
        if (normalizedMealText.isBlank()) {
            return GeminiMealTargetCatalogResult(
                errorMessage = "Testo del pasto vuoto.",
                retryable = false
            )
        }

        val normalizedCandidates = candidates
            .mapNotNull { candidate ->
                val canonicalKey = candidate.canonicalKey.trim()
                if (canonicalKey.isBlank()) {
                    null
                } else {
                    candidate.copy(
                        canonicalKey = canonicalKey,
                        title = candidate.title.trim()
                    )
                }
            }
            .distinctBy { it.canonicalKey }

        if (normalizedCandidates.isEmpty()) {
            return GeminiMealTargetCatalogResult(canonicalKeys = emptyList())
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

            val requestBody = buildRequestBody(
                mealText = normalizedMealText,
                candidates = normalizedCandidates
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
                return GeminiMealTargetCatalogResult(
                    errorMessage = buildErrorMessage(
                        statusCode = statusCode,
                        responseText = responseText
                    ),
                    retryable = statusCode !in listOf(400, 401, 403)
                )
            }

            GeminiMealTargetCatalogResult(
                canonicalKeys = parseResponse(
                    rawResponse = responseText,
                    allowedKeys = normalizedCandidates.map { it.canonicalKey }.toSet()
                )
            )
        } catch (error: IOException) {
            Log.e(TAG, "Gemini meal target catalog IO error", error)
            GeminiMealTargetCatalogResult(
                errorMessage = "Errore di rete durante la chiamata a Gemini.",
                retryable = true
            )
        } catch (error: Exception) {
            Log.e(TAG, "Gemini meal target catalog unexpected error", error)
            GeminiMealTargetCatalogResult(
                errorMessage = "Errore inatteso durante la catalogazione del pasto.",
                retryable = true
            )
        }
    }

    private fun buildRequestBody(
        mealText: String,
        candidates: List<MealTargetCatalogCandidate>
    ): JSONObject {
        val prompt = buildString {
            appendLine("Classifica il pasto sui target di consumo disponibili.")
            appendLine()
            appendLine("Regole:")
            appendLine("- Restituisci solo il JSON richiesto.")
            appendLine("- Usa soltanto canonicalKeys presenti nella lista autorizzata.")
            appendLine("- Puoi restituire zero, uno o piu target se il pasto li rappresenta davvero.")
            appendLine("- Considera solo ingredienti o componenti chiaramente presenti nel testo.")
            appendLine("- Non forzare categorie dubbie.")
            appendLine("- Se il pasto e una combinazione coerente, puoi assegnare anche target trasversali come piatto unico.")
            appendLine()
            appendLine("Pasto:")
            appendLine(mealText)
            appendLine()
            appendLine("Target autorizzati:")
            candidates.forEach { candidate ->
                append("- ${candidate.canonicalKey} | ${candidate.title}")
                candidate.matchTerms
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = ", ")
                    ?.let { append(" | termini: $it") }
                candidate.ruleDescription
                    ?.takeIf { it.isNotBlank() }
                    ?.let { append(" | regola: ${it.trim()}") }
                appendLine()
            }
        }.trim()

        val schema = JSONObject()
            .put("type", "object")
            .put(
                "properties",
                JSONObject().put(
                    "canonicalKeys",
                    JSONObject()
                        .put("type", "array")
                        .put(
                            "items",
                            JSONObject().put("type", "string")
                        )
                )
            )
            .put(
                "required",
                JSONArray().put("canonicalKeys")
            )
            .put("additionalProperties", false)

        return JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(
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

    private fun parseResponse(
        rawResponse: String,
        allowedKeys: Set<String>
    ): List<String> {
        val root = JSONObject(rawResponse)
        val candidates = root.optJSONArray("candidates") ?: return emptyList()
        val firstCandidate = candidates.optJSONObject(0) ?: return emptyList()
        val content = firstCandidate.optJSONObject("content") ?: return emptyList()
        val parts = content.optJSONArray("parts") ?: return emptyList()
        val firstPart = parts.optJSONObject(0) ?: return emptyList()
        val jsonText = firstPart.optString("text").trim()
        if (jsonText.isBlank()) return emptyList()

        val payload = JSONObject(jsonText)
        val keys = payload.optJSONArray("canonicalKeys") ?: return emptyList()

        return buildList {
            for (index in 0 until keys.length()) {
                val key = keys.optString(index).trim()
                if (key.isNotBlank() && key in allowedKeys && key !in this) {
                    add(key)
                }
            }
        }
    }

    private fun buildErrorMessage(
        statusCode: Int,
        responseText: String
    ): String {
        val normalizedResponse = responseText.lowercase()

        return when {
            statusCode == 403 && normalizedResponse.contains("reported as leaked") -> {
                "La Gemini API key configurata e stata segnalata come compromessa."
            }

            statusCode == 401 || statusCode == 403 -> {
                "Gemini ha rifiutato la API key configurata."
            }

            statusCode == 429 -> {
                "Gemini e temporaneamente occupato. Riprova tra poco."
            }

            statusCode == 400 -> {
                "La richiesta inviata a Gemini non e valida."
            }

            else -> {
                "Gemini ha risposto con HTTP $statusCode."
            }
        }
    }

    private companion object {
        const val TAG = "GeminiMealTargets"
    }
}
