package com.itsnyoty.wikichanges.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.itsnyoty.wikichanges.BuildConfig
import com.itsnyoty.wikichanges.data.api.OAuthAccessTokenHolder
import com.itsnyoty.wikichanges.data.model.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

class OAuthManager private constructor(private val context: Context) {

    private val dataStore = context.authDataStore

    companion object {
        @Volatile
        private var INSTANCE: OAuthManager? = null

        fun getInstance(context: Context): OAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        private const val AUTHORIZATION_URL =
            "https://meta.wikimedia.org/w/rest.php/oauth2/authorize"
        private const val TOKEN_URL =
            "https://meta.wikimedia.org/w/rest.php/oauth2/access_token"

        private const val SCOPE = "basic editpage createeditmovepage patrol rollback blockusers protect editprotected"

        val REDIRECT_URI: String
            get() = BuildConfig.WIKIMEDIA_OAUTH_REDIRECT_URI

        val REDIRECT_SCHEME: String
            get() = BuildConfig.WIKIMEDIA_OAUTH_REDIRECT_URI.substringBefore(":")
    }

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val CODE_VERIFIER = stringPreferencesKey("code_verifier")
    }

    val accessToken: Flow<String?> = dataStore.data.map { it[Keys.ACCESS_TOKEN] }

    suspend fun isAuthenticated(): Boolean = accessToken.first().isNullOrBlank().not()

    suspend fun buildAuthorizationUrl(): String {
        val verifier = generateCodeVerifier()
        val challenge = generateCodeChallenge(verifier)

        // Bewaar de verifier zodat we die bij de token exchange kunnen gebruiken
        dataStore.edit { it[Keys.CODE_VERIFIER] = verifier }

        val urlBuilder = AUTHORIZATION_URL.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("response_type", "code")
            ?.addQueryParameter("client_id", BuildConfig.WIKIMEDIA_OAUTH_CLIENT_ID)
            ?.addQueryParameter("redirect_uri", REDIRECT_URI)
            ?.addQueryParameter("scope", SCOPE)
            ?.addQueryParameter("state", generateState())
            ?.addQueryParameter("code_challenge", challenge)
            ?.addQueryParameter("code_challenge_method", "S256")

        val url = urlBuilder?.build()?.toString()
            ?: throw IllegalStateException("Kon geen autorisatie-URL bouwen")
        android.util.Log.d("OAuthAuthorizeUrl", "Authorization URL: $url")
        return url
    }

    suspend fun handleAuthorizationCode(code: String?, state: String?): UiState<String> {
        if (code.isNullOrBlank()) {
            return UiState.Error("Geen authorisatie-code ontvangen. Mogelijk is de toestemming geweigerd of is er een fout in de redirect.")
        }

        val verifier = dataStore.data.map { it[Keys.CODE_VERIFIER] }.first()
            ?: return UiState.Error("Geen code verifier gevonden. Probeer opnieuw in te loggen.")

        return withContext(Dispatchers.IO) {
            exchangeCode(code, verifier)
        }
    }

    private suspend fun exchangeCode(code: String, verifier: String): UiState<String> {
        val client = OkHttpClient()
        val body = FormBody.Builder().apply {
            add("grant_type", "authorization_code")
            add("client_id", BuildConfig.WIKIMEDIA_OAUTH_CLIENT_ID)
            if (BuildConfig.WIKIMEDIA_OAUTH_CLIENT_SECRET.isNotBlank()) {
                add("client_secret", BuildConfig.WIKIMEDIA_OAUTH_CLIENT_SECRET)
            }
            add("redirect_uri", REDIRECT_URI)
            add("code", code)
            add("code_verifier", verifier)
        }.build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(body)
            .header(
                "User-Agent",
                "WikiChanges/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID}; ${BuildConfig.CONTACT_EMAIL})"
            )
            .build()

        android.util.Log.d("OAuthTokenRequest", "POST $TOKEN_URL")

        return try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                android.util.Log.d("OAuthTokenResponse", "HTTP ${response.code}: $bodyString")
                if (!response.isSuccessful) {
                    return UiState.Error("Token-aanvraag mislukt (HTTP ${response.code}): $bodyString")
                }
                val json = JSONObject(bodyString)
                val accessToken = json.optString("access_token").takeIf { it.isNotBlank() }
                val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }

                if (!accessToken.isNullOrBlank()) {
                    dataStore.edit {
                        it[Keys.ACCESS_TOKEN] = accessToken
                        if (!refreshToken.isNullOrBlank()) it[Keys.REFRESH_TOKEN] = refreshToken
                    }
                    OAuthAccessTokenHolder.token = accessToken
                    UiState.Success("Succesvol ingelogd via Wikimedia")
                } else {
                    UiState.Error("Geen access token in reactie: $bodyString")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OAuthTokenError", "Fout bij token exchange", e)
            UiState.Error("Netwerkfout bij inloggen: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(Keys.ACCESS_TOKEN)
            it.remove(Keys.REFRESH_TOKEN)
            it.remove(Keys.CODE_VERIFIER)
        }
        OAuthAccessTokenHolder.token = null
    }

    suspend fun loadTokenIntoMemory() {
        OAuthAccessTokenHolder.token = accessToken.first()
    }

    suspend fun refreshToken(): String? {
        val refreshToken = dataStore.data.map { it[Keys.REFRESH_TOKEN] }.first()
        if (refreshToken.isNullOrBlank()) return null

        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val body = FormBody.Builder().apply {
                add("grant_type", "refresh_token")
                add("client_id", BuildConfig.WIKIMEDIA_OAUTH_CLIENT_ID)
                if (BuildConfig.WIKIMEDIA_OAUTH_CLIENT_SECRET.isNotBlank()) {
                    add("client_secret", BuildConfig.WIKIMEDIA_OAUTH_CLIENT_SECRET)
                }
                add("refresh_token", refreshToken)
            }.build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .header(
                    "User-Agent",
                    "WikiChanges/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID}; ${BuildConfig.CONTACT_EMAIL})"
                )
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        if (response.code == 400 || response.code == 401) {
                            // Refresh token possibly invalid/expired, clear login
                            clear()
                        }
                        return@withContext null
                    }
                    val json = JSONObject(bodyString)
                    val newAccessToken = json.optString("access_token").takeIf { it.isNotBlank() }
                    val newRefreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }

                    if (!newAccessToken.isNullOrBlank()) {
                        dataStore.edit {
                            it[Keys.ACCESS_TOKEN] = newAccessToken
                            if (!newRefreshToken.isNullOrBlank()) it[Keys.REFRESH_TOKEN] = newRefreshToken
                        }
                        OAuthAccessTokenHolder.token = newAccessToken
                        return@withContext newAccessToken
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("OAuthRefreshError", "Fout bij token refresh", e)
            }
            null
        }
    }

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return base64UrlEncodeNoPadding(bytes)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.UTF_8))
        return base64UrlEncodeNoPadding(digest)
    }

    private fun generateState(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return base64UrlEncodeNoPadding(bytes)
    }

    private fun base64UrlEncodeNoPadding(input: ByteArray): String {
        return Base64.encodeToString(
            input,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        ).trim()
    }
}
