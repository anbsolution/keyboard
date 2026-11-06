package com.voicetype.keyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 36, 24, 24)
            setBackgroundColor(Color.rgb(247, 249, 253))
        }

        val title = TextView(this).apply {
            text = "Voice Keyboard"
            textSize = 28f
            setTextColor(Color.rgb(20, 55, 110))
            gravity = Gravity.CENTER
        }
        val description = TextView(this).apply {
            text = "A private keyboard with on-device typing. Offline voice recognition is planned for Phase 2 and will be enabled only after language-model testing."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, 18, 0, 24)
        }

        val enable = actionButton("1. Enable keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        val choose = actionButton("2. Select Voice Keyboard") {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
        val privacy = TextView(this).apply {
            text = "Privacy: Phase 1 does not request microphone or internet access. Typed text is sent only to the focused app through Android's InputMethod API."
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(0, 24, 0, 0)
        }

        page.addView(title)
        page.addView(description)
        page.addView(enable)
        page.addView(choose)
        page.addView(privacy)
        setContentView(page)
    }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(28, 86, 170))
            setOnClickListener { action() }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 12
            layoutParams = params
        }
}
