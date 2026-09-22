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
