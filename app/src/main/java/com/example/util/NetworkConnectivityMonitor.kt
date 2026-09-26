package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Monitors network connectivity in real-time.
 * Immediately notifies subscribers and executes sync callbacks when internet access
 * is detected, ensuring users who are often offline receive timely prayer time updates.
 */
object NetworkConnectivityMonitor {
  private const val TAG = "NetworkMonitor"

  private val _isOnline = MutableStateFlow(true)
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  // Emits an event whenever the device regains internet connection after being offline
  private val _internetRestoredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val internetRestoredEvents: SharedFlow<Unit> = _internetRestoredEvents.asSharedFlow()

  private var isRegistered = false
  private var lastRestoredTimestamp = 0L

  fun isOnlineNow(context: Context): Boolean {
    return try {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
      val activeNetwork = cm.activeNetwork ?: return false
      val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (e: Exception) {
      true
    }
  }

  fun startMonitoring(
    context: Context,
    onInternetRestored: (suspend () -> Unit)? = null
  ) {
    val appContext = context.applicationContext
    val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

    val currentStatus = isOnlineNow(appContext)
    _isOnline.value = currentStatus

    if (isRegistered) return

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()

    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        Log.d(TAG, "Network available: Device has active connection")
        handleNetworkAvailability(appContext, onInternetRestored)
      }

      override fun onLost(network: Network) {
        Log.d(TAG, "Network lost: Device disconnected from internet")
        _isOnline.value = isOnlineNow(appContext)
      }

      override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (hasInternet && !_isOnline.value) {
          handleNetworkAvailability(appContext, onInternetRestored)
        } else {
          _isOnline.value = hasInternet
        }
      }
    }

    try {
      cm.registerNetworkCallback(request, callback)
      isRegistered = true
      Log.d(TAG, "Network connectivity monitor registered successfully")
    } catch (e: Exception) {
      Log.w(TAG, "Unable to register NetworkCallback: ${e.message}")
    }
  }

  private fun handleNetworkAvailability(
    context: Context,
    onInternetRestored: (suspend () -> Unit)?
  ) {
    val wasOffline = !_isOnline.value
    _isOnline.value = true

    val now = System.currentTimeMillis()
    // Debounce to prevent multiple immediate triggers when switching WiFi/Mobile
    if (wasOffline || (now - lastRestoredTimestamp > 10_000L)) {
      lastRestoredTimestamp = now
      Log.d(TAG, "Internet connection confirmed! Emitting internetRestored event...")
      _internetRestoredEvents.tryEmit(Unit)
      if (onInternetRestored != null) {
        CoroutineScope(Dispatchers.IO).launch {
          try {
            onInternetRestored.invoke()
          } catch (e: Exception) {
            Log.e(TAG, "Error in onInternetRestored listener", e)
          }
        }
      }
    }
  }
}
