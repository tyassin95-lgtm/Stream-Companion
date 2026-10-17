package hu.unideb.studyversity.ui.community

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.AuthRequiredException
import hu.unideb.studyversity.data.model.CommunityMember
import hu.unideb.studyversity.databinding.ActivityCommunityBinding
import hu.unideb.studyversity.ui.base.BaseActivity
import hu.unideb.studyversity.ui.community.selfview.SelfProfileDisplayAndEditActivity
import hu.unideb.studyversity.ui.community.wizard.WizardContainerActivity
import hu.unideb.studyversity.ui.user.login.LoginActivity
import hu.unideb.studyversity.utils.ImageLoader
import kotlinx.coroutines.launch

/** State of the community area: my profile, the list mode and the search text. */
class CommunityViewModel : androidx.lifecycle.ViewModel() {
    var selfProfile: CommunityMember? = null
    var mode = MemberListMode.MY_INTERESTS
    var search = ""
}

/**
 * UD Community: shows the member list. Students without a profile are sent through the registration wizard first.
 */
class CommunityActivity : BaseActivity() {
    private lateinit var binding: ActivityCommunityBinding
    val viewModel: CommunityViewModel by viewModels()

    private val wizardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) loadProfile() else finish()
    }
    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == LoginActivity.RESULT_LOGGED_IN) loadProfile() else finish()
    }
    private val profileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        // the profile was deleted: nothing left to show here
        if (r.resultCode == Activity.RESULT_FIRST_USER) finish() else loadProfile()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar(upEnabled = true)
        updateSubTitle(getString(R.string.ud_community_title))
        binding.profileButton.visibility = View.GONE
        binding.profileButton.setOnClickListener { profileLauncher.launch(Intent(this, SelfProfileDisplayAndEditActivity::class.java)) }
        loadProfile()
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val me = ServiceLocator.backend.myCommunityProfile()
                if (me == null) {
                    wizardLauncher.launch(Intent(this@CommunityActivity, WizardContainerActivity::class.java))
                    return@launch
                }
                viewModel.selfProfile = me
                binding.profileButton.visibility = View.VISIBLE
                ImageLoader.load(binding.profileImageView, me.photoUrl)
            } catch (e: AuthRequiredException) {
                loginLauncher.launch(Intent(this@CommunityActivity, LoginActivity::class.java))
            } catch (e: Exception) {
                showSnackbar(e.localizedMessage ?: getString(R.string.unknown_error))
            }
        }
    }
}
