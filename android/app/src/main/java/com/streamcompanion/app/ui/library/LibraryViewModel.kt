package com.streamcompanion.app.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamcompanion.app.App
import com.streamcompanion.app.data.ApiException
import com.streamcompanion.app.data.BrowseResult
import com.streamcompanion.app.data.SavedServer
import com.streamcompanion.app.data.ServerApi
import com.streamcompanion.app.data.VideoItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface LibState {
    data object Loading : LibState
    data class Error(val message: String, val unauthorized: Boolean = false) : LibState
    data class Ready(val data: BrowseResult, val recent: List<VideoItem>) : LibState
}

class LibraryViewModel(application: Application, val serverId: String, val folderId: String?) : AndroidViewModel(application) {
    private val app = application as App
    private val _state = MutableStateFlow<LibState>(LibState.Loading)
    val state: StateFlow<LibState> = _state.asStateFlow()

    val query = MutableStateFlow("")
    private val _results = MutableStateFlow<List<VideoItem>?>(null)
    val results: StateFlow<List<VideoItem>?> = _results.asStateFlow()

    private var revision = -1L
    private var loadJob: Job? = null

    val server: SavedServer? get() = app.prefs.server(serverId)
    val api: ServerApi? get() = server?.let { app.apiFor(it) }

    init {
        load(spinner = true)
        poll()
        observeSearch()
    }

    fun retry() = load(spinner = true)

    fun load(spinner: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (spinner) _state.value = LibState.Loading
            _state.value = fetch() ?: return@launch
        }
    }

    private suspend fun fetch(allowRediscovery: Boolean = true): LibState? {
        val a = api ?: return LibState.Error("This computer is no longer paired.", unauthorized = true)
        return try {
            val data = a.browse(folderId)
            revision = data.revision
            val recent = if (folderId == null) runCatching { a.recent(12) }.getOrDefault(emptyList()) else emptyList()
            LibState.Ready(data, recent)
        } catch (e: ApiException) {
            when {
                e.unauthorized -> LibState.Error("This computer no longer recognises your phone. Pair again with a new code.", unauthorized = true)
                e.code == 404 -> LibState.Error("This folder no longer exists.")
                else -> LibState.Error(e.message ?: "Something went wrong.")
            }
        } catch (e: java.io.IOException) {
            // The PC may have moved to a new address: look for it again before giving up.
            if (allowRediscovery && rediscover()) fetch(allowRediscovery = false)
            else LibState.Error("Can't reach your PC. Make sure it is on, Stream Companion is running and you are on the same Wi-Fi.")
        }
    }

    private suspend fun rediscover(): Boolean {
        val found = withTimeoutOrNull(4500) { app.discovery.discover().first { it.id == serverId } } ?: return false
        app.prefs.updateAddress(serverId, found.host, found.port)
        return true
    }

    /** Quietly refreshes when the server reports that its library changed. */
    private fun poll() {
        viewModelScope.launch {
            while (true) {
                delay(6000)
                val a = api ?: continue
                val s = _state.value
                if (s is LibState.Ready || s is LibState.Error) {
                    val r = runCatching { a.revision() }.getOrNull()
                    if (s is LibState.Ready && r != null && r != revision) {
                        (fetch(allowRediscovery = false) as? LibState.Ready)?.let { _state.value = it }
                    } else if (s is LibState.Error && !s.unauthorized && r != null) {
                        load(spinner = false)
                    }
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            query.debounce(300).distinctUntilChanged().collect { q ->
                if (q.isBlank()) { _results.value = null; return@collect }
                _results.value = runCatching { api?.search(q.trim()) }.getOrNull() ?: emptyList()
            }
        }
    }
}
