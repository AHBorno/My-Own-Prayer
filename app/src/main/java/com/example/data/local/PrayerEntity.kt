package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_prayer_times")
data class PrayerEntity(
  @PrimaryKey
  val date: String, // e.g. "2026-09-22"
  val city: String,
  val country: String,
  val fajr: String,     // "05:12"
  val sunrise: String,  // "06:30"
  val dhuhr: String,    // "12:15"
  val asr: String,      // "15:40"
  val maghrib: String,  // "18:22"
  val isha: String,     // "19:35"
  val lastSyncedAt: Long = System.currentTimeMillis(),
  val syncSource: String = "Google & Aladhan API (Idle Sync)"
)
