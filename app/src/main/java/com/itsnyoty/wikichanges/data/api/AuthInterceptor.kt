package com.itsnyoty.wikichanges.data.api

import com.itsnyoty.wikichanges.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header(
                "User-Agent",
                "WikiChanges/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID}; ${BuildConfig.CONTACT_EMAIL})"
            )
            .header("Accept", "application/json")

        OAuthAccessTokenHolder.token?.let { token ->
            builder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(builder.build())
    }
}

object OAuthAccessTokenHolder {
    @Volatile
    var token: String? = null
}
