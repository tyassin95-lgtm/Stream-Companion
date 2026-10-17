package hu.unideb.studyversity.ui.mood.levelUpgrade

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.databinding.FragmentMoodLevelUpgradeBinding
import hu.unideb.studyversity.ui.happy_challenge.MoodCommons

/** "You reached a new level!" with a shortcut to the reward list. */
class MoodLevelUpgradeFragment : Fragment() {
    private var _binding: FragmentMoodLevelUpgradeBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoodLevelUpgradeBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = _binding!!
        val completed = MoodCommons(ServiceLocator.prefs).currentStoredStage() - 1
        b.levelUpgradeDesc.text = getString(R.string.mood_level_upgrade_description, completed.toString())
        b.rewardButton.setOnClickListener {
            requireActivity().setResult(Activity.RESULT_OK, Intent().putExtra("navigateToRewardScreen", true))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
