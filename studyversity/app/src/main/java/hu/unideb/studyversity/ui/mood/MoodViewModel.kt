package hu.unideb.studyversity.ui.mood

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.NoMoodGameException
import hu.unideb.studyversity.data.model.CompletedMoodGameStage
import hu.unideb.studyversity.data.model.MoodGameGift
import hu.unideb.studyversity.data.model.MoodGameStage
import hu.unideb.studyversity.data.model.StudentMoodGame
import hu.unideb.studyversity.ui.happy_challenge.MoodCommons
import kotlinx.coroutines.launch

/** What the statistics tab shows about the running game. */
data class MoodData(
    val currentStage: Int?, val levelLabel: String?, val nextLevelLabel: String?,
    val progress: Int?, val currentLevelMaxPoints: Int?
)

/** State shared by the three tabs of the mood screen (statistics, rewards, history). */
class MoodViewModel : ViewModel() {
    private val backend get() = ServiceLocator.backend
    val prefs get() = ServiceLocator.prefs
    val commons = MoodCommons(ServiceLocator.prefs)

    private val _moodData = MutableLiveData<MoodData>()
    val moodData: LiveData<MoodData> get() = _moodData
    private val _progressText = MutableLiveData<String>()
    val progressText: LiveData<String> get() = _progressText
    private val _stages = MutableLiveData<List<MoodGameStage>>()
    val stages: LiveData<List<MoodGameStage>> get() = _stages
    private val _completed = MutableLiveData<List<CompletedMoodGameStage>>(emptyList())
    val completed: LiveData<List<CompletedMoodGameStage>> get() = _completed
    val completedStages: List<CompletedMoodGameStage> get() = _completed.value.orEmpty()

    /** Loads the game; [onGame] gets the raw state, [onNoGame] fires when the student has not joined yet. */
    fun loadGameData(onGame: (StudentMoodGame) -> Unit = {}, onNoGame: () -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val game = backend.myCurrentGame()
                process(game)
                onGame(game)
            } catch (e: NoMoodGameException) {
                prefs.clearUserMoodGamePrefs()
                onNoGame()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun listStages() {
        viewModelScope.launch { runCatching { backend.moodGameStages() }.onSuccess { _stages.value = it.sortedBy { s -> s.stageOrder } } }
    }

    /** Level label, progress "x/y" and the numbers for the progress bar (same arithmetic as the original). */
    private fun process(vo: StudentMoodGame) {
        val ctx = ServiceLocator.appContext
        val current = vo.currentStage
        val reported = vo.reportedDays.size
        var done = reported
        var total = current?.days ?: 0
        if (current?.stageOrder == 1) {
            total = vo.nextStage?.days ?: 0
        } else {
            vo.completedStages.forEach { done += it.days ?: 0; total += it.days ?: 0 }
        }
        _progressText.value = "$done/$total"
        _completed.value = vo.completedStages
        _moodData.value = MoodData(
            currentStage = current?.stageOrder,
            levelLabel = ctx.getString(R.string.current_level, current?.stageOrder?.toString()),
            nextLevelLabel = vo.nextStage?.let { ctx.getString(R.string.current_level, (it.stageOrder ?: 0).toString()) },
            progress = reported,
            currentLevelMaxPoints = current?.days ?: 100
        )
    }

    fun chosenGiftByLevel(level: Int): MoodGameGift? = completedStages.firstOrNull { (it.stageOrder ?: 0) == level }?.chosenGift

    fun receivedGiftText(ctx: Context, level: Int): String =
        ctx.getString(if (chosenGiftByLevel(level) == null) R.string.not_taken_over else R.string.received_gift)

    fun chooseGift(stageId: Int, giftId: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { backend.chooseGift(stageId, giftId) }
            loadGameData(onGame = { onDone() })
        }
    }

    fun showAddMoodButton(): Boolean =
        prefs.happyChallengeEnabled && ServiceLocator.session.currentUser != null && !commons.userAddedMoodForToday()

    fun levelImage(ctx: Context, level: Int? = _moodData.value?.currentStage): Drawable? = ContextCompat.getDrawable(ctx, when (level) {
        1 -> R.drawable.mood_level_1
        2 -> R.drawable.mood_level_2
        3 -> R.drawable.mood_level_3
        4 -> R.drawable.mood_level_4
        5 -> R.drawable.mood_level_5
        6 -> R.drawable.mood_level_6
        else -> R.drawable.mood_level_1
    })
}
