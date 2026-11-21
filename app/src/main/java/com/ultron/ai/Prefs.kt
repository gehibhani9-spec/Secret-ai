package com.ultron.ai

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("ultron", Context.MODE_PRIVATE)

    fun raw(c: Context, k: String): String = sp(c).getString(k, "") ?: ""

    fun set(c: Context, k: String, v: String) {
        sp(c).edit().putString(k, v.trim()).apply()
    }

    private fun get(c: Context, k: String, def: String): String =
        sp(c).getString(k, null)?.takeIf { it.isNotBlank() } ?: def

    fun geminiKey(c: Context) = get(c, "gemini", BuildConfig.DEFAULT_GEMINI_KEY)
    fun model(c: Context) = get(c, "model", "gemini-2.5-flash")
    fun elevenKey(c: Context) = get(c, "eleven", BuildConfig.DEFAULT_ELEVEN_KEY)
    fun voiceId(c: Context) = get(c, "voice", BuildConfig.DEFAULT_VOICE_ID)
    fun lang(c: Context) = get(c, "lang", "en-IN")
}
