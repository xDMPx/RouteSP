package com.xdmpx.routesp.recorded_route_details

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.xdmpx.routesp.R
import com.xdmpx.routesp.Utils

const val ARG_DISTANCE_IN_M = "distance_in_m"
const val ARG_TIME_IN_S = "time_in_s"
const val ARG_AVG_SPEED_KMH = "avg_speed_kmh"

class RecordedRouteDetailsFragment : Fragment() {
    private var distanceInM = 0.0
    private var timeInS = 0L
    private var avgSpeedKMH = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            distanceInM = it.getDouble(ARG_DISTANCE_IN_M)
            timeInS = it.getLong(ARG_TIME_IN_S)
            avgSpeedKMH = it.getDouble(ARG_AVG_SPEED_KMH)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recorded_route_details, container, false)

        (view.findViewById(R.id.timeMapText) as TextView).text =
            Utils.convertSecondsToHMmSs(timeInS)
        val distance = String.format("%.2f km", distanceInM / 1000f)
        (view.findViewById(R.id.distanceMapText) as TextView).text = distance
        (view.findViewById(R.id.avgSpeedMapText) as TextView).text =
            String.format("%.2f km/h", avgSpeedKMH)

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(distanceInM: Double, timeInS: Long, avgSpeedKMH: Double) =
            RecordedRouteDetailsFragment().apply {
                arguments = Bundle().apply {
                    putDouble(ARG_DISTANCE_IN_M, distanceInM)
                    putLong(ARG_TIME_IN_S, timeInS)
                    putDouble(ARG_AVG_SPEED_KMH, avgSpeedKMH)
                }
            }
    }
}