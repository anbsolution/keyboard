package com.voicetype.keyboard

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.Gravity
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class KeyboardService : InputMethodService() {
    private var shift = false
    private var hindiMode = false
    private var symbolsMode = false

    override fun onCreateInputView(): View {
        return buildKeyboard()
    }

    private fun buildKeyboard(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(4, 8, 4, 8)
            setBackgroundColor(Color.rgb(232, 237, 246))
        }

        val top = row()
        top.addView(key("EN/हि", 1.25f) {
            hindiMode = !hindiMode
            symbolsMode = false
            setInputView(buildKeyboard())
        })
        top.addView(key("🎙", 1f) {
            Toast.makeText(this, "Offline voice engine Phase 2 में जोड़ा जाएगा", Toast.LENGTH_LONG).show()
        })
        top.addView(key("⚙", 1f) {
            Toast.makeText(this, "Keyboard settings अगली update में आएँगी", Toast.LENGTH_SHORT).show()
        })
        root.addView(top)

        val rows = if (symbolsMode) {
            listOf("1234567890", "@#\$%&*-+()")
        } else KeyboardLayout.englishRows

        rows.forEachIndexed { index, chars ->
            val line = row()
            if (index == 2 && !symbolsMode) {
                line.addView(key("⇧", 1.25f) { shift = !shift; setInputView(buildKeyboard()) })
            }
            chars.forEach { char ->
                val label = if (shift && !hindiMode) char.uppercaseChar().toString() else char.toString()
                line.addView(key(label, 1f) { commitKey(char) })
            }
            if (index == 2 && !symbolsMode) {
                line.addView(key("⌫", 1.25f) {
                    currentInputConnection?.deleteSurroundingText(1, 0)
                })
            }
            root.addView(line)
        }

        val bottom = row()
        bottom.addView(key(if (symbolsMode) "ABC" else "?123", 1.3f) {
            symbolsMode = !symbolsMode
            setInputView(buildKeyboard())
        })
        bottom.addView(key(",", 0.8f) { commitText(",") })
        bottom.addView(key("English/Hindi", 1.2f) {
            hindiMode = !hindiMode
            setInputView(buildKeyboard())
        })
        bottom.addView(key("space", 3.8f) { commitText(" ") })
        bottom.addView(key(".", 0.8f) { commitText(".") })
        bottom.addView(key("↵", 1.2f) { sendEnter() })
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
            val p = LinearLayout.LayoutParams(0, 48, weight)
            p.setMargins(2, 3, 2, 3)
            layoutParams = p
            setOnClickListener { action() }
        }

    private fun commitKey(key: Char) {
        val text = if (hindiMode) KeyboardLayout.hindiCharacter(key) else {
            if (shift) key.uppercaseChar().toString() else key.toString()
        }
        commitText(text)
        if (shift) {
            shift = false
            setInputView(buildKeyboard())
        }
    }

    private fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun sendEnter() {
        val ic: InputConnection = currentInputConnection ?: return
        ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
    }
}
