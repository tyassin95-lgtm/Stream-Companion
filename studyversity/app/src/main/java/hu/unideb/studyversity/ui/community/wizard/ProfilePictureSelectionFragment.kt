package hu.unideb.studyversity.ui.community.wizard

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.FragmentProfilePictureSelectionBinding
import hu.unideb.studyversity.ui.base.LoadingDialog
import hu.unideb.studyversity.utils.ImageLoader
import hu.unideb.studyversity.utils.ImageUtils

/** Page 1: pick one of the default avatars or a photo from the gallery, plus a short description. */
class ProfilePictureSelectionFragment : Fragment() {
    private var _binding: FragmentProfilePictureSelectionBinding? = null
    private val binding get() = _binding!!
    private val vm: WizardViewModel by activityViewModels()
    private var loading: LoadingDialog? = null

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { upload(it) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfilePictureSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ImageLoader.load(binding.profileImage, vm.current.photoUrl)
        AvatarPicker.attach(binding.avatarHolderRv, vm.avatars) { url ->
            vm.setPhoto(url)
            ImageLoader.load(binding.profileImage, url)
        }
        binding.uploadImageLabel.setOnClickListener { picker.launch("image/*") }
        binding.profileImage.setOnClickListener { picker.launch("image/*") }
        binding.searchTextInputEditText.setText(vm.current.description)
        binding.searchTextInputEditText.doAfterTextChanged { vm.setDescription(it?.toString().orEmpty()) }
    }

    private fun upload(uri: Uri) {
        val bytes = ImageUtils.readScaledJpeg(requireContext(), uri)
        if (bytes == null) {
            (requireActivity() as WizardContainerActivity).showAlertDialog(getString(R.string.unknown_error))
            return
        }
        loading = LoadingDialog(requireContext()).also { it.show() }
        vm.uploadPhoto(bytes,
            onDone = { url -> loading?.dismiss(); _binding?.let { ImageLoader.load(it.profileImage, url) } },
            onError = { e ->
                loading?.dismiss()
                (activity as? WizardContainerActivity)?.showAlertDialog(e.localizedMessage ?: getString(R.string.unknown_error))
            })
    }

    override fun onDestroyView() { loading?.dismiss(); _binding = null; super.onDestroyView() }
}
