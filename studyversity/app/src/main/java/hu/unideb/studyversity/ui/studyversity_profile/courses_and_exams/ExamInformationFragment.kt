package hu.unideb.studyversity.ui.studyversity_profile.courses_and_exams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.StudentExamItem
import hu.unideb.studyversity.databinding.ExamInformationFragmentBinding
import hu.unideb.studyversity.databinding.ListCustomTextEmptyLayoutBinding
import hu.unideb.studyversity.databinding.StudentExamListItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.InfoRows
import hu.unideb.studyversity.ui.base.ListItemAdapter

/** Exam results; cards are green for passed and red for failed exams. */
class ExamInformationFragment : BaseFragment() {
    private var _binding: ExamInformationFragmentBinding? = null
    private val binding get() = _binding!!

    private var all: List<StudentExamItem> = emptyList()
    private var semesters: List<String> = emptyList()
    private var modules: List<String> = emptyList()
    private val filter = SemesterAndModuleFilter { render() }

    private val adapter = ListItemAdapter<Any>(
        layoutFor = { item, _ ->
            when {
                item === HEADER -> R.layout.student_exam_list_header
                item === EMPTY -> R.layout.list_custom_text_empty_layout
                else -> R.layout.student_exam_list_item
            }
        }
    ) { layout, view, item, _ ->
        when (layout) {
            R.layout.student_exam_list_header -> filter.bind(view, semesters, modules)
            R.layout.list_custom_text_empty_layout -> ListCustomTextEmptyLayoutBinding.bind(view).apply {
                emptyTitle.text = getString(R.string.exams_no_result_title)
                emptyText.text = getString(R.string.exams_no_result_text)
            }
            else -> {
                val e = item as StudentExamItem
                val c = view.context
                (view as CardView).setCardBackgroundColor(ContextCompat.getColor(c, if (e.examSuccess) R.color.light_green else R.color.light_red))
                StudentExamListItemBinding.bind(view).title.text = e.formattedSubjectName(c)
                InfoRows.fill(view, listOf(
                    R.string.type to e.examType,
                    R.string.exam_start to hu.unideb.studyversity.core.Formats.dateAndTime(e.examDate),
                    R.string.exam_record_type to e.examRecordType,
                    R.string.exam_register_date to hu.unideb.studyversity.core.Formats.dateAndTime(e.examRecordDate),
                    R.string.exam_value to e.examValue
                ))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ExamInformationFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.menu_taken_exams)
        binding.trainingList.layoutManager = LinearLayoutManager(requireContext())
        binding.trainingList.adapter = adapter
        loadExams()
    }

    private fun loadExams() = load(retry = ::loadExams) {
        all = backend.exams().sortedBy { it.semester }
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
