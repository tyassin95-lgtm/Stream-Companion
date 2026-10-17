package hu.unideb.studyversity.ui.studyversity_profile.courses_and_exams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.StudentCourseItem
import hu.unideb.studyversity.databinding.CoursesInformationFragmentBinding
import hu.unideb.studyversity.databinding.ListCustomTextEmptyLayoutBinding
import hu.unideb.studyversity.databinding.StudentCoursesListItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.InfoRows
import hu.unideb.studyversity.ui.base.ListItemAdapter

/** Courses the student has taken, filtered by semester and module. */
class CoursesInformationFragment : BaseFragment() {
    private var _binding: CoursesInformationFragmentBinding? = null
    private val binding get() = _binding!!

    private var all: List<StudentCourseItem> = emptyList()
    private var semesters: List<String> = emptyList()
    private var modules: List<String> = emptyList()
    private val filter = SemesterAndModuleFilter { render() }

    /** Row 0 is the header, [EMPTY] is shown instead of cards when nothing matches. */
    private val adapter = ListItemAdapter<Any>(
        layoutFor = { item, _ ->
            when {
                item === HEADER -> R.layout.student_courses_list_header
                item === EMPTY -> R.layout.list_custom_text_empty_layout
                else -> R.layout.student_courses_list_item
            }
        }
    ) { layout, view, item, _ ->
        when (layout) {
            R.layout.student_courses_list_header -> filter.bind(view, semesters, modules)
            R.layout.list_custom_text_empty_layout -> ListCustomTextEmptyLayoutBinding.bind(view).apply {
                emptyTitle.text = getString(R.string.courses_no_result_title)
                emptyText.text = getString(R.string.courses_no_result_text)
            }
            else -> {
                val c = item as StudentCourseItem
                StudentCoursesListItemBinding.bind(view).title.text = c.subjectName
                InfoRows.fill(view, listOf(
                    R.string.courseCode to c.courseCode,
                    R.string.module to c.moduleName,
                    R.string.courseType to c.courseType,
                    R.string.credit to c.credit
                ))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CoursesInformationFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.menu_course_informations)
        binding.trainingList.layoutManager = LinearLayoutManager(requireContext())
        binding.trainingList.adapter = adapter
        loadCourses()
    }

    private fun loadCourses() = load(retry = ::loadCourses) {
        all = backend.courses().sortedBy { it.semester }
        semesters = all.mapNotNull { it.semester }.distinct().sorted()
        modules = all.mapNotNull { it.moduleName }.distinct()
        filter.preselect(semesters, modules)
        render()
    }

    private fun render() {
        val filtered = all.filter { it.semester == filter.semester && it.moduleName == filter.module }
        val rows = ArrayList<Any>()
        rows.add(HEADER)
        if (filtered.isEmpty()) rows.add(EMPTY) else rows.addAll(filtered)
        adapter.submit(rows)
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    private companion object {
        val HEADER = Any()
        val EMPTY = Any()
    }
}
