package com.voicetype.keyboard

object KeyboardLayout {
    val englishRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    val symbolRows = listOf("1234567890", "@#$%&*-+()")

    private val hindiMap = mapOf(
        'q' to "ौ", 'w' to "ै", 'e' to "ा", 'r' to "ी", 't' to "ू",
        'y' to "ब", 'u' to "ह", 'i' to "ग", 'o' to "द", 'p' to "ज",
        'a' to "ो", 's' to "े", 'd' to "्", 'f' to "ि", 'g' to "ु",
        'h' to "प", 'j' to "र", 'k' to "क", 'l' to "त",
        'z' to "ं", 'x' to "म", 'c' to "न", 'v' to "व", 'b' to "ल",
        'n' to "स", 'm' to "य"
    )

    private val punjabiMap = mapOf(
        'q' to "ੌ", 'w' to "ੈ", 'e' to "ਾ", 'r' to "ੀ", 't' to "ੂ",
        'y' to "ਬ", 'u' to "ਹ", 'i' to "ਗ", 'o' to "ਦ", 'p' to "ਜ",
        'a' to "ੋ", 's' to "ੇ", 'd' to "੍", 'f' to "ਿ", 'g' to "ੁ",
        'h' to "ਪ", 'j' to "ਰ", 'k' to "ਕ", 'l' to "ਤ",
        'z' to "ੰ", 'x' to "ਮ", 'c' to "ਨ", 'v' to "ਵ", 'b' to "ਲ",
        'n' to "ਸ", 'm' to "ਯ"
    )

    fun outputFor(key: Char, language: String, symbolsMode: Boolean, shift: Boolean): String {
        if (symbolsMode) return key.toString()
        val lower = key.lowercaseChar()
        return when (language) {
            "Hindi" -> hindiMap[lower] ?: key.toString()
            "Punjabi" -> punjabiMap[lower] ?: key.toString()
            else -> if (shift) key.uppercaseChar().toString() else key.toString()
        }
    }

    fun labelFor(key: Char, language: String, symbolsMode: Boolean, shift: Boolean) =
        outputFor(key, language, symbolsMode, shift)
}
