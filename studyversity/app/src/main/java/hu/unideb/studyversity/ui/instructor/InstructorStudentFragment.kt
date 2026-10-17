package hu.unideb.studyversity.ui.instructor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.Formats
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.model.StaffStudent
import hu.unideb.studyversity.data.model.StudentUpdate
import hu.unideb.studyversity.databinding.FragmentInstructorStudentBinding
import kotlinx.coroutines.launch
import java.util.Date

/** Name, university ID, e-mail, balance and expected graduation of the student. */
class InstructorStudentFragment : Fragment() {
    private var _binding: FragmentInstructorStudentBinding? = null
    private val binding get() = _binding!!
    private val vm: InstructorViewModel by activityViewModels()
    private var graduation: Date? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInstructorStudentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.graduationInput.setOnClickListener {
            Pickers.date(requireContext(), graduation, getString(R.string.student_graduation_clear), onClear = { setGraduation(null) }) { y, m, d ->
                setGraduation(Pickers.withDay(graduation, y, m, d))
            }
        }
        binding.saveStudentButton.setOnClickListener { save() }
        vm.student.observe(viewLifecycleOwner) { it?.let(::show) }
    }

    private fun setGraduation(d: Date?) {
        graduation = d
        binding.graduationInput.setText(Formats.date(d))
    }

    private fun show(s: StaffStudent) {
        binding.studentHeader.text = s.fullName
        binding.studentProgramme.text = getString(R.string.student_programme) + ": " + listOfNotNull(s.trainingName, s.facultyName).joinToString(" · ")
        binding.lastNameInput.setText(s.lastName)
        binding.firstNameInput.setText(s.firstName)
        binding.universityIdInput.setText(s.neptunCode)
        binding.emailInput.setText(s.email)
        binding.balanceInput.setText((s.balance ?: 0).toString())
        setGraduation(s.trainingPlanEnd)
    }

    private fun save() {
        val student = vm.student.value ?: return
        val last = binding.lastNameInput.text.toString().trim()
        val first = binding.firstNameInput.text.toString().trim()
        val code = binding.universityIdInput.text.toString().trim()
        val balance = binding.balanceInput.text.toString().trim().ifEmpty { "0" }.toIntOrNull()
        when {
            (last.isEmpty() && first.isEmpty()) || code.isEmpty() -> { snack(R.string.student_required); return }
            balance == null -> { snack(R.string.student_balance_invalid); return }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val saved = ServiceLocator.backend.updateStudent(
                    StudentUpdate(student.id, first, last, code, binding.emailInput.text.toString().trim(), balance!!, graduation)
                )
                vm.update(saved)
                snack(R.string.student_saved)
            } catch (e: Exception) {
                snack(e.localizedMessage ?: getString(R.string.unknown_error))
            }
        }
    }

    private fun snack(res: Int) = snack(getString(res))
    private fun snack(text: String) { _binding?.let { Snackbar.make(it.root, text, Snackbar.LENGTH_LONG).show() } }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
