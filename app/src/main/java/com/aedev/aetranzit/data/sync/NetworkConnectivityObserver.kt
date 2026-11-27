package com.aedev.aetranzit.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.aedev.aetranzit.domain.sync.ConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context
) : ConnectivityObserver {

    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)

    private val _isOnline = MutableStateFlow(queryActiveNetworkConnected())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = queryActiveNetworkConnected()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val validated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = if (validated) true else queryActiveNetworkConnected()
        }

        override fun onLost(network: Network) {
            _isOnline.value = queryActiveNetworkConnected()
        }

        override fun onUnavailable() {
            _isOnline.value = false
        }
    }

    init {
        runCatching {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        }
    }

    override fun isCurrentlyOnline(): Boolean {
        val current = queryActiveNetworkConnected()
        _isOnline.value = current
        return current
    }

    private fun queryActiveNetworkConnected(): Boolean {
        val cm = connectivityManager ?: return false
        return runCatching {
            val activeNetwork = cm.activeNetwork ?: return@runCatching false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return@runCatching false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }.getOrDefault(false)
    }
}
