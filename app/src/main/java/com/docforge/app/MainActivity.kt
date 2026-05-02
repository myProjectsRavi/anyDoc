package com.docforge.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.docforge.app.navigation.DocForgeNavHost
import com.docforge.app.runtime.EngineWarmup
import com.docforge.app.share.ShareIntentRouter
import com.docforge.app.share.ShareLaunchRequest
import com.docforge.core.ui.theme.DocForgeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val deps: AppDependencies get() = (application as DocForgeApp).dependencies
    private var sharedLaunchRequest by mutableStateOf<ShareLaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedLaunchRequest = ShareIntentRouter.fromIntent(intent)?.also { request ->
            takePersistablePermissions(request)
        }
        EngineWarmup.preWarm(applicationContext)
        enableEdgeToEdge()

        setContent {
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
                    onSharedLaunchHandled = { sharedLaunchRequest = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedLaunchRequest = ShareIntentRouter.fromIntent(intent)?.also { request ->
            takePersistablePermissions(request)
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
