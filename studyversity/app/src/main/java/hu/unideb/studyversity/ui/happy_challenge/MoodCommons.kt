package hu.unideb.studyversity.ui.happy_challenge

import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.AppPreferences
import hu.unideb.studyversity.data.model.StudentMoodGame
import java.util.Date

/**
 * Rules that decide which mood game screen comes next (ported 1:1 from the original MoodCommons):
 * game over (stage 6 without a following stage) → stage went up → questionnaire after ten identical moods.
 */
class MoodCommons(private val prefs: AppPreferences) {

    fun currentStoredStage(): Int = prefs.lastCurrentGame?.currentStage?.stageOrder ?: 1

    fun formUrlRes(): Int = when (prefs.userSelectedMood) {
        Mood.BAD -> R.string.happy_challenge_questionnaire_bad_mood_url
        Mood.GREAT -> R.string.happy_challenge_questionnaire_great_mood_url
        else -> R.string.error
    }

    fun processStudentMoodGame(game: StudentMoodGame): MoodScreen {
        val previous = prefs.lastCurrentGame
        if (game.currentStage?.stageOrder == GAME_END_STAGE && game.nextStage == null) {
            prefs.lastCurrentGame = game
            return MoodScreen.GAME_ENDED
        }
        val before = previous?.currentStage?.stageOrder ?: 1
        val now = game.currentStage?.stageOrder ?: 1
        prefs.lastCurrentGame = game
        return when {
            before < now -> MoodScreen.LEVEL_UPGRADE
            shouldPresentForm() -> MoodScreen.FORM
            else -> MoodScreen.NONE
        }
    }

    /** The questionnaire is offered once after ten identical moods in a row. */
    fun shouldPresentForm(): Boolean {
        val mood = prefs.userSelectedMood ?: return false
        if (mood == Mood.NORMAL || prefs.hasFilledForm(mood)) return false
        return prefs.moodStreak(mood) >= STREAK_FOR_FORM
    }

    fun shouldShowMoodAddView(): Boolean = !(prefs.userSkippedMoodAdd || userAddedMoodForToday())

    fun userAddedMoodForToday(): Boolean {
        if (prefs.userHasAddedMoodToday) return true
        val days = prefs.lastCurrentGame?.reportedDays ?: return false
        return days.any { AppPreferences.isSameDay(it, Date()) }
    }

    companion object {
        const val GAME_END_STAGE = 6
        const val STREAK_FOR_FORM = 10
    }
}
