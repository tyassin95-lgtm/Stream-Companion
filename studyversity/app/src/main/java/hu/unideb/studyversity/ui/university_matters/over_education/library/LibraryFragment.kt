package hu.unideb.studyversity.ui.university_matters.over_education.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.Formats
import hu.unideb.studyversity.data.model.LibraryLoanedContainerItem
import hu.unideb.studyversity.databinding.LibraryFragmentBinding
import hu.unideb.studyversity.databinding.LibraryLoanedItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.ListItemAdapter
import hu.unideb.studyversity.ui.base.singleLayoutAdapter
import java.util.Locale

/** The student's current library loans; tapping a card shows publication details and the lending library. */
class LibraryFragment : BaseFragment() {
    private var _binding: LibraryFragmentBinding? = null
    private val binding get() = _binding!!

    private val adapter: ListItemAdapter<LibraryLoanedContainerItem> = singleLayoutAdapter<LibraryLoanedContainerItem>(R.layout.library_loaned_item) { v, item, position ->
        val b = LibraryLoanedItemBinding.bind(v)
        val ctx = v.context
        val lang = Locale.getDefault().language
        b.titleText.text = item.bibRecord?.title
        b.authorText.text = item.bibRecord?.author
        b.dueDateText.text = Formats.date(item.dueDate)
        b.arrowDown.setImageResource(if (item.isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
        b.detailsHolder.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        b.publicationText.text = ctx.getString(R.string.publication) + " " + item.bibRecord?.publication
        b.locationText.text = item.library?.displayName(lang)?.takeIf { it.isNotEmpty() } ?: ctx.getString(R.string.rental_locaition)
        v.setOnClickListener {
            item.isExpanded = !item.isExpanded
            this.adapter.notifyItemChanged(position)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LibraryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.library_my_loans_title)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        loadLoans()
    }

    private fun loadLoans() = load(retry = ::loadLoans) {
        val loans = backend.loanedBooks()
        binding.emptyLayout.visibility = if (loans.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (loans.isEmpty()) View.GONE else View.VISIBLE
        adapter.submit(loans)
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
