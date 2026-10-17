package hu.unideb.studyversity.core

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import hu.unideb.studyversity.data.model.StudentMoodGame
import hu.unideb.studyversity.ui.happy_challenge.Mood
import java.util.Calendar
import java.util.Date

/** Typed access to the app's SharedPreferences (mirrors the original AppPreferences). */
class AppPreferences(context: Context, private val gson: Gson) {
    private val sp: SharedPreferences = context.getSharedPreferences("studyversity_prefs", Context.MODE_PRIVATE)

    var language: String
        get() = sp.getString("language", "") ?: ""
        set(v) = sp.edit().putString("language", v).apply()

    var deviceToken: String
        get() = sp.getString("device_token", "") ?: ""
        set(v) = sp.edit().putString("device_token", v).apply()

    // push settings (settings screen)
    var pushStudentInformation: Boolean
        get() = sp.getBoolean("push_student_information", true)
        set(v) = sp.edit().putBoolean("push_student_information", v).apply()
    var pushPromotion: Boolean
        get() = sp.getBoolean("push_promotion", true)
        set(v) = sp.edit().putBoolean("push_promotion", v).apply()
    var pushOther: Boolean
        get() = sp.getBoolean("push_other", true)
        set(v) = sp.edit().putBoolean("push_other", v).apply()

    var allEventsSavedToCalendar: Boolean
        get() = sp.getBoolean("all_events_saved_to_calendar", false)
        set(v) = sp.edit().putBoolean("all_events_saved_to_calendar", v).apply()

    // happy challenge / mood game ------------------------------------------------------------------------------
    var happyChallengeEnabled: Boolean
        get() = sp.getBoolean("happy_challenge_enabled", true)
        set(v) = sp.edit().putBoolean("happy_challenge_enabled", v).apply()
    var happyChallengeAutoTurnedOn: Boolean
        get() = sp.getBoolean("happy_challenge_auto_turned_on", false)
        set(v) = sp.edit().putBoolean("happy_challenge_auto_turned_on", v).apply()
    var happyChallengeAnswerDate: Long
        get() = sp.getLong("happy_challenge_answer_date", 0L)
        set(v) = sp.edit().putLong("happy_challenge_answer_date", v).apply()
    /** Last time the "how is your day" sheet was completed (MOOD_WIZARD_LAST_PRESENTED). */
    var moodWizardLastPresented: Long
        get() = sp.getLong("mood_wizard_last_presented", -1L)
        set(v) = sp.edit().putLong("mood_wizard_last_presented", v).apply()
    var registeredToMoodGame: Boolean
        get() = sp.getBoolean("registered_to_mood_game", false)
        set(v) = sp.edit().putBoolean("registered_to_mood_game", v).apply()
    var userSkippedForToday: Long
        get() = sp.getLong("user_skipped_for_today", -1L)
        set(v) = sp.edit().putLong("user_skipped_for_today", v).apply()
    var userSelectedMood: Mood?
        get() = sp.getString("user_selected_mood", null)?.let { runCatching { Mood.valueOf(it) }.getOrNull() }
        set(v) = sp.edit().putString("user_selected_mood", v?.name).apply()
    var lastMoodTime: Long
        get() = sp.getLong("last_mood_time", -1L)
        set(v) = sp.edit().putLong("last_mood_time", v).apply()
    var lastCurrentGame: StudentMoodGame?
        get() = sp.getString("last_current_game", null)?.let { runCatching { gson.fromJson(it, StudentMoodGame::class.java) }.getOrNull() }
        set(v) = sp.edit().putString("last_current_game", v?.let { gson.toJson(it) }).apply()

    val isHappyChallengeAnsweredToday: Boolean
        get() = happyChallengeAnswerDate != 0L && isSameDay(Date(happyChallengeAnswerDate), Date())
    val userHasAddedMoodToday: Boolean
        get() = moodWizardLastPresented > 0 && isSameDay(Date(moodWizardLastPresented), Date())
    val userSkippedMoodAdd: Boolean
        get() = userSkippedForToday > 0 && isSameDay(Date(userSkippedForToday), Date())

    fun hasFilledForm(mood: Mood) = sp.getBoolean(if (mood == Mood.BAD) "has_filled_form_bad" else "has_filled_form_good", false)
    fun saveUserFilledFormByMood() {
        val mood = userSelectedMood ?: return
        sp.edit().putBoolean(if (mood == Mood.BAD) "has_filled_form_bad" else "has_filled_form_good", true).apply()
    }

    /** The mood was submitted today: remembers it and extends the "same mood in a row" streak (reset after 7 idle days). */
    fun recordMood(mood: Mood) {
        val now = System.currentTimeMillis()
        if (lastMoodTime > 0 && now - lastMoodTime > WEEK_MS) { sp.edit().remove("bad_mood_count").remove("good_mood_count").apply() }
        when (mood) {
            Mood.BAD -> sp.edit().remove("good_mood_count").putInt("bad_mood_count", moodStreak(Mood.BAD) + 1).apply()
            Mood.GREAT -> sp.edit().remove("bad_mood_count").putInt("good_mood_count", moodStreak(Mood.GREAT) + 1).apply()
            Mood.NORMAL -> sp.edit().remove("bad_mood_count").remove("good_mood_count").apply()
        }
        lastMoodTime = now
        userSelectedMood = mood
        moodWizardLastPresented = now
        happyChallengeAnswerDate = now
    }

    fun moodStreak(mood: Mood) = when (mood) {
        Mood.BAD -> sp.getInt("bad_mood_count", 0)
        Mood.GREAT -> sp.getInt("good_mood_count", 0)
        Mood.NORMAL -> 0
    }

    fun saveUserSkipTime() { userSkippedForToday = System.currentTimeMillis() }

    fun clearUserMoodGamePrefs() {
        sp.edit().remove("bad_mood_count").remove("good_mood_count").remove("mood_wizard_last_presented")
            .remove("user_selected_mood").remove("last_mood_time").remove("last_current_game").apply()
    }

    /** Clears everything that belongs to a signed-in user. */
    fun deleteUserPrefs() {
        clearUserMoodGamePrefs()
        sp.edit().remove("all_events_saved_to_calendar").remove("user_skipped_for_today").remove("happy_challenge_answer_date")
            .remove("has_filled_form_bad").remove("has_filled_form_good").apply()
    }

    fun getString(key: String, def: String? = null): String? = sp.getString(key, def)
    fun putString(key: String, value: String?) = sp.edit().putString(key, value).apply()
    fun getLong(key: String, def: Long = 0L) = sp.getLong(key, def)
    fun putLong(key: String, value: Long) = sp.edit().putLong(key, value).apply()
    fun getBoolean(key: String, def: Boolean = false) = sp.getBoolean(key, def)
    fun putBoolean(key: String, value: Boolean) = sp.edit().putBoolean(key, value).apply()
    fun remove(key: String) = sp.edit().remove(key).apply()

    companion object {
        private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

        fun isSameDay(a: Date, b: Date): Boolean {
            val c1 = Calendar.getInstance().apply { time = a }
            val c2 = Calendar.getInstance().apply { time = b }
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        }
    }
}
