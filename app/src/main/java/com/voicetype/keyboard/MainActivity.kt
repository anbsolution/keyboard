package com.voicetype.keyboard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val prefs = getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(0xFFF7F9FD.toInt())
        }
        page.addView(TextView(this).apply {
            text = "Voice Keyboard 1.1.0"
            textSize = 27f
            setTextColor(0xFF173B70.toInt())
            gravity = Gravity.CENTER
        })
        page.addView(TextView(this).apply {
            text = "English + Hindi typing with speech input"
            textSize = 15f
            setTextColor(0xFF444444.toInt())
            setPadding(0, 12, 0, 24)
        })
        page.addView(actionButton("Grant microphone permission") {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission already granted", Toast.LENGTH_SHORT).show()
            } else requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
        })
        page.addView(actionButton("1. Enable keyboard") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        page.addView(actionButton("2. Select Voice Keyboard") {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })
        page.addView(actionButton("Theme: ${prefs.getString("theme", "Light")} (tap to change)") {
            val next = when (prefs.getString("theme", "Light")) {
                "Light" -> "Dark"
                "Dark" -> "Blue"
                else -> "Light"
            }
            prefs.edit().putString("theme", next).apply()
            render()
        })
        page.addView(actionButton("Voice language: ${prefs.getString("voice_language", "Auto")} (tap to change)") {
            val next = when (prefs.getString("voice_language", "Auto")) {
                "Auto" -> "Hindi"
                "Hindi" -> "English"
                else -> "Auto"
            }
            prefs.edit().putString("voice_language", next).apply()
            render()
        })
        page.addView(TextView(this).apply {
            text = "Privacy: the microphone is used only after permission is granted and you tap the voice key. Speech recognition depends on the Android recognition service and its language/offline support."
            textSize = 13f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 22, 0, 0)
        })
        setContentView(page)
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 10 }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            Toast.makeText(this, if (granted) "Microphone permission granted" else "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    companion object { private const val REQUEST_AUDIO = 2101 }
}
