package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object QiblaHelper {
  const val KAABA_LAT = 21.422487
  const val KAABA_LNG = 39.826206

  /**
   * Calculates the exact Great Circle bearing from the user's location to the Kaaba in Makkah.
   * Returns degrees [0..360) clockwise from True North.
   */
  fun calculateQiblaBearing(userLat: Double, userLng: Double): Float {
    val kaabaLatRad = Math.toRadians(KAABA_LAT)
    val kaabaLngRad = Math.toRadians(KAABA_LNG)
    val userLatRad = Math.toRadians(userLat)
    val userLngRad = Math.toRadians(userLng)

    val deltaLng = kaabaLngRad - userLngRad
    val y = sin(deltaLng) * cos(kaabaLatRad)
    val x = cos(userLatRad) * sin(kaabaLatRad) - sin(userLatRad) * cos(kaabaLatRad) * cos(deltaLng)

    val bearingRad = atan2(y, x)
    val bearingDeg = Math.toDegrees(bearingRad)
    return ((bearingDeg + 360.0) % 360.0).toFloat()
  }

  /**
   * Calculates distance to the Kaaba in kilometers using the Haversine formula.
   */
  fun calculateDistanceKm(userLat: Double, userLng: Double): Int {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(KAABA_LAT - userLat)
    val dLon = Math.toRadians(KAABA_LNG - userLng)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(userLat)) * cos(Math.toRadians(KAABA_LAT)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (r * c).toInt()
  }

  /**
   * Returns a Flow of device azimuth (compass heading in degrees 0..360 from North).
   */
  fun getCompassHeadingFlow(context: Context): Flow<Float> = callbackFlow {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    if (sensorManager == null) {
      trySend(0f)
      close()
      return@callbackFlow
    }

    val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val rotationMatrix = FloatArray(9)
    val orientationAngles = FloatArray(3)

    var lastGravity = FloatArray(3)
    var lastGeomagnetic = FloatArray(3)
    var haveGravity = false
    var haveGeomagnetic = false

    val listener = object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
          SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
          SensorManager.getOrientation(rotationMatrix, orientationAngles)
          val azimuthRad = orientationAngles[0]
          val azimuthDeg = (Math.toDegrees(azimuthRad.toDouble()) + 360.0) % 360.0
          trySend(azimuthDeg.toFloat())
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
          System.arraycopy(event.values, 0, lastGravity, 0, 3)
          haveGravity = true
          computeFallback()
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
          System.arraycopy(event.values, 0, lastGeomagnetic, 0, 3)
          haveGeomagnetic = true
          computeFallback()
        }
      }

      private fun computeFallback() {
        if (haveGravity && haveGeomagnetic) {
          if (SensorManager.getRotationMatrix(rotationMatrix, null, lastGravity, lastGeomagnetic)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthRad = orientationAngles[0]
            val azimuthDeg = (Math.toDegrees(azimuthRad.toDouble()) + 360.0) % 360.0
            trySend(azimuthDeg.toFloat())
          }
        }
      }

      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    if (rotationSensor != null) {
      sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
    } else {
      if (accelSensor != null) sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
      if (magnetSensor != null) sensorManager.registerListener(listener, magnetSensor, SensorManager.SENSOR_DELAY_UI)
    }

    awaitClose {
      sensorManager.unregisterListener(listener)
    }
  }
}
