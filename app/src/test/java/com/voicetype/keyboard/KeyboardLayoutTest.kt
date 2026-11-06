package com.voicetype.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardLayoutTest {
    @Test fun englishRowsAreCorrect() {
        assertEquals(listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"), KeyboardLayout.englishRows)
    }
    @Test fun hindiMappingWorks() {
        assertEquals("क", KeyboardLayout.outputFor('k', "Hindi", false, false))
        assertEquals("य", KeyboardLayout.outputFor('m', "Hindi", false, false))
    }
    @Test fun punjabiMappingWorks() {
        assertEquals("ਕ", KeyboardLayout.outputFor('k', "Punjabi", false, false))
        assertEquals("ਯ", KeyboardLayout.outputFor('m', "Punjabi", false, false))
    }
    @Test fun symbolsRemainLiteral() {
        assertEquals("@", KeyboardLayout.outputFor('@', "Hindi", true, false))
        assertEquals("7", KeyboardLayout.outputFor('7', "Punjabi", true, false))
    }
    @Test fun englishShiftWorks() {
        assertEquals("A", KeyboardLayout.outputFor('a', "English", false, true))
    }
    @Test fun clipboardHistoryDeduplicatesAndLimits() {
        val result = KeyboardFeatures.addClipboardItem(listOf("old", "same"), "same")
        assertEquals(listOf("same", "old"), result)
        val long = (1..30).map { "item$it" }
        assertEquals(20, KeyboardFeatures.addClipboardItem(long, "new").size)
    }
    @Test fun blankClipboardIsIgnored() {
        assertEquals(listOf("keep"), KeyboardFeatures.addClipboardItem(listOf("keep"), "  "))
    }
    @Test fun voiceLanguageSelectionIsMapped() {
        assertEquals("hi-IN", KeyboardFeatures.voiceLanguageTag("Hindi"))
        assertEquals("en-IN", KeyboardFeatures.voiceLanguageTag("English"))
        assertEquals("pa-IN", KeyboardFeatures.voiceLanguageTag("Punjabi"))
        assertEquals(null, KeyboardFeatures.voiceLanguageTag("Auto"))
    }
}
