package com.xdmpx.routesp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.animation.AlphaAnimation
import android.widget.CheckBox
import androidx.activity.enableEdgeToEdge
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SettingsActivity : AppCompatActivity() {

    private val DEBUG_TAG = "SettingsActivity"
    private val scopeIO = CoroutineScope(Dispatchers.IO)

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
            scopeIO.launch {
                val routeDBDao = RouteDatabase.getInstance(this@SettingsActivity).routeDatabaseDao
                routeDBDao.getRoutes().forEach { route ->
                    routeDBDao.deleteRoute(route)
                }
            }
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

}