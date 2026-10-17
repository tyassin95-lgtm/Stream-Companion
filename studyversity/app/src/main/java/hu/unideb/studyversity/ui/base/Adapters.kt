package hu.unideb.studyversity.ui.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

/**
 * Small multi-view-type adapter. [layoutFor] picks the item layout; [bind] fills the inflated view.
 * Mirrors the original DataBinding adapters without needing a binding class per layout.
 */
open class ListItemAdapter<T : Any>(
    private val layoutFor: (item: T, position: Int) -> Int,
    private val onCreate: ((layout: Int, view: View) -> Unit)? = null,
    private val bind: (layout: Int, view: View, item: T, position: Int) -> Unit
) : RecyclerView.Adapter<ListItemAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view)

    var items: List<T> = emptyList()
        private set

    fun submit(list: List<T>) {
        val old = items
        items = list
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = list.size
            override fun areItemsTheSame(o: Int, n: Int) = old[o] == list[n]
            override fun areContentsTheSame(o: Int, n: Int) = old[o] == list[n]
        }).dispatchUpdatesTo(this)
    }

    fun item(position: Int): T = items[position]

    override fun getItemViewType(position: Int) = layoutFor(items[position], position)
    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        onCreate?.invoke(viewType, v)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        bind(getItemViewType(position), holder.itemView, items[position], position)
    }
}

fun <T : Any> singleLayoutAdapter(@LayoutRes layout: Int, onCreate: ((View) -> Unit)? = null, bind: (View, T, Int) -> Unit) =
    ListItemAdapter<T>({ _, _ -> layout }, onCreate?.let { f -> { _: Int, v: View -> f(v) } }) { _, v, item, pos -> bind(v, item, pos) }

/** Fills the repeated `title_and_label_view` includes of a card in document order. */
object InfoRows {
    private const val TAG = "layout/title_and_label_view_0"

    fun fill(root: View, rows: List<Pair<Int, String?>>) {
        val views = mutableListOf<View>()
        collect(root, views)
        val ctx = root.context
        rows.forEachIndexed { i, (titleRes, label) ->
            val row = views.getOrNull(i) as? ViewGroup ?: return@forEachIndexed
            (row.getChildAt(0) as android.widget.TextView).text = ctx.getString(titleRes)
            (row.getChildAt(1) as android.widget.TextView).text = label.orEmpty()
        }
    }

    private fun collect(v: View, out: MutableList<View>) {
        if (v.tag == TAG) { out.add(v); return }
        if (v is ViewGroup) for (i in 0 until v.childCount) collect(v.getChildAt(i), out)
    }
}
