package hu.unideb.studyversity.ui.mood.gameEnded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.model.StudentMoodGame
import hu.unideb.studyversity.databinding.FragmentMoodGameEndedBinding

/** End of the 365 day game: winner screen, or a consolation when the target was missed. */
class MoodGameEndedFragment : Fragment() {
    private var _binding: FragmentMoodGameEndedBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoodGameEndedBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val win = gameEndedWithVictory(ServiceLocator.prefs.lastCurrentGame)
        val b = _binding!!
        b.gameEndedImage.setImageResource(if (win) R.drawable.mood_game_ended_win else R.drawable.mood_game_ended_no_victory)
        b.gameEndedTitle.setText(if (win) R.string.mood_game_ended_success else R.string.mood_game_ended_failed)
    }

    /** Victory = the last stage the game reached was completed, no failed stage matches it. */
    private fun gameEndedWithVictory(game: StudentMoodGame?): Boolean {
        game ?: return false
        val last = game.currentStage?.stageOrder ?: return false
        return game.completedStages.any { it.stageOrder == last - 1 || it.stageOrder == last } &&
            game.failedStages.none { it.stageOrder == last }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
