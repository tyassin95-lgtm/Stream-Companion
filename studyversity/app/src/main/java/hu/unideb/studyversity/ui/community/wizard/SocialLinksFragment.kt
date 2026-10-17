package hu.unideb.studyversity.ui.community.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import hu.unideb.studyversity.databinding.FragmentSocialLinksBinding

/** Page 3: links to the student's social profiles (at least one is required). */
class SocialLinksFragment : Fragment() {
    private var _binding: FragmentSocialLinksBinding? = null
    private val binding get() = _binding!!
    private val vm: WizardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSocialLinksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val m = vm.current
        binding.facebookInput.setText(m.facebookLink)
        binding.linkedInInput.setText(m.linkedInLink)
        binding.instagramInput.setText(m.instagramLink)
        binding.tiktokInput.setText(m.tiktokLink)
        val push = {
            vm.setLinks(
                binding.facebookInput.text.toString(), binding.linkedInInput.text.toString(),
                binding.instagramInput.text.toString(), binding.tiktokInput.text.toString()
            )
        }
        listOf(binding.facebookInput, binding.linkedInInput, binding.instagramInput, binding.tiktokInput)
            .forEach { it.doAfterTextChanged { push() } }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
