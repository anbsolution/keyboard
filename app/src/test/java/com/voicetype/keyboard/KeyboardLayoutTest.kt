package com.voicetype.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardLayoutTest {
    @Test
    fun englishRowsAreCorrect() {
        assertEquals(listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"), KeyboardLayout.englishRows)
    }

    @Test
    fun hindiModeOutputsDevanagariCharacters() {
        assertEquals("क", KeyboardLayout.outputFor('k', hindiMode = true, symbolsMode = false, shift = false))
        assertEquals("य", KeyboardLayout.outputFor('m', hindiMode = true, symbolsMode = false, shift = false))
        assertEquals("ौ", KeyboardLayout.outputFor('q', hindiMode = true, symbolsMode = false, shift = false))
    }

    @Test
    fun symbolModeAlwaysOutputsLiteralSymbol() {
        assertEquals("@", KeyboardLayout.outputFor('@', hindiMode = true, symbolsMode = true, shift = false))
        assertEquals("7", KeyboardLayout.outputFor('7', hindiMode = true, symbolsMode = true, shift = false))
    }

    @Test
    fun englishShiftIsApplied() {
        assertEquals("A", KeyboardLayout.outputFor('a', hindiMode = false, symbolsMode = false, shift = true))
        assertEquals("a", KeyboardLayout.outputFor('a', hindiMode = false, symbolsMode = false, shift = false))
    }

    @Test
    fun hindiModeDoesNotUppercaseCharacters() {
        assertEquals("क", KeyboardLayout.outputFor('k', hindiMode = true, symbolsMode = false, shift = true))
    }
}
