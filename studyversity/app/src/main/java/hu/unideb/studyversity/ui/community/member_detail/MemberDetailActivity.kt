package hu.unideb.studyversity.ui.community.member_detail

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.model.CategoryItem
import hu.unideb.studyversity.data.model.CommunityMember
import hu.unideb.studyversity.databinding.ActivityMemberDetailBinding
import hu.unideb.studyversity.ui.base.BaseActivity
import hu.unideb.studyversity.utils.ImageLoader
import hu.unideb.studyversity.utils.UtilActivityOpener
import hu.unideb.studyversity.utils.dialog.CustomViewDialog
import kotlinx.coroutines.launch

/** Another student's community profile with social buttons and the "I'm interested" toggle. Extra: `memberId`. */
class MemberDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityMemberDetailBinding
    private var member: CommunityMember? = null
    private var interested = false
    private val backend get() = ServiceLocator.backend

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar(upEnabled = true)
        updateSubTitle(getString(R.string.ud_community_title))
        val id = intent.getLongExtra("memberId", -1L)
        if (id == -1L) { finish(); return }
        lifecycleScope.launch {
            try {
                val m = backend.communityMember(id) ?: run { finish(); return@launch }
                interested = backend.searchMyInterests().any { it.id == id }
                show(m)
            } catch (e: Exception) { showSnackbar(e.localizedMessage ?: getString(R.string.unknown_error)) }
        }
    }

    private fun show(m: CommunityMember) {
        member = m
        ImageLoader.load(binding.profileImage, m.photoUrl)
        binding.profileImage.setOnClickListener { m.photoUrl?.takeIf { it.startsWith("http") }?.let { UtilActivityOpener.zoomImage(this, it) } }
        binding.nameText.text = m.fullName
        binding.modulesText.text = m.modules
        binding.descriptionText.text = m.description
        fillChips(binding.interestsChipGroup, m.interests)
        fillChips(binding.facultiesChipGroup, m.faculties)
        fillChips(binding.languagesChipGroup, m.preferredLanguages, allSignificant = true)
        social(binding.facebookButton, m.facebookLink)
        social(binding.linkedInButton, m.linkedInLink)
        social(binding.instagramButton, m.instagramLink)
        social(binding.tiktokButton, m.tiktokLink)
        updateButton()
        binding.markAsFriendButton.setOnClickListener { if (interested) remove() else confirmMark() }
    }

    private fun fillChips(group: FlexboxLayout, items: List<CategoryItem>, allSignificant: Boolean = false) {
        group.removeAllViews()
        items.forEach { c ->
            val chip = LayoutInflater.from(this).inflate(
                if (c.isSignificant || allSignificant) R.layout.chip_significant_item else R.layout.chip_item, group, false) as Chip
            chip.text = c.name
            group.addView(chip)
        }
    }

    private fun social(button: android.view.View, link: String?) {
        button.visibility = if (link.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
        button.setOnClickListener {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
                .onFailure { showSnackbar(getString(R.string.cannot_open_intent_error_msg)) }
        }
    }

    private fun updateButton() {
        val b = binding.markAsFriendButton
        if (interested) {
            b.text = getString(R.string.remove_from_my_interest_button)
            b.setTextColor(ContextCompat.getColorStateList(this, R.color.white))
            b.backgroundTintList = ContextCompat.getColorStateList(this, R.color.orangeRed)
            b.icon = ContextCompat.getDrawable(this, R.drawable.ic_close)
            b.iconTint = ContextCompat.getColorStateList(this, R.color.white)
        } else {
            b.text = getString(R.string.mark_as_my_interest_button)
            b.setTextColor(ContextCompat.getColorStateList(this, R.color.black))
            b.backgroundTintList = ContextCompat.getColorStateList(this, R.color.yellowishOrange)
            b.icon = ContextCompat.getDrawable(this, R.drawable.ic_handshake)
            b.iconTint = ContextCompat.getColorStateList(this, R.color.black)
        }
    }

    private fun confirmMark() {
        CustomViewDialog(
            title = getString(R.string.app_name), message = getString(R.string.mark_as_friend_dialog_text),
            positiveText = getString(R.string.ok), negativeText = getString(R.string.cancel), mainIcon = R.drawable.ic_handshake,
            onPositive = {
                lifecycleScope.launch {
                    runCatching { backend.addToMyInterests(member?.id ?: return@launch) }
                        .onSuccess { interested = true; updateButton(); setResult(Activity.RESULT_OK) }
                        .onFailure { showSnackbar(it.localizedMessage ?: getString(R.string.unknown_error)) }
                }
            }
        ).show(supportFragmentManager, "mark")
    }

    private fun remove() {
        lifecycleScope.launch {
            runCatching { backend.removeFromMyInterests(member?.id ?: return@launch) }
                .onSuccess { setResult(Activity.RESULT_OK); finish() }
                .onFailure { showSnackbar(it.localizedMessage ?: getString(R.string.unknown_error)) }
        }
    }
}
