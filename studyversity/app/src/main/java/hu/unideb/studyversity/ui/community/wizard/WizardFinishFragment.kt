package hu.unideb.studyversity.ui.community.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import hu.unideb.studyversity.databinding.FragmentWizardFinishBinding

class WizardFinishFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentWizardFinishBinding.inflate(inflater, container, false)
        val host = requireActivity() as WizardContainerActivity
        binding.finishRegistration.setOnClickListener { host.finishRegistered() }
        // the profile exists already: going back must not return to the pages
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = host.finishRegistered()
        })
        return binding.root
    }
}
