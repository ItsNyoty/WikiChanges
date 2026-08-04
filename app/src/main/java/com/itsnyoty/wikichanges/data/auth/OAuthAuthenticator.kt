package com.itsnyoty.wikichanges.data.auth

import android.content.Context
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class OAuthAuthenticator(context: Context) : Authenticator {
    private val oAuthManager = OAuthManager.getInstance(context)

    override fun authenticate(route: Route?, response: Response): Request? {
        // Alleen proberen te refreshen als de vorige poging geen 'Authorization' header had
        // of als de token in het geheugen inmiddels anders is dan die in de aanvraag.
        
        synchronized(this) {
            val currentToken = com.itsnyoty.wikichanges.data.api.OAuthAccessTokenHolder.token
            val requestToken = response.request.header("Authorization")?.substringAfter("Bearer ")

            // Als de token inmiddels is bijgewerkt door een andere thread, retry met de nieuwe token
            if (currentToken != null && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            // Anders: probeer de token te refreshen (blokkerend, want OkHttp verwacht een resultaat)
            val newToken = runBlocking {
                oAuthManager.refreshToken()
            }

            if (newToken != null) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }
        }

        return null // Geef op als refresh niet lukt
    }
}
