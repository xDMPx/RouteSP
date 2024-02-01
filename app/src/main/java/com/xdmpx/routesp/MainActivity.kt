package com.xdmpx.routesp

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnDismissListener
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView


class MainActivity : AppCompatActivity() {
    private val DEBUG_TAG = "MainActivity"
    private lateinit var map: MapView
    private lateinit var mapController: MapController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        setContentView(R.layout.activity_main)

        map = findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)

        mapController = map.controller as MapController
        mapController.setZoom(13.0)
        val startPoint = GeoPoint(52.22977, 21.01178)
        mapController.setCenter(startPoint)

    }

    override fun onResume() {
        requestLocation()
        super.onResume()
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause()
    }

    private fun requestLocation() {
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
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    Log.d(DEBUG_TAG, "LOCATION: $location")
                    if (location == null) {
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY, null
                        ).addOnSuccessListener { location: Location? ->
                            Log.d(DEBUG_TAG, "LOCATION: $location")
                            location?.let {
                                val startPoint = GeoPoint(location.latitude, location.longitude)
                                mapController.setCenter(startPoint)
                            }
                        }
                        return@addOnSuccessListener
                    }
                    val startPoint = GeoPoint(location.latitude, location.longitude)
                    mapController.setCenter(startPoint)
                }
            }
        }


        //TODO: Manifest.permission.ACCESS_BACKGROUND_LOCATION
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

    fun onOSMCopyrightNoticeClick(view: View) {
        val browserIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/copyright"))
        startActivity(browserIntent, null)
    }

}