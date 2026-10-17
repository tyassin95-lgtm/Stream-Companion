package hu.unideb.studyversity.ui.university_matters.university_locale

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.ActivityInstitutionListBinding
import hu.unideb.studyversity.ui.base.BaseActivity

/**
 * Hosts the institution graph (list → detail / map). The launching intent's extras
 * (fragmentTitle, categorieCodes, autoload, headerEnabled, hideBottomLayout, searchIsForSport)
 * become the arguments of the start destination.
 */
class InstitutionListActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityInstitutionListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar(upEnabled = true)
        if (savedInstanceState == null) {
            val host = supportFragmentManager.findFragmentById(R.id.inst_nav_host_fragment) as NavHostFragment
            host.navController.setGraph(R.navigation.institution_navigation_graph, intent.extras)
        }
    }
}
