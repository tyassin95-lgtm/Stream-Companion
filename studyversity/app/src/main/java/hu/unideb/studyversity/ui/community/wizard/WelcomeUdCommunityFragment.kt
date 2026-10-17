package hu.unideb.studyversity.ui.community.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.FragmentWelcomeUdCommunityBinding

class WelcomeUdCommunityFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentWelcomeUdCommunityBinding.inflate(inflater, container, false)
        binding.registrationButton.setOnClickListener { findNavController().navigate(R.id.action_welcomeUdCommunityFragment_to_wizardFragment) }
        return binding.root
    }
}
