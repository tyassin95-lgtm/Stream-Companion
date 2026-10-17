package hu.unideb.studyversity.ui.contact

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.ContactFragmentBinding
import hu.unideb.studyversity.ui.base.BaseFragment

/** "Contact": validates the form, then hands the text over to the user's mail app. */
class ContactFragment : BaseFragment() {
    private var _binding: ContactFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContactFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.menu_contact)
        binding.sendEmailButton.setOnClickListener { send() }
    }

    private fun send() {
        if (binding.nameEt.text.isNullOrEmpty()) {
            binding.nameEt.error = getString(R.string.name_field_mandantory)
            return
        }
        binding.nameEt.error = null
        val message = binding.messageEt.text
        if (message.isNullOrEmpty() || message.length < MIN_MESSAGE_LENGTH) {
            binding.messageEt.error = getString(R.string.please_tell_more)
            return
        }
        binding.messageEt.error = null
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "plain/text"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.contact_email_address)))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.contact_from_study_app_email_subject))
            putExtra(Intent.EXTRA_TEXT, message)
        }
        runCatching { startActivity(Intent.createChooser(intent, getString(R.string.send_email))) }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    private companion object { const val MIN_MESSAGE_LENGTH = 60 }
}
