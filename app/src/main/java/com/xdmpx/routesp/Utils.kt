package com.xdmpx.routesp

object Utils {

    fun convertSecondsToHMmSs(seconds: Long): String {
        val s = seconds % 60
        val m = seconds / 60 % 60
        val h = seconds / (60 * 60) % 24
        return String.format("%dh %dm %ds", h, m, s)
    }

}