package com.itsnyoty.wikichanges.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.res.painterResource
import com.itsnyoty.wikichanges.R
import com.itsnyoty.wikichanges.data.auth.OAuthManager
import androidx.core.net.toUri
import kotlinx.coroutines.launch

@Composable
fun LandingScreen(
    onSkip: () -> Unit,
    onAuthenticated: () -> Unit,
    onNavigateToDeveloperSettings: () -> Unit,
    oAuthManager: OAuthManager = OAuthManager.getInstance(LocalContext.current)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val accessToken by oAuthManager.accessToken.collectAsState(initial = null)
    LaunchedEffect(accessToken) {
        if (!accessToken.isNullOrBlank()) onAuthenticated()
    }

    var logoClicks by remember { mutableStateOf(0) }

    // Wikimedia-groene tint
    val wikimediaGreen = Color(0xFF00695C)
    val wikimediaDarkGreen = Color(0xFF004D40)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(wikimediaGreen, wikimediaDarkGreen)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_favicon),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        logoClicks++
                        if (logoClicks >= 10) {
                            logoClicks = 0
                            onNavigateToDeveloperSettings()
                        }
                    }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name),
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.landing_subtitle),
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val url = oAuthManager.buildAuthorizationUrl()
                            val intent = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                            intent.launchUrl(context, url.toUri())
                        } catch (e: Exception) {
                            errorMessage = context.getString(R.string.login_error_prefix, e.localizedMessage)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = wikimediaGreen,
                    disabledContainerColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = wikimediaGreen,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        stringResource(R.string.login_with_wikimedia),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onSkip,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) {
                Text(stringResource(R.string.skip_to_viewer), fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.created_by_disclaimer),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = it,
                    color = Color(0xFFFFCDD2),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
