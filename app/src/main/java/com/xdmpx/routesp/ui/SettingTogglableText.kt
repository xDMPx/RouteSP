package com.xdmpx.routesp.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.xdmpx.routesp.R
import androidx.core.content.withStyledAttributes
import androidx.core.view.get

class SettingTogglableText : ConstraintLayout {
    lateinit var textValues: List<String>
    var textOption: Int = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        context.withStyledAttributes(attrs, R.styleable.SettingTogglableText) {
            this@SettingTogglableText.findViewById<TextView>(R.id.settingText).text =
                getString(R.styleable.SettingTogglableText_setting_text)
            this@SettingTogglableText.findViewById<ImageView>(R.id.settingIcon)
                .setImageResource(getResourceId(R.styleable.SettingTogglableText_setting_icon, 0))
            getString(R.styleable.SettingTogglableText_setting_text_values)?.let {
                textValues = it.split(';')
            }
            ((this@SettingTogglableText[0] as ConstraintLayout)[2] as TextView).text =
                textValues[textOption]
        }
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        context.withStyledAttributes(attrs, R.styleable.SettingTogglableText) {
            this@SettingTogglableText.findViewById<TextView>(R.id.settingText).text =
                getString(R.styleable.SettingTogglableText_setting_text)
            this@SettingTogglableText.findViewById<ImageView>(R.id.settingIcon)
                .setImageResource(getResourceId(R.styleable.SettingTogglableText_setting_icon, 0))
            getString(R.styleable.SettingTogglableText_setting_text_values)?.let {
                textValues = it.split(';')
            }
            ((this@SettingTogglableText[0] as ConstraintLayout)[2] as TextView).text =
                textValues[textOption]
        }
    }

    init {
        val view =
            LayoutInflater.from(context).inflate(R.layout.setting_togglable_text, this, false)
        val set = ConstraintSet()
        addView(view)

        val settingIcon = view.findViewById<ImageView>(R.id.settingIcon)
        settingIcon.setColorFilter(view.findViewById<TextView>(R.id.settingText).currentTextColor)

        set.clone(this)
        set.match(view, this)
    }

    fun choseTextOption(i: Int) {
        textOption = i
        ((this@SettingTogglableText[0] as ConstraintLayout)[2] as TextView).text = textValues[i]
    }

    private fun ConstraintSet.match(view: View, parentView: View) {
        this.connect(view.id, ConstraintSet.TOP, parentView.id, ConstraintSet.TOP)
        this.connect(view.id, ConstraintSet.START, parentView.id, ConstraintSet.START)
        this.connect(view.id, ConstraintSet.END, parentView.id, ConstraintSet.END)
        this.connect(view.id, ConstraintSet.BOTTOM, parentView.id, ConstraintSet.BOTTOM)
    }
}