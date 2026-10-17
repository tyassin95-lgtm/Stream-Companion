package hu.unideb.studyversity.ui.university_matters.university_locale

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.data.model.CachedInstitution
import hu.unideb.studyversity.data.model.MapCategoryItem
import hu.unideb.studyversity.databinding.BottomHelperViewBinding
import hu.unideb.studyversity.databinding.InstituteListHeaderBinding
import hu.unideb.studyversity.databinding.InstituteListItemBinding
import hu.unideb.studyversity.databinding.InstitutionListFragmentBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.ListItemAdapter
import hu.unideb.studyversity.utils.Animations
import hu.unideb.studyversity.utils.BottomSheetInit

/**
 * Search places of the university by category and name. Arguments: fragmentTitle, categorieCodes (restrict the
 * search), autoload (search immediately), headerEnabled (show the search form), hideBottomLayout, searchIsForSport.
 */
class InstitutionListFragment : BaseFragment() {
    private var _binding: InstitutionListFragmentBinding? = null
    private val binding get() = _binding!!

    private val titleArg get() = requireArguments().getString("fragmentTitle").orEmpty()
    private val requestedCodes get() = requireArguments().getStringArray("categorieCodes")?.toList().orEmpty()
    private val autoload get() = requireArguments().getBoolean("autoload")
    private val headerEnabled get() = requireArguments().getBoolean("headerEnabled")
    private val hideBottomLayout get() = requireArguments().getBoolean("hideBottomLayout", true)
    private val forSport get() = requireArguments().getBoolean("searchIsForSport")

    private var categories: List<MapCategoryItem> = emptyList()
    private var selectedCategory: MapCategoryItem? = null
    private var query = ""
    private var results: List<CachedInstitution> = emptyList()
    private var logoVisible = true
    private val header = CachedInstitution(id = -1, poiId = -1)

    private val adapter = ListItemAdapter<CachedInstitution>(
        layoutFor = { item, _ -> if (item.id == -1L) R.layout.institute_list_header else R.layout.institute_list_item }
    ) { layout, view, item, _ ->
        if (layout == R.layout.institute_list_header) bindHeader(view) else bindItem(view, item)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = InstitutionListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(titleArg)
        binding.progressBar.visibility = View.GONE
        val helper = BottomHelperViewBinding.bind(binding.root.findViewById(R.id.bottom_sheet))
        helper.helperTitle.text = getString(if (forSport) R.string.how_can_i_search else R.string.search_on_our_map)
        helper.detailText.text = getString(if (forSport) R.string.search_on_our_map_message_sport else R.string.search_on_our_map_message)
        BottomSheetInit.initBottomSheet(helper.topHolder, helper.detailText, helper.bottomSheetIndicator)
        if (hideBottomLayout) {
            binding.root.findViewById<View>(R.id.bottom_sheet).visibility = View.GONE
            binding.recyclerView.setPadding(0, 0, 0, 0)
        }

        logoVisible = headerEnabled
        binding.recyclerView.apply {
            layoutManager = object : LinearLayoutManager(requireContext()) {
                override fun supportsPredictiveItemAnimations() = false
            }
            setHasFixedSize(true)
            itemAnimator = DefaultItemAnimator()
            adapter = this@InstitutionListFragment.adapter
        }
        render()

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = menuInflater.inflate(R.menu.menu_map_button, menu)
            override fun onPrepareMenu(menu: Menu) {
                // The toolbar map button replaces the in-header one when the search form is hidden.
                menu.findItem(R.id.show_map)?.isVisible = !headerEnabled && results.size >= 2
            }
            override fun onMenuItemSelected(item: MenuItem): Boolean {
                if (item.itemId != R.id.show_map) return false
                openMap("", -1)
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        if (!autoload) loadCategories() else search()
    }

    private fun loadCategories() = load(showProgress = false) {
        categories = backend.mapCategories(null)
        adapter.notifyItemChanged(0)
    }

    private fun render() {
        val rows = ArrayList<CachedInstitution>()
        if (headerEnabled) rows.add(header)
        rows.addAll(results)
        adapter.submit(rows)
        requireActivity().invalidateMenu()
    }

    /** Category codes to query: the selected one, otherwise the requested ones (or all when nothing was requested). */
    private fun codes(): List<String>? = when {
        selectedCategory != null -> listOf(selectedCategory!!.code ?: "")
        requestedCodes.isNotEmpty() -> requestedCodes
        else -> null
    }

    private fun search() = load(retry = ::search) {
        results = backend.institutions(codes(), query.trim().ifEmpty { null })
        render()
    }

    private fun bindHeader(v: View) {
        val b = InstituteListHeaderBinding.bind(v)
        val ctx = v.context
        if (!logoVisible) b.headerLogo.visibility = View.GONE
        if (forSport) {
            b.dropDownEditText.visibility = View.GONE
            b.mapSearchItemButton.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.sport_red)
            b.mapSearchItemButton.setTextColor(ContextCompat.getColorStateList(ctx, R.color.white))
            b.mapSearchItemButton.setIconTintResource(R.color.white)
        } else {
            b.mapSearchItemButton.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.yellowishOrange)
            b.mapSearchItemButton.setTextColor(ContextCompat.getColorStateList(ctx, R.color.black))
            b.mapSearchItemButton.setIconTintResource(R.color.black)
            b.mySpinnerDropdown.setDropDownBackgroundDrawable(ContextCompat.getDrawable(ctx, R.drawable.autocomplete_dropdown))
            b.dropDownEditText.isEnabled = categories.isNotEmpty()
            b.mySpinnerDropdown.setAdapter(ArrayAdapter(ctx, R.layout.dropdown_list_item, categories.map { it.name ?: "" }))
            b.mySpinnerDropdown.setText(selectedCategory?.name ?: "", false)
            b.mySpinnerDropdown.setOnItemClickListener { _, _, pos, _ ->
                selectedCategory = categories[pos]
                updateSearchButton(b)
            }
        }
        b.searchTextInputEditText.setText(query)
        b.searchTextInputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b4: Int, c: Int) {}
            override fun afterTextChanged(s: Editable) { query = s.toString(); updateSearchButton(b) }
        })
        b.searchTextInputEditText.setOnFocusChangeListener { _, focused ->
            if (focused && logoVisible) { logoVisible = false; Animations.slideView(b.headerLogo, b.headerLogo.height, 0) }
        }
        b.searchTextInputEditText.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH && b.mapSearchItemButton.isEnabled) { hideKeyboard(v); search(); true } else false
        }
        b.mapSearchItemButton.setOnClickListener { hideKeyboard(v); search() }
        b.closeMapIcon.visibility = if (results.size > 1) View.VISIBLE else View.GONE
        b.closeMapIcon.setOnClickListener { openMap("", -1) }
        updateSearchButton(b)
    }

    private fun updateSearchButton(b: InstituteListHeaderBinding) {
        b.mapSearchItemButton.isEnabled = selectedCategory != null || query.trim().isNotEmpty()
    }

    private fun hideKeyboard(anchor: View) {
        (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(anchor.windowToken, 0)
    }

    private fun bindItem(v: View, item: CachedInstitution) {
        val b = InstituteListItemBinding.bind(v)
        b.textView3.text = item.name
        b.textView5.text = item.categoryName
        val hasLocation = item.location != null
        b.separator.visibility = if (hasLocation) View.VISIBLE else View.GONE
        b.markerIcon.visibility = if (hasLocation) View.VISIBLE else View.GONE
        b.markerIcon.setOnClickListener { openMap(item.name.orEmpty(), item.poiId) }
        v.setOnClickListener {
            findNavController().navigate(R.id.institutionDetailFragment, bundleOf("poiId" to item.poiId, "localizedName" to item.name.orEmpty()))
        }
    }

    private fun openMap(name: String, poiId: Long) {
        findNavController().navigate(R.id.institutionMapViewFragment, bundleOf("poiId" to poiId, "localizedName" to name))
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
