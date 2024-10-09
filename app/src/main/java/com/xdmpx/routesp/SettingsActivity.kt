package com.xdmpx.routesp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.animation.AlphaAnimation
import android.widget.CheckBox
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.xdmpx.routesp.database.RouteDatabase
import com.xdmpx.routesp.settings.Settings
import com.xdmpx.routesp.ui.Setting
import com.xdmpx.routesp.ui.SettingButton
import com.xdmpx.routesp.ui.ThemeSelectorSetting
import com.xdmpx.routesp.utils.Utils
import com.xdmpx.routesp.utils.Utils.ShortToast
import com.xdmpx.routesp.utils.Utils.showAlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class SettingsActivity : AppCompatActivity() {

    private val DEBUG_TAG = "SettingsActivity"
    private val scopeIO = CoroutineScope(Dispatchers.IO)

    private val createDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            exportToJSONCallback(
                uri
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Utils.syncThemeWithSettings(this@SettingsActivity)

        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        setSupportActionBar(findViewById<Toolbar>(R.id.materialToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val themeSelectorSetting = findViewById<ThemeSelectorSetting>(R.id.themeSelectorSetting)
        themeSelectorSetting.setOnThemeUpdate {
            Utils.syncThemeWithSettings(this@SettingsActivity)
        }

        val usePureDarkSetting = findViewById<Setting>(R.id.usePureDarkSetting)
        val usePureDark = Settings.getInstance().settingsState.value.usePureDark
        usePureDarkSetting.findViewById<CheckBox>(R.id.settingCheckBox).isChecked = usePureDark
        usePureDarkSetting.setOnClickListener {
            Settings.getInstance().toggleUsePureDark()
            val usePureDark = Settings.getInstance().settingsState.value.usePureDark
            usePureDarkSetting.findViewById<CheckBox>(R.id.settingCheckBox).isChecked = usePureDark
            Utils.syncThemeWithSettings(this@SettingsActivity)
            recreate()
        }

        val buttonClick = AlphaAnimation(1f, 0.8f)

        val deleteAllSetting = findViewById<SettingButton>(R.id.deleteAllSetting)
        deleteAllSetting.isClickable = true
        deleteAllSetting.setOnClickListener {
            it.startAnimation(buttonClick)
            showAlertDialog(this@SettingsActivity,
                getString(R.string.settings_delete_all),
                getString(R.string.confirmation_delete_all),
                getString(R.string.delete),
                onDismissListener = {}) { _, _ ->
                scopeIO.launch {
                    val routeDBDao =
                        RouteDatabase.getInstance(this@SettingsActivity).routeDatabaseDao
                    routeDBDao.getRoutes().forEach { route ->
                        routeDBDao.deleteRoute(route)
                    }
                }
            }
        }

        val exportSetting = findViewById<SettingButton>(R.id.exportSetting)
        exportSetting.setOnClickListener {
            val date = LocalDate.now()
            val year = date.year
            val month = String.format(null, "%02d", date.monthValue)
            val day = date.dayOfMonth
            createDocument.launch("apks_export_${year}_${month}_$day.json")
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStop() {
        scopeIO.launch {
            Settings.getInstance().saveSettings(this@SettingsActivity)
        }
        super.onStop()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            true
        }

        else -> {
            // The user's action isn't recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun exportToJSONCallback(uri: Uri?) {
        if (uri == null) return

        scopeIO.launch {
            if (exportToJSON(uri)) {
                runOnUiThread {
                    ShortToast(
                        this@SettingsActivity, resources.getString(R.string.export_successful)
                    )
                }
            } else {
                runOnUiThread {
                    ShortToast(
                        this@SettingsActivity, resources.getString(R.string.export_failed)
                    )
                }
            }
        }
    }

    private suspend fun routesDataToJsonArray(): JSONArray {
        val routeDBDao = RouteDatabase.getInstance(this@SettingsActivity).routeDatabaseDao
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
                jsonObject.put("date", point.date)

                jsonObject
            }

            val pauses = routeDBDao.getRouteWithPauses(routeID)?.pauses
            val jsonPauses = pauses?.map { pause ->
                val jsonObject = JSONObject()
                jsonObject.put("routeID", pause.routeID)
                jsonObject.put("pauseStart", pause.pauseStart)
                jsonObject.put("pauseEnd", pause.pauseEnd)

                jsonObject
            }


            val route = routeWithPoints?.route
            val jsonObject = JSONObject()
            jsonObject.put("id", route?.id)
            jsonObject.put("distanceInM", route?.distanceInM)
            jsonObject.put("startDate", route?.startDate)
            jsonObject.put("endDate", route?.endDate)
            jsonObject.put("points", JSONArray(jsonPoints))
            jsonObject.put("kilometerPoints", JSONArray(jsonKilometerPoints))
            jsonObject.put("pauses", JSONArray(jsonPauses))

            jsonObject
        })

        return jsonArray
    }

    private suspend fun exportToJSON(uri: Uri): Boolean {
        val jsonArray = routesDataToJsonArray()
        return try {
            this@SettingsActivity.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonArray.toString().toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

}