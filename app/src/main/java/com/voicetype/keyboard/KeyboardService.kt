package com.voicetype.keyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import java.util.Locale

class KeyboardService : InputMethodService(), RecognitionListener {
    private var shift = false
    private var hindiMode = false
    private var symbolsMode = false
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    override fun onCreateInputView(): View = buildKeyboard()

    private fun buildKeyboard(): View {
        val prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE)
        val theme = prefs.getString("theme", "Light")
        val bg = when (theme) {
            "Dark" -> Color.rgb(35, 39, 47)
            "Blue" -> Color.rgb(20, 54, 100)
            else -> Color.rgb(232, 237, 246)
        }
        val keyBg = if (theme == "Dark") Color.rgb(67, 73, 84) else Color.WHITE
        val fg = if (theme == "Dark") Color.WHITE else Color.rgb(24, 43, 72)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 8, 4, 8)
            setBackgroundColor(bg)
        }
        val top = row()
        top.addView(key(if (hindiMode) "हि" else "EN", 1.2f, keyBg, fg) {
            hindiMode = !hindiMode; symbolsMode = false; refreshKeyboard()
        })
        top.addView(key(if (listening) "■ Stop" else "🎙 Voice", 1.5f, keyBg, fg) {
            if (listening) stopVoice() else startVoice()
        })
        top.addView(key("⚙", 0.8f, keyBg, fg) {
            Toast.makeText(this, "Change theme and voice language in the Voice Keyboard app", Toast.LENGTH_SHORT).show()
        })
        root.addView(top)

        val rows = if (symbolsMode) KeyboardLayout.symbolRows else KeyboardLayout.englishRows
        rows.forEachIndexed { index, chars ->
            val line = row()
            if (index == 2 && !symbolsMode) line.addView(key("⇧", 1.2f, keyBg, fg) {
                shift = !shift; refreshKeyboard()
            })
            chars.forEach { char ->
                line.addView(key(KeyboardLayout.labelFor(char, hindiMode, symbolsMode, shift), 1f, keyBg, fg) {
                    commitText(KeyboardLayout.outputFor(char, hindiMode, symbolsMode, shift))
                    if (shift && !hindiMode && !symbolsMode) { shift = false; refreshKeyboard() }
                })
            }
            if (index == 2 && !symbolsMode) line.addView(key("⌫", 1.2f, keyBg, fg) { currentInputConnection?.deleteSurroundingText(1, 0) })
            root.addView(line)
        }
        val bottom = row()
        bottom.addView(key(if (symbolsMode) "ABC" else "?123", 1.2f, keyBg, fg) {
            symbolsMode = !symbolsMode; refreshKeyboard()
        })
        bottom.addView(key(",", .8f, keyBg, fg) { commitText(",") })
        bottom.addView(key("space", 3f, keyBg, fg) { commitText(" ") })
        bottom.addView(key(".", .8f, keyBg, fg) { commitText(".") })
        bottom.addView(key("↵", 1f, keyBg, fg) { sendEnter() })
        root.addView(bottom)
        return root
    }

    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
    private fun key(label: String, weight: Float, bg: Int, fg: Int, action: () -> Unit) =
        Button(this).apply {
            text = label; textSize = if (label.length > 4) 11f else 17f
            isAllCaps = false; setTextColor(fg); setBackgroundColor(bg)
            setPadding(1, 2, 1, 2)
            layoutParams = LinearLayout.LayoutParams(0, 48, weight).apply { setMargins(2, 3, 2, 3) }
            setOnClickListener { action() }
        }

    private fun startVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Open Voice Keyboard app and grant microphone permission first", Toast.LENGTH_LONG).show()
            val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "No speech recognition service is available on this device", Toast.LENGTH_LONG).show()
            return
        }
        try {
            if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
            val prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE)
            val selected = prefs.getString("voice_language", "Auto")
            val language = when (selected) {
                "Hindi" -> "hi-IN"
                "English" -> "en-IN"
                else -> if (hindiMode) "hi-IN" else Locale.getDefault().toLanguageTag()
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            listening = true
            refreshKeyboard()
            recognizer?.startListening(intent)
            Toast.makeText(this, "Listening…", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            listening = false
            refreshKeyboard()
            Toast.makeText(this, "Could not start voice recognition", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopVoice() {
        recognizer?.stopListening()
        listening = false
        refreshKeyboard()
    }

    private fun commitText(text: String) { currentInputConnection?.commitText(text, 1) }
    private fun sendEnter() {
        val connection: InputConnection = currentInputConnection ?: return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }
    private fun refreshKeyboard() { setInputView(buildKeyboard()) }

    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (!text.isNullOrBlank()) commitText(text)
        listening = false
        refreshKeyboard()
    }
    override fun onError(error: Int) {
        listening = false
        refreshKeyboard()
        Toast.makeText(this, "Voice recognition ended (code $error)", Toast.LENGTH_SHORT).show()
    }
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { listening = false }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onDestroy() {
        recognizer?.destroy()
        recognizer = null
        super.onDestroy()
    }
}
