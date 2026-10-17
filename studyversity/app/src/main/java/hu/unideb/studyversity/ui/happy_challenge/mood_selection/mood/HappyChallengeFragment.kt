package hu.unideb.studyversity.ui.happy_challenge.mood_selection.mood

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.HappyChallengeFragmentBinding
import hu.unideb.studyversity.ui.happy_challenge.HappyChallengeActivity
import hu.unideb.studyversity.ui.happy_challenge.Mood
import hu.unideb.studyversity.ui.happy_challenge.mood_selection.choosePackage.ChoosePackageFragment
import hu.unideb.studyversity.utils.dialog.CustomViewDialog

/** "How is your day?" – great / normal / bad, or skip. */
class HappyChallengeFragment : Fragment() {
    private var _binding: HappyChallengeFragmentBinding? = null
    private val binding get() = _binding!!
    private val host get() = requireActivity() as HappyChallengeActivity
    private val vm get() = host.viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = HappyChallengeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.greetingText.text = vm.userName?.let { getString(R.string.hello_name, it) } ?: getString(R.string.hello)
        binding.greatCard.setOnClickListener { save(Mood.GREAT) }
        binding.normalCard.setOnClickListener { save(Mood.NORMAL) }
        binding.badCard.setOnClickListener { save(Mood.BAD) }
        binding.skipText.setOnClickListener { host.showSkipView() }
    }

    private fun save(mood: Mood) {
        vm.saveUserMood(mood,
            onDone = {
                if (vm.showPackageChoose) host.changeFragment(ChoosePackageFragment())
                else host.closeOk()
            },
            onAlreadyAnswered = { host.showAlreadyAnswered(it) },
            onError = { e -> _binding?.let { b -> Snackbar.make(b.root, e.localizedMessage ?: getString(R.string.error), Snackbar.LENGTH_LONG).setAction(R.string.retry) { save(mood) }.show() } }
        )
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
