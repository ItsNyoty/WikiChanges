package com.itsnyoty.wikichanges.data.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object DebugSettings {
    private val _isDryModeEnabled = MutableStateFlow(false)
    val isDryModeEnabled = _isDryModeEnabled.asStateFlow()

    fun toggleDryMode() {
        _isDryModeEnabled.value = !_isDryModeEnabled.value
    }
}
