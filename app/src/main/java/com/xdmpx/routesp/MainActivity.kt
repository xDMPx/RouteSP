package com.xdmpx.routesp

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.coroutineScope
import com.google.android.material.color.DynamicColors
import com.xdmpx.routesp.database.RouteDatabase
import com.xdmpx.routesp.utils.RecordedRouteItem
import com.xdmpx.routesp.utils.RecordedRouteItemArrayAdapter
import com.xdmpx.routesp.utils.Utils
import com.xdmpx.routesp.utils.Utils.showAlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.DateFormat.getDateTimeInstance

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ui_pref")

class MainActivity : AppCompatActivity() {
    private val DEBUG_TAG = "MainActivity"

    private lateinit var recordedRoutesListView: ListView
    private var totalDistance: Double = 0.0
    private var totalTime: Long = 0
    private var distanceInKM = true
    private val scopeIO = CoroutineScope(Dispatchers.IO)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) requestPermissions(PermissionType.BATTERY)
    }

    private val requestLocationPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivitiesIfAvailable(this@MainActivity.application)

        scopeIO.launch {
            if (!com.xdmpx.routesp.settings.Settings.getInstance().settingsState.value.loaded) {
                com.xdmpx.routesp.settings.Settings.getInstance().loadSettings(this@MainActivity)
                runOnUiThread {
                    recreate()
                }
            }
        }
        super.onCreate(savedInstanceState)

        Utils.syncThemeWithSettings(this@MainActivity)

        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.mainToolbar))

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainToolbar)) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, insets.top, 0, 0)
            WindowInsetsCompat.CONSUMED
        }

        recordedRoutesListView = this.findViewById(R.id.recordedRoutesList)

        recordedRoutesListView.setOnItemClickListener { adapterView, _, position, _ ->
            val recordedRouteItem = adapterView.adapter.getItem(position) as RecordedRouteItem
            goToRecordedRouteDetailsActivity(recordedRouteItem.routeID, position)
        }
        recordedRoutesListView.setOnItemLongClickListener { adapterView, _, position, _ ->
            val recordedRouteItem = adapterView.adapter.getItem(position) as RecordedRouteItem
            showAlertDialog(this@MainActivity,
                getString(R.string.delete_record),
                getString(R.string.delete_record_confirmation),
                getString(R.string.delete),
                onDismissListener = {}) { _, _ ->
                deleteRecordedRoute(recordedRouteItem.routeID)
            }
            true
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            true
        }

        R.id.action_about -> {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
            true
        }

        else -> {
            // The user's action isn't recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        Log.d(DEBUG_TAG, "onResume")
        super.onResume()

        fillRecordedRoutesListView()
    }

    override fun onStop() {
        scopeIO.launch {
            com.xdmpx.routesp.settings.Settings.getInstance().saveSettings(this@MainActivity)
            Log.d(DEBUG_TAG, "onStop")
        }
        super.onStop()
    }

    private fun fillRecordedRoutesListView() {
        val recordedRoutesListItems = ArrayList<RecordedRouteItem>()
        totalDistance = 0.0
        totalTime = 0

        val scope = CoroutineScope(Dispatchers.IO)
        val routeDBDao = RouteDatabase.getInstance(this).routeDatabaseDao
        scope.launch {
            val recordedRoutes = routeDBDao.getRoutes()
            recordedRoutes.forEach {
                val startDateString = getDateTimeInstance().format(it.startDate)

                val pauses = routeDBDao.getPauses(it.id)
                val distance = Utils.distanceText(it.distanceInM, true)
                val timeInS = Utils.calculateTimeDiffS(it.startDate, it.endDate, pauses)
                val time = Utils.convertSecondsToHMmSs(timeInS)

                totalDistance += it.distanceInM
                totalTime += timeInS

                recordedRoutesListItems.add(
                    RecordedRouteItem(
                        it.id,
                        startDateString,
                        "${getString(R.string.distance)}: $distance ${getString(R.string.time)}: $time"
                    )
                )
            }

            val recordedRoutesListArrayAdapter =
                RecordedRouteItemArrayAdapter(this@MainActivity, recordedRoutesListItems)

            runOnUiThread {
                val recordedRoutesCount = "${recordedRoutes.size} ${getString(R.string.recordings)}"
                recordedRoutesListView.adapter = recordedRoutesListArrayAdapter
                this@MainActivity.findViewById<TextView>(R.id.distanceTextView).text =
                    Utils.distanceText(totalDistance, distanceInKM)
                this@MainActivity.findViewById<TextView>(R.id.timeTextView).text =
                    Utils.convertSecondsToHMmSs(totalTime)
                this@MainActivity.findViewById<TextView>(R.id.recordedTextView).text =
                    recordedRoutesCount

                val maxHeight =
                    this@MainActivity.findViewById<LinearLayout>(R.id.recordedRoutesListContainer).measuredHeight
                Log.d("MainActivity", "maxHeight  $maxHeight")
                if (recordedRoutesListArrayAdapter.count >= 1) {
                    val item: View =
                        recordedRoutesListArrayAdapter.getView(0, null, recordedRoutesListView)
                    item.measure(0, 0)
                    Log.d("MainActivity", "item height  ${item.measuredHeight}")
                    recordedRoutesListView.layoutParams.height =
                        (maxHeight.toFloat() - (item.measuredHeight * 0.5f)).toInt()
                }


                val scrollValueKey = intPreferencesKey("scroll_value")

                val scrollValue: Flow<Int> =
                    this@MainActivity.dataStore.data.catch { }.map { uiPref ->
                        uiPref[scrollValueKey] ?: 0
                    }
                this@MainActivity.lifecycle.coroutineScope.launch {
                    recordedRoutesListView.setSelection(scrollValue.first())
                }
            }
        }

    }

    fun goToMapActivity(view: View) {
        if (requestPermissions()) {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
    }

    private fun goToRecordedRouteDetailsActivity(routeID: Int, pos: Int) {
        val scrollValueKey = intPreferencesKey("scroll_value")
        scopeIO.launch {
            this@MainActivity.dataStore.edit { uiPref ->
                uiPref[scrollValueKey] = pos
            }
        }

        val intent = Intent(this, RecordedRouteDetailsActivity::class.java)
        intent.putExtra("routeID", routeID)
        startActivity(intent)
    }

    enum class PermissionType {
        NOTIFICATION, LOCATION, BATTERY
    }

    private fun requestPermissions(permissionType: PermissionType = PermissionType.NOTIFICATION): Boolean {
        return when (permissionType) {
            PermissionType.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return requestNotificationPermission()
                } else {
                    requestPermissions(PermissionType.BATTERY)
                }
            }

            PermissionType.BATTERY -> {
                requestBackgroundActivity()
            }

            PermissionType.LOCATION -> {
                requestLocation()
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_DENIED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            false
        } else {
            requestPermissions(PermissionType.BATTERY)
        }
    }

    private fun requestBackgroundActivity(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val ignoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)

        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val isBackgroundRestricted = activityManager.isBackgroundRestricted
        Log.d(
            "MainActivity",
            "isBackgroundRestricted: $isBackgroundRestricted || ignoringBatteryOptimizations $ignoringBatteryOptimizations"
        )

        return if (!isBackgroundRestricted && ignoringBatteryOptimizations) {
            requestPermissions(PermissionType.LOCATION)
        } else {
            showAlertDialog(this@MainActivity,
                getString(R.string.background_restricted),
                getString(R.string.background_restricted_msg),
                getString(R.string.ok),
                {}) { dialog, _ ->
                dialog?.dismiss()
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    ("package:" + BuildConfig.APPLICATION_ID).toUri()
                ).apply { startActivity(this) }
            }
            false
        }
    }

    private fun requestLocation(): Boolean {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_DENIED -> {
                requestLocationPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                )
                Log.e(DEBUG_TAG, "ACCESS_COARSE_LOCATION")
            }

            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_DENIED -> {
                requestLocationPermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                )
                Log.e(DEBUG_TAG, "ACCESS_FINE_LOCATION")
            }

            !isLocationEnabled() -> {
                showAlertDialog(this@MainActivity,
                    getString(R.string.gps_disabled),
                    getString(R.string.please_turn_on_gps),
                    getString(R.string.ok),
                    {}) { dialog, _ ->
                    dialog?.dismiss()
                    this@MainActivity.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                }
                Log.e(DEBUG_TAG, "GPS_PROVIDER")
            }

            else -> {
                return true
            }
        }

        return false
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = this@MainActivity.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    fun onDistanceClick(view: View) {
        distanceInKM = !distanceInKM
        runOnUiThread {
            val distanceText = Utils.distanceText(totalDistance, distanceInKM)
            (this@MainActivity.findViewById<TextView>(R.id.distanceTextView)).text = distanceText
        }
    }

    private fun deleteRecordedRoute(routeID: Int) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val routeDBDao = RouteDatabase.getInstance(this@MainActivity).routeDatabaseDao
            routeDBDao.getRouteByID(routeID)?.let { routeDBDao.deleteRoute(it) }

            fillRecordedRoutesListView()
        }
    }

}