package com.docforge.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docforge.app.navigation.DocForgeNavHost
import com.docforge.app.runtime.EngineWarmup
import com.docforge.app.share.ShareIntentRouter
import com.docforge.app.share.ShareLaunchRequest
import com.docforge.app.share.ShareLaunchViewModel
import com.docforge.core.ui.theme.DocForgeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val deps: AppDependencies get() = (application as DocForgeApp).dependencies
    private val shareLaunchViewModel: ShareLaunchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && shareLaunchViewModel.pendingRequest.value == null) {
            ShareIntentRouter.fromIntent(intent)?.also { request ->
                takePersistablePermissions(request)
                shareLaunchViewModel.accept(request)
            }
        }
        EngineWarmup.preWarm(applicationContext)
        enableEdgeToEdge()

        setContent {
            val sharedLaunchRequest by shareLaunchViewModel.pendingRequest.collectAsStateWithLifecycle()
            val startDestination = remember {
                if (deps.settingsRepository.isOnboardingCompleted()) {
                    com.docforge.app.navigation.Routes.HOME
                } else {
                    com.docforge.app.navigation.Routes.ONBOARDING
                }
            }
            DocForgeTheme {
                DocForgeNavHost(
                    dependencies = deps,
                    startDestination = startDestination,
                    sharedLaunchRequest = sharedLaunchRequest,
                    onSharedLaunchHandled = { request -> shareLaunchViewModel.consume(request) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ShareIntentRouter.fromIntent(intent)?.also { request ->
            takePersistablePermissions(request)
            shareLaunchViewModel.accept(request)
        }
    }

    private fun takePersistablePermissions(request: ShareLaunchRequest) {
        request.uris.forEach { uri ->
            // Try read+write first, fall back to read-only
            val readWrite = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            val readOnly = Intent.FLAG_GRANT_READ_URI_PERMISSION
            runCatching {
                contentResolver.takePersistableUriPermission(uri, readWrite)
            }.recoverCatching {
                contentResolver.takePersistableUriPermission(uri, readOnly)
            }
        }
    }
}
