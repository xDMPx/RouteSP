package com.xdmpx.routesp.recorded_route_details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.xdmpx.routesp.BuildConfig
import com.xdmpx.routesp.R
import com.xdmpx.routesp.database.RouteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

const val ARG_ROUTE_ID = "routeID"

class RecordedRouteMapFragment : Fragment() {

    private var routeID: Int? = null

    private lateinit var map: MapView
    private val routeLine = Polyline()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            routeID = it.getInt(ARG_ROUTE_ID)
        }

        val activity = requireActivity()
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        Configuration.getInstance()
            .load(activity, PreferenceManager.getDefaultSharedPreferences(activity))

        val routeDBDao = RouteDatabase.getInstance(activity).routeDatabaseDao
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val routeWithPoints = routeDBDao.getRouteWithPoints(routeID!!)!!
            val points = routeWithPoints.points.map { point ->
                GeoPoint(
                    point.latitude, point.longitude
                )
            }
            routeLine.setPoints(points)

            activity.runOnUiThread {
                val mapController = map.controller
                mapController.setCenter(points[0])
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recorded_route_map, container, false)

        map = view.findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)

        val mapController = map.controller
        mapController.setZoom(13.0)
        map.overlays.add(routeLine)

        val rotationGestureOverlay = RotationGestureOverlay(map)
        rotationGestureOverlay.isEnabled
        map.setMultiTouchControls(true)
        map.overlays.add(rotationGestureOverlay)

        val osmCopyrightNotice = view.findViewById<TextView>(R.id.OSMCopyrightNotice)
        osmCopyrightNotice.setOnClickListener { view ->
            onOSMCopyrightNoticeClick(view)
        }

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(routeID: Int) = RecordedRouteMapFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_ROUTE_ID, routeID)
            }
        }
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

    private fun onOSMCopyrightNoticeClick(view: View) {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/copyright"))
        startActivity(browserIntent, null)
    }

}
