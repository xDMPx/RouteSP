package com.xdmpx.routesp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.xdmpx.routesp.database.RouteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

class RecordedRouteDetailsActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private val routeLine = Polyline()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recorded_route_details)

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        map = findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)

        val routeDBDao = RouteDatabase.getInstance(this).routeDatabaseDao
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            routeDBDao.getLastRouteID()?.let {
                val points = routeDBDao.getRouteWithPoints(it)?.points?.map { point ->
                    GeoPoint(
                        point.latitude, point.longitude
                    )
                }
                routeLine.setPoints(points)

                runOnUiThread {
                    val mapController = map.controller
                    mapController.setZoom(13.0)
                    mapController.setCenter(points?.get(0) ?: GeoPoint(52.22977, 21.01178))
                }
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