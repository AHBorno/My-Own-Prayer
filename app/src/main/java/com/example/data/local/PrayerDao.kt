package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {
  @Query("SELECT * FROM daily_prayer_times WHERE date = :date LIMIT 1")
  fun getPrayerTimesForDate(date: String): Flow<PrayerEntity?>

  @Query("SELECT * FROM daily_prayer_times WHERE date = :date LIMIT 1")
  suspend fun getPrayerTimesForDateSync(date: String): PrayerEntity?

  @Query("SELECT * FROM daily_prayer_times WHERE date <= :currentDate ORDER BY date DESC LIMIT 1")
  fun getLatestPrayerTimesOnOrBefore(currentDate: String): Flow<PrayerEntity?>

  @Query("SELECT * FROM daily_prayer_times WHERE date <= :currentDate ORDER BY date DESC LIMIT 1")
  suspend fun getLatestPrayerTimesOnOrBeforeSync(currentDate: String): PrayerEntity?

  @Query("SELECT COUNT(*) FROM daily_prayer_times WHERE date >= :fromDate")
  suspend fun countFutureCachedDays(fromDate: String): Int

  @Query("SELECT * FROM daily_prayer_times ORDER BY date DESC LIMIT 1")
  fun getLatestPrayerTimes(): Flow<PrayerEntity?>

  @Query("SELECT * FROM daily_prayer_times ORDER BY date DESC LIMIT 1")
  suspend fun getLatestPrayerTimesSync(): PrayerEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPrayerTimes(prayerEntity: PrayerEntity)

  @Query("DELETE FROM daily_prayer_times WHERE date < :beforeDate")
  suspend fun deleteOldPrayers(beforeDate: String)
}
