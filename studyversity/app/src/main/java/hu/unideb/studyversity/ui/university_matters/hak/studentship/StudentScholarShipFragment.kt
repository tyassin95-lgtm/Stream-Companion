package hu.unideb.studyversity.ui.university_matters.hak.studentship

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.StudentScholarShipItem
import hu.unideb.studyversity.databinding.StudentScholarShipFragmentBinding
import hu.unideb.studyversity.databinding.StudentScholarsShipListItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.ListItemAdapter
import hu.unideb.studyversity.ui.webview.WebViewFragment

/** Scholarship information documents: a short intro line followed by one card per document. */
class StudentScholarShipFragment : BaseFragment() {
    private var _binding: StudentScholarShipFragmentBinding? = null
    private val binding get() = _binding!!

    private val adapter = ListItemAdapter<Any>(
        layoutFor = { item, _ -> if (item is StudentScholarShipItem) R.layout.student_scholars_ship_list_item else R.layout.student_scholars_ship_list_header }
    ) { layout, view, item, _ ->
        if (layout == R.layout.student_scholars_ship_list_item) {
            val doc = item as StudentScholarShipItem
            StudentScholarsShipListItemBinding.bind(view).scholarshipName.text = doc.name
            view.setOnClickListener { open(doc) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = StudentScholarShipFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.student_scholar_ship)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        loadDocuments()
    }

    private fun loadDocuments() = load(retry = ::loadDocuments) {
        val docs = backend.scholarships()
        binding.emptyLayout.root.visibility = if (docs.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (docs.isEmpty()) View.GONE else View.VISIBLE
        adapter.submit(if (docs.isEmpty()) emptyList() else listOf<Any>(HEADER) + docs)
    }

    private fun open(doc: StudentScholarShipItem) {
        val url = doc.url
        val args = if (url.isNullOrBlank()) bundleOf("fileName" to "", "fileMode" to WebViewFragment.MODE_NOT_FOUND, "title" to doc.name)
        else bundleOf("fileName" to url, "fileMode" to WebViewFragment.MODE_URL, "title" to doc.name)
        findNavController().navigate(R.id.webViewFragment, args)
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    private companion object { val HEADER = Any() }
}
