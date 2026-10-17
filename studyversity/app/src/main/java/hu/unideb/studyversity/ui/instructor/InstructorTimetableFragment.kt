package hu.unideb.studyversity.ui.instructor

import android.os.Bundle
import android.text.format.DateFormat
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
import hu.unideb.studyversity.data.model.TimeTableItem
import hu.unideb.studyversity.databinding.DialogClassEditBinding
import hu.unideb.studyversity.databinding.FragmentInstructorListBinding
import hu.unideb.studyversity.databinding.InstructorClassItemBinding
import hu.unideb.studyversity.ui.base.ListItemAdapter
import hu.unideb.studyversity.ui.base.singleLayoutAdapter
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/** The student's classes: add one (e.g. an extra practice) or cancel one. */
class InstructorTimetableFragment : Fragment() {
    private var _binding: FragmentInstructorListBinding? = null
    private val binding get() = _binding!!
    private val vm: InstructorViewModel by activityViewModels()
    private val backend get() = ServiceLocator.backend

    private val adapter: ListItemAdapter<TimeTableItem> = singleLayoutAdapter(R.layout.instructor_class_item) { v, c, _ ->
        val b = InstructorClassItemBinding.bind(v)
        b.classWhen.text = DateFormat.format("EEEE", c.classStart ?: Date()).toString().replaceFirstChar { it.uppercase() } + ", " +
            Formats.date(c.classStart) + "  " + c.classStartAndEnd
        b.classSubject.text = c.subjectName
        b.classMeta.text = listOfNotNull(c.courseType, c.roomName?.let { v.context.getString(R.string.class_room_label) + " " + it }).joinToString(" · ")
        b.classRemove.setOnClickListener { confirmRemove(c) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInstructorListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.instructorList.layoutManager = LinearLayoutManager(requireContext())
        binding.instructorList.adapter = adapter
        binding.addButton.setText(R.string.class_add)
        binding.emptyText.setText(R.string.class_empty)
        binding.addButton.setOnClickListener { add() }
        vm.student.observe(viewLifecycleOwner) { if (it != null) reload() }
    }

    private fun reload() {
        val id = vm.student.value?.id ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val classes = backend.studentClasses(id)
                adapter.submit(classes)
                binding.emptyText.visibility = if (classes.isEmpty()) View.VISIBLE else View.GONE
                // open on the next class
                val next = classes.indexOfFirst { (it.classStart?.time ?: 0L) >= System.currentTimeMillis() }
                if (next > 0) binding.instructorList.scrollToPosition(next)
            } catch (e: Exception) { snack(e.localizedMessage ?: getString(R.string.unknown_error)) }
        }
    }

    private fun confirmRemove(c: TimeTableItem) {
        val studentId = vm.student.value?.id ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.class_remove)
            .setMessage("${c.subjectName}\n${Formats.dateAndTime(c.classStart)}")
            .setPositiveButton(R.string.class_remove) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        backend.deleteStudentClass(studentId, c.id ?: return@launch)
                        snack(getString(R.string.class_removed))
                        reload()
                    } catch (e: Exception) { snack(e.localizedMessage ?: getString(R.string.unknown_error)) }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun add() {
        val studentId = vm.student.value?.id ?: return
        val ctx = requireContext()
        val b = DialogClassEditBinding.inflate(layoutInflater)
        val types = listOf("L" to getString(R.string.class_type_lecture), "P" to getString(R.string.class_type_practice))
        b.classTypeInput.setDropDownBackgroundDrawable(ContextCompat.getDrawable(ctx, R.drawable.autocomplete_dropdown))
        b.classTypeInput.setAdapter(ArrayAdapter(ctx, R.layout.dropdown_list_item, types.map { it.second }))
        b.classTypeInput.setText(types[0].second, false)

        var day: Date? = null
        var startH = 8; var startM = 0
        var endH = 9; var endM = 0
        var startSet = false; var endSet = false
        fun two(i: Int) = i.toString().padStart(2, '0')
        b.classDateInput.setOnClickListener {
            Pickers.date(ctx, day) { y, m, d -> day = Pickers.withDay(day, y, m, d); b.classDateInput.setText(Formats.date(day)) }
        }
        b.classStartInput.setOnClickListener {
            Pickers.time(ctx, startH, startM) { h, m -> startH = h; startM = m; startSet = true; b.classStartInput.setText("${two(h)}:${two(m)}") }
        }
        b.classEndInput.setOnClickListener {
            Pickers.time(ctx, endH, endM) { h, m -> endH = h; endM = m; endSet = true; b.classEndInput.setText("${two(h)}:${two(m)}") }
        }

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.class_add)
            .setView(b.root)
            .setPositiveButton(R.string.done, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val subject = b.classSubjectInput.text.toString().trim()
            val d = day
            when {
                subject.isEmpty() -> { snack(getString(R.string.class_subject_required)); return@setOnClickListener }
                d == null || !startSet || !endSet -> { snack(getString(R.string.class_date_required)); return@setOnClickListener }
            }
            val start = Pickers.withTime(d, startH, startM)
            val end = Pickers.withTime(d, endH, endM)
            val code = types.firstOrNull { it.second == b.classTypeInput.text.toString() }?.first ?: "L"
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    backend.addStudentClass(studentId, TimeTableItem(
                        subjectName = subject, subjectCode = null, roomName = b.classRoomInput.text.toString().trim().ifEmpty { null },
                        courseType = code, classStart = start, classEnd = end
                    ))
                    snack(getString(R.string.class_added))
                    reload()
                    dialog.dismiss()
                } catch (e: Exception) { snack(e.localizedMessage ?: getString(R.string.unknown_error)) }
            }
        }
    }

    private fun snack(text: String) { _binding?.let { Snackbar.make(it.root, text, Snackbar.LENGTH_LONG).show() } }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
