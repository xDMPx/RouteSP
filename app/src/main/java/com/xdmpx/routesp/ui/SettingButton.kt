package com.xdmpx.routesp.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.xdmpx.routesp.R
import androidx.core.content.withStyledAttributes

class SettingButton : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        context.withStyledAttributes(attrs, R.styleable.SettingButton) {
            this@SettingButton.findViewById<TextView>(R.id.settingText).text =
                getString(R.styleable.SettingButton_setting_text)
            this@SettingButton.findViewById<ImageView>(R.id.settingIcon)
                .setImageResource(getResourceId(R.styleable.SettingButton_setting_icon, 0))
        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        context.withStyledAttributes(attrs, R.styleable.SettingButton) {
            this@SettingButton.findViewById<TextView>(R.id.settingText).text =
                getString(R.styleable.SettingButton_setting_text)
            this@SettingButton.findViewById<ImageView>(R.id.settingIcon)
                .setImageResource(getResourceId(R.styleable.SettingButton_setting_icon, 0))
        }
    }


    init {
        val view = LayoutInflater.from(context).inflate(R.layout.setting_button, this, false)
        val set = ConstraintSet()
        addView(view)

        val settingIcon = view.findViewById<ImageView>(R.id.settingIcon)
        settingIcon.setColorFilter(view.findViewById<TextView>(R.id.settingText).currentTextColor)

        set.clone(this)
        set.match(view, this)
    }

    private fun ConstraintSet.match(view: View, parentView: View) {
        this.connect(view.id, ConstraintSet.TOP, parentView.id, ConstraintSet.TOP)
        this.connect(view.id, ConstraintSet.START, parentView.id, ConstraintSet.START)
        this.connect(view.id, ConstraintSet.END, parentView.id, ConstraintSet.END)
        this.connect(view.id, ConstraintSet.BOTTOM, parentView.id, ConstraintSet.BOTTOM)
    }

}