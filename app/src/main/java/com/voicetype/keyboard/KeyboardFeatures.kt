package com.voicetype.keyboard

object KeyboardFeatures {
    const val MAX_CLIPBOARD_ITEMS = 20

    fun addClipboardItem(history: List<String>, value: String): List<String> {
        val clean = value.trim()
        if (clean.isEmpty()) return history.take(MAX_CLIPBOARD_ITEMS)
        val updated = listOf(clean) + history.filterNot { it == clean }
        return updated.take(MAX_CLIPBOARD_ITEMS)
    }

    fun voiceLanguageTag(selection: String): String? = when (selection) {
        "Hindi" -> "hi-IN"
        "English" -> "en-IN"
        "Punjabi" -> "pa-IN"
        "Auto" -> null
        else -> null
    }

    fun shortcutLabel(action: String): String = when (action) {
        "left" -> "←"
        "right" -> "→"
        "select_all" -> "Select all"
        "delete_word" -> "Del word"
        else -> action
    }
}
