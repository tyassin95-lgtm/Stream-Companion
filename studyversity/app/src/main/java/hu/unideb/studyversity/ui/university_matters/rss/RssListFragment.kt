package hu.unideb.studyversity.ui.university_matters.rss

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.unideb.studyversity.core.Formats
import hu.unideb.studyversity.data.model.NewsChannel
import hu.unideb.studyversity.data.model.RssFeedItem
import hu.unideb.studyversity.databinding.RssListFragmentBinding
import hu.unideb.studyversity.ui.base.BaseFragment
import hu.unideb.studyversity.ui.base.ListItemAdapter
import hu.unideb.studyversity.R

/**
 * News / events / sport feeds. Arguments: `list_item_layout` (news or events card), `fragmentTitle`,
 * `channelID` (int array of [NewsChannel] values).
 */
class RssListFragment : BaseFragment() {
    private var _binding: RssListFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = RssListFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setScreenTitle(requireArguments().getString("fragmentTitle"))
        val layout = requireArguments().getInt("list_item_layout")
        val adapter = ListItemAdapter<RssFeedItem>({ _, _ -> layout }) { _, v, item, _ ->
            val date = v.findViewById<TextView>(R.id.dateText)
            val title = v.findViewById<TextView>(R.id.titleText)
            val detail = v.findViewById<TextView>(R.id.detail_holder)
            date.text = Formats.date(item.pubDate)
            title.text = item.title
            detail.text = item.formattedDescription
            v.setOnClickListener { item.link?.let { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) } }
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            itemAnimator = DefaultItemAnimator()
            this.adapter = adapter
            // The "back to top" button appears once the list is scrolled.
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    if (rv.computeVerticalScrollOffset() > 0) binding.backToTopFab.show() else binding.backToTopFab.hide()
                }
            })
        }
        binding.backToTopFab.hide()
        binding.backToTopFab.setOnClickListener { binding.recyclerView.smoothScrollToPosition(0) }
        loadFeed(adapter)
    }

    private fun loadFeed(adapter: ListItemAdapter<RssFeedItem>): Unit = load(retry = { loadFeed(adapter) }) {
        val channels = requireArguments().getIntArray("channelID")?.toList().orEmpty()
        val items = channels.mapNotNull { v -> NewsChannel.values().firstOrNull { it.value == v } }
            .flatMap { backend.rss(it) }
            .sortedByDescending { it.pubDate }
        binding.emptyText.root.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        adapter.submit(items)
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
