package hu.unideb.studyversity.ui.happy_challenge.mood_selection.questionnaire

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import hu.unideb.studyversity.databinding.QuestionnaireFragmentBinding
import hu.unideb.studyversity.ui.happy_challenge.HappyChallengeActivity

/** Optional questionnaire step of the sheet (`QuestionnaireUrl` argument); the original keeps it disabled. */
class QuestionnaireFragment : Fragment() {
    private var _binding: QuestionnaireFragmentBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = QuestionnaireFragmentBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val host = requireActivity() as HappyChallengeActivity
        _binding!!.fillButton.setOnClickListener {
            arguments?.getString("QuestionnaireUrl")?.let { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
            host.closeOk()
        }
        _binding!!.skipTextView.setOnClickListener { host.closeOk() }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
