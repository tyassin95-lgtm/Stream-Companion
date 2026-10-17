package hu.unideb.studyversity.ui.university_matters.over_education.library.search

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.LibrarySearchItem
import hu.unideb.studyversity.databinding.BottomHelperViewBinding
import hu.unideb.studyversity.databinding.LibrarySearchFragmentBinding
import hu.unideb.studyversity.databinding.LibrarySearchListHeaderBinding
import hu.unideb.studyversity.databinding.LibrarySeachItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.ListItemAdapter
import hu.unideb.studyversity.ui.base.PagedSearch
import hu.unideb.studyversity.utils.Animations
import hu.unideb.studyversity.utils.BottomSheetInit
import java.util.Locale

/** Library catalogue search with expandable results showing the copies and where to find them. */
class LibrarySearchFragment : BaseFragment() {
    private var _binding: LibrarySearchFragmentBinding? = null
    private val binding get() = _binding!!

    private var term = ""
    private var logoVisible = true
    private lateinit var pager: PagedSearch<LibrarySearchItem>
    private val header = LibrarySearchItem(uniqueID = "__header__")

    private val adapter = ListItemAdapter<LibrarySearchItem>(
        layoutFor = { item, _ -> if (item === header) R.layout.library_search_list_header else R.layout.library_seach_item }
    ) { layout, view, item, position ->
        if (layout == R.layout.library_search_list_header) bindHeader(view) else bindItem(view, item, position)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LibrarySearchFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.library_search)
        val helper = BottomHelperViewBinding.bind(binding.root.findViewById(R.id.bottom_sheet))
        helper.helperTitle.text = getString(R.string.search_in_our_catalog)
        helper.detailText.text = getString(R.string.search_in_our_catalog_message)
        BottomSheetInit.initBottomSheet(helper.topHolder, helper.detailText, helper.bottomSheetIndicator)

        val lm = LinearLayoutManager(requireContext())
        binding.recyclerView.apply {
            layoutManager = lm
            adapter = this@LibrarySearchFragment.adapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0 && lm.findLastVisibleItemPosition() >= adapter!!.itemCount - 3) pager.loadNext(viewLifecycleOwner.lifecycleScope)
                }
            })
        }
        pager = PagedSearch(
            fetch = { start, end -> backend.searchLibrary(term.trim(), start, end) },
            onUpdate = {
                binding.emptyLayout.root.visibility = if (it.isEmpty() && term.isNotBlank()) View.VISIBLE else View.GONE
                adapter.submit(listOf(header) + it)
            },
            onError = { handleError(it) }
        )
        adapter.submit(listOf(header))
    }

    private fun bindHeader(v: View) {
        val b = LibrarySearchListHeaderBinding.bind(v)
        if (!logoVisible) b.headerLogo.visibility = View.GONE
        b.searchTextInputEditText.setText(term)
        b.mapSearchItemButton.isEnabled = term.trim().isNotEmpty()
        b.searchTextInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b4: Int, c: Int) {}
            override fun afterTextChanged(s: Editable) {
                term = s.toString()
                b.mapSearchItemButton.isEnabled = term.trim().isNotEmpty()
            }
        })
        b.searchTextInputEditText.setImeActionLabel(getString(R.string.search), EditorInfo.IME_ACTION_SEARCH)
        b.searchTextInputEditText.imeOptions = EditorInfo.IME_ACTION_SEARCH
        b.searchTextInputEditText.setOnFocusChangeListener { _, focused -> if (focused) hideLogo(b.headerLogo) }
        b.searchTextInputEditText.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH && term.isNotBlank()) { search(v); true } else false
        }
        b.mapSearchItemButton.setOnClickListener { search(v) }
    }

    private fun hideLogo(logo: ImageView) {
        if (!logoVisible) return
        logoVisible = false
        Animations.slideView(logo, logo.height, 0)
    }

    private fun search(anchor: View) {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(anchor.windowToken, 0)
        pager.restart(viewLifecycleOwner.lifecycleScope)
    }

    private fun bindItem(v: View, item: LibrarySearchItem, position: Int) {
        val b = LibrarySeachItemBinding.bind(v)
        val ctx = v.context
        val lang = Locale.getDefault().language
        b.titleText.text = item.title
        b.authorText.text = item.author
        b.publicationText.text = ctx.getString(R.string.publication) + " " + item.publication
        b.arrowDown.setImageResource(if (item.isExpanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more)
        b.detailsHolder.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        // One "Location - call number" line per copy, or a note that there are none.
        val copies = item.holdings.mapNotNull { h ->
            h.location?.displayName(lang)?.takeIf { it.isNotEmpty() }?.let { "$it - ${h.callNumber}" }
        }
        b.copiesText.text = if (copies.isEmpty()) ctx.getString(R.string.no_available_copies) else copies.joinToString("\n")
        v.setOnClickListener {
            item.isExpanded = !item.isExpanded
            adapter.notifyItemChanged(position)
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
