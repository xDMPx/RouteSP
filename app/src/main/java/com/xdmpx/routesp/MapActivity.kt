package com.xdmpx.routesp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.xdmpx.routesp.database.RouteDatabase
import com.xdmpx.routesp.database.entities.PointEntity
import com.xdmpx.routesp.database.entities.RouteEntity
import com.xdmpx.routesp.services.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask

class MapActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var mapController: MapController
    private lateinit var mLocationOverlay: MyLocationNewOverlay
    private lateinit var compassOverlay: CompassOverlay
    private lateinit var mLocationService: LocationService
    private lateinit var mServiceIntent: Intent

    private var lastSpeed = 0f
    private var routeLine = Polyline()
    private lateinit var updateTimer: Timer

    private var mBound: Boolean = false

    private var speedInKMH = true
    private var avgSpeed = false
    private var distanceInKMH = true

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as LocationService.LocalBinder
            mLocationService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        map = findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)

        mapController = map.controller as MapController
        mapController.setZoom(13.0)
        val startPoint = GeoPoint(52.22977, 21.01178)
        mapController.setCenter(startPoint)

        val gpsLocationProvider = object : GpsMyLocationProvider(this) {
            override fun onLocationChanged(locations: MutableList<Location>) {
                val location = locations.last()
                if (location.provider != "gps") {
                    super.onLocationChanged(locations)
                    return
                }

                lastSpeed = location.speed
                setSpeedMapText()
                //mapController.setCenter(GeoPoint(location.latitude, location.longitude))

                super.onLocationChanged(locations)
            }

            override fun onLocationChanged(location: Location) {
                if (location.provider != "gps") {
                    super.onLocationChanged(location)
                    return
                }

                lastSpeed = location.speed
                setSpeedMapText()
                //mapController.setCenter(GeoPoint(location.latitude, location.longitude))

                super.onLocationChanged(location)
            }
        }
        mLocationOverlay = MyLocationNewOverlay(gpsLocationProvider, map)
        mLocationOverlay.setDirectionIcon(
            (ResourcesCompat.getDrawable(
                map.context.resources,
                org.osmdroid.library.R.drawable.twotone_navigation_black_48,
                theme
            ) as BitmapDrawable).bitmap
        )
        mLocationOverlay.enableMyLocation()
        map.overlays.add(this.mLocationOverlay)

        compassOverlay = CompassOverlay(this, InternalCompassOrientationProvider(this), map)
        compassOverlay.enableCompass()
        map.overlays.add(compassOverlay)

        val rotationGestureOverlay = RotationGestureOverlay(map)
        rotationGestureOverlay.isEnabled
        map.setMultiTouchControls(true)
        map.overlays.add(rotationGestureOverlay)

        mLocationOverlay.runOnFirstFix {
            mLocationService = LocationService()
            mServiceIntent = Intent(this@MapActivity, mLocationService.javaClass).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
            startForegroundService(mServiceIntent)
            scheduleTimer()
        }
        mLocationOverlay.enableFollowLocation()

        map.overlays.add(routeLine)
    }

    override fun onResume() {
        super.onResume()
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume()

        if (mBound) scheduleTimer()
    }

    override fun onPause() {
        super.onPause()
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause()

        if (::updateTimer.isInitialized) updateTimer.cancel()
    }

    fun onOSMCopyrightNoticeClick(view: View) {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/copyright"))
        startActivity(browserIntent, null)
    }

    fun onSpeedClick(view: View) {
        speedInKMH = !speedInKMH
        setSpeedMapText()
    }

    fun onSpeedIconClick(view: View) {
        avgSpeed = !avgSpeed
        if (avgSpeed) {
            runOnUiThread {
                (this@MapActivity.findViewById(R.id.speedImageView) as ImageView).setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.rounded_avg_pace_24, theme)
                )
            }
        } else {
            (this@MapActivity.findViewById(R.id.speedImageView) as ImageView).setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_speed_24, theme)
            )
        }
    }

    private fun setSpeedMapText() {
        val speedText = when (avgSpeed) {
            true -> speedText(getAvgSpeed())
            false -> speedText(lastSpeed)
        }
        runOnUiThread {
            (this@MapActivity.findViewById(R.id.speedMapText) as TextView).text = speedText
        }
    }

    private fun getAvgSpeed(): Float {
        var timeDif = Calendar.getInstance().time.time - mLocationService.getStartDate().time
        timeDif /= 1000

        val avgSpeed = routeLine.distance / (timeDif.toDouble())
        return avgSpeed.toFloat()
    }

    private fun speedText(speedMS: Float): String {
        val speed = when (speedInKMH) {
            true -> speedMS * 3.6
            false -> speedMS
        }
        val speedText = when (speedInKMH) {
            true -> String.format("%.2f km/h", speed)
            false -> String.format("%.2f m/s", speed)
        }

        return speedText
    }

    fun onDistanceClick(view: View) {
        distanceInKMH = !distanceInKMH
        runOnUiThread {
            val distanceText = when (distanceInKMH) {
                true -> String.format("%.2f km", routeLine.distance / 1000f)
                false -> String.format("%.2f m", routeLine.distance)
            }
            (this@MapActivity.findViewById(R.id.distanceMapText) as TextView).text = distanceText
        }
    }

    fun onFollowLocationClick(view: View) {
        mLocationOverlay.enableFollowLocation()
    }

    private fun scheduleTimer() {
        updateTimer = Timer()
        updateTimer.schedule(
            object : TimerTask() {
                override fun run() {
                    runOnUiThread(object : TimerTask() {
                        override fun run() {
                            val recordedGeoPoints = mLocationService.getRecordedGeoPoints()
                            if (recordedGeoPoints.isNotEmpty()) {
                                routeLine.setPoints(recordedGeoPoints)
                                val distanceText = when (distanceInKMH) {
                                    true -> String.format("%.2f km", routeLine.distance / 1000f)
                                    false -> String.format("%.2f m", routeLine.distance)
                                }
                                var timeDif =
                                    Calendar.getInstance().time.time - mLocationService.getStartDate().time
                                timeDif /= 1000

                                setSpeedMapText()
                                runOnUiThread {
                                    (this@MapActivity.findViewById(R.id.distanceMapText) as TextView).text =
                                        distanceText
                                    (this@MapActivity.findViewById(R.id.timeMapText) as TextView).text =
                                        convertSecondsToHMmSs(timeDif)
                                }
                                map.invalidate()
                            }
                        }
                    })
                }
            }, 0, 500
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mBound) {
            mBound = false
            updateTimer.cancel()
            unbindService(connection)
            if (::mServiceIntent.isInitialized) mLocationService.stopService(mServiceIntent)
        }

        val scope = CoroutineScope(Dispatchers.IO)
        val recordedGeoPoints = mLocationService.getRecordedGeoPoints()
        val routeDBDao = RouteDatabase.getInstance(this).routeDatabaseDao
        scope.launch {
            routeDBDao.insertRoute(RouteEntity())
            val latRouteID = routeDBDao.getLastRouteID()
            if (latRouteID != null) {
                recordedGeoPoints.forEach {
                    routeDBDao.insertPoint(
                        PointEntity(
                            routeID = latRouteID, latitude = it.latitude, longitude = it.longitude
                        )
                    )
                }
            }
            Log.d("Map onDestroy", "SAVED: ${recordedGeoPoints.size}")
        }
    }

    private fun convertSecondsToHMmSs(seconds: Long): String {
        val s = seconds % 60
        val m = seconds / 60 % 60
        val h = seconds / (60 * 60) % 24
        return String.format("%dh %dm %ds", h, m, s)
    }

}