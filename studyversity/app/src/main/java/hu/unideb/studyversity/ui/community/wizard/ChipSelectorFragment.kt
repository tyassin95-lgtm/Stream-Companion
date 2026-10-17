package hu.unideb.studyversity.ui.community.wizard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import hu.unideb.studyversity.R
import hu.unideb.studyversity.core.ServiceLocator
import hu.unideb.studyversity.data.model.CategoryItem
import hu.unideb.studyversity.databinding.FragmentChipSelectorBinding
import hu.unideb.studyversity.ui.base.ListItemAdapter
import hu.unideb.studyversity.ui.community.ChipSelectionType
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

/**
 * Searchable multi select list of categories (interests / faculties / languages).
 * Arguments: `chipSelectionType` (name of [ChipSelectionType]) and `selectedChips` (long[] of category ids).
 * The result goes to the [ChipSelectorHost] activity.
 */
class ChipSelectorFragment : Fragment() {
    private object Header
    private data class Row(val item: CategoryItem, val selected: Boolean)

    private var _binding: FragmentChipSelectorBinding? = null
    private val binding get() = _binding!!

    private val type by lazy { ChipSelectionType.valueOf(requireArguments().getString(ARG_TYPE) ?: ChipSelectionType.INTERESTS.name) }
    private val selected = LinkedHashSet<Long>()
    private var all: List<CategoryItem> = emptyList()
    private var query = ""

    private lateinit var adapter: ListItemAdapter<Any>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChipSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            selected.addAll(requireArguments().getLongArray(ARG_SELECTED)?.toList().orEmpty())
        } else {
            selected.addAll(savedInstanceState.getLongArray(STATE_SELECTED)?.toList().orEmpty())
        }
        val subtitle = when (type) {
            ChipSelectionType.INTERESTS -> R.string.significant_interests
            ChipSelectionType.FACULTY_NAMES -> R.string.faculties
            ChipSelectionType.LANGUAGES -> R.string.languages
        }
        binding.chipSelectorTextview.setText(subtitle)
        binding.toolbar.apply {
            title = getString(R.string.app_name)
            this.subtitle = getString(subtitle)
            setNavigationIcon(R.drawable.ic_arrow_left)
            setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
            inflateMenu(R.menu.menu_save_button)
            setOnMenuItemClickListener { if (it.itemId == R.id.done) { done(); true } else false }
        }

        adapter = ListItemAdapter(
            layoutFor = { item, _ -> if (item === Header) R.layout.chip_selector_list_item_heaeder else R.layout.chip_selector_list_item },
            onCreate = { layout, v ->
                if (layout == R.layout.chip_selector_list_item_heaeder) {
                    v.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.searchTextInputEditText).doAfterTextChanged {
                        query = it?.toString().orEmpty(); refresh()
                    }
                }
            },
            bind = { layout, v, item, _ ->
                if (layout == R.layout.chip_selector_list_item) {
                    val row = item as Row
                    v.findViewById<TextView>(R.id.chipName).text = row.item.name
                    val icon = v.findViewById<ImageView>(R.id.chip_icon)
                    icon.setImageResource(R.drawable.ic_check)
                    icon.visibility = if (row.selected) View.VISIBLE else View.INVISIBLE
                    v.findViewById<androidx.cardview.widget.CardView>(R.id.icon_bg).setCardBackgroundColor(
                        ContextCompat.getColor(v.context, if (row.selected) R.color.orangeRed else R.color.brownGrey))
                    v.setOnClickListener {
                        if (!selected.remove(row.item.id)) selected.add(row.item.id)
                        refresh()
                    }
                }
            })
        binding.chipList.layoutManager = LinearLayoutManager(requireContext())
        binding.chipList.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val collator = Collator.getInstance(Locale.getDefault())
                val loaded = ServiceLocator.backend.categories(listOf(type.code))
                // already selected categories first, then alphabetical, like the original list
                val (sel, rest) = loaded.partition { it.id in selected }
                all = sel.sortedWith { a, b -> collator.compare(a.name ?: "", b.name ?: "") } +
                    rest.sortedWith { a, b -> collator.compare(a.name ?: "", b.name ?: "") }
                refresh()
            } catch (e: Exception) {
                Snackbar.make(binding.root, e.localizedMessage ?: getString(R.string.unknown_error), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun refresh() {
        val q = query.trim()
        val rows = all.filter { q.isEmpty() || (it.name ?: "").contains(q, ignoreCase = true) }
            .map { Row(it, it.id in selected) }
        adapter.submit(listOf<Any>(Header) + rows)
    }

    private fun done() {
        if (selected.isEmpty()) {
            Snackbar.make(binding.root, R.string.chip_selector_must_select_at_least_one_item, Snackbar.LENGTH_LONG).show()
            return
        }
        (requireActivity() as ChipSelectorHost).onChipsChosen(type, all.filter { it.id in selected })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLongArray(STATE_SELECTED, selected.toLongArray())
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    companion object {
        const val ARG_TYPE = "chipSelectionType"
        const val ARG_SELECTED = "selectedChips"
        private const val STATE_SELECTED = "state_selected"

        fun args(type: ChipSelectionType, selectedIds: List<Long>) = Bundle().apply {
            putString(ARG_TYPE, type.name)
            putLongArray(ARG_SELECTED, selectedIds.toLongArray())
        }
    }
}
