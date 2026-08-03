package com.itsnyoty.wikichanges.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class RecentChangesViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(RecentChangesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecentChangesViewModel(
                application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class SettingsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

object WikiChangesViewModelProvider {
    val recentChangesFactory = RecentChangesViewModelFactory()
    val settingsFactory = SettingsViewModelFactory()
}
