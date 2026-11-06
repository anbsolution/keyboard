package com.voicetype.keyboard

import android.graphics.Color
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class KeyboardService : InputMethodService() {
    private var shift = false
    private var hindiMode = false
    private var symbolsMode = false

    override fun onCreateInputView(): View = buildKeyboard()

    private fun buildKeyboard(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 8, 4, 8)
            setBackgroundColor(Color.rgb(232, 237, 246))
        }

        val top = row()
        top.addView(key(if (hindiMode) "हि" else "EN", 1.25f) {
            hindiMode = !hindiMode
            symbolsMode = false
            refreshKeyboard()
        })
        top.addView(key("🎙", 1f) {
            Toast.makeText(this, "Offline voice typing will be added in Phase 2", Toast.LENGTH_SHORT).show()
        })
        top.addView(key("⚙", 1f) {
            Toast.makeText(this, "Keyboard settings will be added in a later update", Toast.LENGTH_SHORT).show()
        })
        root.addView(top)

        val rows = if (symbolsMode) KeyboardLayout.symbolRows else KeyboardLayout.englishRows
        rows.forEachIndexed { index, chars ->
            val line = row()
            if (index == 2 && !symbolsMode) {
                line.addView(key("⇧", 1.25f) {
                    shift = !shift
                    refreshKeyboard()
                })
            }
            chars.forEach { char ->
                val label = KeyboardLayout.labelFor(char, hindiMode, symbolsMode, shift)
                line.addView(key(label, 1f) {
                    commitText(KeyboardLayout.outputFor(char, hindiMode, symbolsMode, shift))
                    if (shift && !hindiMode && !symbolsMode) {
                        shift = false
                        refreshKeyboard()
                    }
                })
            }
            if (index == 2 && !symbolsMode) {
                line.addView(key("⌫", 1.25f) { deletePreviousCharacter() })
            }
            root.addView(line)
        }

        val bottom = row()
        bottom.addView(key(if (symbolsMode) "ABC" else "?123", 1.25f) {
            symbolsMode = !symbolsMode
            refreshKeyboard()
        })
        bottom.addView(key(",", 0.8f) { commitText(",") })
        bottom.addView(key(if (hindiMode) "हि" else "EN", 1.1f) {
            hindiMode = !hindiMode
            refreshKeyboard()
        })
        bottom.addView(key("space", 3.2f) { commitText(" ") })
        bottom.addView(key(".", 0.8f) { commitText(".") })
        bottom.addView(key("↵", 1.1f) { sendEnter() })
        root.addView(bottom)
        return root
    }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }

    private fun key(label: String, weight: Float, action: () -> Unit) =
        Button(this).apply {
            text = label
            textSize = if (label.length > 3) 11f else 18f
            isAllCaps = false
            setTextColor(Color.rgb(24, 43, 72))
            setBackgroundColor(Color.WHITE)
            typeface = Typeface.DEFAULT
            setPadding(1, 2, 1, 2)
            layoutParams = LinearLayout.LayoutParams(0, 48, weight).apply {
                setMargins(2, 3, 2, 3)
            }
            setOnClickListener { action() }
        }

    private fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun deletePreviousCharacter() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun sendEnter() {
        val connection: InputConnection = currentInputConnection ?: return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun refreshKeyboard() {
        setInputView(buildKeyboard())
    }
}
