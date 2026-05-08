package com.example.background_remove

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.background_remove.adsManager.AdBanner
import com.example.background_remove.adsManager.NativeAdManager
import com.example.background_remove.utils.RemoteConfigManager
import com.example.background_remove.utils.setSafeClickListenertools
import com.example.background_remove.utils.setupStatusBar
import com.photo.editor.background.eraser.bgeraser.databinding.ActivityExitBinding

/**
 * Shown after optional home-back interstitial ([MainActivity] loads ad first when RC allows).
 * This screen only confirms exit; no second interstitial here.
 */
class ExitActivity : BaseActivity() {
    companion object {
        const val EXTRA_FROM_HOME = "extra_from_home"
    }

    private lateinit var binding: ActivityExitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!intent.getBooleanExtra(EXTRA_FROM_HOME, false)) {
            finish()
            return
        }
        binding = ActivityExitBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupStatusBar(window, Color.BLACK)
        loadExitAds()

        binding.btnCancel.setSafeClickListenertools {
            finish()
        }

        binding.btnExit.setOnClickListener {
            finishAffinity()
        }
    }

    private fun loadExitAds() {
        if (RemoteConfigManager.isExitBannerEnabled()) {
            binding.cvAdContainerbanner.visibility = View.VISIBLE
            binding.cvAdContainernative.visibility = View.GONE
            loadBannerAd()
        } else if (RemoteConfigManager.isExitNativeEnabled()) {
            binding.cvAdContainernative.visibility = View.VISIBLE
            binding.cvAdContainerbanner.visibility = View.GONE
            loadNativeAd()
        } else {
            binding.cvAdContainernative.visibility = View.GONE
            binding.cvAdContainerbanner.visibility = View.GONE
        }
    }

    private fun loadBannerAd() {
        AdBanner.loadBannerAdsplash(
            this,
            binding.cvAdContainerbanner,
            RemoteConfigManager.getExitBannerAdId(),
            false,
            binding.loadingLayoutbanner,
            false,
        ).callback { _, failed ->
            if (failed) {
                binding.cvAdContainerbanner.visibility = View.GONE
            }
        }
    }

    private fun loadNativeAd() {
        NativeAdManager.loadAndShowNativeAd(
            activity = this,
            adUnitId = RemoteConfigManager.getExitNativeAdId(),
            container = binding.cvAdContainernative,
            style = RemoteConfigManager.exitnativelayout(),
            shimmerFrameLayout = binding.loadingLayoutnative,
        ) { loaded, failed, fromCache ->
            Log.d("Ads", "Exit native loaded=$loaded failed=$failed fromCache=$fromCache")
            if (failed) {
                binding.cvAdContainernative.visibility = View.GONE
            }
        }
    }
}
