package com.voicetype.keyboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

/**
 * Voice Keyboard v1.3.1 modern pastel UI.
 * Keeps typing, voice recognition, language selection and clipboard behavior local.
 */
class KeyboardService : InputMethodService(), RecognitionListener {
    private var shift = false
    private var language = "English"
    private var symbolsMode = false
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    private val pageBg = Color.rgb(249, 224, 238)
    private val keyBg = Color.rgb(255, 247, 252)
    private val keyAlt = Color.rgb(239, 220, 246)
    private val keyFg = Color.rgb(48, 43, 62)
    private val accent = Color.rgb(225, 91, 123)
    private val specialFg = Color.rgb(74, 58, 91)

    override fun onCreateInputView(): View = buildKeyboard()

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int = 11, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke > 0) setStroke(dp(1), stroke)
        }

    private fun buildKeyboard(): View {
        val prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE)
        language = prefs.getString("layout_language", "English") ?: "English"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(6), dp(4), dp(7))
            setBackgroundColor(pageBg)
        }

        // Suggestion strip: lightweight local word variants, not cloud prediction.
        val suggestions = row()
        val words = localSuggestions()
        words.forEachIndexed { index, word ->
            suggestions.addView(keyView(
                if (word.isBlank()) listOf("EN • HG", "Voice", "Clipboard")[index] else word,
                if (index == 1) 1.15f else 1f,
                if (index == 1 && listening) accent else pageBg,
                if (index == 1 && listening) Color.WHITE else specialFg,
                34, 10
            ) {
                when (index) {
                    0 -> if (word.isNotBlank()) replacePreviousWord(word) else showLanguageMenu(it)
                    1 -> if (listening) stopVoice() else startVoice()
                    2 -> showClipboardMenu(it)
                }
            })
        }
        suggestions.addView(keyView("⚙", .55f, keyAlt, specialFg, 34, 10) {
            Toast.makeText(this, "Theme and voice settings are in the Voice Keyboard app", Toast.LENGTH_SHORT).show()
        })
        root.addView(suggestions)

        // Dedicated number row.
        val numbers = row()
        "1234567890".forEach { digit ->
            numbers.addView(keyView(digit.toString(), 1f, keyBg, keyFg, 39, 12) {
                commitText(digit.toString())
            })
        }
        root.addView(numbers)

        val rows = if (symbolsMode) KeyboardLayout.symbolRows else KeyboardLayout.englishRows
        rows.forEachIndexed { index, chars ->
            val line = row()
            if (index == 2 && !symbolsMode) {
                line.addView(keyView("⇧", 1.12f, keyAlt, specialFg, 43, 10) {
                    shift = !shift
                    refreshKeyboard()
                })
            }
            chars.forEach { char ->
                line.addView(keyView(
                    KeyboardLayout.labelFor(char, language, symbolsMode, shift),
                    1f, keyBg, keyFg, 43, 11
                ) {
                    commitText(KeyboardLayout.outputFor(char, language, symbolsMode, shift))
                    if (shift && language == "English" && !symbolsMode) {
                        shift = false
                        refreshKeyboard()
                    }
                })
            }
            if (index == 2 && !symbolsMode) {
                line.addView(keyView("⌫", 1.12f, keyAlt, specialFg, 43, 10) {
                    currentInputConnection?.deleteSurroundingText(1, 0)
                })
            }
            root.addView(line)
        }

        val bottom = row()
        bottom.addView(keyView(if (symbolsMode) "ABC" else "?123", 1.15f, keyAlt, specialFg, 45, 10) {
            symbolsMode = !symbolsMode
            refreshKeyboard()
        })
        bottom.addView(keyView(",", .55f, keyBg, keyFg, 45, 11) { commitText(",") })
        bottom.addView(keyView("☺", .72f, keyAlt, specialFg, 45, 15) { showEmojiMenu(it) })
        bottom.addView(keyView("space\n${language.take(2).uppercase()} • HG", 2.8f, keyBg, keyFg, 45, 10) {
            commitText(" ")
        })
        bottom.addView(keyView(".", .55f, keyBg, keyFg, 45, 12) { commitText(".") })
        bottom.addView(keyView("↵", .95f, accent, Color.WHITE, 45, 16) { sendEnter() })
        root.addView(bottom)

        return root
    }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    private fun keyView(
        label: String,
        weight: Float,
        backgroundColor: Int,
        foreground: Int,
        heightDp: Int,
        textSizeSp: Int,
        action: (View) -> Unit
    ) = TextView(this).apply {
        text = label
        textSize = textSizeSp.toFloat()
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        gravity = Gravity.CENTER
        setTextColor(foreground)
        isAllCaps = false
        this.background = rounded(backgroundColor, 11)
        setPadding(dp(1), dp(2), dp(1), dp(2))
        layoutParams = LinearLayout.LayoutParams(0, dp(heightDp), weight).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
        setOnClickListener { action(it) }
    }

    private fun localSuggestions(): List<String> {
        val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val token = before.takeLastWhile { !it.isWhitespace() }
        if (token.isBlank()) return listOf("", "", "")
        val capitalized = token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val upper = token.uppercase(Locale.getDefault())
        return listOf(token, capitalized, upper).distinct().take(3).let {
            if (it.size == 1) listOf(it[0], "", "") else if (it.size == 2) listOf(it[0], it[1], "") else it
        }
    }

    private fun replacePreviousWord(replacement: String) {
        val connection = currentInputConnection ?: return
        val before = connection.getTextBeforeCursor(100, 0)?.toString().orEmpty()
        val token = before.takeLastWhile { !it.isWhitespace() }
        if (token.isNotEmpty()) connection.deleteSurroundingTextInCodePoints(token.codePointCount(0, token.length), 0)
        connection.commitText(replacement, 1)
    }

    private fun showEmojiMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            listOf("😊", "😂", "❤️", "👍", "🙏", "🎉", "😍", "😎").forEach { emoji ->
                menu.add(emoji).setOnMenuItemClickListener {
                    commitText(emoji)
                    true
                }
            }
            show()
        }
    }

    private fun showLanguageMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            listOf("English", "Hindi", "Punjabi").forEach { item ->
                menu.add(item).setOnMenuItemClickListener {
                    language = item
                    getSharedPreferences("keyboard_prefs", MODE_PRIVATE).edit()
                        .putString("layout_language", item).apply()
                    refreshKeyboard()
                    true
                }
            }
            show()
        }
    }

    private fun clipboard(): ClipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

    private fun showClipboardMenu(anchor: View) {
        val clip = clipboard().primaryClip
        val copied = if (clip != null && clip.itemCount > 0)
            clip.getItemAt(0).coerceToText(this)?.toString().orEmpty() else ""
        val prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE)
        val old = prefs.getString("clipboard_history", "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
        val updated = KeyboardFeatures.addClipboardItem(old, copied)
        prefs.edit().putString("clipboard_history", updated.joinToString("\n")).apply()
        if (updated.isEmpty()) {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            return
        }
        PopupMenu(this, anchor).apply {
            updated.take(KeyboardFeatures.MAX_CLIPBOARD_ITEMS).forEach { value ->
                menu.add(value.replace("\n", " ").take(32)).setOnMenuItemClickListener {
                    commitText(value)
                    true
                }
            }
            show()
        }
    }

    private fun startVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Open Voice Keyboard app and grant microphone permission first", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "No speech recognition service is available", Toast.LENGTH_LONG).show()
            return
        }
        try {
            if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(this).also {
                it.setRecognitionListener(this)
            }
            val prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE)
            val selected = prefs.getString("voice_language", "Auto") ?: "Auto"
            val tag = KeyboardFeatures.voiceLanguageTag(selected) ?: when (language) {
                "Hindi" -> "hi-IN"
                "Punjabi" -> "pa-IN"
                else -> Locale.getDefault().toLanguageTag()
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, tag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            }
            listening = true
            refreshKeyboard()
            recognizer?.startListening(intent)
            Toast.makeText(this, "Listening…", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
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
