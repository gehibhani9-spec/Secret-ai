package com.ultron.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), UltronListener {

    private lateinit var holo: HologramView
    private lateinit var status: TextView
    private lateinit var clock: TextView
    private lateinit var banner: TextView
    private lateinit var log: TextView
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var micBtn: TextView
    private lateinit var handsBtn: TextView

    private val ui = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var busy = false
    private var handsFree = false
    private var resumed = false

    private val clockTick = object : Runnable {
        override fun run() {
            clock.text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            ui.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        holo = findViewById(R.id.holo)
        status = findViewById(R.id.status)
        clock = findViewById(R.id.clock)
        banner = findViewById(R.id.banner)
        log = findViewById(R.id.log)
        scroll = findViewById(R.id.scroll)
        input = findViewById(R.id.input)
        micBtn = findViewById(R.id.mic)
        handsBtn = findViewById(R.id.hands)

        UltronService.listener = this

        holo.setOnClickListener { onOrbTap() }
        micBtn.setOnClickListener { onOrbTap() }
        findViewById<TextView>(R.id.send).setOnClickListener { submit(input.text.toString()) }
        input.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEND) { submit(input.text.toString()); true } else false
        }
        findViewById<TextView>(R.id.settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        banner.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        handsBtn.setOnClickListener {
            handsFree = !handsFree
            handsBtn.text = if (handsFree) "HANDS-FREE ON" else "HANDS-FREE OFF"
            handsBtn.setTextColor(if (handsFree) 0xFF3FE0FF.toInt() else 0xFF6F9AAA.toInt())
            if (handsFree && !busy && !listening) startListening()
        }

        addLog("ULTRON", "Systems online. Tap the core and speak, or type a command.")
        setMode("idle")
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        ui.post(clockTick)
        banner.visibility = if (UltronService.instance == null) View.VISIBLE else View.GONE
        if (Prefs.geminiKey(this).isBlank()) {
            status.text = "ADD GEMINI KEY UNDER KEYS"
        }
    }

    override fun onPause() {
        super.onPause()
        resumed = false
        ui.removeCallbacks(clockTick)
        if (listening) stopListening()
    }

    override fun onDestroy() {
        if (UltronService.listener === this) UltronService.listener = null
        recognizer?.destroy()
        super.onDestroy()
    }

    // ---- UltronListener (called on the main thread) ----

    override fun onState(state: String) {
        when (state) {
            "thinking", "acting" -> { busy = true; setMode("thinking") }
            "speaking" -> { busy = true; setMode("speaking") }
            else -> {
                busy = false
                setMode("idle")
                if (handsFree && resumed) ui.postDelayed({ if (!busy && !listening && resumed) startListening() }, 500)
            }
        }
    }

    override fun onSay(text: String) = addLog("ULTRON", text)

    // ---- actions ----

    private fun onOrbTap() {
        when {
            busy -> UltronService.instance?.cancel()
            listening -> stopListening()
            else -> startListening()
        }
    }

    private fun submit(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        input.setText("")
        addLog("YOU", text)
        val svc = UltronService.instance
        if (svc == null) {
            banner.visibility = View.VISIBLE
            addLog("ULTRON", "I can't control the phone yet. Turn on Ultron in Accessibility settings.")
            return
        }
        busy = true
        setMode("thinking")
        svc.run(text)
    }

    private fun setMode(m: String) {
        holo.mode = m
        status.text = "STATUS " + when (m) {
            "listening" -> "LISTENING"
            "thinking" -> "WORKING"
            "speaking" -> "SPEAKING"
            else -> "ONLINE"
        }
        micBtn.text = if (busy) "TAP TO STOP" else if (listening) "LISTENING..." else "TAP TO SPEAK"
    }

    private fun addLog(who: String, text: String) {
        log.append((if (log.text.isEmpty()) "" else "\n\n") + who + ": " + text)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    // ---- speech recognition (Google's on-phone recognizer, no key needed) ----

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            addLog("ULTRON", "Speech recognition isn't available on this phone. Type your command instead.")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { r ->
            r.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) { holo.level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f) }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onPartialResults(partialResults: Bundle?) {
                    val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!t.isNullOrBlank()) input.setText(t)
                }
                override fun onError(error: Int) {
                    listening = false
                    holo.level = 0f
                    setMode("idle")
                    val quiet = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    if (handsFree && quiet && resumed && !busy) ui.postDelayed({ if (!busy && !listening && resumed) startListening() }, 300)
                }
                override fun onResults(results: Bundle?) {
                    listening = false
                    holo.level = 0f
                    val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (t.isNullOrBlank()) setMode("idle") else submit(t)
                }
            })
        }
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Prefs.lang(this@MainActivity))
        }
        listening = true
        setMode("listening")
        recognizer?.startListening(i)
    }

    private fun stopListening() {
        recognizer?.stopListening()
        listening = false
        holo.level = 0f
        setMode("idle")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startListening()
        else addLog("ULTRON", "I need the microphone to hear you. You can still type commands.")
    }
}
