package hu.unideb.studyversity.ui.community.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import hu.unideb.studyversity.databinding.FragmentInterestsBinding
import hu.unideb.studyversity.ui.community.ChipSelectionType

/** Page 2: interests, faculties and languages; each group opens the chip selector. */
class InterestsFragment : Fragment() {
    private var _binding: FragmentInterestsBinding? = null
    private val binding get() = _binding!!
    private val vm: WizardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInterestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.member.observe(viewLifecycleOwner) { m ->
            val host = requireActivity() as WizardContainerActivity
            ChipGroups.fill(binding.interestsChipGroup, m.interests) { host.openChipSelector(ChipSelectionType.INTERESTS) }
            ChipGroups.fill(binding.facultiesChipGroup, m.faculties) { host.openChipSelector(ChipSelectionType.FACULTY_NAMES) }
            ChipGroups.fill(binding.languagesChipGroup, m.preferredLanguages) { host.openChipSelector(ChipSelectionType.LANGUAGES) }
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
