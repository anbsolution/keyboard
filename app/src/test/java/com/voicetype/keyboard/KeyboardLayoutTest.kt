package com.voicetype.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardLayoutTest {
    @Test fun englishRowsAreCorrect() {
        assertEquals(listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"), KeyboardLayout.englishRows)
    }
    @Test fun hindiMappingWorks() {
        assertEquals("क", KeyboardLayout.outputFor('k', true, false, false))
        assertEquals("य", KeyboardLayout.outputFor('m', true, false, false))
    }
    @Test fun symbolsRemainLiteral() {
        assertEquals("@", KeyboardLayout.outputFor('@', true, true, false))
        assertEquals("7", KeyboardLayout.outputFor('7', true, true, false))
    }
    @Test fun englishShiftWorks() {
        assertEquals("A", KeyboardLayout.outputFor('a', false, false, true))
    }
}
