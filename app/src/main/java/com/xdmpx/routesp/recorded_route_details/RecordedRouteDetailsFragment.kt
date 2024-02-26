package com.xdmpx.routesp.recorded_route_details

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.xdmpx.routesp.R
import com.xdmpx.routesp.utils.Utils

const val ARG_RECORDING_DATE = "startDateString"
const val ARG_DISTANCE_IN_M = "distance_in_m"
const val ARG_TIME_IN_S = "time_in_s"
const val ARG_AVG_SPEED_KMH = "avg_speed_kmh"

class RecordedRouteDetailsFragment : Fragment() {
    private var recordingDate: String = ""
    private var distanceInM = 0.0
    private var timeInS = 0L
    private var avgSpeedKMH = 0.0
    private var distanceInKM = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recordingDate = it.getString(ARG_RECORDING_DATE)!!
            distanceInM = it.getDouble(ARG_DISTANCE_IN_M)
            timeInS = it.getLong(ARG_TIME_IN_S)
            avgSpeedKMH = it.getDouble(ARG_AVG_SPEED_KMH)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recorded_route_details, container, false)

        (view.findViewById(R.id.dateValueView) as TextView).text = recordingDate
        (view.findViewById(R.id.durationValueView) as TextView).text =
            Utils.convertSecondsToHMmSs(timeInS)

        val distance = Utils.distanceText(distanceInM, distanceInKM)
        val distanceValueView = view.findViewById<TextView>(R.id.distanceValueView)
        distanceValueView.text = distance
        distanceValueView.setOnClickListener { view -> onDistanceClick(view) }

        (view.findViewById(R.id.avgSpeedValueView) as TextView).text =
            String.format("%.2f km/h", avgSpeedKMH)

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(
            recordingDate: String, distanceInM: Double, timeInS: Long, avgSpeedKMH: Double
        ) = RecordedRouteDetailsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_RECORDING_DATE, recordingDate)
                putDouble(ARG_DISTANCE_IN_M, distanceInM)
                putLong(ARG_TIME_IN_S, timeInS)
                putDouble(ARG_AVG_SPEED_KMH, avgSpeedKMH)
            }
        }
    }

    private fun onDistanceClick(view: View) {
        distanceInKM = !distanceInKM
        requireActivity().runOnUiThread {
            val distanceText = Utils.distanceText(distanceInM, distanceInKM)
            view.findViewById<TextView>(R.id.distanceValueView).text = distanceText
        }
    }

}