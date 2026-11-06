package com.voicetype.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ScrollView
import android.widget.Toast

/** Local dictionary controls. No account, network or cloud synchronization. */
class SmartDictionaryActivity : Activity() {
    private lateinit var engine: WordLearningEngine
    private lateinit var list: LinearLayout
    private lateinit var count: TextView
    private val settings by lazy { getSharedPreferences("smart_learning_settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = WordLearningEngine(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFFF7F9FD.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Smart Learning Dictionary"
            textSize = 24f
            setTextColor(0xFF173B70.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Words are stored locally on this device. No cloud sync."
            textSize = 14f
            setPadding(0, 8, 0, 12)
        })
        addToggle(root, "Learn words in normal text fields", "learning_enabled", true)
        addToggle(root, "Predict next word", "prediction_enabled", true)
        addToggle(root, "Show spelling correction suggestions", "correction_enabled", true)
        count = TextView(this).apply { textSize = 16f; setPadding(0, 14, 0, 8) }
        root.addView(count)
        root.addView(Button(this).apply {
            text = "Clear personal dictionary"
            isAllCaps = false
            setOnClickListener {
                AlertDialog.Builder(this@SmartDictionaryActivity)
                    .setTitle("Clear dictionary?")
                    .setMessage("All locally learned words and frequency data will be deleted.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Clear") { _, _ -> engine.clear(); renderWords(); Toast.makeText(this@SmartDictionaryActivity, "Dictionary cleared", Toast.LENGTH_SHORT).show() }
                    .show()
            }
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
        renderWords()
    }

    private fun addToggle(root: LinearLayout, label: String, key: String, default: Boolean) {
        root.addView(CheckBox(this).apply {
            text = label
            isChecked = settings.getBoolean(key, default)
            setOnCheckedChangeListener { _, checked -> settings.edit().putBoolean(key, checked).apply() }
        })
    }

    private fun renderWords() {
        list.removeAllViews()
        val words = engine.allWords()
        count.text = "Saved words: ${words.size} / ${WordLearningEngine.MAX_WORDS}"
        words.take(250).forEach { entry ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 3, 0, 3)
            }
            row.addView(TextView(this).apply {
                text = "${entry.word}  ·  ${entry.frequency}"
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(Button(this).apply {
                text = "Delete"
                isAllCaps = false
                setOnClickListener { engine.removeWord(entry.word); renderWords() }
            })
            list.addView(row)
        }
        if (words.isEmpty()) list.addView(TextView(this).apply { text = "No learned words yet."; setPadding(0, 12, 0, 0) })
        if (words.size > 250) list.addView(TextView(this).apply { text = "Showing the 250 most-used words."; setPadding(0, 8, 0, 0) })
    }
}
