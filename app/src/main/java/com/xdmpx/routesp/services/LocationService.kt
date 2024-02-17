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
import org.osmdroid.util.GeoPoint
import java.util.Calendar
import java.util.Date

class LocationService : Service() {

    private var recordedGeoPoints: ArrayList<GeoPoint> = ArrayList()
    private lateinit var startDate: Date

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        createNotificationChanel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (!::fusedLocationClient.isInitialized) {
            startDate = Calendar.getInstance().time
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
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun setRequestLocationUpdates() {
        val locationRequest =
            LocationRequest.Builder(2500).setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setWaitForAccurateLocation(true).setGranularity(Granularity.GRANULARITY_FINE)
                .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                location?.let { location ->
                    val newGeoPoint = GeoPoint(location.latitude, location.longitude)
                    Log.d("LocationService", "Location Updated: $location")
                    recordedGeoPoints.add(newGeoPoint)
                }

            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }


    private fun createNotificationChanel() {
        val NOTIFICATION_CHANNEL_ID = "com.xdmpx.routesp"

        val channelName = "RouteSP LocationService"
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        val manager = (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification: Notification =
            notificationBuilder.setOngoing(true).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("RouteSP is running").setCategory(Notification.CATEGORY_NAVIGATION)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setOngoing(true).build()

        startForeground(2, notification)
    }

    fun getRecordedGeoPoints(): ArrayList<GeoPoint> {
        return recordedGeoPoints
    }

    fun getStartDate(): Date {
        return startDate
    }

}
