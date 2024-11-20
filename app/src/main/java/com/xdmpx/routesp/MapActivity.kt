package com.xdmpx.routesp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.xdmpx.routesp.utils.Utils.convertSecondsToHMmSs
import com.xdmpx.routesp.database.RouteDatabase
import com.xdmpx.routesp.database.entities.KilometerPointEntity
import com.xdmpx.routesp.database.entities.PauseEntity
import com.xdmpx.routesp.database.entities.PointEntity
import com.xdmpx.routesp.database.entities.RouteEntity
import com.xdmpx.routesp.services.LocationService
import com.xdmpx.routesp.services.Pause
import com.xdmpx.routesp.utils.Utils
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
    private val DEBUG_TAG = "MapActivity"

    private lateinit var onBackPressedCallback: OnBackPressedCallback

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
    private var distanceInKM = true
    private var paused = false
    private var pauses: Array<Pause> = arrayOf()

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

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Hide the system bars.
        windowInsetsController.hide(systemBars())


        Utils.syncThemeWithSettings(this@MapActivity)

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
                setSpeedMapText(pauses)
                //mapController.setCenter(GeoPoint(location.latitude, location.longitude))

                super.onLocationChanged(locations)
            }

            override fun onLocationChanged(location: Location) {
                if (location.provider != "gps") {
                    super.onLocationChanged(location)
                    return
                }

                lastSpeed = location.speed

                setSpeedMapText(pauses)
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

        onBackPressedCallback = this.onBackPressedDispatcher.addCallback(this) {
            synchronized(this@MapActivity) {
                saveRecordedRoute()
            }
        }

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
        setSpeedMapText(pauses)
    }

    fun onSpeedIconClick(view: View) {
        avgSpeed = !avgSpeed
        if (avgSpeed) {
            runOnUiThread {
                (this@MapActivity.findViewById<ImageView>(R.id.speedImageView)).setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.rounded_avg_pace_24, theme)
                )
            }
        } else {
            (this@MapActivity.findViewById<ImageView>(R.id.speedImageView)).setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_baseline_speed_24, theme)
            )
        }
    }

    private fun setSpeedMapText(pauses: Array<Pause>) {
        val speedText = when (avgSpeed) {
            true -> Utils.speedText(calculateAvgSpeed(pauses), distanceInKM)
            false -> Utils.speedText(lastSpeed.toDouble(), speedInKMH)
        }
        runOnUiThread {
            (this@MapActivity.findViewById<TextView>(R.id.speedMapText)).text = speedText
        }
    }

    private fun calculateAvgSpeed(pauses: Array<Pause>): Double {
        val timeInS = Utils.calculateTimeDiffS(
            mLocationService.getStartDate(), Calendar.getInstance().time, pauses
        )
        val distanceInM = routeLine.distance

        return Utils.calculateAvgSpeedMS(distanceInM, timeInS)
    }

    fun onDistanceClick(view: View) {
        distanceInKM = !distanceInKM
        runOnUiThread {
            val distanceText = Utils.distanceText(routeLine.distance, distanceInKM)
            (this@MapActivity.findViewById<TextView>(R.id.distanceMapText)).text = distanceText
        }
    }

    fun onFollowLocationClick(view: View) {
        mLocationOverlay.enableFollowLocation()
    }

    fun onPauseClick(view: View) {
        paused = !paused
        if (paused) {
            runOnUiThread {
                (this@MapActivity.findViewById<FloatingActionButton>(R.id.facPause)).setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.round_pause_24, theme)
                )
            }
            mLocationService.pause()
        } else {
            (this@MapActivity.findViewById<ImageView>(R.id.facPause)).setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.round_play_arrow_24, theme)
            )
            mLocationService.resume()
        }
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
                                val distanceText =
                                    Utils.distanceText(routeLine.distance, distanceInKM)

                                pauses = mLocationService.getPausesArray()
                                val timeInS = Utils.calculateTimeDiffS(
                                    mLocationService.getStartDate(),
                                    Calendar.getInstance().time,
                                    pauses
                                )
                                setSpeedMapText(pauses)

                                runOnUiThread {
                                    (this@MapActivity.findViewById<TextView>(R.id.distanceMapText)).text =
                                        distanceText
                                    (this@MapActivity.findViewById<TextView>(R.id.timeMapText)).text =
                                        convertSecondsToHMmSs(timeInS)
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
        Log.d(DEBUG_TAG, "onDestroy")
    }

    private fun saveRecordedRoute() {
        this@MapActivity.onBackPressedCallback.isEnabled = false
        onBackPressedCallback = this.onBackPressedDispatcher.addCallback(this) {}

        val progressBarLinearLayout =
            this@MapActivity.findViewById<LinearLayout>(R.id.progressBarLinearLayout)
        progressBarLinearLayout.visibility = View.VISIBLE

        val endDate = Calendar.getInstance().time
        val startDate = mLocationService.getStartDate()
        val recordedGeoPoints = mLocationService.getRecordedGeoPointsArray()
        val recordedAltitudes = mLocationService.getRecordedAltitudesArray()
        val recordedKilometerPoints = mLocationService.getRecordedKilometerPoints()
        val pauses = mLocationService.getPausesArray()
        val routeDBDao = RouteDatabase.getInstance(this).routeDatabaseDao

        if (recordedGeoPoints.isEmpty()) {
            runOnUiThread {
                progressBarLinearLayout.visibility = View.GONE
                this@MapActivity.onBackPressedCallback.isEnabled = false
                this@MapActivity.onBackPressedDispatcher.onBackPressed()
            }
            return
        }

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val recordedRouteLine = Polyline()
            recordedRouteLine.setPoints(recordedGeoPoints.toList())

            val distanceInM = routeLine.distance

            routeDBDao.insertRoute(
                RouteEntity(
                    distanceInM = distanceInM, startDate = startDate, endDate = endDate
                )
            )
            val lastRouteID = routeDBDao.getLastRouteID()
            if (lastRouteID != null) {
                val points = recordedGeoPoints.zip(recordedAltitudes).map { (it, altitude) ->
                    PointEntity(
                        routeID = lastRouteID,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        altitude = altitude,
                    )
                }

                routeDBDao.insertPoints(points)

                val kmPoints = recordedKilometerPoints.map {
                    KilometerPointEntity(
                        0, lastRouteID, it.date
                    )
                }
                routeDBDao.insertKilometerPoints(kmPoints)

                val pauses = pauses.map {
                    PauseEntity(
                        0, lastRouteID, it.startDate, it.endDate
                    )
                }
                routeDBDao.insertPauses(pauses)

                Log.d(DEBUG_TAG, "Saved ${recordedGeoPoints.size}")
                Log.d(DEBUG_TAG, "Saved Pauses: ${pauses.size}")

                runOnUiThread {
                    progressBarLinearLayout.visibility = View.GONE
                    this@MapActivity.onBackPressedCallback.isEnabled = false
                    this@MapActivity.onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

}