package hu.unideb.studyversity.ui.happy_challenge

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import hu.unideb.studyversity.R

/** The three answers of the daily "how is your day" question. */
enum class Mood {
    GREAT, NORMAL, BAD;

    companion object {
        @JvmStatic
        fun getBackgroundByMood(context: Context, mood: String?): Drawable? = when (mood?.let { runCatching { valueOf(it) }.getOrNull() }) {
            GREAT -> ContextCompat.getDrawable(context, R.drawable.ic_hc_mood_great)
            NORMAL -> ContextCompat.getDrawable(context, R.drawable.ic_hc_mood_normal)
            BAD -> ContextCompat.getDrawable(context, R.drawable.ic_hc_mood_bad)
            null -> null
        }
    }
}

/** Which mood game screen should be presented next. */
enum class MoodScreen { NONE, REGISTRATION, ADD, LEVEL_UPGRADE, GAME_ENDED, FORM }
