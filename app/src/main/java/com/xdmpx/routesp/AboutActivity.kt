package com.xdmpx.routesp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.DynamicColors
import com.xdmpx.routesp.utils.Utils

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DynamicColors.applyToActivitiesIfAvailable(this@AboutActivity.application)
        Utils.syncThemeWithSettings(this@AboutActivity)

        enableEdgeToEdge()
        setContentView(R.layout.activity_about)

        setSupportActionBar(findViewById<Toolbar>(R.id.materialToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        this@AboutActivity.onBackPressedDispatcher.addCallback {
            val intent = Intent(this@AboutActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }

        val drawable = packageManager.getApplicationIcon(BuildConfig.APPLICATION_ID)
        val iconView = this@AboutActivity.findViewById<ImageView>(R.id.iconView)
        iconView.setImageDrawable(drawable)

        val version = this@AboutActivity.findViewById<ConstraintLayout>(R.id.version)
        version.setOnClickListener {
            copyVersionToClipboard(this@AboutActivity)
        }
        val versionTextView = this@AboutActivity.findViewById<TextView>(R.id.versionTextView)
        val versionText =
            "${getString(R.string.about_version)} v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        versionTextView.text = versionText

        val sourceCode = this@AboutActivity.findViewById<ConstraintLayout>(R.id.sourceCode)
        sourceCode.setOnClickListener {
            openURL(
                this@AboutActivity,
                ContextCompat.getString(this@AboutActivity, R.string.about_source_code_url)
            )
        }

        val license = this@AboutActivity.findViewById<ConstraintLayout>(R.id.license)
        license.setOnClickListener {
            openURL(
                this@AboutActivity,
                ContextCompat.getString(this@AboutActivity, R.string.about_license_url)
            )
        }
        val licenseTextView = this@AboutActivity.findViewById<TextView>(R.id.licenseTextView)
        val licenseText =
            "${getString(R.string.about_license)}: ${getString(R.string.about_license_name)}"
        licenseTextView.text = licenseText


        val author = this@AboutActivity.findViewById<ConstraintLayout>(R.id.author)
        author.setOnClickListener {
            openURL(
                this@AboutActivity,
                ContextCompat.getString(this@AboutActivity, R.string.about_author_url)
            )
        }
        val authorTextView = this@AboutActivity.findViewById<TextView>(R.id.authorTextView)
        val authorText =
            "${getString(R.string.about_author)}: ${getString(R.string.about_author_name)}"
        authorTextView.text = authorText

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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

    private fun copyVersionToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText(
            "${context.getString(context.applicationInfo.labelRes)} Version",
            "${context.getString(context.applicationInfo.labelRes)} v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun openURL(context: Context, url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(browserIntent, null)
    }

}