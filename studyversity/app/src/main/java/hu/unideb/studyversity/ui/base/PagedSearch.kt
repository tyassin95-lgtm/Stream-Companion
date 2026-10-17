package hu.unideb.studyversity.ui.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Row-window paging used by the search screens (phone book, library catalogue): the first request asks for
 * [initialSize] rows, every following one for [pageSize]; the end is reached when a page comes back short.
 */
class PagedSearch<T>(
    private val initialSize: Int = 20,
    private val pageSize: Int = 10,
    private val fetch: suspend (startRow: Int, endRow: Int) -> List<T>,
    private val onUpdate: (List<T>) -> Unit,
    private val onError: (Throwable) -> Unit
) {
    private val items = ArrayList<T>()
    private var nextRow = 0
    private var loading = false
    var endReached = true
        private set

    fun restart(scope: CoroutineScope) {
        items.clear(); nextRow = 0; endReached = false
        loadNext(scope)
    }

    fun loadNext(scope: CoroutineScope) {
        if (loading || endReached) return
        loading = true
        val size = if (nextRow == 0) initialSize else pageSize
        scope.launch {
            try {
                val page = fetch(nextRow, nextRow + size)
                items.addAll(page)
                nextRow += page.size
                endReached = page.size < size
                onUpdate(items.toList())
            } catch (e: Exception) {
                onError(e)
            } finally {
                loading = false
            }
        }
    }
}
