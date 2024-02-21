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
import android.widget.ListView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.xdmpx.routesp.database.RouteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat.getDateTimeInstance
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private val DEBUG_TAG = "MainActivity"

    private lateinit var recordedRoutesListView: ListView

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

    }

    override fun onResume() {
        super.onResume()

        fillRecordedRoutesListView()
    }

    private fun fillRecordedRoutesListView() {
        val recordedRoutesListItems = ArrayList<RecordedRouteItem>()

        val scope = CoroutineScope(Dispatchers.IO)
        val routeDBDao = RouteDatabase.getInstance(this).routeDatabaseDao
        scope.launch {
            val recordedRoutes = routeDBDao.getRoutes()
            recordedRoutes.forEach {
                val startDate = Calendar.getInstance()
                startDate.timeInMillis = it.startDate
                val startDateString = getDateTimeInstance().format(startDate.time)

                val distance = String.format("%.2f km", it.distanceInM / 1000f)
                var timeDif = it.endDate - it.startDate
                timeDif /= 1000
                val time = Utils.convertSecondsToHMmSs(timeDif)

                recordedRoutesListItems.add(
                    RecordedRouteItem(
                        it.id, startDateString, "Distance: $distance Time: $time"
                    )
                )
            }

            val recordedRoutesListArrayAdapter =
                RecordedRouteItemArrayAdapter(this@MainActivity, recordedRoutesListItems)

            if (recordedRoutesListArrayAdapter.count >= 5) {
                val item: View =
                    recordedRoutesListArrayAdapter.getView(0, null, recordedRoutesListView)
                item.measure(0, 0)
                recordedRoutesListView.layoutParams.height = (5.5 * item.measuredHeight).toInt()
            }

            runOnUiThread {
                recordedRoutesListView.adapter = recordedRoutesListArrayAdapter
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
        if (requestPermissions()) {
            val intent = Intent(this, RecordedRouteDetailsActivity::class.java)
            intent.putExtra("routeID", routeID)
            startActivity(intent)
        }
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

        //TODO: Manifest.permission.ACCESS_BACKGROUND_LOCATION

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

}