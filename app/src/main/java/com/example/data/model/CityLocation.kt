package com.example.data.model

data class CityLocation(
  val name: String,
  val country: String,
  val latitude: Double,
  val longitude: Double,
  val timeZoneId: String
) {
  val displayName: String get() = "$name, $country"

  companion object {
    val PRESET_CITIES = PresetCities.ALL_CITIES

    val DEFAULT_CITY = PRESET_CITIES[0] // Makkah
  }
}
