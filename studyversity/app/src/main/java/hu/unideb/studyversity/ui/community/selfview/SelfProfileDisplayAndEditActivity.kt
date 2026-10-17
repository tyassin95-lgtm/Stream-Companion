package hu.unideb.studyversity.ui.community.selfview

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.AuthRequiredException
import hu.unideb.studyversity.data.model.CommunityMember
import hu.unideb.studyversity.databinding.ActivityEditSelfProfileBinding
import hu.unideb.studyversity.ui.base.BaseActivity
import hu.unideb.studyversity.ui.base.LoadingDialog
import hu.unideb.studyversity.ui.community.ChipSelectionType
import hu.unideb.studyversity.ui.community.wizard.AvatarPicker
import hu.unideb.studyversity.ui.community.wizard.ChipGroups
import hu.unideb.studyversity.ui.community.wizard.ChipSelectorActivity
import hu.unideb.studyversity.ui.community.wizard.WizardViewModel
import hu.unideb.studyversity.utils.ImageLoader
import hu.unideb.studyversity.utils.ImageUtils
import hu.unideb.studyversity.utils.UtilActivityOpener
import hu.unideb.studyversity.utils.dialog.CustomViewDialog
import kotlinx.coroutines.launch

/**
 * "My profile": the student's own community profile, read-only until the edit button is pressed.
 * Result [Activity.RESULT_FIRST_USER] means the profile was deleted.
 */
class SelfProfileDisplayAndEditActivity : BaseActivity() {
    private lateinit var binding: ActivityEditSelfProfileBinding
    private val vm: WizardViewModel by viewModels()
    private var editMode = false
    private var loading: LoadingDialog? = null
    private val backend get() = ServiceLocator.backend

    private val chipLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val data = r.data
        if (r.resultCode != Activity.RESULT_OK || data == null) return@registerForActivityResult
        val type = ChipSelectionType.valueOf(data.getStringExtra(ChipSelectorActivity.RESULT_TYPE) ?: return@registerForActivityResult)
        val ids = data.getLongArrayExtra(ChipSelectorActivity.RESULT_IDS)?.toSet().orEmpty()
        lifecycleScope.launch {
            runCatching { vm.categories(type).filter { it.id in ids } }
                .onSuccess { vm.setSelection(type, it); render() }
                .onFailure { showSnackbar(it.localizedMessage ?: getString(R.string.unknown_error)) }
        }
    }
    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { upload(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditSelfProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar(upEnabled = true)
        updateSubTitle(getString(R.string.my_profile))
        editMode = savedInstanceState?.getBoolean(STATE_EDIT) ?: false

        AvatarPicker.attach(binding.avatarHolderRv, vm.avatars) { url -> vm.setPhoto(url); renderPhoto() }
        binding.uploadImageLabel.setOnClickListener { photoPicker.launch("image/*") }
        binding.refreshImageView.setOnClickListener { photoPicker.launch("image/*") }
        binding.deleteAccountButton.setOnClickListener { confirmDelete() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (editMode) { editMode = false; invalidateOptionsMenu(); load() } else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })
        // after a rotation in edit mode the fields are empty until the profile arrives
        var refillInputs = editMode
        vm.member.observe(this) {
            if (it.id == null) return@observe
            if (refillInputs) { fillInputs(); refillInputs = false }
            render()
        }
        if (vm.member.value?.id == null) load()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_EDIT, editMode)
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val me = backend.myCommunityProfile()
                if (me == null) { setResult(Activity.RESULT_FIRST_USER); finish(); return@launch }
                vm.load(me)
            } catch (e: AuthRequiredException) {
                finish()
            } catch (e: Exception) {
                showSnackbar(e.localizedMessage ?: getString(R.string.unknown_error))
            }
        }
    }

    private fun render() {
        val m = vm.current
        renderPhoto()
        binding.nameText.text = m.fullName
        binding.modulesText.text = m.modules
        binding.descriptionText.text = m.description

        val openSelector = { type: ChipSelectionType ->
            chipLauncher.launch(ChipSelectorActivity.intent(this, type, vm.selection(type).map { it.id }))
        }
        fun add(type: ChipSelectionType): (() -> Unit)? = if (editMode) ({ openSelector(type) }) else null
        ChipGroups.fill(binding.interestsChipGroup, m.interests, add(ChipSelectionType.INTERESTS))
        ChipGroups.fill(binding.facultiesChipGroup, m.faculties, add(ChipSelectionType.FACULTY_NAMES))
        ChipGroups.fill(binding.languagesChipGroup, m.preferredLanguages, add(ChipSelectionType.LANGUAGES))

        fun vis(v: View, show: Boolean) { v.visibility = if (show) View.VISIBLE else View.GONE }
        vis(binding.refreshImageView, editMode)
        vis(binding.avatarPicker, editMode)
        vis(binding.descriptionLabel, !editMode)
        vis(binding.descriptionText, !editMode)
        vis(binding.descriptionInputLayout, editMode)
        vis(binding.socialButtonsRow, !editMode)
        vis(binding.socialInputs, editMode)
        vis(binding.deleteAccountButton, editMode)
        if (!editMode) {
            social(binding.facebookButton, m.facebookLink)
            social(binding.linkedInButton, m.linkedInLink)
            social(binding.instagramButton, m.instagramLink)
            social(binding.tiktokButton, m.tiktokLink)
        }
    }

    /** Copies the stored profile into the edit fields; called once when edit mode starts so chip changes don't overwrite typing. */
    private fun fillInputs() {
        val m = vm.current
        binding.searchTextInputEditText.setText(m.description)
        binding.facebookInput.setText(m.facebookLink)
        binding.linkedInInput.setText(m.linkedInLink)
        binding.instagramInput.setText(m.instagramLink)
        binding.tiktokInput.setText(m.tiktokLink)
    }

    private fun renderPhoto() {
        ImageLoader.load(binding.profileImage, vm.current.photoUrl)
        binding.profileImage.setOnClickListener {
            if (!editMode) vm.current.photoUrl?.takeIf { it.startsWith("http") }?.let { UtilActivityOpener.zoomImage(this, it) }
        }
    }

    private fun social(button: View, link: String?) {
        button.isEnabled = !link.isNullOrBlank()
        button.alpha = if (link.isNullOrBlank()) 0.3f else 1f
        button.setOnClickListener {
            if (link.isNullOrBlank()) return@setOnClickListener
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                .onFailure { showSnackbar(getString(R.string.cannot_open_intent_error_msg)) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(if (editMode) R.menu.menu_save_button else R.menu.menu_edit_button, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.edit -> { editMode = true; invalidateOptionsMenu(); fillInputs(); render(); true }
        R.id.done -> { save(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun save() {
        vm.setDescription(binding.searchTextInputEditText.text?.toString().orEmpty())
        vm.setLinks(
            binding.facebookInput.text.toString(), binding.linkedInInput.text.toString(),
            binding.instagramInput.text.toString(), binding.tiktokInput.text.toString()
        )
        val m = vm.current
        val error = when {
            m.photoUrl.isNullOrBlank() -> R.string.please_upload_an_image_error
            m.interests.isEmpty() || m.faculties.isEmpty() || m.preferredLanguages.isEmpty() -> R.string.interests_and_faculties_error_msg
            listOf(m.facebookLink, m.linkedInLink, m.instagramLink, m.tiktokLink).all { it.isNullOrBlank() } -> R.string.social_tab_error_msg
            else -> null
        }
        if (error != null) { showSnackbar(getString(error)); return }
        showLoading()
        vm.save(
            onDone = {
                hideLoading()
                editMode = false
                invalidateOptionsMenu()
                setResult(Activity.RESULT_OK)
                render()
            },
            onError = { hideLoading(); showSnackbar(it.localizedMessage ?: getString(R.string.unknown_error)) }
        )
    }

    private fun upload(uri: Uri) {
        val bytes = ImageUtils.readScaledJpeg(this, uri)
        if (bytes == null) { showSnackbar(getString(R.string.unknown_error)); return }
        showLoading()
        vm.uploadPhoto(bytes,
            onDone = { hideLoading(); renderPhoto() },
            onError = { hideLoading(); showSnackbar(it.localizedMessage ?: getString(R.string.unknown_error)) })
    }

    private fun confirmDelete() {
        CustomViewDialog(
            title = getString(R.string.account_delete_confirm_label), message = getString(R.string.account_delete_confirm_description),
            positiveText = getString(R.string.ok), negativeText = getString(R.string.cancel),
            mainIcon = R.drawable.ic_exclamation_circle_solid,
            onPositive = {
                showLoading()
                lifecycleScope.launch {
                    try {
                        backend.deleteCommunityProfile(vm.current.id ?: return@launch)
                        hideLoading()
                        setResult(Activity.RESULT_FIRST_USER)
                        finish()
                    } catch (e: Exception) {
                        hideLoading()
                        showSnackbar(e.localizedMessage ?: getString(R.string.unknown_error))
                    }
                }
            }
        ).show(supportFragmentManager, "delete")
    }

    private fun showLoading() { if (loading == null) loading = LoadingDialog(this).also { it.show() } }
    private fun hideLoading() { loading?.dismiss(); loading = null }

    override fun onDestroy() { hideLoading(); super.onDestroy() }

    companion object { private const val STATE_EDIT = "edit_mode" }
}
