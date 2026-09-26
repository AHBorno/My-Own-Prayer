package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AladhanApiResponse(
  val code: Int,
  val status: String,
  val data: AladhanData?
)

@JsonClass(generateAdapter = true)
data class AladhanData(
  val timings: AladhanTimings?,
  val date: AladhanDate?
)

@JsonClass(generateAdapter = true)
data class AladhanTimings(
  @Json(name = "Fajr") val fajr: String?,
  @Json(name = "Sunrise") val sunrise: String?,
  @Json(name = "Dhuhr") val dhuhr: String?,
  @Json(name = "Asr") val asr: String?,
  @Json(name = "Maghrib") val maghrib: String?,
  @Json(name = "Isha") val isha: String?
)

@JsonClass(generateAdapter = true)
data class AladhanDate(
  val readable: String?,
  val timestamp: String?
)

interface PrayerApiService {
  @GET("v1/timingsByCity")
  suspend fun getTimingsByCity(
    @Query("city") city: String,
    @Query("country") country: String,
    @Query("method") method: Int = 2 // 2 = Islamic Society of North America (ISNA), 3 = Muslim World League, 4 = Umm Al-Qura
  ): AladhanApiResponse

  @GET("v1/timings/{timestamp}")
  suspend fun getTimingsByCoordinates(
    @Path("timestamp") timestamp: Long,
    @Query("latitude") latitude: Double,
    @Query("longitude") longitude: Double,
    @Query("method") method: Int = 2
  ): AladhanApiResponse

  companion object {
    private const val BASE_URL = "https://api.aladhan.com/"

    fun getAladhanMethodForCountry(country: String): Int {
      val c = country.trim().lowercase(java.util.Locale.US)
      return when {
        c.contains("bangladesh") || c.contains("pakistan") || c.contains("india") ||
          c.contains("afghanistan") || c.contains("sri lanka") || c.contains("nepal") -> 1 // Karachi
        c.contains("saudi") -> 4 // Umm Al-Qura
        c.contains("egypt") || c.contains("morocco") || c.contains("algeria") ||
          c.contains("tunisia") || c.contains("libya") || c.contains("sudan") ||
          c.contains("yemen") || c.contains("syria") || c.contains("lebanon") ||
          c.contains("jordan") || c.contains("palestine") || c.contains("iraq") -> 5 // Egyptian General Authority
        c.contains("emirates") || c.contains("dubai") || c.contains("uae") ||
          c.contains("qatar") || c.contains("kuwait") || c.contains("oman") -> 8 // Gulf
        c.contains("turkey") -> 13 // Diyanet
        c.contains("singapore") || c.contains("malaysia") || c.contains("indonesia") ||
          c.contains("brunei") -> 11 // Singapore / Majlis Ugama Islam
        c.contains("united states") || c.contains("usa") || c.contains("canada") -> 2 // ISNA
        else -> 3 // Muslim World League
      }
    }

    fun create(): PrayerApiService {
      val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

      val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

      return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(PrayerApiService::class.java)
    }
  }
}
