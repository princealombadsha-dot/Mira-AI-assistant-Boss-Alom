package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.LanguageMode
import com.example.data.model.Personality
import com.example.data.model.ThemeMode
import com.example.data.model.UserMemoryItem
import org.json.JSONArray
import org.json.JSONObject

class AssistantPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bangla_ai_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FIRST_LAUNCH = "key_first_launch"
        private const val KEY_PERSONALITY = "key_personality"
        private const val KEY_LANGUAGE_MODE = "key_language_mode"
        private const val KEY_SPEAKING_RATE = "key_speaking_rate"
        private const val KEY_SPEAKING_PITCH = "key_speaking_pitch"
        private const val KEY_IS_MUTED = "key_is_muted"
        private const val KEY_FLOATING_OVERLAY = "key_floating_overlay"
        private const val KEY_HANDS_FREE = "key_hands_free"
        private const val KEY_IDLE_TIMEOUT = "key_idle_timeout"
        private const val KEY_THEME = "key_theme"
        private const val KEY_BATTERY_SAVER = "key_battery_saver"
        private const val KEY_CUSTOM_API_KEY = "key_custom_api_key"
        private const val KEY_USER_MEMORIES = "key_user_memories"
        private const val KEY_OWNER_NAME = "key_owner_name"
        private const val KEY_OWNER_TITLE = "key_owner_title"
        private const val KEY_ASSISTANT_NAME = "key_assistant_name"
        private const val KEY_WAKE_WORD_ENABLED = "key_wake_word_enabled"
        private const val KEY_WAKE_WORD = "key_wake_word"
        private const val KEY_QUIET_HOURS = "key_quiet_hours"
        private const val KEY_FIRST_GREETING_GIVEN = "key_first_greeting_given"
        private const val KEY_ALL_PERMISSIONS_GRANTED = "key_all_permissions_granted"
        private const val KEY_ALWAYS_ON_MIC = "key_always_on_mic"
        private const val KEY_LAST_LOW_BATTERY_ALERT_LEVEL = "key_last_low_battery_alert_level"
    }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var personality: Personality
        get() {
            val name = prefs.getString(KEY_PERSONALITY, Personality.FRIENDLY.name) ?: Personality.FRIENDLY.name
            return try { Personality.valueOf(name) } catch (e: Exception) { Personality.FRIENDLY }
        }
        set(value) = prefs.edit().putString(KEY_PERSONALITY, value.name).apply()

    var languageMode: LanguageMode
        get() {
            val name = prefs.getString(KEY_LANGUAGE_MODE, LanguageMode.BANGLA.name) ?: LanguageMode.BANGLA.name
            return try { LanguageMode.valueOf(name) } catch (e: Exception) { LanguageMode.BANGLA }
        }
        set(value) = prefs.edit().putString(KEY_LANGUAGE_MODE, value.name).apply()

    var speakingRate: Float
        get() = prefs.getFloat(KEY_SPEAKING_RATE, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEAKING_RATE, value).apply()

    var speakingPitch: Float
        get() = prefs.getFloat(KEY_SPEAKING_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_SPEAKING_PITCH, value).apply()

    var isMuted: Boolean
        get() = prefs.getBoolean(KEY_IS_MUTED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_MUTED, value).apply()

    var floatingOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_OVERLAY, false)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_OVERLAY, value).apply()

    var handsFreeEnabled: Boolean
        get() = prefs.getBoolean(KEY_HANDS_FREE, false)
        set(value) = prefs.edit().putBoolean(KEY_HANDS_FREE, value).apply()

    var idleTimeoutSeconds: Int
        get() = prefs.getInt(KEY_IDLE_TIMEOUT, 15)
        set(value) = prefs.edit().putInt(KEY_IDLE_TIMEOUT, value).apply()

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME, ThemeMode.EMERALD_CYBER.name) ?: ThemeMode.EMERALD_CYBER.name
            return try { ThemeMode.valueOf(name) } catch (e: Exception) { ThemeMode.EMERALD_CYBER }
        }
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    var batterySaverEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_SAVER, false)
        set(value) = prefs.edit().putBoolean(KEY_BATTERY_SAVER, value).apply()

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_KEY, value).apply()

    var ownerName: String
        get() = prefs.getString(KEY_OWNER_NAME, "Alom Boss") ?: "Alom Boss"
        set(value) = prefs.edit().putString(KEY_OWNER_NAME, value).apply()

    var ownerTitle: String
        get() = prefs.getString(KEY_OWNER_TITLE, "Alom Boss") ?: "Alom Boss"
        set(value) = prefs.edit().putString(KEY_OWNER_TITLE, value).apply()

    var assistantName: String
        get() = prefs.getString(KEY_ASSISTANT_NAME, "Maira Bot") ?: "Maira Bot"
        set(value) = prefs.edit().putString(KEY_ASSISTANT_NAME, value).apply()

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()

    var wakeWord: String
        get() = prefs.getString(KEY_WAKE_WORD, "হাই মাইরা") ?: "হাই মাইরা"
        set(value) = prefs.edit().putString(KEY_WAKE_WORD, value).apply()

    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUIET_HOURS, false)
        set(value) = prefs.edit().putBoolean(KEY_QUIET_HOURS, value).apply()

    var firstGreetingGiven: Boolean
        get() = prefs.getBoolean(KEY_FIRST_GREETING_GIVEN, false)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_GREETING_GIVEN, value).apply()

    var allPermissionsGranted: Boolean
        get() = prefs.getBoolean(KEY_ALL_PERMISSIONS_GRANTED, false)
        set(value) = prefs.edit().putBoolean(KEY_ALL_PERMISSIONS_GRANTED, value).apply()

    var alwaysOnMicEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALWAYS_ON_MIC, false)
        set(value) = prefs.edit().putBoolean(KEY_ALWAYS_ON_MIC, value).apply()

    var lastLowBatteryAlertLevel: Int
        get() = prefs.getInt(KEY_LAST_LOW_BATTERY_ALERT_LEVEL, -1)
        set(value) = prefs.edit().putInt(KEY_LAST_LOW_BATTERY_ALERT_LEVEL, value).apply()

    fun getMemories(): List<UserMemoryItem> {
        val raw = prefs.getString(KEY_USER_MEMORIES, null) ?: return defaultMemories()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<UserMemoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    UserMemoryItem(
                        id = obj.optString("id"),
                        key = obj.optString("key"),
                        value = obj.optString("value"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            if (list.isEmpty()) defaultMemories() else list
        } catch (e: Exception) {
            defaultMemories()
        }
    }

    fun saveMemories(list: List<UserMemoryItem>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("key", item.key)
                put("value", item.value)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_USER_MEMORIES, array.toString()).apply()
    }

    private fun defaultMemories(): List<UserMemoryItem> {
        return listOf(
            UserMemoryItem(key = "সহকারীর নাম", value = "Maira Bot"),
            UserMemoryItem(key = "মালিকের নাম", value = "Alom Boss"),
            UserMemoryItem(key = "সম্বোধন", value = "Alom Boss"),
            UserMemoryItem(key = "পছন্দের ভাষা", value = "প্রমিত বাংলা ও Banglish"),
            UserMemoryItem(key = "সহকারীর ধরন", value = "ভদ্র, বুদ্ধিমান, বন্ধুত্বপূর্ণ ও পেশাদার VIP সহকারী")
        )
    }
}
