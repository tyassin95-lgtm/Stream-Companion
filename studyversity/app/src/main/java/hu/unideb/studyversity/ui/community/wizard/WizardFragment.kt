package hu.unideb.studyversity.ui.community.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.AuthRequiredException
import hu.unideb.studyversity.databinding.FragmentWizardBinding
import hu.unideb.studyversity.ui.base.LoadingDialog

/** The four profile pages in a pager. Each page is validated before the next one opens; the last one registers the profile. */
class WizardFragment : Fragment() {
    private var _binding: FragmentWizardBinding? = null
    private val binding get() = _binding!!
    private val vm: WizardViewModel by activityViewModels()
    private var loading: LoadingDialog? = null

    private val pageCount = 4

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWizardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.fragmentPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = pageCount
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> ProfilePictureSelectionFragment()
                1 -> InterestsFragment()
                2 -> SocialLinksFragment()
                else -> GDPRFragment()
            }
        }
        // swiping would skip the validation
        binding.fragmentPager.isUserInputEnabled = false
        TabLayoutMediator(binding.tabDots, binding.fragmentPager) { _, _ -> }.attach()
        binding.tabDots.touchables.forEach { it.isEnabled = false }

        binding.prevButton.setOnClickListener {
            val page = binding.fragmentPager.currentItem
            if (page > 0) binding.fragmentPager.currentItem = page - 1 else findNavController().popBackStack()
        }
        binding.nextButton.setOnClickListener { next() }
    }

    private fun next() {
        val page = binding.fragmentPager.currentItem
        val error = validate(page)
        if (error != null) {
            (requireActivity() as WizardContainerActivity).showAlertDialog(getString(error))
            return
        }
        if (page < pageCount - 1) binding.fragmentPager.currentItem = page + 1 else register()
    }

    /** @return the error message for the page, or null when the page is complete. */
    private fun validate(page: Int): Int? {
        val m = vm.current
        return when (page) {
            0 -> if (m.photoUrl.isNullOrBlank()) R.string.please_upload_an_image_error else null
            1 -> if (m.interests.isEmpty() || m.faculties.isEmpty() || m.preferredLanguages.isEmpty()) R.string.interests_and_faculties_error_msg else null
            2 -> if (listOf(m.facebookLink, m.linkedInLink, m.instagramLink, m.tiktokLink).all { it.isNullOrBlank() }) R.string.social_tab_error_msg else null
            else -> if (vm.privacyAccepted && vm.termsAccepted) null else R.string.gdpr_tab_accept_error_msg
        }
    }

    private fun register() {
        loading = LoadingDialog(requireContext()).also { it.show() }
        vm.register(
            onDone = {
                loading?.dismiss()
                if (isAdded) findNavController().navigate(R.id.action_wizardFragment_to_wizardFinishFragment)
            },
            onError = { e ->
                loading?.dismiss()
                if (isAdded) {
                    val msg = if (e is AuthRequiredException) getString(R.string.login_was_not_success) else e.localizedMessage ?: getString(R.string.unknown_error)
                    (requireActivity() as WizardContainerActivity).showAlertDialog(msg)
                }
            }
        )
    }

    override fun onDestroyView() {
        loading?.dismiss()
        _binding = null
        super.onDestroyView()
    }
}
