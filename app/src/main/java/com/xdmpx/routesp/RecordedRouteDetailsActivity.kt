package com.xdmpx.routesp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.add
import androidx.fragment.app.commit
import com.google.android.material.tabs.TabLayout
import com.xdmpx.routesp.database.RouteDatabase
import com.xdmpx.routesp.recorded_route_details.ARG_ROUTE_ID
import com.xdmpx.routesp.recorded_route_details.RecordedRouteDetailsFragment
import com.xdmpx.routesp.recorded_route_details.RecordedRouteMapFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar

class RecordedRouteDetailsActivity : AppCompatActivity() {

    private var distanceInM = 0.0
    private var timeInS = 0L
    private var avgSpeedKMH = 0.0
    private var routeID: Int = 0
    private var recordingDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        routeID = intent.extras!!.getInt("routeID")

        if (savedInstanceState == null) {
            val bundle = bundleOf(ARG_ROUTE_ID to routeID)
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<RecordedRouteMapFragment>(R.id.fragment_container_view, args = bundle)
            }
        } else {
            showRecordedRouteMapFragment()
        }

        setContentView(R.layout.activity_recorded_route_details)

        val routeDBDao = RouteDatabase.getInstance(this).routeDatabaseDao
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val routeWithPoints = routeDBDao.getRouteWithPoints(routeID)!!
            val route = routeWithPoints.route

            recordingDate = DateFormat.getDateTimeInstance().format(route.startDate)

            var timeDif = route.endDate.time - route.startDate.time
            timeDif /= 1000
            timeInS = timeDif
            distanceInM = route.distanceInM

            val avgSpeedMS = route.distanceInM / timeDif
            val avgSpeedKMH = avgSpeedMS * 3.6
            this@RecordedRouteDetailsActivity.avgSpeedKMH = avgSpeedKMH
        }

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> showRecordedRouteMapFragment()
                    1 -> showRecordedRouteDetailsFragment()
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })

    }

    private fun showRecordedRouteMapFragment() {
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container_view, RecordedRouteMapFragment.newInstance(routeID))
        ft.commit()
    }

    private fun showRecordedRouteDetailsFragment() {
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(
            R.id.fragment_container_view, RecordedRouteDetailsFragment.newInstance(
                recordingDate, distanceInM, timeInS, avgSpeedKMH
            )
        )
        ft.commit()
    }

}