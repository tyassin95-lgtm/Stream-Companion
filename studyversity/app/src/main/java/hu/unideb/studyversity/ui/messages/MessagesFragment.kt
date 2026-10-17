package hu.unideb.studyversity.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.Formats
import hu.unideb.studyversity.data.model.MessageListItem
import hu.unideb.studyversity.databinding.FragmentMessagesBinding
import hu.unideb.studyversity.databinding.MessageListItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.singleLayoutAdapter
import hu.unideb.studyversity.ui.webview.WebViewFragment

/** The student's messages. A tapped message opens in the in-app browser (as HTML or as a document link). */
class MessagesFragment : BaseFragment() {
    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private val adapter = singleLayoutAdapter<MessageListItem>(R.layout.message_list_item) { v, item, _ ->
        val b = MessageListItemBinding.bind(v)
        b.headerText.text = item.messageHeader
        b.dateText.text = Formats.dateAndTime(item.messageDate)
        b.messageIcon.setImageResource(R.drawable.ic_envelope)
        v.setOnClickListener { item.id?.let { openMessage(it) } }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.menu_my_messages)
        binding.messageList.layoutManager = LinearLayoutManager(requireContext())
        binding.messageList.setHasFixedSize(true)
        binding.messageList.adapter = adapter
        loadMessages()
    }

    private fun loadMessages() = load(retry = ::loadMessages) {
        adapter.submit(backend.messages())
        // A push notification deep-links straight into one message.
        val pushed = arguments?.getLong("messageId", -1L) ?: -1L
        if (pushed != -1L) { arguments?.remove("messageId"); openMessage(pushed) }
    }

    private fun openMessage(id: Long) = load {
        val m = backend.message(id) ?: return@load
        val doc = m.attachedDocuments.firstOrNull()?.url
        val args = if (m.format.equals("url", true) && doc != null)
            bundleOf("fileName" to doc, "fileMode" to WebViewFragment.MODE_URL, "title" to m.messageHeader)
        else bundleOf("fileName" to (m.messageBody ?: ""), "fileMode" to WebViewFragment.MODE_HTML_STRING, "title" to m.messageHeader)
        findNavController().navigate(R.id.webViewFragment, args)
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
