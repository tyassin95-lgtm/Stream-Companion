package hu.unideb.studyversity.ui.happy_challenge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.activity.viewModels
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.HappyChallengeActivityBinding
import hu.unideb.studyversity.ui.happy_challenge.mood_selection.mood.HappyChallengeFragment
import hu.unideb.studyversity.ui.happy_challenge.skip.HappyChallengeSkipFragment
import hu.unideb.studyversity.utils.dialog.CustomViewDialog

/**
 * Full screen sheet that asks "How is your day?" and then lets the student open a surprise package.
 * Extras: `showPackageChoose`, `enableToQuitWithoutAsking`. Returns RESULT_OK + `moodScreen` (next screen).
 */
class HappyChallengeActivity : AppCompatActivity() {
    val viewModel: HappyChallengeViewModel by viewModels()
    private val skipFragment = HappyChallengeSkipFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = HappyChallengeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.showPackageChoose = intent.getBooleanExtra("showPackageChoose", true)
        if (savedInstanceState == null) changeFragment(HappyChallengeFragment(), addToBackStack = false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    intent.getBooleanExtra("enableToQuitWithoutAsking", false) -> finish()
                    skipFragment.isVisible -> { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
                    else -> changeFragment(skipFragment)
                }
            }
        })
    }

    fun changeFragment(f: Fragment, addToBackStack: Boolean = true) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, f)
            .apply { if (addToBackStack) addToBackStack(null) }
            .commit()
    }

    fun showSkipView() = changeFragment(skipFragment)

    /** Today's mood was already reported (e.g. from another device): tell the student and leave. */
    fun showAlreadyAnswered(message: String) {
        CustomViewDialog(
            title = getString(R.string.app_name), message = message.ifEmpty { getString(R.string.error) },
            positiveText = getString(R.string.ok), mainIcon = R.drawable.ic_exclamation_circle_solid,
            onPositive = { setResult(Activity.RESULT_CANCELED); finish() }
        ).show(supportFragmentManager, "answered")
    }

    fun closeWith(screen: MoodScreen) {
        setResult(Activity.RESULT_OK, Intent().putExtra(MoodFlow.EXTRA_SCREEN, screen))
        finish()
    }

    /** Mood saved, package choosing switched off, or skipped: close and report "nothing more". */
    fun closeOk() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}
