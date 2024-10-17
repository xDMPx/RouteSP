package com.xdmpx.routesp.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.xdmpx.routesp.R
import com.xdmpx.routesp.utils.Utils
import com.xdmpx.routesp.utils.Utils.convertSecondsToHMmSs
import org.osmdroid.util.GeoPoint
import java.util.Calendar
import java.util.Date

data class KilometerPoint(
    val pointIndex: Int, val date: Date
)

data class Pause(
    val startDate: Date, val endDate: Date
)

class LocationService : Service() {

    private val NOTIFICATION_CHANNEL_ID = "com.xdmpx.routesp"
    private val NOTIFICATION_ID = 69420

    private var recordedGeoPoints: ArrayList<GeoPoint> = ArrayList()
    private var recordedAltitudes: ArrayList<Double> = ArrayList()
    private var recordedKilometerPoints: ArrayList<KilometerPoint> = ArrayList()
    private var distance: Double = 0.0
    private var totalDistance: Double = 0.0
    private lateinit var startDate: Date
    private var paused = false
    private var pauses: ArrayList<Pause> = ArrayList()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var latestLocationTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChanel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (!::fusedLocationClient.isInitialized) {
            startDate = Calendar.getInstance().time
            latestLocationTime = startDate.time
            setRequestLocationUpdates()
        }
        return Service.START_STICKY
    }

    private val localBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    override fun onBind(intent: Intent?): IBinder {
        return localBinder
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    private fun setRequestLocationUpdates() {
        val locationRequest =
            LocationRequest.Builder(2500).setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setWaitForAccurateLocation(true).setGranularity(Granularity.GRANULARITY_FINE)
                .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (paused) return

                val locations =
                    locationResult.locations.filter { location -> location.accuracy <= 35.0f }

                for (location in locations) {
                    if (location.time < latestLocationTime) continue
                    latestLocationTime = location.time

                    val newGeoPoint = GeoPoint(location.latitude, location.longitude)
                    Log.d(
                        "LocationService",
                        "Location Updated: $location altitude: ${location.altitude}"
                    )
                    if (recordedGeoPoints.size >= 1) {
                        val diff = newGeoPoint.distanceToAsDouble(recordedGeoPoints.last())
                        distance += diff
                        totalDistance += diff
                    }
                    if (distance.toInt() / 1000 == 1) {
                        distance = 0.0
                        recordedKilometerPoints.add(
                            KilometerPoint(
                                recordedGeoPoints.lastIndex, Calendar.getInstance().time
                            )
                        )
                    }

                    recordedGeoPoints.add(newGeoPoint)
                    recordedAltitudes.add(location.altitude)

                    val timeInS = Utils.calculateTimeDiffS(
                        getStartDate(), Calendar.getInstance().time, pauses.toTypedArray()
                    )
                    val distanceText = Utils.distanceText(totalDistance, true)
                    val speedText = Utils.speedText(location.speed.toDouble(), true)
                    updateNotification(speedText, distanceText, convertSecondsToHMmSs(timeInS))
                }
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    private fun createNotificationChanel() {
        val channelName = getString(R.string.notification_channel_name)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification: Notification =
            notificationBuilder.setOngoing(true).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notification_content_title))
                .setCategory(Notification.CATEGORY_NAVIGATION)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setOnlyAlertOnce(true).build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(speed: String, distance: String, time: String) {
        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification: Notification =
            notificationBuilder.setOngoing(true).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notification_content_title))
                .setContentText("V: $speed S: $distance T: $time")
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setOnlyAlertOnce(true).build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    fun pause() {
        paused = true
        pauses.add(Pause(Calendar.getInstance().time, Calendar.getInstance().time))

        val timeInS = Utils.calculateTimeDiffS(
            getStartDate(), Calendar.getInstance().time, pauses.toTypedArray()
        )
        val distanceText = Utils.distanceText(distance, true)
        val speedText = Utils.speedText(0.0, true)
        updateNotification(speedText, distanceText, convertSecondsToHMmSs(timeInS))
    }

    fun resume() {
        paused = false
        pauses[pauses.lastIndex] = Pause(pauses.last().startDate, Calendar.getInstance().time)
    }

    fun getRecordedGeoPoints(): ArrayList<GeoPoint> {
        return recordedGeoPoints
    }

    fun getStartDate(): Date {
        return startDate
    }

    fun getRecordedGeoPointsArray(): Array<GeoPoint> {
        return recordedGeoPoints.toTypedArray()
    }

    fun getRecordedAltitudesArray(): Array<Double> {
        return recordedAltitudes.toTypedArray()
    }

    fun getRecordedKilometerPoints(): Array<KilometerPoint> {
        return recordedKilometerPoints.toTypedArray()
    }

    fun getPausesArray(): Array<Pause> {
        return pauses.toTypedArray()
    }

}
