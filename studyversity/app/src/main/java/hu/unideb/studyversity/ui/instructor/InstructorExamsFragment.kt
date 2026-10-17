package hu.unideb.studyversity.ui.instructor

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.Formats
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.model.CourseOption
import hu.unideb.studyversity.data.model.StaffExam
import hu.unideb.studyversity.databinding.DialogExamEditBinding
import hu.unideb.studyversity.databinding.FragmentInstructorListBinding
import hu.unideb.studyversity.databinding.InstructorExamItemBinding
import hu.unideb.studyversity.ui.base.ListItemAdapter
import hu.unideb.studyversity.ui.base.singleLayoutAdapter
import kotlinx.coroutines.launch
import java.util.Date

/** Every exam / grade record of the student: open an exam, set its date, enter or change the grade. */
class InstructorExamsFragment : Fragment() {
    private var _binding: FragmentInstructorListBinding? = null
    private val binding get() = _binding!!
    private val vm: InstructorViewModel by activityViewModels()
    private val backend get() = ServiceLocator.backend
    private var courses: List<CourseOption> = emptyList()

    private val adapter: ListItemAdapter<StaffExam> = singleLayoutAdapter(R.layout.instructor_exam_item) { v, e, _ ->
        val b = InstructorExamItemBinding.bind(v)
        val ctx = v.context
        b.examSubject.text = e.subjectName
        b.examMeta.text = listOfNotNull(e.semester, typeLabel(e), e.credit?.takeIf { it > 0 }?.let { "$it cr" }).joinToString(" · ")
        b.examDateText.text = e.examDate?.let { Formats.dateAndTime(it) } ?: ctx.getString(R.string.exam_no_date)
        b.examGrade.text = e.grade?.toString() ?: "–"
        b.examGrade.alpha = if (e.grade == null) 0.35f else 1f
        b.examStatus.text = ctx.getString(when {
            e.grade != null -> R.string.exam_status_graded
            e.opened -> R.string.exam_status_open
            else -> R.string.exam_status_closed
        })
        v.setOnClickListener { edit(e) }
    }

    private fun typeLabel(e: StaffExam) = getString(when (e.examType) {
        "MARK" -> R.string.exam_kind_mark
        "FINAL" -> R.string.exam_kind_final
        else -> R.string.exam_kind_exam
    })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInstructorListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.instructorList.layoutManager = LinearLayoutManager(requireContext())
        binding.instructorList.adapter = adapter
        binding.addButton.setText(R.string.exam_open_new)
        binding.emptyText.setText(R.string.exam_empty)
        binding.addButton.setOnClickListener { edit(null) }
        vm.student.observe(viewLifecycleOwner) { if (it != null) reload() }
    }

    private fun reload() {
        val id = vm.student.value?.id ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val exams = backend.studentExams(id)
                courses = backend.studentCourseOptions(id)
                adapter.submit(exams)
                binding.emptyText.visibility = if (exams.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) { snack(e.localizedMessage ?: getString(R.string.unknown_error)) }
        }
    }

    /** Opens the editor for [exam], or for a new exam when null. */
    private fun edit(exam: StaffExam?) {
        val studentId = vm.student.value?.id ?: return
        val b = DialogExamEditBinding.inflate(layoutInflater)
        val ctx = requireContext()
        var date: Date? = exam?.examDate
        var opened = exam?.opened ?: true

        fun dropdown(view: android.widget.AutoCompleteTextView, values: List<String>) {
            view.setDropDownBackgroundDrawable(ContextCompat.getDrawable(ctx, R.drawable.autocomplete_dropdown))
            view.setAdapter(ArrayAdapter(ctx, R.layout.dropdown_list_item, values))
        }

        // course: chosen for a new exam, fixed for an existing one
        var chosen: CourseOption? = null
        if (exam == null) {
            val labels = courses.map { "${it.subjectName} (${it.semester})" }
            dropdown(b.examCourseInput, labels)
            b.examCourseInput.setOnItemClickListener { _, _, pos, _ -> chosen = courses[pos] }
        } else {
            b.examCourseLayout.visibility = View.GONE
            b.examCourseFixed.visibility = View.VISIBLE
            b.examCourseFixed.text = "${exam.subjectName} (${exam.semester})"
        }

        val gradeLabels = listOf(getString(R.string.exam_no_grade)) + (1..5).map { it.toString() }
        dropdown(b.examGradeInput, gradeLabels)
        b.examGradeInput.setText(gradeLabels[exam?.grade ?: 0], false)
        b.examDateInput.setText(Formats.dateAndTime(date).ifEmpty { "" })
        b.examOpenSwitch.isChecked = opened
        b.examDateInput.setOnClickListener {
            Pickers.date(ctx, date) { y, m, d ->
                val withDay = Pickers.withDay(date, y, m, d)
                val c = java.util.Calendar.getInstance().apply { time = withDay }
                Pickers.time(ctx, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE)) { h, min ->
                    date = Pickers.withTime(withDay, h, min)
                    b.examDateInput.setText(Formats.dateAndTime(date))
                }
            }
        }

        val builder = MaterialAlertDialogBuilder(ctx)
            .setTitle(if (exam == null) R.string.exam_new_title else R.string.exam_edit_title)
            .setView(b.root)
            .setPositiveButton(R.string.done, null)
            .setNegativeButton(R.string.cancel, null)
        if (exam != null) builder.setNeutralButton(R.string.exam_delete, null)
        val dialog: Dialog = builder.create()
        dialog.show()
        val alert = dialog as androidx.appcompat.app.AlertDialog

        alert.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val course = chosen
            if (exam == null && course == null) { snack(getString(R.string.exam_choose_course)); return@setOnClickListener }
            val grade = gradeLabels.indexOf(b.examGradeInput.text.toString()).takeIf { it > 0 }
            val record = exam?.copy(examDate = date, grade = grade, opened = b.examOpenSwitch.isChecked)
                ?: StaffExam(
                    courseCode = course!!.courseCode, subjectName = course.subjectName, courseType = "L", semester = course.semester,
                    credit = course.credit, examType = "EXAM", examDate = date, grade = grade, opened = b.examOpenSwitch.isChecked
                )
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    backend.saveStudentExam(studentId, record)
                    snack(getString(R.string.exam_saved))
                    reload()
                    alert.dismiss()
                } catch (e: Exception) { snack(e.localizedMessage ?: getString(R.string.unknown_error)) }
            }
        }
        if (exam != null) alert.getButton(android.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    backend.deleteStudentExam(studentId, exam.id ?: return@launch)
                    snack(getString(R.string.exam_deleted))
                    reload()
                    alert.dismiss()
                } catch (e: Exception) { snack(e.localizedMessage ?: getString(R.string.unknown_error)) }
            }
        }
    }

    private fun snack(text: String) { _binding?.let { Snackbar.make(it.root, text, Snackbar.LENGTH_LONG).show() } }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
