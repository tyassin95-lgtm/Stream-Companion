package hu.unideb.studyversity.ui.university_matters.phonebook

import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.PhoneBookSearchItem
import hu.unideb.studyversity.databinding.BottomHelperViewBinding
import hu.unideb.studyversity.databinding.PhonebookFragmentBinding
import hu.unideb.studyversity.databinding.PhonebookListHeaderBinding
import hu.unideb.studyversity.databinding.PhonebookListItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.ListItemAdapter
import hu.unideb.studyversity.ui.base.PagedSearch
import hu.unideb.studyversity.utils.Animations
import hu.unideb.studyversity.utils.BottomSheetInit

/** University phone book: search employees by name (paged, 10 per page) and open their details. */
class PhonebookFragment : BaseFragment() {
    private var _binding: PhonebookFragmentBinding? = null
    private val binding get() = _binding!!

    private var query = ""
    private var logoVisible = true
    private lateinit var pager: PagedSearch<PhoneBookSearchItem>

    private val header = PhoneBookSearchItem(id = -1)

    private val adapter = ListItemAdapter<PhoneBookSearchItem>(
        layoutFor = { item, _ -> if (item.id == -1L) R.layout.phonebook_list_header else R.layout.phonebook_list_item }
    ) { layout, view, item, _ ->
        if (layout == R.layout.phonebook_list_header) bindHeader(view)
        else PhonebookListItemBinding.bind(view).apply {
            nameText.text = item.name
            departmentName.text = Html.fromHtml(item.formattedDepartmentNameWithHtml, Html.FROM_HTML_MODE_LEGACY)
            departmentName.visibility = View.VISIBLE
            contactWithDepartmentName.text = Html.fromHtml(item.contactWithDepartmentName ?: "", Html.FROM_HTML_MODE_LEGACY)
            root.setOnClickListener {
                findNavController().navigate(R.id.employeeDetailFragment, bundleOf("phoneBookId" to item.id))
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = PhonebookFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(R.string.menu_phone_book)
        val helper = BottomHelperViewBinding.bind(binding.root.findViewById(R.id.bottom_sheet))
        helper.helperTitle.text = getString(R.string.how_can_i_search)
        helper.detailText.text = getString(R.string.how_can_i_search_message)
        BottomSheetInit.initBottomSheet(helper.topHolder, helper.detailText, helper.bottomSheetIndicator)

        val lm = LinearLayoutManager(requireContext())
        binding.recyclerView.apply {
            layoutManager = lm
            setHasFixedSize(false)
            itemAnimator = DefaultItemAnimator()
            adapter = this@PhonebookFragment.adapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0 && lm.findLastVisibleItemPosition() >= adapter!!.itemCount - 3) pager.loadNext(viewLifecycleOwner.lifecycleScope)
                }
            })
        }
        pager = PagedSearch(
            fetch = { start, end -> backend.searchEmployees(query.trim(), start, end) },
            onUpdate = { adapter.submit(listOf(header) + it) },
            onError = { handleError(it) }
        )
        adapter.submit(listOf(header))
    }

    private fun bindHeader(v: View) {
        val b = PhonebookListHeaderBinding.bind(v)
        if (!logoVisible) b.headerLogo.visibility = View.GONE
        b.searchTextInputEditText.setText(query)
        b.mapSearchItemButton.isEnabled = query.trim().isNotEmpty()
        b.searchTextInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b4: Int, c: Int) {}
            override fun afterTextChanged(s: Editable) {
                query = s.toString()
                b.mapSearchItemButton.isEnabled = s.toString().trim().isNotEmpty()
            }
        })
        // Focusing the search box collapses the big logo (like the original "hide logo" animation).
        b.searchTextInputEditText.setOnFocusChangeListener { _, focused -> if (focused) hideLogo(b.headerLogo) }
        b.searchTextInputEditText.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) { search(v); true } else false
        }
        b.mapSearchItemButton.setOnClickListener { search(v) }
    }

    private fun hideLogo(logo: ImageView) {
        if (!logoVisible) return
        logoVisible = false
        Animations.slideView(logo, logo.height, 0)
    }

    private fun search(anchor: View) {
        (requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(anchor.windowToken, 0)
        pager.restart(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
