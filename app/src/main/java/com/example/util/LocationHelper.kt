package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.example.data.model.CityLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone

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
        if (locationManager.isProviderEnabled(provider)) {
          val loc = locationManager.getLastKnownLocation(provider)
          if (loc != null) {
            if (bestLocation == null || loc.accuracy < bestLocation.accuracy || loc.time > bestLocation.time) {
              bestLocation = loc
            }
          }
        }
      }

      if (bestLocation == null) {
        Log.w(TAG, "No cached location found from providers")
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
}
