package com.voicetype.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardLayoutTest {
    @Test fun englishRowsHaveExpectedKeys() {
        assertEquals(listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"), KeyboardLayout.englishRows)
    }

    @Test fun hindiMappingReturnsExpectedDevanagariCharacters() {
        assertEquals("ौ", KeyboardLayout.hindiCharacter('q'))
        assertEquals("क", KeyboardLayout.hindiCharacter('k'))
        assertEquals("य", KeyboardLayout.hindiCharacter('m'))
    }

    @Test fun hindiMappingIsCaseInsensitive() {
        assertEquals(KeyboardLayout.hindiCharacter('q'), KeyboardLayout.hindiCharacter('Q'))
    }
}
