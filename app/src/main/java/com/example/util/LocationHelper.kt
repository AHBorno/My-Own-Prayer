package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.example.data.model.CityLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

object LocationHelper {
  private const val TAG = "LocationHelper"

  @SuppressLint("MissingPermission")
  suspend fun getCurrentCityLocation(context: Context): CityLocation? = withContext(Dispatchers.IO) {
    try {
      val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return@withContext null

      // Check providers in order of precision and availability
      val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
      )

      var bestLocation: Location? = null
      for (provider in providers) {
        try {
          if (locationManager.isProviderEnabled(provider)) {
            val loc = locationManager.getLastKnownLocation(provider)
            if (loc != null) {
              if (bestLocation == null || loc.time > bestLocation.time) {
                bestLocation = loc
              }
            }
          }
        } catch (e: Exception) {
          Log.w(TAG, "Error checking provider $provider", e)
        }
      }

      // If no cached location or cached location is older than 30 minutes, actively request fresh GPS fix
      val isStale = bestLocation == null || (System.currentTimeMillis() - bestLocation.time > 30 * 60 * 1000)
      if (isStale) {
        val freshLocation = withTimeoutOrNull(5000L) {
          requestFreshLocation(locationManager)
        }
        if (freshLocation != null) {
          bestLocation = freshLocation
        }
      }

      if (bestLocation == null) {
        Log.w(TAG, "No cached or fresh location found from providers")
        return@withContext null
      }

      val lat = bestLocation.latitude
      val lng = bestLocation.longitude
      val timeZoneId = TimeZone.getDefault().id

      var cityName = "Detected Location"
      var countryName = "GPS"

      try {
        if (Geocoder.isPresent()) {
          val geocoder = Geocoder(context, Locale.getDefault())
          val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(lat, lng, 1)
          } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lng, 1)
          }

          if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            cityName = addr.locality
              ?: addr.subAdminArea
              ?: addr.adminArea
              ?: "Current City"
            countryName = addr.countryName ?: "Current Country"
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Geocoder reverse lookup failed, using coordinates", e)
        cityName = String.format(Locale.US, "Lat: %.2f, Lng: %.2f", lat, lng)
      }

      CityLocation(
        name = cityName,
        country = countryName,
        latitude = lat,
        longitude = lng,
        timeZoneId = timeZoneId
      )
    } catch (e: Exception) {
      Log.e(TAG, "Error getting location", e)
      null
    }
  }

  @SuppressLint("MissingPermission")
  private suspend fun requestFreshLocation(locationManager: LocationManager): Location? =
    suspendCancellableCoroutine { cont ->
      val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
          if (cont.isActive) {
            cont.resume(location)
          }
          try {
            locationManager.removeUpdates(this)
          } catch (_: Exception) {}
        }

        override fun onProviderDisabled(provider: String) {}
        override fun onProviderEnabled(provider: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
      }

      cont.invokeOnCancellation {
        try {
          locationManager.removeUpdates(listener)
        } catch (_: Exception) {}
      }

      var requested = false
      val looper = Looper.getMainLooper()
      for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
        try {
          if (locationManager.isProviderEnabled(provider)) {
            locationManager.requestLocationUpdates(provider, 0L, 0f, listener, looper)
            requested = true
          }
        } catch (e: Exception) {
          Log.w(TAG, "Could not request updates for $provider", e)
        }
      }

      if (!requested) {
        if (cont.isActive) cont.resume(null)
      }
    }
}
