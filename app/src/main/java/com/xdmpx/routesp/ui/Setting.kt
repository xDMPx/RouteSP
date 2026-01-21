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
import androidx.core.view.get
import com.xdmpx.routesp.R

class Setting : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.Setting)
        this.findViewById<TextView>(R.id.settingText).text =
            attributes.getString(R.styleable.Setting_setting_text)
        this.findViewById<ImageView>(R.id.settingIcon)
            .setImageResource(attributes.getResourceId(R.styleable.Setting_setting_icon, 0))
        ((this@Setting[0] as ConstraintLayout)[2] as CheckBox).isChecked = attributes.getBoolean(R.styleable.Setting_setting_value, false)
        attributes.recycle()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.Setting)
        this.findViewById<TextView>(R.id.settingText).text =
            attributes.getString(R.styleable.Setting_setting_text)
        this.findViewById<ImageView>(R.id.settingIcon)
            .setImageResource(attributes.getResourceId(R.styleable.Setting_setting_icon, 0))
        ((this@Setting[0] as ConstraintLayout)[2] as CheckBox).isChecked = attributes.getBoolean(R.styleable.Setting_setting_value, false)
        attributes.recycle()
    }

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.setting, this, false)
        val set = ConstraintSet()
        addView(view)

        val settingIcon = view.findViewById<ImageView>(R.id.settingIcon)
        settingIcon.setColorFilter(view.findViewById<TextView>(R.id.settingText).currentTextColor)

        set.clone(this)
        set.match(view, this)
    }

    fun getCheckBox(): CheckBox {
        return (this@Setting[0] as ConstraintLayout)[2] as CheckBox
    }

    private fun ConstraintSet.match(view: View, parentView: View) {
        this.connect(view.id, ConstraintSet.TOP, parentView.id, ConstraintSet.TOP)
        this.connect(view.id, ConstraintSet.START, parentView.id, ConstraintSet.START)
        this.connect(view.id, ConstraintSet.END, parentView.id, ConstraintSet.END)
        this.connect(view.id, ConstraintSet.BOTTOM, parentView.id, ConstraintSet.BOTTOM)
    }

}