package com.xdmpx.routesp.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.xdmpx.routesp.R
import com.xdmpx.routesp.datastore.ThemeType
import com.xdmpx.routesp.settings.Settings

class ThemeSelectorSetting : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    private val DEBUG_TAG = "ThemeSelectorSetting"

    init {
        val view =
            LayoutInflater.from(context).inflate(R.layout.theme_selector_setting, this, false)
        val set = ConstraintSet()
        addView(view)

        updateThemeSelectorText(view)

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

    private fun ConstraintSet.match(view: View, parentView: View) {
        this.connect(view.id, ConstraintSet.TOP, parentView.id, ConstraintSet.TOP)
        this.connect(view.id, ConstraintSet.START, parentView.id, ConstraintSet.START)
        this.connect(view.id, ConstraintSet.END, parentView.id, ConstraintSet.END)
        this.connect(view.id, ConstraintSet.BOTTOM, parentView.id, ConstraintSet.BOTTOM)
    }
}