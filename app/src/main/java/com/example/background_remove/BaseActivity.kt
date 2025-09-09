package com.example.background_remove

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.background_remove.adsManager.Ads

import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {

        setTranslatorLocale(newBase)
        super.attachBaseContext(newBase)
    }

    private fun setTranslatorLocale(activity: Context): Configuration {
        val pref = activity.getSharedPreferences("Languages", MODE_PRIVATE)
        val languageCode = pref.getString("languageCode", "en")

        val locale = Locale(languageCode!!)
        val res: Resources = activity.resources
        val newConfig = Configuration(res.configuration)
        Locale.setDefault(locale)
        newConfig.locale = locale
        newConfig.setLayoutDirection(locale)
        res.updateConfiguration(newConfig, res.displayMetrics)
        return newConfig
    }
    override fun onPause() {
        super.onPause()
        Ads.setPaused(true,this)

    }

    override fun onResume() {
        super.onResume()
        Ads.setPaused(false,this)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val wic = WindowInsetsControllerCompat(window, window.decorView)
        wic.isAppearanceLightStatusBars = true



    }

}