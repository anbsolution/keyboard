package com.voicetype.keyboard

object KeyboardLayout {
    val englishRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    val hindiRows = listOf("ौैाीूब ह ग द ज", "ोे्िुप र क त च", "ंम न व ल स य")
        .map { it.replace(" ", "") }

    fun hindiCharacter(key: Char): String = when (key.lowercaseChar()) {
        'q' -> "ौ"; 'w' -> "ै"; 'e' -> "ा"; 'r' -> "ी"; 't' -> "ू"
        'y' -> "ब"; 'u' -> "ह"; 'i' -> "ग"; 'o' -> "द"; 'p' -> "ज"
        'a' -> "ो"; 's' -> "े"; 'd' -> "्"; 'f' -> "ि"; 'g' -> "ु"
        'h' -> "प"; 'j' -> "र"; 'k' -> "क"; 'l' -> "त"
        'z' -> "ं"; 'x' -> "म"; 'c' -> "न"; 'v' -> "व"; 'b' -> "ल"
        'n' -> "स"; 'm' -> "य"
        else -> key.toString()
    }
}
