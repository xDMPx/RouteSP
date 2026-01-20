package com.xdmpx.routesp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.DynamicColors
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
import java.time.LocalDate

class SettingsActivity : AppCompatActivity() {

    private val DEBUG_TAG = "SettingsActivity"
    private val scopeIO = CoroutineScope(Dispatchers.IO)
    private lateinit var progressBarLinearLayout: LinearLayout

    private val createDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            exportToJSONCallback(
                uri
            )
        }

    private val openDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            importFromJSONCallback(
                uri
            )
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DynamicColors.applyToActivitiesIfAvailable(this@SettingsActivity.application)
        Utils.syncThemeWithSettings(this@SettingsActivity)

        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        setSupportActionBar(findViewById<Toolbar>(R.id.materialToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        progressBarLinearLayout =
            this@SettingsActivity.findViewById<LinearLayout>(R.id.progressBarLinearLayout)

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
            createDocument.launch("routesp_export_${year}_${month}_$day.json")
        }

        val importSetting = findViewById<SettingButton>(R.id.importSetting)
        importSetting.setOnClickListener {
            openDocument.launch(arrayOf("application/json"))
        }

        this@SettingsActivity.onBackPressedDispatcher.addCallback {
            val intent = Intent(this@SettingsActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
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
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
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
        val callback = this@SettingsActivity.onBackPressedDispatcher.addCallback {}
        progressBarLinearLayout.visibility = View.VISIBLE

        scopeIO.launch {
            if (Utils.exportToJSON(this@SettingsActivity, uri)) {
                runOnUiThread {
                    progressBarLinearLayout.visibility = View.GONE
                    callback.remove()
                    ShortToast(
                        this@SettingsActivity, resources.getString(R.string.export_successful)
                    )
                }
            } else {
                runOnUiThread {
                    progressBarLinearLayout.visibility = View.GONE
                    callback.remove()
                    ShortToast(
                        this@SettingsActivity, resources.getString(R.string.export_failed)
                    )
                }
            }
        }
    }

    private fun importFromJSONCallback(uri: Uri?) {
        if (uri == null) return
        val callback = this@SettingsActivity.onBackPressedDispatcher.addCallback {}
        progressBarLinearLayout.visibility = View.VISIBLE
        val textViewProgress = progressBarLinearLayout.getChildAt(1) as TextView
        textViewProgress.text = resources.getString(R.string.initializing)

        scopeIO.launch {
            if (Utils.importFromJSON(this@SettingsActivity, uri) { p ->
                    runOnUiThread {
                        textViewProgress.text = p
                    }
                }) {
                runOnUiThread {
                    progressBarLinearLayout.visibility = View.GONE
                    callback.remove()
                    ShortToast(
                        this@SettingsActivity, resources.getString(R.string.import_successful)
                    )
                }
            } else {
                runOnUiThread {
                    progressBarLinearLayout.visibility = View.GONE
                    callback.remove()
                    ShortToast(
                        this@SettingsActivity, resources.getString(R.string.import_failed)
                    )
                }
            }
        }
    }

}