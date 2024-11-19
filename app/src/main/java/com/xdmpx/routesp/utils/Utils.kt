package com.xdmpx.routesp.utils

import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnDismissListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.xdmpx.routesp.R
import com.xdmpx.routesp.database.entities.PointEntity
import com.xdmpx.routesp.database.entities.PauseEntity
import com.xdmpx.routesp.database.entities.RouteEntity
import com.xdmpx.routesp.database.entities.KilometerPointEntity
import com.xdmpx.routesp.datastore.ThemeType
import com.xdmpx.routesp.services.Pause
import java.util.Date
import org.json.JSONArray
import org.json.JSONObject
import com.xdmpx.routesp.database.RouteDatabase
import android.net.Uri
import android.util.Log
import com.xdmpx.routesp.database.entities.Converters
import java.io.BufferedReader
import java.io.InputStreamReader

object Utils {

    fun convertSecondsToHMmSs(seconds: Long): String {
        val s = seconds % 60
        val m = seconds / 60 % 60
        val h = seconds / (60 * 60)
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

    fun syncThemeWithSettings(activity: AppCompatActivity) {
        val theme = com.xdmpx.routesp.settings.Settings.getInstance().settingsState.value.theme
        val usePureDark =
            com.xdmpx.routesp.settings.Settings.getInstance().settingsState.value.usePureDark
        if ((theme == ThemeType.DARK || (theme == ThemeType.SYSTEM && activity.resources.configuration.isNightModeActive)) && usePureDark) {
            activity.setTheme(R.style.Base_Theme_RouteSP_PureDark)
        }
        activity.runOnUiThread {
            when (theme) {
                ThemeType.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                ThemeType.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                ThemeType.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                ThemeType.UNRECOGNIZED -> {}
            }
        }
    }

    private suspend fun routesDataToJsonArray(context: Context): JSONArray {
        val routeDBDao = RouteDatabase.getInstance(context).routeDatabaseDao
        val routesIDs = routeDBDao.getRoutes().map { route -> route.id }
        val jsonArray = JSONArray(routesIDs.map { routeID ->
            val routeWithPoints = routeDBDao.getRouteWithPoints(routeID)

            val points = routeWithPoints?.points
            val jsonPoints = points?.map { point ->
                val jsonObject = JSONObject()
                jsonObject.put("routeID", point.routeID)
                jsonObject.put("latitude", point.latitude)
                jsonObject.put("longitude", point.longitude)
                jsonObject.put("altitude", point.altitude)

                jsonObject
            }

            val kilometerPoints = routeDBDao.getRouteWithKilometerPoints(routeID)?.points
            val jsonKilometerPoints = kilometerPoints?.map { point ->
                val jsonObject = JSONObject()
                jsonObject.put("routeID", point.routeID)
                jsonObject.put("date", Converters().dateToTimestamp(point.date))

                jsonObject
            }

            val pauses = routeDBDao.getRouteWithPauses(routeID)?.pauses
            val jsonPauses = pauses?.map { pause ->
                val jsonObject = JSONObject()
                jsonObject.put("routeID", pause.routeID)
                jsonObject.put("pauseStart", Converters().dateToTimestamp(pause.pauseStart))
                jsonObject.put("pauseEnd", Converters().dateToTimestamp(pause.pauseEnd))

                jsonObject
            }

            val route = routeWithPoints?.route
            val jsonObject = JSONObject()
            jsonObject.put("id", route?.id)
            jsonObject.put("distanceInM", route?.distanceInM)
            jsonObject.put("startDate", Converters().dateToTimestamp(route?.startDate))
            jsonObject.put("endDate", Converters().dateToTimestamp(route?.endDate))
            jsonObject.put("points", JSONArray(jsonPoints))
            jsonObject.put("kilometerPoints", JSONArray(jsonKilometerPoints))
            jsonObject.put("pauses", JSONArray(jsonPauses))

            jsonObject
        })

        return jsonArray
    }

    suspend fun exportToJSON(context: Context, uri: Uri): Boolean {
        val jsonArray = routesDataToJsonArray(context)
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonArray.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun jsonArrayToJsonObjectArray(jsonArray: JSONArray): Array<JSONObject> {
        val points = Array<JSONObject>(jsonArray.length()) { it -> jsonArray.getJSONObject(it) }
        return points
    }

    private fun pointsJSONToPointsList(
        lastRouteID: Int, pointsJSON: JSONArray
    ): List<PointEntity> {
        val pointsArray = jsonArrayToJsonObjectArray(pointsJSON)
        val points = pointsArray.map {
            PointEntity(
                routeID = lastRouteID,
                latitude = it.getDouble("latitude"),
                longitude = it.getDouble("longitude"),
                altitude = it.getDouble("altitude")
            )
        }

        return points
    }

    private fun kmpointsJSONToKMPointsList(
        lastRouteID: Int, kilometerPointsJSON: JSONArray
    ): List<KilometerPointEntity> {
        val pointsArray = jsonArrayToJsonObjectArray(kilometerPointsJSON)
        val points = pointsArray.map {
            KilometerPointEntity(
                0, routeID = lastRouteID, date = Converters().fromTimestamp(it.getLong("date"))!!
            )
        }

        return points
    }

    private fun pausesJSONToPausesList(
        lastRouteID: Int, pausesJSON: JSONArray
    ): List<PauseEntity> {
        val pausesArray = jsonArrayToJsonObjectArray(pausesJSON)
        val pauses = pausesArray.map {
            PauseEntity(
                0,
                routeID = lastRouteID,
                pauseStart = Converters().fromTimestamp(it.getLong("pauseStart"))!!,
                pauseEnd = Converters().fromTimestamp(it.getLong("pauseEnd"))!!
            )
        }

        return pauses
    }

    suspend fun importFromJSON(
        context: Context, uri: Uri, progressCallback: (progressStatus: String) -> Unit
    ): Boolean {
        try {
            val routeDBDao = RouteDatabase.getInstance(context).routeDatabaseDao
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val importedJson = JSONArray(bufferedReader.readText())
                inputStream.close()

                for (i in 0..<importedJson.length()) {
                    val routeJSON = importedJson.getJSONObject(i)

                    val distanceInM = routeJSON.getDouble("distanceInM")

                    val startDate = Converters().fromTimestamp(routeJSON.getLong("startDate"))!!
                    val endDate = Converters().fromTimestamp(routeJSON.getLong("endDate"))!!

                    routeDBDao.insertRoute(
                        RouteEntity(
                            distanceInM = distanceInM, startDate = startDate, endDate = endDate
                        )
                    )
                    val lastRouteID = routeDBDao.getLastRouteID()!!

                    val pointsArray =
                        pointsJSONToPointsList(lastRouteID, routeJSON.getJSONArray("points"))
                    routeDBDao.insertPoints(pointsArray)

                    val kilometerPointsArray = kmpointsJSONToKMPointsList(
                        lastRouteID, routeJSON.getJSONArray("kilometerPoints")
                    )
                    routeDBDao.insertKilometerPoints(kilometerPointsArray)

                    val pausesArray =
                        pausesJSONToPausesList(lastRouteID, routeJSON.getJSONArray("pauses"))
                    routeDBDao.insertPauses(pausesArray)

                    progressCallback.invoke("${i + 1}/${importedJson.length()}")
                }
            }
            return true
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error importing JSON: ${e.message}", e)
            return false
        }
    }

    fun ShortToast(context: Context, text: CharSequence) {
        Toast.makeText(
            context, text, Toast.LENGTH_SHORT
        ).show()
    }

    fun showAlertDialog(
        context: Context,
        title: String,
        message: String,
        buttonText: String,
        onDismissListener: OnDismissListener,
        onClickListener: DialogInterface.OnClickListener
    ) {
        val alertDialog = AlertDialog.Builder(context).create()
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setOnDismissListener(onDismissListener)
        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, buttonText, onClickListener)
        alertDialog.show()
    }

}
