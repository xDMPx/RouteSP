package com.xdmpx.routesp.ui

import android.app.Dialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.xdmpx.routesp.R
import com.xdmpx.routesp.datastore.ThemeType
import com.xdmpx.routesp.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ThemeSelectorSetting : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    private val DEBUG_TAG = "ThemeSelectorSetting"
    private val scopeIO = CoroutineScope(Dispatchers.IO)

    private var onThemeUpdate: () -> Unit = {}

    public fun setOnThemeUpdate(onThemeUpdate: () -> Unit) {
        this@ThemeSelectorSetting.onThemeUpdate = onThemeUpdate
    }

    init {
        val view =
            LayoutInflater.from(context).inflate(R.layout.theme_selector_setting, this, false)
        val set = ConstraintSet()
        addView(view)

        updateThemeSelectorText(view)
        view.setOnClickListener { onClick(view) }

        set.clone(this)
        set.match(view, this)
    }

    private fun updateThemeSelectorText(view: View) {
        val theme = Settings.getInstance().settingsState.value.theme
        view.findViewById<TextView>(R.id.themeSelectorSettingValue).text = when (theme) {
            ThemeType.LIGHT -> resources.getString(R.string.settings_theme_light)
            ThemeType.DARK -> resources.getString(R.string.settings_theme_dark)
            ThemeType.SYSTEM -> resources.getString(R.string.settings_theme_system)
            ThemeType.UNRECOGNIZED -> ""
        }
    }

    private fun onClick(view: View) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.theme_selector_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val radioGroup = dialog.findViewById<RadioGroup>(R.id.radioGroupTheme)
        val theme = Settings.getInstance().settingsState.value.theme
        when (theme) {
            ThemeType.SYSTEM -> radioGroup.check(R.id.radioSystem)
            ThemeType.DARK -> radioGroup.check(R.id.radioDark)
            ThemeType.LIGHT -> radioGroup.check(R.id.radioLight)
            ThemeType.UNRECOGNIZED -> {}
        }

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            val themeType = when (checkedId) {
                R.id.radioSystem -> ThemeType.SYSTEM
                R.id.radioLight -> ThemeType.LIGHT
                R.id.radioDark -> ThemeType.DARK
                else -> null
            }

            if (themeType != null) {
                Settings.getInstance().setTheme(themeType)
                scopeIO.launch {
                    Settings.getInstance().saveSettings(context)
                }
                onThemeUpdate()
            }
            updateThemeSelectorText(view)

            dialog.dismiss()
        }
        dialog.show()
    }

    private fun ConstraintSet.match(view: View, parentView: View) {
        this.connect(view.id, ConstraintSet.TOP, parentView.id, ConstraintSet.TOP)
        this.connect(view.id, ConstraintSet.START, parentView.id, ConstraintSet.START)
        this.connect(view.id, ConstraintSet.END, parentView.id, ConstraintSet.END)
        this.connect(view.id, ConstraintSet.BOTTOM, parentView.id, ConstraintSet.BOTTOM)
    }

}