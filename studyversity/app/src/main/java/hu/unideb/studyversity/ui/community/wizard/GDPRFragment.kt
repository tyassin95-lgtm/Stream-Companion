package hu.unideb.studyversity.ui.community.wizard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.FragmentGdprBinding
import hu.unideb.studyversity.utils.ConnectivityUtil

/** Page 4: privacy policy and terms of use, both must be accepted. */
class GDPRFragment : Fragment() {
    private var _binding: FragmentGdprBinding? = null
    private val binding get() = _binding!!
    private val vm: WizardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGdprBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.privacyPolicySwitch.isChecked = vm.privacyAccepted
        binding.termsOfUseSwitch.isChecked = vm.termsAccepted
        binding.privacyPolicySwitch.setOnCheckedChangeListener { _, on -> vm.privacyAccepted = on }
        binding.termsOfUseSwitch.setOnCheckedChangeListener { _, on -> vm.termsAccepted = on }

        binding.webView.setBackgroundColor(0)
        binding.webView.settings.javaScriptEnabled = true
        if (ConnectivityUtil.isNetworkAvailable(requireContext())) {
            binding.webView.loadUrl(getString(R.string.privacy_policy_url))
        } else {
            binding.webView.visibility = View.GONE
        }
    }

    override fun onDestroyView() { binding.webView.destroy(); _binding = null; super.onDestroyView() }
}
