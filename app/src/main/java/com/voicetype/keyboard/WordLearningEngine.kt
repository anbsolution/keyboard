package com.voicetype.keyboard

import android.content.Context
import org.json.JSONObject

/** Private, on-device word dictionary. Entries never leave app storage. */
class WordLearningEngine(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun learn(word: String, previousWord: String = "") {
        val updated = WordLearningLogic.learnWord(readEntries(), word, previousWord, System.currentTimeMillis())
            .entries.sortedByDescending { it.value.lastUsedAt }
            .take(MAX_WORDS).associate { it.toPair() }
        writeEntries(updated)
    }

    @Synchronized
    fun suggestions(prefix: String, previousWord: String = "", limit: Int = 3): List<String> =
        WordLearningLogic.suggestions(readEntries(), prefix, previousWord, limit)

    @Synchronized
    fun correctionCandidates(word: String, limit: Int = 3): List<String> =
        WordLearningLogic.correctionCandidates(readEntries(), word, limit)

    @Synchronized
    fun allWords(): List<LearnedWord> = readEntries().values.sortedWith(
        compareByDescending<LearnedWord> { it.frequency }.thenByDescending { it.lastUsedAt }
    )

    @Synchronized
    fun removeWord(word: String) {
        val entries = readEntries().toMutableMap()
        entries.remove(WordLearningLogic.normalize(word))
        writeEntries(entries)
    }

    @Synchronized
    fun clear() { prefs.edit().remove(KEY_ENTRIES).apply() }

    private fun readEntries(): Map<String, LearnedWord> {
        val root = try { JSONObject(prefs.getString(KEY_ENTRIES, "{}") ?: "{}") }
        catch (_: Exception) { return emptyMap() }
        val result = linkedMapOf<String, LearnedWord>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val item = root.optJSONObject(key) ?: continue
            val nextJson = item.optJSONObject("nextWords") ?: JSONObject()
            val next = linkedMapOf<String, Int>()
            val nextKeys = nextJson.keys()
            while (nextKeys.hasNext()) {
                val nextKey = nextKeys.next()
                val count = nextJson.optInt(nextKey, 0)
                if (count > 0) next[nextKey] = count
            }
            val word = item.optString("word", key)
            if (word.isNotBlank()) result[key] = LearnedWord(
                word = word,
                frequency = item.optInt("frequency", 1).coerceAtLeast(1),
                lastUsedAt = item.optLong("lastUsedAt", 0L),
                nextWords = next
            )
        }
        return result
    }

    private fun writeEntries(entries: Map<String, LearnedWord>) {
        val root = JSONObject()
        entries.forEach { (key, entry) ->
            val next = JSONObject()
            entry.nextWords.forEach { (word, count) -> next.put(word, count) }
            root.put(key, JSONObject().apply {
                put("word", entry.word)
                put("frequency", entry.frequency)
                put("lastUsedAt", entry.lastUsedAt)
                put("nextWords", next)
            })
        }
        prefs.edit().putString(KEY_ENTRIES, root.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "personal_word_dictionary"
        private const val KEY_ENTRIES = "learned_words_v1"
        const val MAX_WORDS = 5000
    }
}
