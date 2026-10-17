package hu.unideb.studyversity.ui.happy_challenge.skip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import hu.unideb.studyversity.databinding.HappyChallengeSkipFragmentBinding
import hu.unideb.studyversity.ui.happy_challenge.HappyChallengeActivity

/** "Are you sure you want to skip it?" – skipping hides the sheet for the rest of the day. */
class HappyChallengeSkipFragment : Fragment() {
    private var _binding: HappyChallengeSkipFragmentBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = HappyChallengeSkipFragmentBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val host = requireActivity() as HappyChallengeActivity
        _binding!!.skipButton.setOnClickListener { host.viewModel.skipChallenge(); host.closeOk() }
        _binding!!.continueButton.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
