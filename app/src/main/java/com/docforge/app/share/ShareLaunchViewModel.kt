package com.docforge.app.share

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.ArrayList
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns a shared-file launch until the destination explicitly consumes it.
 *
 * Keeping this state outside Compose prevents duplicate/lost prefills across Activity recreation.
 * SavedStateHandle also preserves the pending request across normal process recreation without
 * requiring the original SEND intent to be replayed.
 */
@HiltViewModel
class ShareLaunchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _pendingRequest = MutableStateFlow(restoreRequest())
    val pendingRequest: StateFlow<ShareLaunchRequest?> = _pendingRequest.asStateFlow()

    fun accept(request: ShareLaunchRequest) {
        savedStateHandle[KEY_ROUTE] = request.targetRoute
        savedStateHandle[KEY_URIS] = ArrayList(request.uris.map(Uri::toString))
        savedStateHandle[KEY_MIME] = request.mimeType
        _pendingRequest.value = request
    }

    fun consume(expected: ShareLaunchRequest) {
        if (_pendingRequest.value != expected) return

        savedStateHandle.remove<String>(KEY_ROUTE)
        savedStateHandle.remove<ArrayList<String>>(KEY_URIS)
        savedStateHandle.remove<String>(KEY_MIME)
        _pendingRequest.value = null
    }

    private fun restoreRequest(): ShareLaunchRequest? {
        val route = savedStateHandle.get<String>(KEY_ROUTE) ?: return null
        val uriStrings = savedStateHandle.get<ArrayList<String>>(KEY_URIS).orEmpty()
        if (uriStrings.isEmpty()) return null

        val uris = uriStrings.mapNotNull { raw ->
            runCatching { Uri.parse(raw) }.getOrNull()
        }
        if (uris.isEmpty()) return null

        return ShareLaunchRequest(
            targetRoute = route,
            uris = uris,
            mimeType = savedStateHandle.get(KEY_MIME)
        )
    }

    private companion object {
        const val KEY_ROUTE = "share_launch_route"
        const val KEY_URIS = "share_launch_uris"
        const val KEY_MIME = "share_launch_mime"
    }
}
