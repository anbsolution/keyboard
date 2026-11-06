package com.voicetype.keyboard

object KeyboardLayout {
    val englishRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    val symbolRows = listOf("1234567890", "@#\$%&*-+()")

    // Hindi labels are shown on the corresponding QWERTY positions.
    // Dependent vowel signs are used as on a conventional Hindi layout.
    private val hindiMap = mapOf(
        'q' to "ौ", 'w' to "ै", 'e' to "ा", 'r' to "ी", 't' to "ू",
        'y' to "ब", 'u' to "ह", 'i' to "ग", 'o' to "द", 'p' to "ज",
        'a' to "ो", 's' to "े", 'd' to "्", 'f' to "ि", 'g' to "ु",
        'h' to "प", 'j' to "र", 'k' to "क", 'l' to "त",
        'z' to "ं", 'x' to "म", 'c' to "न", 'v' to "व", 'b' to "ल",
        'n' to "स", 'm' to "य"
    )

    fun outputFor(key: Char, hindiMode: Boolean, symbolsMode: Boolean, shift: Boolean): String {
        if (symbolsMode) return key.toString()
        if (hindiMode) return hindiMap[key.lowercaseChar()] ?: key.toString()
        return if (shift) key.uppercaseChar().toString() else key.toString()
    }

    fun labelFor(key: Char, hindiMode: Boolean, symbolsMode: Boolean, shift: Boolean): String =
        outputFor(key, hindiMode, symbolsMode, shift)
}
