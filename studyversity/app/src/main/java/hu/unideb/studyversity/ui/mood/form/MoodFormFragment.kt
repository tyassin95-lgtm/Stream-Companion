package hu.unideb.studyversity.ui.mood.form

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.databinding.FragmentMoodFormBinding
import hu.unideb.studyversity.ui.happy_challenge.MoodCommons

/** Offers the wellbeing questionnaire after ten equal moods in a row. */
class MoodFormFragment : Fragment() {
    private var _binding: FragmentMoodFormBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoodFormBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = ServiceLocator.prefs
        _binding!!.formButton.setOnClickListener {
            prefs.saveUserFilledFormByMood()
            var url = getString(MoodCommons(prefs).formUrlRes())
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            requireActivity().finish()
        }
        // Skipping also marks the form as handled so it is not offered again for the same streak.
        _binding!!.skipText.setOnClickListener { prefs.saveUserFilledFormByMood(); requireActivity().finish() }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
