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
import hu.unideb.studyversity.databinding.FragmentInstructorMailBinding
import hu.unideb.studyversity.databinding.InstructorMailItemBinding
import kotlinx.coroutines.launch

/** Write an e-mail to the student; it lands in the student's messages. */
class InstructorMailFragment : Fragment() {
    private var _binding: FragmentInstructorMailBinding? = null
    private val binding get() = _binding!!
    private val vm: InstructorViewModel by activityViewModels()
    private val backend get() = ServiceLocator.backend

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInstructorMailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sendMailButton.setOnClickListener { send() }
        vm.student.observe(viewLifecycleOwner) { s ->
            if (s == null) return@observe
            binding.mailToInput.setText(listOfNotNull(s.fullName.ifEmpty { null }, s.email?.takeIf { it.isNotBlank() }?.let { "<$it>" }).joinToString(" "))
            reload()
        }
    }

    private fun reload() {
        val id = vm.student.value?.id ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val mails = backend.sentEmails(id)
                val holder = _binding?.mailHistory ?: return@launch
                holder.removeAllViews()
                mails.forEach { m ->
                    val row = InstructorMailItemBinding.inflate(layoutInflater, holder, false)
                    row.mailSubject.text = m.subject
                    row.mailMeta.text = Formats.dateAndTime(m.sentAt) + " · " + m.to
                    row.mailPreview.text = m.body
                    holder.addView(row.root)
                }
                binding.mailEmptyText.visibility = if (mails.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) { snack(e.localizedMessage ?: getString(R.string.unknown_error)) }
        }
    }

    private fun send() {
        val student = vm.student.value ?: return
        val subject = binding.mailSubjectInput.text.toString().trim()
        val body = binding.mailBodyInput.text.toString().trim()
        if (subject.isEmpty() || body.isEmpty()) { snack(getString(R.string.email_fields_required)); return }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                backend.sendEmail(student.id, subject, body)
                binding.mailSubjectInput.setText("")
                binding.mailBodyInput.setText("")
                snack(getString(R.string.email_sent, student.fullName))
                reload()
            } catch (e: Exception) { snack(e.localizedMessage ?: getString(R.string.unknown_error)) }
        }
    }

    private fun snack(text: String) { _binding?.let { Snackbar.make(it.root, text, Snackbar.LENGTH_LONG).show() } }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
