package com.docforge.app.share

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.docforge.app.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShareLaunchViewModelTest {

    @Test
    fun pendingRequest_restoresFromSavedStateUntilDestinationConsumesIt() {
        val savedState = SavedStateHandle()
        val original = request(
            route = Routes.PDF_MERGE,
            uri = "content://provider/shared/original.pdf",
            mimeType = "application/pdf"
        )

        ShareLaunchViewModel(savedState).accept(original)

        // A new ViewModel backed by the restored handle models process recreation.
        val restored = ShareLaunchViewModel(savedState)
        assertEquals(original, restored.pendingRequest.value)

        restored.consume(original)

        assertNull(restored.pendingRequest.value)
        // A second recreation must not replay a request that was already consumed.
        assertNull(ShareLaunchViewModel(savedState).pendingRequest.value)
    }

    @Test
    fun newIntent_supersedesPendingRequest_andLateOldConsumeCannotClearIt() {
        val savedState = SavedStateHandle()
        val viewModel = ShareLaunchViewModel(savedState)
        val first = request(
            route = Routes.IMAGE_FORMAT,
            uri = "content://provider/shared/first.png",
            mimeType = "image/png"
        )
        val second = request(
            route = Routes.AUDIO_FORMAT,
            uri = "content://provider/shared/second.wav",
            mimeType = "audio/wav"
        )

        viewModel.accept(first)
        viewModel.accept(second)

        // A destination callback for the older request may arrive after onNewIntent().
        viewModel.consume(first)
        assertEquals(second, viewModel.pendingRequest.value)

        // The newer request remains durable across recreation until it is consumed.
        assertEquals(second, ShareLaunchViewModel(savedState).pendingRequest.value)

        viewModel.consume(second)
        assertNull(viewModel.pendingRequest.value)
    }

    private fun request(route: String, uri: String, mimeType: String) = ShareLaunchRequest(
        targetRoute = route,
        uris = listOf(Uri.parse(uri)),
        mimeType = mimeType
    )
}
