package com.xdmpx.routesp.utils

import com.xdmpx.routesp.database.entities.PauseEntity
import com.xdmpx.routesp.services.Pause
import java.util.Date

object Utils {

    fun convertSecondsToHMmSs(seconds: Long): String {
        val s = seconds % 60
        val m = seconds / 60 % 60
        val h = seconds / (60 * 60) % 24
        return String.format(null, "%dh %dm %ds", h, m, s)
    }

    fun calculateTimeDiffS(startDate: Date, endDate: Date): Long {
        var timeDif = endDate.time - startDate.time
        timeDif /= 1000
        return timeDif
    }

    fun calculateTimeDiffS(startDate: Date, endDate: Date, pauses: Array<Pause>): Long {
        val pauses = pauses.map {
            if (it.startDate != it.endDate) Pause(it.startDate, it.endDate) else Pause(
                it.startDate, endDate
            )
        }
        return calculateTimeDiff(startDate, endDate, pauses)
    }

    fun calculateTimeDiffS(startDate: Date, endDate: Date, pauses: List<PauseEntity>): Long {
        val pauses = pauses.map {
            if (it.pauseStart != it.pauseEnd) Pause(
                it.pauseStart, it.pauseEnd
            ) else Pause(it.pauseStart, endDate)
        }
        return calculateTimeDiff(startDate, endDate, pauses)
    }

    private fun calculateTimeDiff(startDate: Date, endDate: Date, pauses: List<Pause>): Long {
        var timeDif = endDate.time - startDate.time

        for (pause in pauses) {
            if (pause.endDate <= startDate || pause.startDate >= endDate) continue
            val pstart = if (pause.startDate.time > startDate.time) {
                pause.startDate.time
            } else {
                startDate.time
            }
            val pend = if (pause.endDate.time < endDate.time) {
                pause.endDate.time
            } else {
                endDate.time
            }
            val pauseDif = pend - pstart
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
            true -> String.format(null, "%.2f km", distanceInM / 1000f)
            false -> String.format(null, "%.2f m", distanceInM)
        }
    }

    fun speedText(speedMS: Double, speedInKMH: Boolean): String {
        val speed = when (speedInKMH) {
            true -> speedMS * 3.6
            false -> speedMS
        }
        val speedText = when (speedInKMH) {
            true -> String.format(null, "%.2f km/h", speed)
            false -> String.format(null, "%.2f m/s", speed)
        }

        return speedText
    }

}