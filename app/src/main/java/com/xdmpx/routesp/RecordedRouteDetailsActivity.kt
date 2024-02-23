package com.xdmpx.routesp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.xdmpx.routesp.Utils.convertSecondsToHMmSs
import com.xdmpx.routesp.database.RouteDatabase
import com.xdmpx.routesp.recorded_route_details.ROUTE_ID
import com.xdmpx.routesp.recorded_route_details.RecordedRouteMapFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecordedRouteDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val routeID = intent.extras!!.getInt("routeID")

        if (savedInstanceState == null) {
            val bundle = bundleOf(ROUTE_ID to routeID)
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<RecordedRouteMapFragment>(R.id.fragment_container_view, args = bundle)
            }
        }

        setContentView(R.layout.activity_recorded_route_details)

        val routeDBDao = RouteDatabase.getInstance(this).routeDatabaseDao
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val routeWithPoints = routeDBDao.getRouteWithPoints(routeID)!!
            val route = routeWithPoints.route

            val distance = String.format("%.2f km", route.distanceInM / 1000f)
            var timeDif = route.endDate - route.startDate
            timeDif /= 1000

            val avgSpeedMS = route.distanceInM / timeDif
            val avgSpeedKMH = avgSpeedMS * 3.6

            runOnUiThread {
                (this@RecordedRouteDetailsActivity.findViewById(R.id.timeMapText) as TextView).text =
                    convertSecondsToHMmSs(timeDif)
                (this@RecordedRouteDetailsActivity.findViewById(R.id.distanceMapText) as TextView).text =
                    distance
                (this@RecordedRouteDetailsActivity.findViewById(R.id.avgSpeedMapText) as TextView).text =
                    String.format("%.2f km/h", avgSpeedKMH)
            }
        }

    }

}