package com.voicetype.keyboard

import java.util.Locale

/** Pure local dictionary logic. No network access and no Android dependencies. */
data class LearnedWord(
    val word: String,
    val frequency: Int,
    val lastUsedAt: Long,
    val nextWords: Map<String, Int> = emptyMap()
)

object WordLearningLogic {
    private val tokenPattern = Regex("[\\p{L}\\p{M}]+(?:['’][\\p{L}\\p{M}]+)*")

    fun normalize(word: String): String = word.lowercase(Locale.ROOT)

    fun tokenize(text: String): List<String> =
        tokenPattern.findAll(text).map { it.value }.toList()

    fun learnWord(
        entries: Map<String, LearnedWord>,
        word: String,
        previousWord: String = "",
        now: Long
    ): Map<String, LearnedWord> {
        val clean = word.trim()
        if (clean.isBlank() || clean.none { it.isLetter() }) return entries
        val key = normalize(clean)
        val previousKey = normalize(previousWord.trim())
        val old = entries[key]
        val next = old?.nextWords?.toMutableMap() ?: mutableMapOf()
        if (previousKey.isNotBlank() && previousKey != key) {
            next[previousKey] = (next[previousKey] ?: 0) + 1
            if (next.size > 20) {
                val keep = next.entries.sortedByDescending { it.value }.take(20).associate { it.toPair() }
                next.clear()
                next.putAll(keep)
            }
        }
        return entries + (key to LearnedWord(
            word = old?.word ?: clean,
            frequency = (old?.frequency ?: 0) + 1,
            lastUsedAt = now,
            nextWords = next
        ))
    }

    fun learnText(
        entries: Map<String, LearnedWord>,
        text: String,
        now: Long
    ): Map<String, LearnedWord> {
        var result = entries
        var previous = ""
        tokenize(text).forEach { word ->
            result = learnWord(result, word, previous, now)
            previous = word
        }
        return result
    }

    fun suggestions(
        entries: Map<String, LearnedWord>,
        prefix: String,
        previousWord: String = "",
        limit: Int = 3
    ): List<String> {
        if (limit <= 0) return emptyList()
        val cleanPrefix = normalize(prefix.trim())
        if (cleanPrefix.isNotEmpty()) {
            return entries.values.asSequence()
                .filter { normalize(it.word).startsWith(cleanPrefix) }
                .sortedWith(compareByDescending<LearnedWord> { it.frequency }.thenByDescending { it.lastUsedAt })
                .take(limit).map { it.word }.toList()
        }
        val previousKey = normalize(previousWord.trim())
        if (previousKey.isBlank()) return emptyList()
        return entries.values.asSequence()
            .filter { (it.nextWords[previousKey] ?: 0) > 0 }
            .sortedWith(compareByDescending<LearnedWord> { it.nextWords[previousKey] ?: 0 }
                .thenByDescending { it.frequency }.thenByDescending { it.lastUsedAt })
            .take(limit).map { it.word }.toList()
    }

    fun correctionCandidates(entries: Map<String, LearnedWord>, word: String, limit: Int = 3): List<String> {
        val query = normalize(word.trim())
        if (query.length < 3 || limit <= 0) return emptyList()
        return entries.values.asSequence()
            .filter { candidate ->
                val value = normalize(candidate.word)
                value != query && value.firstOrNull() == query.firstOrNull() &&
                    kotlin.math.abs(value.length - query.length) <= 2 &&
                    editDistanceAtMostTwo(query, value)
            }
            .sortedWith(compareBy<LearnedWord> { editDistance(query, normalize(it.word)) }
                .thenByDescending { it.frequency }.thenByDescending { it.lastUsedAt })
            .take(limit).map { it.word }.toList()
    }

    private fun editDistanceAtMostTwo(a: String, b: String): Boolean = editDistance(a, b) <= 2

    private fun editDistance(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 2) return 3
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            var rowMin = current[0]
            for (j in b.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (a[i] == b[j]) 0 else 1
                )
                rowMin = minOf(rowMin, current[j + 1])
            }
            if (rowMin > 2) return 3
            previous = current
        }
        return previous[b.length]
    }

    /** Learn only ordinary text fields; unknown, numeric and sensitive field types fail closed. */
    fun isLearningAllowed(inputType: Int): Boolean {
        val inputClass = inputType and 0x0000000f
        val variation = inputType and 0x00000ff0
        val noSuggestions = inputType and 0x00080000 != 0
        if (noSuggestions || inputClass != 0x00000001) return false // TYPE_CLASS_TEXT only
        return variation in setOf(
            0x00000000, // TYPE_TEXT_VARIATION_NORMAL
            0x00000060, // TYPE_TEXT_VARIATION_PERSON_NAME
            0x00000070  // TYPE_TEXT_VARIATION_POSTAL_ADDRESS
        )
    }
}
