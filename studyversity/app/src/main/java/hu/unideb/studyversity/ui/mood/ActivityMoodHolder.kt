package hu.unideb.studyversity.ui.mood

import android.os.Bundle
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.ActivityMoodHolderBinding
import hu.unideb.studyversity.ui.base.BaseActivity

/** The "My mood" area (statistics / rewards / history tabs) in its own activity with the mood navigation graph. */
class ActivityMoodHolder : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMoodHolderBinding.inflate(layoutInflater).root)
        initToolbar(upEnabled = true)
    }

    override fun onResume() {
        super.onResume()
        updateSubTitle(getString(R.string.menu_my_mood))
    }
}
