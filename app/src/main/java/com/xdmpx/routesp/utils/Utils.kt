package com.xdmpx.routesp.utils

import com.xdmpx.routesp.services.Pause
import java.util.Date

object Utils {

    fun convertSecondsToHMmSs(seconds: Long): String {
        val s = seconds % 60
        val m = seconds / 60 % 60
        val h = seconds / (60 * 60) % 24
        return String.format("%dh %dm %ds", h, m, s)
    }

    fun calculateTimeDiffS(startDate: Date, endDate: Date): Long {
        var timeDif = endDate.time - startDate.time
        timeDif /= 1000
        return timeDif
    }

    fun calculateTimeDiffS(startDate: Date, endDate: Date, pauses: Array<Pause>): Long {
        var timeDif = endDate.time - startDate.time
        for (pause in pauses) {
            var pauseDif = pause.endDate.time - pause.startDate.time
            if (pauseDif == 0L) pauseDif = endDate.time - pause.startDate.time
            timeDif -= pauseDif
        }
        timeDif /= 1000
        return timeDif
    }

    fun calculateAvgSpeedMS(distanceInM: Double, timeInS: Long): Double {
        return distanceInM / timeInS
    }

    fun distanceText(distanceInM: Double, distanceInKM: Boolean): String {
        return when (distanceInKM) {
            true -> String.format("%.2f km", distanceInM / 1000f)
            false -> String.format("%.2f m", distanceInM)
        }
    }

    fun speedText(speedMS: Double, speedInKMH: Boolean): String {
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

}