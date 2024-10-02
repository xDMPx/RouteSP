package com.xdmpx.routesp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.xdmpx.routesp.ui.ThemeSelectorSetting
import com.xdmpx.routesp.utils.Utils

class SettingsActivity : AppCompatActivity() {

    private val DEBUG_TAG = "SettingsActivity"

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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