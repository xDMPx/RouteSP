package com.xdmpx.routesp

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnDismissListener
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.xdmpx.routesp.database.RouteDatabase
import com.xdmpx.routesp.utils.RecordedRouteItem
import com.xdmpx.routesp.utils.RecordedRouteItemArrayAdapter
import com.xdmpx.routesp.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat.getDateTimeInstance

class MainActivity : AppCompatActivity() {
    private val DEBUG_TAG = "MainActivity"

    private lateinit var recordedRoutesListView: ListView
    private var totalDistance: Double = 0.0
    private var totalTime: Long = 0
    private var distanceInKM = true

    private var requestedNotificationPermission = false
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        requestedNotificationPermission = true
        requestPermissions(PermissionType.LOCATION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        recordedRoutesListView = this.findViewById(R.id.recordedRoutesList)

        recordedRoutesListView.setOnItemClickListener { adapterView, _, position, _ ->
            val recordedRouteItem = adapterView.adapter.getItem(position) as RecordedRouteItem
            goToRecordedRouteDetailsActivity(recordedRouteItem.routeID)
        }
        recordedRoutesListView.setOnItemLongClickListener { adapterView, _, position, _ ->
            val recordedRouteItem = adapterView.adapter.getItem(position) as RecordedRouteItem
            showAlertDialog(this@MainActivity,
                "Delete recorded route",
                "Are you sure you want to delete recorded route? This action cannot be undone.",
                "DELETE",
                onDismissListener = {}) { _, _ ->
                deleteRecordedRoute(recordedRouteItem.routeID)
            }
            true
        }

    }

    override fun onResume() {
        super.onResume()

        fillRecordedRoutesListView()
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

                val distance = Utils.distanceText(it.distanceInM, true)
                val timeInS = Utils.calculateTimeDiffS(it.startDate, it.endDate)
                val time = Utils.convertSecondsToHMmSs(timeInS)

                totalDistance += it.distanceInM
                totalTime += timeInS

                recordedRoutesListItems.add(
                    RecordedRouteItem(
                        it.id, startDateString, "Distance: $distance Time: $time"
                    )
                )
            }

            val recordedRoutesListArrayAdapter =
                RecordedRouteItemArrayAdapter(this@MainActivity, recordedRoutesListItems)

            val maxHeight =
                this@MainActivity.findViewById<LinearLayout>(R.id.recordedRoutesListContainer).measuredHeight
            Log.d("MainActivity", "maxHeight  $maxHeight")
            if (recordedRoutesListArrayAdapter.count >= 1) {
                val item: View =
                    recordedRoutesListArrayAdapter.getView(0, null, recordedRoutesListView)
                item.measure(0, 0)
                Log.d("MainActivity", "item  ${item.measuredHeight}")
                recordedRoutesListView.layoutParams.height =
                    ((maxHeight / item.measuredHeight - 0.5) * item.measuredHeight).toInt()
            }

            runOnUiThread {
                val recordedRoutesCount = "${recordedRoutes.size} Recordings"
                recordedRoutesListView.adapter = recordedRoutesListArrayAdapter
                this@MainActivity.findViewById<TextView>(R.id.distanceTextView).text =
                    Utils.distanceText(totalDistance, distanceInKM)
                this@MainActivity.findViewById<TextView>(R.id.timeTextView).text =
                    Utils.convertSecondsToHMmSs(totalTime)
                this@MainActivity.findViewById<TextView>(R.id.recordedTextView).text =
                    recordedRoutesCount
            }
        }

    }

    fun goToMapActivity(view: View) {
        if (requestPermissions()) {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
    }

    private fun goToRecordedRouteDetailsActivity(routeID: Int) {
        val intent = Intent(this, RecordedRouteDetailsActivity::class.java)
        intent.putExtra("routeID", routeID)
        startActivity(intent)
    }

    enum class PermissionType {
        NOTIFICATION, LOCATION,
    }

    private fun requestPermissions(permissionType: PermissionType = PermissionType.NOTIFICATION): Boolean {
        return when (permissionType) {
            PermissionType.NOTIFICATION -> {
                if (!requestedNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return requestNotificationPermission()
                } else {
                    requestPermissions(PermissionType.LOCATION)
                }
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
            true
        }
    }

    private fun requestLocation(): Boolean {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_DENIED -> {
                showAlertDialog(this@MainActivity,
                    getString(R.string.please_grant_required_permission),
                    getString(R.string.location_permission),
                    getString(R.string.ok),
                    {}) { dialog, _ ->
                    dialog?.dismiss()
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(
                            "package:" + BuildConfig.APPLICATION_ID
                        )
                    ).apply { startActivity(this) }
                }
                Log.e(DEBUG_TAG, "ACCESS_COARSE_LOCATION")
            }

            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_DENIED -> {
                showAlertDialog(this@MainActivity,
                    getString(R.string.please_grant_required_permission),
                    getString(R.string.location_permission),
                    getString(R.string.ok),
                    {}) { dialog, _ ->
                    dialog?.dismiss()
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(
                            "package:" + BuildConfig.APPLICATION_ID
                        )
                    ).apply { startActivity(this) }
                }
                Log.e(DEBUG_TAG, "ACCESS_FINE_LOCATION")
            }

            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_DENIED -> {
                showAlertDialog(this@MainActivity,
                    getString(R.string.please_grant_required_permission),
                    getString(R.string.location_permission),
                    getString(R.string.ok),
                    {}) { dialog, _ ->
                    dialog?.dismiss()
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse(
                            "package:" + BuildConfig.APPLICATION_ID
                        )
                    ).apply { startActivity(this) }
                }
                Log.e(DEBUG_TAG, "ACCESS_BACKGROUND_LOCATION")
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

    private fun showAlertDialog(
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

    fun onDistanceClick(view: View) {
        distanceInKM = !distanceInKM
        runOnUiThread {
            val distanceText = Utils.distanceText(totalDistance, distanceInKM)
            (this@MainActivity.findViewById(R.id.distanceTextView) as TextView).text = distanceText
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