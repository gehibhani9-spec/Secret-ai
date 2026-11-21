package com.ultron.ai

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Speaks with ElevenLabs when a key and voice ID are set, otherwise with the phone's own voice. */
class Voice(private val ctx: Context) {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var player: MediaPlayer? = null
    @Volatile private var current: CompletableDeferred<Unit>? = null

    init {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = Locale.getDefault()
        }
    }

    fun stop() {
        current?.complete(Unit)
        try { player?.release() } catch (_: Exception) {}
        player = null
        try { tts?.stop() } catch (_: Exception) {}
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
    }

    /** Returns a short note on which voice was used, handy for the Test Voice button. */
    suspend fun speak(text: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext "nothing to say"
        stop()
        val key = Prefs.elevenKey(ctx)
        val voice = Prefs.voiceId(ctx)
        var note = "phone voice (no ElevenLabs key or voice ID set)"
        if (key.isNotBlank() && voice.isNotBlank()) {
            try {
                playEleven(text, key, voice)
                return@withContext "ElevenLabs voice OK"
            } catch (e: Exception) {
                Log.w("Ultron", "ElevenLabs failed", e)
                note = "phone voice (ElevenLabs failed: ${e.message})"
            }
        }
        speakLocal(text)
        note
    }

    private suspend fun playEleven(text: String, key: String, voice: String) {
        val conn = URL("https://api.elevenlabs.io/v1/text-to-speech/$voice?output_format=mp3_22050_32")
            .openConnection() as HttpURLConnection
        val file = File(ctx.cacheDir, "say_${System.nanoTime()}.mp3")
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 20000
            conn.doOutput = true
            conn.setRequestProperty("xi-api-key", key)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "audio/mpeg")
            val body = JSONObject().put("text", text).put("model_id", "eleven_flash_v2_5")
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw IllegalStateException("HTTP $code ${err.take(120)}")
            }
            conn.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
        } finally {
            conn.disconnect()
        }

        val done = CompletableDeferred<Unit>()
        current = done
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setDataSource(file.path)
            mp.setOnCompletionListener { done.complete(Unit) }
            mp.setOnErrorListener { _, _, _ -> done.complete(Unit); true }
            mp.prepare()
            mp.start()
            withTimeoutOrNull(60_000) { done.await() }
        } finally {
            try { mp.release() } catch (_: Exception) {}
            if (player === mp) player = null
            file.delete()
        }
    }

    private suspend fun speakLocal(text: String) {
        val t = tts ?: return
        if (!ttsReady) return
        val done = CompletableDeferred<Unit>()
        current = done
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { done.complete(Unit) }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { done.complete(Unit) }
        })
        t.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ultron")
        withTimeoutOrNull(20_000) { done.await() }
    }
}
