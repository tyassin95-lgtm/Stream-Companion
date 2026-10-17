package hu.unideb.studyversity.ui.happy_challenge

import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.NoMoodGameException
import hu.unideb.studyversity.ui.mood.form.ActivityMoodForm
import hu.unideb.studyversity.ui.mood.gameEnded.ActivityMoodGameEnded
import hu.unideb.studyversity.ui.mood.levelUpgrade.ActivityMoodLevelUpgrade
import hu.unideb.studyversity.utils.dialog.CustomViewDialog
import kotlinx.coroutines.launch

/**
 * Opens the mood game screens in the same order as the original home screen / statistics tab did:
 * registration → "how is your day" sheet → level up / game ended / questionnaire.
 * Create it as a property of the fragment (it registers activity result launchers).
 */
class MoodFlow(private val fragment: Fragment, private val onAfterFlow: () -> Unit = {}) {
    private val prefs get() = ServiceLocator.prefs
    private val commons = MoodCommons(ServiceLocator.prefs)
    private var addAlreadyHandled = false

    private val addLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        addAlreadyHandled = true
        if (r.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        when (r.data?.getSerializableExtra(EXTRA_SCREEN) as? MoodScreen) {
            MoodScreen.LEVEL_UPGRADE -> openLevelUpgrade()
            MoodScreen.GAME_ENDED -> openGameEnded()
            MoodScreen.FORM -> openForm()
            else -> onAfterFlow()
        }
    }
    private val levelLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        // "Collect your reward" returns navigateToRewardScreen = true
        if (r.resultCode == Activity.RESULT_OK && r.data?.getBooleanExtra("navigateToRewardScreen", false) == true) {
            fragment.startActivity(Intent(fragment.requireContext(), hu.unideb.studyversity.ui.mood.ActivityMoodHolder::class.java).putExtra("moodTabNumber", 1))
        }
        onAfterFlow()
    }

    /** Called when the home screen / statistics tab becomes visible for a signed in student. */
    fun presentIfNeeded() {
        if (!prefs.happyChallengeEnabled) return
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            try {
                val game = ServiceLocator.backend.myCurrentGame()
                prefs.lastCurrentGame = game
                if (commons.shouldShowMoodAddView()) { if (!addAlreadyHandled) openAdd() }
                else if (commons.shouldPresentForm()) openForm()
            } catch (e: NoMoodGameException) {
                prefs.clearUserMoodGamePrefs()
                if (!addAlreadyHandled) openRegistration()
            } catch (e: Exception) {
                // offline / not signed in: nothing to present
            }
        }
    }

    fun resetHandled() { addAlreadyHandled = false }

    fun openAdd() {
        addLauncher.launch(Intent(fragment.requireContext(), HappyChallengeActivity::class.java)
            .putExtra("showPackageChoose", true).putExtra("enableToQuitWithoutAsking", true))
        fragment.requireActivity().overridePendingTransition(R.anim.slide_up, R.anim.fake_anim)
    }

    /** Replaces the original web registration page: the student joins the game here. */
    fun openRegistration() {
        val ctx = fragment.requireContext()
        CustomViewDialog(
            title = ctx.getString(R.string.menu_happy_challenge),
            message = ctx.getString(R.string.mood_register_message),
            positiveText = ctx.getString(R.string.mood_register_join),
            negativeText = ctx.getString(R.string.mood_register_later),
            mainIcon = R.drawable.ic_happy_challenge,
            onPositive = {
                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { ServiceLocator.backend.registerMoodGame() }
                    prefs.registeredToMoodGame = true
                    openAdd()
                }
            }
        ).show(fragment.parentFragmentManager, "mood_register")
    }

    fun openForm() {
        fragment.startActivity(Intent(fragment.requireContext(), ActivityMoodForm::class.java))
        fragment.requireActivity().overridePendingTransition(R.anim.slide_up, R.anim.fake_anim)
    }

    fun openGameEnded() {
        fragment.startActivity(Intent(fragment.requireContext(), ActivityMoodGameEnded::class.java))
        fragment.requireActivity().overridePendingTransition(R.anim.slide_up, R.anim.fake_anim)
    }

    fun openLevelUpgrade() {
        levelLauncher.launch(Intent(fragment.requireContext(), ActivityMoodLevelUpgrade::class.java))
        fragment.requireActivity().overridePendingTransition(R.anim.slide_up, R.anim.fake_anim)
    }

    companion object { const val EXTRA_SCREEN = "moodScreen" }
}
