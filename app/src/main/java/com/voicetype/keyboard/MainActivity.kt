package com.voicetype.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 36, 24, 24)
            setBackgroundColor(Color.rgb(247, 249, 253))
        }

        page.addView(TextView(this).apply {
            text = "Voice Keyboard"
            textSize = 28f
            setTextColor(Color.rgb(20, 55, 110))
            gravity = Gravity.CENTER
        })

        page.addView(TextView(this).apply {
            text = "English and Hindi typing keyboard. Offline voice typing is planned for a future release."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, 18, 0, 24)
        })

        page.addView(actionButton("1. Enable keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        page.addView(actionButton("2. Select Voice Keyboard") {
            val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            manager.showInputMethodPicker()
        })

        page.addView(TextView(this).apply {
            text = "Privacy: this release does not request microphone or internet access. Text is inserted into the focused app using Android's InputMethod API."
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(0, 24, 0, 0)
        })

        setContentView(page)
    }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(28, 86, 170))
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
}
