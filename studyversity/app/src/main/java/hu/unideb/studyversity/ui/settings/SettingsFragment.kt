package hu.unideb.studyversity.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.DevicePreference
import hu.unideb.studyversity.databinding.SettingsFragmentBinding
import hu.unideb.studyversity.ui.base.BaseFragment

/** App settings: happy challenge, mobile data and the three push notification groups. */
class SettingsFragment : BaseFragment() {
    private var _binding: SettingsFragmentBinding? = null
    private val binding get() = _binding!!
    private var updating = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SettingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.menu_settings)
        val info = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        @Suppress("DEPRECATION")
        binding.versionText.text = "v${info.versionName} (${info.versionCode})"

        showPrefs()
        binding.happyChallangeSwitch.setOnCheckedChangeListener { _, on -> prefs.happyChallengeEnabled = on }
        binding.mobileNetworkSwitch.setOnCheckedChangeListener { _, on -> prefs.putBoolean(KEY_ANY_NETWORK, on) }
        binding.pushAll.setOnCheckedChangeListener(onPushChanged)
        binding.pushStudentInformation.setOnCheckedChangeListener(onPushChanged)
        binding.pushPromotion.setOnCheckedChangeListener(onPushChanged)
        binding.pushOther.setOnCheckedChangeListener(onPushChanged)
    }

    private fun showPrefs() {
        updating = true
        binding.happyChallangeSwitch.isChecked = prefs.happyChallengeEnabled
        binding.mobileNetworkSwitch.isChecked = prefs.getBoolean(KEY_ANY_NETWORK, true)
        binding.pushStudentInformation.isChecked = prefs.pushStudentInformation
        binding.pushPromotion.isChecked = prefs.pushPromotion
        binding.pushOther.isChecked = prefs.pushOther
        binding.pushAll.isChecked = prefs.pushStudentInformation && prefs.pushPromotion && prefs.pushOther
        updating = false
    }

    /** "Everything" switches all groups; a single group only updates itself and the "Everything" state. */
    private val onPushChanged = CompoundButton.OnCheckedChangeListener { button, checked ->
        if (updating) return@OnCheckedChangeListener
        when (button.id) {
            R.id.pushAll -> { prefs.pushStudentInformation = checked; prefs.pushPromotion = checked; prefs.pushOther = checked }
            R.id.pushStudentInformation -> prefs.pushStudentInformation = checked
            R.id.pushPromotion -> prefs.pushPromotion = checked
            R.id.pushOther -> prefs.pushOther = checked
        }
        showPrefs()
        load(showProgress = false) {
            backend.saveDevicePreferences(DevicePreference(deviceId = prefs.deviceToken, promotion = prefs.pushPromotion))
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    private companion object { const val KEY_ANY_NETWORK = "any_network" }
}
