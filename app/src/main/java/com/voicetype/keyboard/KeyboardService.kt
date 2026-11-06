package com.voicetype.keyboard

import android.Manifest
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

/** Ultra UI refresh. Existing voice, clipboard, language and key mapping features retained. */
class KeyboardService : InputMethodService(), RecognitionListener {
    private var shift = false
    private var language = "English"
    private var symbolsMode = false
    private var listening = false
    private var recognizer: SpeechRecognizer? = null

    private val pink = Color.rgb(237, 139, 183)
    private val lavender = Color.rgb(181, 158, 225)
    private val ink = Color.rgb(55, 43, 73)
    private val white = Color.rgb(255, 250, 255)
    private val muted = Color.rgb(237, 226, 246)

    override fun onCreateInputView(): View = buildKeyboard()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun shape(color: Int, radius: Int, stroke: Int = 0): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(color, color)).apply {
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun buildKeyboard(): View {
        val prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE)
        language = prefs.getString("layout_language", "English") ?: "English"
        val themeName = prefs.getString("ultra_theme", "Pink Lavender") ?: "Pink Lavender"
        val palette = when (themeName) {
            "Royal Blue" -> intArrayOf(Color.rgb(22, 52, 98), Color.rgb(72, 119, 184), Color.WHITE, Color.rgb(235, 243, 255))
            "Mint" -> intArrayOf(Color.rgb(180, 226, 211), Color.rgb(111, 180, 157), Color.rgb(35, 65, 58), Color.WHITE)
            "Midnight" -> intArrayOf(Color.rgb(29, 29, 43), Color.rgb(92, 74, 122), Color.WHITE, Color.rgb(58, 53, 72))
            else -> intArrayOf(pink, lavender, ink, white)
        }
        val bg1 = palette[0]
        val bg2 = palette[1]
        val fg = palette[2]
        val keyColor = palette[3]
        val keyHeight = prefs.getInt("ultra_key_height", 44).coerceIn(38, 56)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(7), dp(5), dp(8))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(bg1, bg2)).apply {
                cornerRadius = dp(18).toFloat()
            }
        }

        // Floating smart-action dock
        val dock = row()
        dock.addView(chip("☷", .75f, muted, ink, 34) { showClipboardMenu(it) })
        dock.addView(chip(if (listening) "■ Stop" else "🎙 Voice", 1.15f, accentColor(bg2), Color.WHITE, 34) {
            if (listening) stopVoice() else startVoice()
        })
        dock.addView(chip("🌐 ${language.take(2).uppercase()}", 1.05f, muted, ink, 34) { showLanguageMenu(it) })
        dock.addView(chip("☺", .7f, muted, ink, 34) { showEmojiMenu(it) })
        dock.addView(chip("⚙", .7f, muted, ink, 34) { showSettingsMenu(it) })
        root.addView(dock)

        // Three local text variants; explicitly not marketed as AI prediction.
        val suggestionRow = row()
        localSuggestions().forEach { suggestion ->
            suggestionRow.addView(chip(suggestion.ifBlank { "···" }, 1f, Color.argb(105, 255,255,255), fg, 29) {
                if (suggestion.isNotBlank()) replacePreviousWord(suggestion)
            })
        }
        root.addView(suggestionRow)

        val numberRow = row()
        "1234567890".forEach { c ->
            numberRow.addView(key(c.toString(), 1f, keyColor, fg, keyHeight - 5, 12) { commitText(c.toString()) })
        }
        root.addView(numberRow)

        val rows = if (symbolsMode) KeyboardLayout.symbolRows else KeyboardLayout.englishRows
        rows.forEachIndexed { index, chars ->
            val line = row()
            if (index == 2 && !symbolsMode) line.addView(key("⇧", 1.12f, muted, fg, keyHeight, 14) {
                shift = !shift; refreshKeyboard()
            })
            chars.forEach { c ->
                line.addView(key(KeyboardLayout.labelFor(c, language, symbolsMode, shift), 1f, keyColor, fg, keyHeight, 14) {
                    commitText(KeyboardLayout.outputFor(c, language, symbolsMode, shift))
                    if (shift && language == "English" && !symbolsMode) { shift = false; refreshKeyboard() }
                })
            }
            if (index == 2 && !symbolsMode) line.addView(key("⌫", 1.12f, muted, fg, keyHeight, 15) {
                currentInputConnection?.deleteSurroundingText(1, 0)
            })
            root.addView(line)
        }

        val bottom = row()
        bottom.addView(key(if (symbolsMode) "ABC" else "?123", 1.15f, muted, fg, keyHeight + 2, 11) {
            symbolsMode = !symbolsMode; refreshKeyboard()
        })
        bottom.addView(key(",", .55f, keyColor, fg, keyHeight + 2, 13) { commitText(",") })
        bottom.addView(key("🌐", .65f, muted, fg, keyHeight + 2, 13) { showLanguageMenu(it) })
        bottom.addView(key("space", 2.55f, keyColor, fg, keyHeight + 2, 12) { commitText(" ") })
        bottom.addView(key(".", .55f, keyColor, fg, keyHeight + 2, 13) { commitText(".") })
        bottom.addView(key("↵", .95f, accentColor(bg2), Color.WHITE, keyHeight + 2, 16) { sendEnter() })
        root.addView(bottom)
        return root
    }

    private fun accentColor(color: Int) = color
    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    private fun key(label: String, weight: Float, color: Int, textColor: Int, height: Int, size: Int, action: (View) -> Unit) =
        chip(label, weight, color, textColor, height, size, action)

    private fun chip(label: String, weight: Float, color: Int, textColor: Int, height: Int, size: Int = 11, action: (View) -> Unit) =
        TextView(this).apply {
            text = label
            textSize = size.toFloat()
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setTextColor(textColor)
            maxLines = 2
            background = shape(color, if (height < 38) 16 else 12, Color.argb(25, 255, 255, 255))
            setPadding(dp(2), dp(2), dp(2), dp(2))
            layoutParams = LinearLayout.LayoutParams(0, dp(height), weight).apply {
                setMargins(dp(2), dp(3), dp(2), dp(3))
            }
            setOnClickListener { action(it) }
        }

    private fun localSuggestions(): List<String> {
        val before = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val token = before.takeLastWhile { !it.isWhitespace() }
        if (token.isBlank()) return listOf("Type", "Voice", "Hindi")
        val cap = token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        return listOf(token, cap, token.uppercase(Locale.getDefault())).distinct().take(3).let {
            when (it.size) { 1 -> listOf(it[0], "", ""); 2 -> listOf(it[0], it[1], ""); else -> it }
        }
    }

    private fun replacePreviousWord(value: String) {
        val c = currentInputConnection ?: return
        val before = c.getTextBeforeCursor(100, 0)?.toString().orEmpty()
        val token = before.takeLastWhile { !it.isWhitespace() }
        if (token.isNotEmpty()) c.deleteSurroundingTextInCodePoints(token.codePointCount(0, token.length), 0)
        c.commitText(value, 1)
    }

    private fun showSettingsMenu(anchor: View) {
        val prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE)
        PopupMenu(this, anchor).apply {
            menu.add("Theme: Pink Lavender").setOnMenuItemClickListener { setTheme("Pink Lavender"); true }
            menu.add("Theme: Royal Blue").setOnMenuItemClickListener { setTheme("Royal Blue"); true }
            menu.add("Theme: Mint").setOnMenuItemClickListener { setTheme("Mint"); true }
            menu.add("Theme: Midnight").setOnMenuItemClickListener { setTheme("Midnight"); true }
            menu.add("Key height: Compact").setOnMenuItemClickListener { prefs.edit().putInt("ultra_key_height", 40).apply(); refreshKeyboard(); true }
            menu.add("Key height: Standard").setOnMenuItemClickListener { prefs.edit().putInt("ultra_key_height", 44).apply(); refreshKeyboard(); true }
            menu.add("Key height: Large").setOnMenuItemClickListener { prefs.edit().putInt("ultra_key_height", 50).apply(); refreshKeyboard(); true }
            show()
        }
    }
    private fun setTheme(name: String) {
        getSharedPreferences("keyboard_prefs", MODE_PRIVATE).edit().putString("ultra_theme", name).apply()
        refreshKeyboard()
    }

    private fun showEmojiMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            listOf("😊", "😂", "❤️", "👍", "🙏", "🎉", "😍", "😎", "🥳", "✨", "🌸", "🔥").forEach {
                menu.add(it).setOnMenuItemClickListener { _ -> commitText(it); true }
            }
            show()
        }
    }
    private fun showLanguageMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            listOf("English", "Hindi", "Punjabi").forEach { item ->
                menu.add(item).setOnMenuItemClickListener {
                    language = item
                    getSharedPreferences("keyboard_prefs", MODE_PRIVATE).edit().putString("layout_language", item).apply()
                    refreshKeyboard(); true
                }
            }
            show()
        }
    }
    private fun clipboard() = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    private fun showClipboardMenu(anchor: View) {
        val clip = clipboard().primaryClip
        val copied = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(this)?.toString().orEmpty() else ""
        val prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE)
        val old = prefs.getString("clipboard_history", "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
        val updated = KeyboardFeatures.addClipboardItem(old, copied)
        prefs.edit().putString("clipboard_history", updated.joinToString("\n")).apply()
        if (updated.isEmpty()) { Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show(); return }
        PopupMenu(this, anchor).apply {
            updated.take(KeyboardFeatures.MAX_CLIPBOARD_ITEMS).forEach { value ->
                menu.add(value.replace("\n", " ").take(32)).setOnMenuItemClickListener { commitText(value); true }
            }
            show()
        }
    }
    private fun startVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Grant microphone permission in Voice Keyboard app first", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "No speech recognition service is available", Toast.LENGTH_LONG).show(); return
        }
        try {
            if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
            val selected = getSharedPreferences("keyboard_prefs", MODE_PRIVATE).getString("voice_language", "Auto") ?: "Auto"
            val tag = KeyboardFeatures.voiceLanguageTag(selected) ?: when (language) {
                "Hindi" -> "hi-IN"; "Punjabi" -> "pa-IN"; else -> Locale.getDefault().toLanguageTag()
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, tag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            listening = true; refreshKeyboard(); recognizer?.startListening(intent)
        } catch (_: Exception) { listening = false; refreshKeyboard(); Toast.makeText(this, "Could not start voice recognition", Toast.LENGTH_SHORT).show() }
    }
    private fun stopVoice() { recognizer?.stopListening(); listening = false; refreshKeyboard() }
    private fun commitText(value: String) { currentInputConnection?.commitText(value, 1) }
    private fun sendEnter() {
        val c: InputConnection = currentInputConnection ?: return
        c.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        c.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }
    private fun refreshKeyboard() { setInputView(buildKeyboard()) }
    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { commitText(it) }
        listening = false; refreshKeyboard()
    }
    override fun onError(error: Int) { listening = false; refreshKeyboard(); Toast.makeText(this, "Voice recognition ended (code $error)", Toast.LENGTH_SHORT).show() }
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() { listening = false }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
    override fun onDestroy() { recognizer?.destroy(); recognizer = null; super.onDestroy() }
}
