package com.xdmpx.routesp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.xdmpx.routesp.Utils.convertSecondsToHMmSs
import com.xdmpx.routesp.database.RouteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.util.Calendar

class RecordedRouteDetailsActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private val routeLine = Polyline()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recorded_route_details)

        val routeID = intent.extras!!.getInt("routeID")

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        map = findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)

        val routeDBDao = RouteDatabase.getInstance(this).routeDatabaseDao
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val routeWithPoints = routeDBDao.getRouteWithPoints(routeID)!!
            val route = routeWithPoints.route

            val distance = String.format("%.2f km", route.distanceInM / 1000f)
            var timeDif = route.endDate - route.startDate
            timeDif /= 1000

            val points = routeWithPoints.points.map { point ->
                GeoPoint(
                    point.latitude, point.longitude
                )
            }
            routeLine.setPoints(points)

            runOnUiThread {
                (this@RecordedRouteDetailsActivity.findViewById(R.id.timeMapText) as TextView).text =
                    convertSecondsToHMmSs(timeDif)
                (this@RecordedRouteDetailsActivity.findViewById(R.id.distanceMapText) as TextView).text =
                    distance

                val mapController = map.controller
                mapController.setZoom(13.0)
                mapController.setCenter(points[0])
            }
        }

        map.overlays.add(routeLine)
    }

    override fun onResume() {
        super.onResume()
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause()
    }

}