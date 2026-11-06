package com.voicetype.keyboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import java.util.Locale

class KeyboardService : InputMethodService(), RecognitionListener {
    private var shift = false
    private var language = "English"
    private var symbolsMode = false
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    override fun onCreateInputView(): View = buildKeyboard()

    private fun buildKeyboard(): View {
        val prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE)
        language = prefs.getString("layout_language", "English") ?: "English"
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
            setPadding(4, 7, 4, 7)
            setBackgroundColor(bg)
        }

        val toolbar = row()
        toolbar.addView(key(language.take(3), 1f, keyBg, fg) { showLanguageMenu(itView = root) })
        toolbar.addView(key(if (listening) "■ Stop" else "🎙 Voice", 1.4f, keyBg, fg) {
            if (listening) stopVoice() else startVoice()
        })
        toolbar.addView(key("📋", .8f, keyBg, fg) { anchor -> showClipboardMenu(anchor) })
        toolbar.addView(key("⚙", .8f, keyBg, fg) {
            Toast.makeText(this, "Theme and voice settings are in the Voice Keyboard app", Toast.LENGTH_SHORT).show()
        })
        root.addView(toolbar)

        val rows = if (symbolsMode) KeyboardLayout.symbolRows else KeyboardLayout.englishRows
        rows.forEachIndexed { index, chars ->
            val line = row()
            if (index == 2 && !symbolsMode) line.addView(key("⇧", 1.1f, keyBg, fg) {
                shift = !shift; refreshKeyboard()
            })
            chars.forEach { char ->
                line.addView(key(KeyboardLayout.labelFor(char, language, symbolsMode, shift), 1f, keyBg, fg) {
                    commitText(KeyboardLayout.outputFor(char, language, symbolsMode, shift))
                    if (shift && language == "English" && !symbolsMode) { shift = false; refreshKeyboard() }
                })
            }
            if (index == 2 && !symbolsMode) line.addView(key("⌫", 1.1f, keyBg, fg) {
                currentInputConnection?.deleteSurroundingText(1, 0)
            })
            root.addView(line)
        }

        val bottom = row()
        bottom.addView(key(if (symbolsMode) "ABC" else "?123", 1.1f, keyBg, fg) {
            symbolsMode = !symbolsMode; refreshKeyboard()
        })
        bottom.addView(key("←", .7f, keyBg, fg) { sendKey(KeyEvent.KEYCODE_DPAD_LEFT) })
        bottom.addView(key("space", 2.6f, keyBg, fg) { commitText(" ") })
        bottom.addView(key("→", .7f, keyBg, fg) { sendKey(KeyEvent.KEYCODE_DPAD_RIGHT) })
        bottom.addView(key("↵", .9f, keyBg, fg) { sendEnter() })
        root.addView(bottom)

        val shortcuts = row()
        shortcuts.addView(key("Select all", 1.2f, keyBg, fg) { selectAll() })
        shortcuts.addView(key("Del word", 1f, keyBg, fg) { deletePreviousWord() })
        shortcuts.addView(key("Paste", .8f, keyBg, fg) { pasteClipboard() })
        root.addView(shortcuts)
        return root
    }

    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
    private fun key(label: String, weight: Float, bg: Int, fg: Int, action: (View) -> Unit) =
        Button(this).apply {
            text = label
            textSize = if (label.length > 5) 10f else 15f
            isAllCaps = false
            setTextColor(fg)
            setBackgroundColor(bg)
            setPadding(1, 1, 1, 1)
            layoutParams = LinearLayout.LayoutParams(0, 44, weight).apply { setMargins(2, 2, 2, 2) }
            setOnClickListener { action(it) }
        }

    private fun showLanguageMenu(itView: View) {
        PopupMenu(this, itView).apply {
            listOf("English", "Hindi", "Punjabi").forEach { item ->
                menu.add(item).setOnMenuItemClickListener {
                    language = item
                    getSharedPreferences("keyboard_prefs", MODE_PRIVATE).edit().putString("layout_language", item).apply()
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
        val copied = if (clip != null && clip.itemCount > 0) clip.getItemAt(0).coerceToText(this)?.toString().orEmpty() else ""
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
                val label = value.replace("\n", " ").take(32)
                menu.add(label).setOnMenuItemClickListener {
                    commitText(value)
                    true
                }
            }
            show()
        }
    }

    private fun pasteClipboard() {
        val clip = clipboard().primaryClip
        if (clip != null && clip.itemCount > 0) {
            commitText(clip.getItemAt(0).coerceToText(this)?.toString().orEmpty())
        } else Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
    }

    private fun selectAll() {
        val connection = currentInputConnection ?: return
        connection.performContextMenuAction(android.R.id.selectAll)
    }

    private fun deletePreviousWord() {
        val connection = currentInputConnection ?: return
        val before = connection.getTextBeforeCursor(100, 0)?.toString().orEmpty()
        if (before.isEmpty()) return
        val withoutTrailingSpaces = before.dropLastWhile { it.isWhitespace() }
        val word = withoutTrailingSpaces.takeLastWhile { !it.isWhitespace() }
        val trailingSpaces = before.length - withoutTrailingSpaces.length
        val charsToDelete = word.codePointCount(0, word.length) +
            trailingSpaces
        if (charsToDelete > 0) connection.deleteSurroundingTextInCodePoints(charsToDelete, 0)
    }

    private fun sendKey(code: Int) {
        val connection = currentInputConnection ?: return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
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
            if (recognizer == null) recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
            val prefs = getSharedPreferences("keyboard_prefs", MODE_PRIVATE)
            val selected = prefs.getString("voice_language", "Auto") ?: "Auto"
            val tag = KeyboardFeatures.voiceLanguageTag(selected)
                ?: when (language) {
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
