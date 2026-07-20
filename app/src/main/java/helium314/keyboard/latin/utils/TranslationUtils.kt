package helium314.keyboard.latin.utils

import android.content.SharedPreferences

object TranslationUtils {
    fun getLanguageHistory(prefs: SharedPreferences): List<Pair<String, String>> {
        val historyString = prefs.getString("pref_translation_language_history", "") ?: ""
        if (historyString.isEmpty()) return emptyList()
        return historyString.split("\n").mapNotNull {
            val parts = it.split("|", limit = 2)
            if (parts.size == 2) parts[1] to parts[0] else null
        }
    }

    fun saveLanguageHistory(prefs: SharedPreferences, name: String, code: String) {
        val currentHistory = getLanguageHistory(prefs).toMutableList()
        currentHistory.removeAll { it.second.equals(code, ignoreCase = true) || it.first.equals(name, ignoreCase = true) }
        currentHistory.add(0, name to code)
        val serialized = currentHistory.joinToString("\n") { "${it.second}|${it.first}" }
        prefs.edit().putString("pref_translation_language_history", serialized).apply()
    }

    fun removeLanguageHistory(prefs: SharedPreferences, code: String) {
        val currentHistory = getLanguageHistory(prefs).toMutableList()
        currentHistory.removeAll { it.second.equals(code, ignoreCase = true) || it.first.equals(code, ignoreCase = true) }
        val serialized = currentHistory.joinToString("\n") { "${it.second}|${it.first}" }
        prefs.edit().putString("pref_translation_language_history", serialized).apply()
    }

    fun isSameLanguage(p1: Pair<String, String>, p2: Pair<String, String>): Boolean {
        return p1.first.equals(p2.first, ignoreCase = true) ||
               p1.second.equals(p2.second, ignoreCase = true)
    }
}
