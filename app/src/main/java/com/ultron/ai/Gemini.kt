package com.ultron.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Gemini {
    /** Sends one prompt and returns the model's JSON text. Throws with a readable message on failure. */
    suspend fun ask(c: Context, system: String, user: String): String = withContext(Dispatchers.IO) {
        val key = Prefs.geminiKey(c)
        if (key.isBlank()) throw IllegalStateException("No Gemini key. Add it under KEYS.")
        val model = Prefs.model(c)

        val gen = JSONObject()
            .put("responseMimeType", "application/json")
            .put("temperature", 0.2)
            .put("maxOutputTokens", 700)
        // Flash models can skip "thinking" for much lower latency.
        if (model.contains("2.5-flash")) gen.put("thinkingConfig", JSONObject().put("thinkingBudget", 0))

        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", user)))
                )
            )
            .put("generationConfig", gen)

        val conn = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 25000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", key)
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IllegalStateException("Gemini error $code: ${text.take(160)}")

            val parts = JSONObject(text).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts")
            val sb = StringBuilder()
            for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text"))
            sb.toString()
        } finally {
            conn.disconnect()
        }
    }
}
