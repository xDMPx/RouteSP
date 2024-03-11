package com.xdmpx.routesp.recorded_route_details

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.xdmpx.routesp.R
import com.xdmpx.routesp.utils.Utils

const val ARG_RECORDING_DATE = "startDateString"
const val ARG_DISTANCE_IN_M = "distance_in_m"
const val ARG_TIME_IN_S = "time_in_s"
const val ARG_AVG_SPEED_MS = "avg_speed_ms"
const val ARG_ALTITUDE_ARRAY = "avg_altitude_array"
const val ARG_SPEED_BY_KM = "avg_speed_by_km"
const val ARG_ACCURACY_ARRAY = "avg_accuracy_array"

class RecordedRouteDetailsFragment : Fragment() {
    private var recordingDate: String = ""
    private var distanceInM = 0.0
    private var timeInS = 0L
    private var avgSpeedMS = 0.0
    private var distanceInKM = true
    private var speedInKMH = true
    private lateinit var altitudeArray: DoubleArray
    private var speedsByKM: ArrayList<String> = ArrayList()
    private lateinit var accuracyArray: FloatArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recordingDate = it.getString(ARG_RECORDING_DATE)!!
            distanceInM = it.getDouble(ARG_DISTANCE_IN_M)
            timeInS = it.getLong(ARG_TIME_IN_S)
            avgSpeedMS = it.getDouble(ARG_AVG_SPEED_MS)
            altitudeArray = it.getDoubleArray(ARG_ALTITUDE_ARRAY)!!
            speedsByKM = it.getStringArrayList(ARG_SPEED_BY_KM)!!
            accuracyArray = it.getFloatArray(ARG_ACCURACY_ARRAY)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recorded_route_details, container, false)

        (view.findViewById<TextView>(R.id.dateValueView)).text = recordingDate
        (view.findViewById<TextView>(R.id.durationValueView)).text =
            Utils.convertSecondsToHMmSs(timeInS)

        val distance = Utils.distanceText(distanceInM, distanceInKM)
        val distanceValueView = view.findViewById<TextView>(R.id.distanceValueView)
        distanceValueView.text = distance
        distanceValueView.setOnClickListener { view -> onDistanceClick(view) }

        val avgSpeedValueView = view.findViewById<TextView>(R.id.avgSpeedValueView)
        avgSpeedValueView.text = Utils.speedText(avgSpeedMS, speedInKMH)
        avgSpeedValueView.setOnClickListener { view -> onAvgSpeedClick(view) }

        val minAltitudeText = String.format("min: %.2f m", altitudeArray.min())
        val maxAltitudeText = String.format("max: %.2f m", altitudeArray.max())
        val minAltitudeTextView = view.findViewById<TextView>(R.id.minAltitudeValueView)
        val maxAltitudeTextView = view.findViewById<TextView>(R.id.maxAltitudeValueView)
        minAltitudeTextView.text = minAltitudeText
        maxAltitudeTextView.text = maxAltitudeText

        val listView = view.findViewById<ListView>(R.id.routeList)
        listView.adapter = ArrayAdapter(
            view.context,
            R.layout.speed_list_item,
            R.id.itemTextView,
            speedsByKM as ArrayList<String>
        )
        val minAccuracyText = String.format("min: %.2f m", accuracyArray.min())
        val maxAccuracyText = String.format("max: %.2f m", accuracyArray.max())
        val minAccuracyTextView = view.findViewById<TextView>(R.id.minAccuracyValueView)
        val maxAccuracyTextView = view.findViewById<TextView>(R.id.maxAccuracyValueView)
        minAccuracyTextView.text = minAccuracyText
        maxAccuracyTextView.text = maxAccuracyText

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(
            recordingDate: String,
            distanceInM: Double,
            timeInS: Long,
            avgSpeedMS: Double,
            altitudeArray: DoubleArray,
            speedByKM: ArrayList<String>,
            accuracyArray: FloatArray
        ) = RecordedRouteDetailsFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_RECORDING_DATE, recordingDate)
                putDouble(ARG_DISTANCE_IN_M, distanceInM)
                putLong(ARG_TIME_IN_S, timeInS)
                putDouble(ARG_AVG_SPEED_MS, avgSpeedMS)
                putDoubleArray(ARG_ALTITUDE_ARRAY, altitudeArray)
                putStringArrayList(ARG_SPEED_BY_KM, speedByKM)
                putFloatArray(ARG_ACCURACY_ARRAY, accuracyArray)
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

    private fun onAvgSpeedClick(view: View) {
        speedInKMH = !speedInKMH
        view.findViewById<TextView>(R.id.avgSpeedValueView).text =
            Utils.speedText(avgSpeedMS, speedInKMH)
    }

}