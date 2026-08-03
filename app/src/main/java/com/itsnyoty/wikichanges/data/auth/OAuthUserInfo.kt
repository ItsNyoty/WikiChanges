package com.itsnyoty.wikichanges.data.auth

import com.itsnyoty.wikichanges.data.model.UiState
import com.itsnyoty.wikichanges.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

suspend fun OAuthManager.fetchUserInfo(): UiState<UserInfo> {
    val token = accessToken.first() ?: return UiState.Error("Niet ingelogd")
    return fetchUserInfo(token)
}

private suspend fun OAuthManager.fetchUserInfo(token: String): UiState<UserInfo> {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://meta.wikimedia.org/w/rest.php/oauth2/resource/profile")
                .header("Authorization", "Bearer $token")
                .header(
                    "User-Agent",
                    "WikiChanges/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID}; ${BuildConfig.CONTACT_EMAIL})"
                )
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext UiState.Error("Profiel ophalen mislukt: ${response.code} $bodyString")
                }
                val json = JSONObject(bodyString)
                val username = json.optString("username").takeIf { it.isNotBlank() }
                val realName = json.optString("realname").takeIf { it.isNotBlank() }
                val email = json.optString("email").takeIf { it.isNotBlank() }
                val groups = json.optJSONArray("groups")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                }.orEmpty()

                UiState.Success(
                    UserInfo(
                        username = username ?: "Onbekend",
                        realName = realName,
                        email = email,
                        groups = groups
                    )
                )
            }
        } catch (e: Exception) {
            UiState.Error("Netwerkfout bij profiel ophalen: ${e.localizedMessage}")
        }
    }
}

data class UserInfo(
    val username: String,
    val realName: String?,
    val email: String?,
    val groups: List<String>
)
