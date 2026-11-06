package com.voicetype.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordLearningLogicTest {
    @Test fun learnsWordsAndFrequencyWithoutLosingDisplayCase() {
        var entries = emptyMap<String, LearnedWord>()
        entries = WordLearningLogic.learnWord(entries, "Hello", now = 10L)
        entries = WordLearningLogic.learnWord(entries, "hello", now = 20L)
        assertEquals(1, entries.size)
        assertEquals("Hello", entries["hello"]?.word)
        assertEquals(2, entries["hello"]?.frequency)
        assertEquals(20L, entries["hello"]?.lastUsedAt)
    }

    @Test fun supportsHindiAndPunjabiUnicodeWordsAndShortWords() {
        assertEquals(listOf("नमस्ते", "ਸਤ", "ਸ੍ਰੀ", "ਅਕਾਲ"), WordLearningLogic.tokenize("नमस्ते, ਸਤ ਸ੍ਰੀ ਅਕਾਲ!"))
        val learned = WordLearningLogic.learnWord(emptyMap(), "I", now = 1L)
        assertTrue(learned.containsKey("i"))
    }

    @Test fun predictsNextWordFromPreviousWordContext() {
        var entries = emptyMap<String, LearnedWord>()
        entries = WordLearningLogic.learnWord(entries, "world", "hello", 10L)
        entries = WordLearningLogic.learnWord(entries, "there", "hello", 20L)
        entries = WordLearningLogic.learnWord(entries, "world", "hello", 30L)
        assertEquals("world", WordLearningLogic.suggestions(entries, "", "hello", 1).first())
    }

    @Test fun prefixSuggestionsUseFrequencyAndCaseInsensitiveMatching() {
        var entries = emptyMap<String, LearnedWord>()
        entries = WordLearningLogic.learnWord(entries, "hello", now = 10L)
        entries = WordLearningLogic.learnWord(entries, "help", now = 20L)
        entries = WordLearningLogic.learnWord(entries, "hello", now = 30L)
        assertEquals("hello", WordLearningLogic.suggestions(entries, "HE", limit = 1).first())
    }

    @Test fun correctionCandidatesAreSuggestionsOnlyAndBounded() {
        var entries = emptyMap<String, LearnedWord>()
        entries = WordLearningLogic.learnWord(entries, "hello", now = 10L)
        assertEquals(listOf("hello"), WordLearningLogic.correctionCandidates(entries, "helo"))
        assertTrue(WordLearningLogic.correctionCandidates(entries, "xy").isEmpty())
    }

    @Test fun rejectsPasswordEmailUriAndNonTextFields() {
        assertFalse(WordLearningLogic.isLearningAllowed(0x00000081)) // text password
        assertFalse(WordLearningLogic.isLearningAllowed(0x00000021)) // email
        assertFalse(WordLearningLogic.isLearningAllowed(0x00000011)) // URI
        assertFalse(WordLearningLogic.isLearningAllowed(0x00000002)) // number
        assertFalse(WordLearningLogic.isLearningAllowed(0x00000001 or 0x00080000)) // no suggestions
        assertTrue(WordLearningLogic.isLearningAllowed(0x00000001)) // ordinary text
    }
}
