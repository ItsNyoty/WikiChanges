package com.itsnyoty.wikichanges.data.api

import com.google.gson.GsonBuilder
import com.itsnyoty.wikichanges.data.auth.OAuthAuthenticator
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    private val cookieJar: CookieJar = JavaNetCookieJar(cookieManager)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private var okHttpClient: OkHttpClient? = null

    private fun getOkHttpClient(context: android.content.Context): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .addInterceptor(AuthInterceptor())
                .addInterceptor(loggingInterceptor)
                .authenticator(OAuthAuthenticator(context))
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .build().also { okHttpClient = it }
        }
    }

    fun createService(context: android.content.Context): WikipediaApiService {
        return Retrofit.Builder()
            .baseUrl("https://nl.wikipedia.org/")
            .client(getOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(WikipediaApiService::class.java)
    }

    fun clearCookies() {
        cookieManager.cookieStore.removeAll()
    }
}

