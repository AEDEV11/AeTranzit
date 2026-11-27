package com.aedev.aetranzit.domain.sync

import kotlinx.coroutines.flow.StateFlow

interface ConnectivityObserver {
    val isOnline: StateFlow<Boolean>
    fun isCurrentlyOnline(): Boolean
}
