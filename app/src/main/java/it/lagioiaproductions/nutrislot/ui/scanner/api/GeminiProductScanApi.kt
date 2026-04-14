package it.lagioiaproductions.nutrislot.ui.scanner

import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

internal class GeminiProductScanApi {

    fun scanProduct(
        apiKey: String,
        imageBytes: ByteArray
    ): String {
        val imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val parts = JSONArray()
            .put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", imageBase64)
                )
            )
            .put(JSONObject().put("text", buildScanPrompt()))

        return callGemini(
            apiKey = apiKey,
            requestBody = buildRequestBody(parts)
        )
    }

    fun estimateNutrition(
        apiKey: String,
        descriptor: String
    ): String {
        val parts = JSONArray()
            .put(JSONObject().put("text", buildEstimationPrompt(descriptor)))

        return callGemini(
            apiKey = apiKey,
            requestBody = buildRequestBody(parts)
        )
    }

    private fun callGemini(
        apiKey: String,
        requestBody: JSONObject
    ): String {
        val endpoint = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        )

        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(requestBody.toString())
            writer.flush()
        }

        val statusCode = connection.responseCode
        val responseText = when (statusCode) {
            in 200..299 -> connection.inputStream.bufferedReader().use { it.readText() }
            else -> connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }

        connection.disconnect()

        if (statusCode !in 200..299) {
            Log.e(TAG, "Gemini product scan HTTP $statusCode. Response=$responseText")
            throw IOException("Gemini HTTP $statusCode")
        }

        return responseText
    }

    private fun buildRequestBody(parts: JSONArray): JSONObject {
        return JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put("parts", parts)
                )
            )
            .put("generationConfig", productSchemaConfig())
    }

    private fun buildScanPrompt(): String {
        return """
            Analizza la foto di un prodotto alimentare confezionato.

            Obiettivo:
            restituisci esclusivamente il JSON richiesto.

            Regole:
            - Identifica nome prodotto, marca e formato nel modo piu pulito possibile.
            - Se la tabella nutrizionale e leggibile, estrai calories, protein, carbs e fibre dai valori dell'etichetta.
            - Se la tabella nutrizionale non e leggibile ma il prodotto e identificabile con buona affidabilita, stima calories, protein, carbs e fibre usando valori tipici realistici del prodotto.
            - Se i nutrienti sono stimati, scrivi nutritionSource = "estimated" e spiegalo brevemente in summary.
            - Se i nutrienti vengono letti dall'etichetta, scrivi nutritionSource = "label".
            - Se non riesci davvero a stimare in modo credibile, restituisci null nei campi nutrizionali e nutritionSource = null.
            - Il barcode deve contenere solo cifre.
            - subtitle deve descrivere il formato o la base nutrizionale, per esempio: "vasetto 170 g", "2 fette", "valori per 100 g".
            - calories, protein, carbs e fibre devono essere interi.
            - Il nome prodotto deve essere breve e pulito.
        """.trimIndent()
    }

    private fun buildEstimationPrompt(descriptor: String): String {
        return """
            Devi stimare i valori nutrizionali di un prodotto alimentare confezionato.

            Dati riconosciuti dal prodotto:
            $descriptor

            Regole:
            - Restituisci esclusivamente il JSON richiesto.
            - Usa valori tipici realistici del prodotto quando il prodotto e riconoscibile.
            - Preferisci valori per 100 g o 100 ml se plausibili dal contesto.
            - calories, protein, carbs e fibre devono essere interi.
            - Imposta nutritionSource = "estimated".
            - summary deve dichiarare che i valori sono stimati e su quale base.
            - Se il prodotto non e sufficientemente riconoscibile, restituisci null per i nutrienti e nutritionSource = null.
        """.trimIndent()
    }

    private fun productSchemaConfig(): JSONObject {
        val stringOrNullType = JSONArray().put("string").put("null")
        val integerOrNullType = JSONArray().put("integer").put("null")

        val schema = JSONObject()
            .put("type", "object")
            .put(
                "properties",
                JSONObject()
                    .put("name", JSONObject().put("type", stringOrNullType))
                    .put("brand", JSONObject().put("type", stringOrNullType))
                    .put("subtitle", JSONObject().put("type", stringOrNullType))
                    .put("barcode", JSONObject().put("type", stringOrNullType))
                    .put("calories", JSONObject().put("type", integerOrNullType))
                    .put("protein", JSONObject().put("type", integerOrNullType))
                    .put("carbs", JSONObject().put("type", integerOrNullType))
                    .put("fibre", JSONObject().put("type", integerOrNullType))
                    .put("summary", JSONObject().put("type", stringOrNullType))
                    .put("nutritionSource", JSONObject().put("type", stringOrNullType))
            )
            .put(
                "required",
                JSONArray()
                    .put("name")
                    .put("brand")
                    .put("subtitle")
                    .put("barcode")
                    .put("calories")
                    .put("protein")
                    .put("carbs")
                    .put("fibre")
                    .put("summary")
                    .put("nutritionSource")
            )
            .put("additionalProperties", false)

        return JSONObject()
            .put("responseMimeType", "application/json")
            .put("responseJsonSchema", schema)
    }

    private companion object {
        const val TAG = "GeminiProductScanApi"
    }
}
