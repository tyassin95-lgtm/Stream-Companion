package hu.unideb.studyversity.ui.happy_challenge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.HappyChallengeTodayAnsweredException
import hu.unideb.studyversity.data.model.MoodItem
import hu.unideb.studyversity.data.model.QuoteItem
import hu.unideb.studyversity.data.model.StudentMoodGame
import kotlinx.coroutines.launch
import java.util.Date

/** State of the "how is your day" sheet shared by its fragments. */
class HappyChallengeViewModel : ViewModel() {
    private val prefs get() = ServiceLocator.prefs
    private val backend get() = ServiceLocator.backend
    val commons = MoodCommons(ServiceLocator.prefs)

    var showPackageChoose = true
    private val _userMood = MutableLiveData<Mood>()
    val userMood: LiveData<Mood> get() = _userMood
    val userName: String? get() = ServiceLocator.session.currentUser?.name

    /** Stores today's mood. [onError] gets the message of an "already answered today" rejection separately. */
    fun saveUserMood(mood: Mood, onDone: () -> Unit, onAlreadyAnswered: (String) -> Unit, onError: (Throwable) -> Unit) {
        _userMood.value = mood
        viewModelScope.launch {
            try {
                backend.saveMood(MoodItem(moodDate = Date(), moodValue = mood.name))
                prefs.recordMood(mood)
                onDone()
            } catch (e: HappyChallengeTodayAnsweredException) {
                prefs.recordMood(mood)
                onAlreadyAnswered(e.message.orEmpty())
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun randomQuote(onResult: (QuoteItem) -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try { backend.randomQuote()?.let(onResult) } catch (e: Exception) { onError(e) }
        }
    }

    /** After a package was opened: ask the game for the new state and tell which screen is next. */
    fun resolveNextScreen(onResult: (MoodScreen) -> Unit, onError: (Throwable) -> Unit) {
        viewModelScope.launch {
            try {
                val game: StudentMoodGame = backend.myCurrentGame()
                if (prefs.lastCurrentGame == null) prefs.lastCurrentGame = game
                onResult(commons.processStudentMoodGame(game))
            } catch (e: Exception) { onError(e) }
        }
    }

    fun skipChallenge() = prefs.saveUserSkipTime()
}
