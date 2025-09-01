package com.example.background_remove

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import com.example.background_remove.databinding.ActivitySplashBinding

import com.example.background_remove.utils.GDPRConsentMessage
import com.example.background_remove.utils.Preferences


import com.example.background_remove.utils.setupStatusBar


class SplashActivity : BaseActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var googleMobileAdsConsentManager: GDPRConsentMessage
    private lateinit var animator: ObjectAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setupStatusBar(window, Color.BLACK)
        setContentView(binding.root)

        googleMobileAdsConsentManager = GDPRConsentMessage.getInstance(applicationContext)

        googleMobileAdsConsentManager.gatherConsent(this@SplashActivity) { _ ->

            Handler(Looper.getMainLooper()).postDelayed({
                goNextScreen()
            }, 3000)

        }
    }

fun goNextScreen(){

    if (Preferences.getBoolValue(this@SplashActivity, "firsttime")) {
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    } else {
        startActivity(Intent(this@SplashActivity, LanguageActivity::class.java))
        finish()
    }
}

    override fun onBackPressed() {
        // Disable back on splash
    }
}
