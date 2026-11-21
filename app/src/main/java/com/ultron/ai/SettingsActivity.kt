package com.ultron.ai

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var voice: Voice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val gemini = findViewById<EditText>(R.id.f_gemini)
        val model = findViewById<EditText>(R.id.f_model)
        val eleven = findViewById<EditText>(R.id.f_eleven)
        val voiceId = findViewById<EditText>(R.id.f_voice)
        val lang = findViewById<EditText>(R.id.f_lang)
        val result = findViewById<TextView>(R.id.result)

        gemini.setText(Prefs.raw(this, "gemini"))
        model.setText(Prefs.raw(this, "model"))
        eleven.setText(Prefs.raw(this, "eleven"))
        voiceId.setText(Prefs.raw(this, "voice"))
        lang.setText(Prefs.raw(this, "lang"))

        fun save() {
            Prefs.set(this, "gemini", gemini.text.toString())
            Prefs.set(this, "model", model.text.toString())
            Prefs.set(this, "eleven", eleven.text.toString())
            Prefs.set(this, "voice", voiceId.text.toString())
            Prefs.set(this, "lang", lang.text.toString())
        }

        findViewById<TextView>(R.id.b_save).setOnClickListener {
            save()
            result.text = "Saved."
        }

        findViewById<TextView>(R.id.b_test).setOnClickListener {
            save()
            result.text = "Testing voice..."
            val v = voice ?: Voice(applicationContext).also { voice = it }
            scope.launch {
                val note = v.speak("Systems online. Ultron is ready.")
                result.text = "Voice used: $note"
            }
        }

        findViewById<TextView>(R.id.b_access).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onDestroy() {
        voice?.shutdown()
        scope.cancel()
        super.onDestroy()
    }
}
