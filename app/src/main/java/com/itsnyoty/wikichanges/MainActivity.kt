package com.itsnyoty.wikichanges

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.animation.AnticipateInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.itsnyoty.wikichanges.data.auth.OAuthManager
import com.itsnyoty.wikichanges.data.model.UiState
import com.itsnyoty.wikichanges.ui.screens.WikiChangesApp
import com.itsnyoty.wikichanges.ui.theme.WikiChangesTheme
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Add exit animation
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val iconView = splashScreenView.iconView
            val view = splashScreenView.view

            iconView.animate()
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(500L)
                .setInterpolator(AnticipateInterpolator())
                .start()

            view.animate()
                .alpha(0f)
                .setDuration(500L)
                .setInterpolator(AnticipateInterpolator())
                .withEndAction { splashScreenView.remove() }
                .start()
        }

        handleOAuthRedirect(intent?.data)

        setContent {
            WikiChangesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WikiChangesApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        handleOAuthRedirect(intent?.data)
    }

    private fun handleOAuthRedirect(uri: Uri?) {
        Log.d("OAuthRedirect", "Got URI: $uri")
        if (uri == null) return

        // Check op ons custom scheme, ongeacht exact path
        if (uri.scheme != OAuthManager.REDIRECT_SCHEME) return

        Toast.makeText(this, "Redirect ontvangen: $uri", Toast.LENGTH_LONG).show()

        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")

        if (!error.isNullOrBlank()) {
            Toast.makeText(
                this,
                "OAuth fout: ${errorDescription ?: error}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        lifecycleScope.launch {
            val result = OAuthManager.getInstance(this@MainActivity).handleAuthorizationCode(code, state)
            when (result) {
                is UiState.Success -> Toast.makeText(this@MainActivity, result.data, Toast.LENGTH_SHORT).show()
                is UiState.Error -> Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }
}
