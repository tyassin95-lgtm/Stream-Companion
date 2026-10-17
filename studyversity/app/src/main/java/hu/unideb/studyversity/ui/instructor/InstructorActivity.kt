package hu.unideb.studyversity.ui.instructor

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.databinding.ActivityInstructorBinding
import hu.unideb.studyversity.ui.base.BaseActivity
import kotlinx.coroutines.launch

/**
 * Console for instructors: edit the student's record, open exams and enter grades, manage the timetable and
 * send e-mails. Only reachable when an instructor is signed in.
 */
class InstructorActivity : BaseActivity() {
    private lateinit var binding: ActivityInstructorBinding
    val viewModel: InstructorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInstructorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar(upEnabled = true)
        updateTitle(getString(R.string.app_name))
        updateSubTitle(getString(R.string.menu_instructor_console))

        val pages = listOf<() -> Fragment>(
            { InstructorStudentFragment() }, { InstructorExamsFragment() }, { InstructorTimetableFragment() }, { InstructorMailFragment() }
        )
        val titles = intArrayOf(R.string.instructor_tab_student, R.string.instructor_tab_exams, R.string.instructor_tab_timetable, R.string.instructor_tab_email)
        binding.instructorPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = pages.size
            override fun createFragment(position: Int) = pages[position]()
        }
        // keep the tabs alive: the forms keep what was typed when the tab is left
        binding.instructorPager.offscreenPageLimit = pages.size
        TabLayoutMediator(binding.instructorTabs, binding.instructorPager) { tab, i -> tab.setText(titles[i]) }.attach()

        lifecycleScope.launch {
            val user = ServiceLocator.session.refresh()
            if (user?.isInstructor != true) { finish(); return@launch }
            viewModel.load { showSnackbar(it.localizedMessage ?: getString(R.string.unknown_error)) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_instructor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.instructor_logout) {
            lifecycleScope.launch { ServiceLocator.session.logout(); finish() }
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
