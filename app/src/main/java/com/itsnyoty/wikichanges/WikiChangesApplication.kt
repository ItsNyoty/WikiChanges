package com.itsnyoty.wikichanges

import android.app.Application
import com.itsnyoty.wikichanges.data.auth.OAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WikiChangesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            OAuthManager.getInstance(this@WikiChangesApplication).loadTokenIntoMemory()
        }
    }
}
