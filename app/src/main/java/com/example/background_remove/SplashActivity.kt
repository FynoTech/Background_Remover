package com.example.background_remove

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.background_remove.adsManager.AdBanner
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.adsManager.AppOpenAdManager
import com.example.background_remove.adsManager.NativeAdManager
import com.example.background_remove.billing.BillingsHelper
import com.example.background_remove.billingsubs.BillingsHelperSubsCription
import com.example.background_remove.utils.GDPRConsentMessage
import com.example.background_remove.utils.Preferences
import com.example.background_remove.utils.RemoteConfigManager
import com.example.background_remove.utils.setupStatusBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.photo.editor.background.eraser.bgeraser.BuildConfig
import com.photo.editor.background.eraser.bgeraser.R
import com.photo.editor.background.eraser.bgeraser.databinding.ActivitySplashBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SplashActivity : BaseActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var googleMobileAdsConsentManager: GDPRConsentMessage
    private var progressJob: Job? = null
    private var wasCancelled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setupStatusBar(window, Color.BLACK)
        setContentView(binding.root)
        checkPremium()
        startProgressAnimation()
        Handler(Looper.getMainLooper()).postDelayed({

            googleMobileAdsConsentManager = GDPRConsentMessage.getInstance(applicationContext)

            googleMobileAdsConsentManager.gatherConsent(this@SplashActivity) { _ ->
                if (Ads.isNetworkAvailable(this)) {
                    // 🔹 Fetch remote config first
                    RemoteConfigManager.init()
                    RemoteConfigManager.fetchAndActivate {
                        // 🔹 Check for force update
                        if (isForceUpdateRequired()) {
                            showForceUpdateDialog()
                        } else {
                            // 🔹 Load splash banner ad if enabled
                            if (RemoteConfigManager.isSplashBannerEnabled()) {
                                binding.cvAdContainerbanner.visibility = View.VISIBLE
                                loadSplashBannerAd()
                            } else if (RemoteConfigManager.isSplashNativeEnabled()) {
                                binding.cvAdContainernative.visibility = View.VISIBLE
                                loadSplashNativeAd()
                            }

                            if (RemoteConfigManager.isSplashInterstitialEnabled() && Ads.isNetworkAvailableWithoutPremium(
                                    this
                                ) && !RemoteConfigManager.isSplashAppOpenEnabled()
                            ) {

                                loadSplashInterstitialAd()
                            } else if (Ads.isNetworkAvailableWithoutPremium(this) && RemoteConfigManager.isSplashAppOpenEnabled()) {

                                loadSplashOpenAd()
                            }

                        }
                    }
                } else {
                    // No internet → continue normally

                    startProgressAnimation()
                }
            }
        }, 2000)

    }

    private fun startProgressAnimation() {
        val duration = if (Ads.isNetworkAvailableWithoutPremium(this)) {
            12000L // 12 seconds
        } else {
            4000L // 4 seconds
        }

        wasCancelled = false

        // Cancel any existing job first
        progressJob?.cancel()

        progressJob = lifecycleScope.launch {
            delay(duration)

            goNextScreen()

        }
    }


    private fun isForceUpdateRequired(): Boolean {
        val forceUpdateVersion =
            RemoteConfigManager.getForceUpdate()
        val currentVersionCode = BuildConfig.VERSION_CODE
        return forceUpdateVersion > currentVersionCode
    }

    private fun showForceUpdateDialog() {
        progressJob?.cancel()
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.update_required))
            .setMessage(getString(R.string.update_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.update)) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data =
                        android.net.Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")
                }
                startActivity(intent)
                finish()
            }
            .show()
    }

    override fun onPause() {
        super.onPause()
        Ads.setPaused(true, this)
        AppOpenAdManager.onPause()
    }

    private fun checkPremium() {
        val billingsHelper = BillingsHelper.getInstance(this)
        billingsHelper?.setListener(null)
        billingsHelper?.initializeBillingClient(this)
        BillingsHelper.billingClient = billingsHelper?.connectToBillingService()

        val billingsHelper1 = BillingsHelperSubsCription.getInstance(this)
        billingsHelper1?.setListener(null)
        billingsHelper1?.initializeBillingClient(this)
        BillingsHelperSubsCription.billingClient = billingsHelper1?.connectToBillingService()


    }

    override fun onResume() {
        super.onResume()
        Ads.setPaused(false, this)
        AppOpenAdManager.onResume(this)

    }

    private fun loadSplashBannerAd() {
        AdBanner.loadBannerAdsplash(
            this,
            binding.cvAdContainerbanner,
            RemoteConfigManager.getSplashBannerAdId(),
            false,
            binding.loadingLayoutbanner
        )
            .callback { loaded, failed ->
                if (failed) {
                    binding.cvAdContainerbanner.visibility = View.GONE
                }

            }

    }

    private fun loadSplashNativeAd() {
        NativeAdManager.loadAndShowNativeAd(
            activity = this,
            adUnitId = RemoteConfigManager.getSplashNativeAdId(),
            container = binding.cvAdContainernative,
            style = RemoteConfigManager.splashnativelayout(), binding.loadingLayoutnative
        ) { loaded, failed, fromCache ->
            Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
            if (failed) {
                binding.cvAdContainernative.visibility = View.GONE
            }
        }

    }

    private fun loadSplashOpenAd() {

        AppOpenAdManager.loadAppOpenAd(this, withCallback = true) { loaded, failed ->
            if (loaded) {
                AppOpenAdManager.showAppOpenAdForSplash(this) { showed, dismissed, failed ->
                    when {
                        showed -> {
                            progressJob?.cancel()
                        }

                        dismissed -> {
                            Log.d("Splash", "👋 AppOpenAd dismissed → continue app flow")
                            goNextScreen()
                        }

                        failed -> {
                            Log.d("Splash", "⚠️ AppOpenAd failed → continue app flow")
                            goNextScreen()
                        }
                    }
                }

            } else if (failed) {
                goNextScreen()
                Log.d("Splash", "❌ Failed to load AppOpenAd")
            }
        }
    }

    private fun loadSplashInterstitialAd() {
        Ads.loadAndShowInterstitial(
            activity = this,
            adUnitId = RemoteConfigManager.getSplashInterstitialAdId(),
            placement = "splash",
            useAnyCachedFallback = true,
            showDelayMs = 500 // splash style
        ) { loaded, failed, showed, dismissed ->
            when {
                failed -> {
                    goNextScreen()
                }

                dismissed -> {
                    goNextScreen()
                }

                loaded -> { /* optional: ad ready */
                }

                showed -> {

                    progressJob?.cancel()
                }
            }
        }

    }

    fun goNextScreen() {

        progressJob?.cancel()
        Log.d("abcd", "goNextScreen: ")
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
