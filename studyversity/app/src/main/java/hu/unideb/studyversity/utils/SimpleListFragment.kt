package hu.unideb.studyversity.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import hu.unideb.studyversity.R
import hu.unideb.studyversity.databinding.SimpleListFragmentBinding
import hu.unideb.studyversity.databinding.SimpleListItemBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.singleLayoutAdapter
import hu.unideb.studyversity.ui.webview.WebViewFragment

/** One card of a [SimpleListFragment]: [icon] is the name of a drawable (`ic_*`), [value] an optional link/subtitle. */
data class ListEntry(val name: String, val icon: String, val value: String? = null)

/** A list of icon + title cards (HAK, beyond education, sport organisations, DEXPO sub menus). */
abstract class SimpleListFragment : BaseFragment() {
    protected abstract val items: List<ListEntry>
    protected abstract fun onItemClicked(position: Int, item: ListEntry)

    @DrawableRes protected open val iconBackground = R.drawable.selector_viewgroup_orange_inverse
    @DrawableRes protected open val itemBackground = R.drawable.selector_viewgroup_orange
    @DrawableRes protected open val iconTint = R.drawable.icon_selector_orange
    protected open val hideSubTitle = false

    private var _binding: SimpleListFragmentBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SimpleListFragmentBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = singleLayoutAdapter<ListEntry>(R.layout.simple_list_item) { v, item, position ->
            val b = SimpleListItemBinding.bind(v)
            b.simpleItemHolder.setBackgroundResource(itemBackground)
            b.simpleListIcon.setBackgroundResource(iconBackground)
            ContextCompat.getDrawable(v.context, iconFor(item))?.let { d ->
                val wrapped = DrawableCompat.wrap(d.mutate())
                DrawableCompat.setTintList(wrapped, ContextCompat.getColorStateList(v.context, iconTint))
                b.simpleListIcon.setImageDrawable(wrapped)
            }
            b.simpleItemTitle.text = item.name
            b.simpleitemSubtitle.text = item.value
            b.simpleitemSubtitle.visibility = if (item.value == null || hideSubTitle) View.GONE else View.VISIBLE
            v.setOnClickListener { onItemClicked(position, item) }
        }
        _binding!!.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            itemAnimator = DefaultItemAnimator()
            this.adapter = adapter
        }
        adapter.submit(items)
    }

    private fun iconFor(item: ListEntry): Int {
        val id = resources.getIdentifier(item.icon.trim(), "drawable", requireContext().packageName)
        return if (id != 0) id else R.drawable.ic_info_circle
    }

    protected fun openWebView(url: String, title: String?, mode: String = WebViewFragment.MODE_URL) {
        runCatching {
            findNavController().navigate(R.id.webViewFragment, bundleOf("fileName" to url, "fileMode" to mode, "title" to title))
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
