package com.example.background_remove.adsManager

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ApplicationInfo
import android.util.Log
import com.example.background_remove.utils.Dialogs
import com.example.background_remove.utils.RemoteConfigManager

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

@SuppressLint("StaticFieldLeak")
object AppOpenAdManager {

    private const val TAG = "AppOpenAdManager"

    private var startOpenAd: AppOpenAd? = null
    private var adLoadingTime: Long = 0
    private var isLoading: Boolean = false
    private var pendingShowCallback: ((showed: Boolean, dismissed: Boolean, failed: Boolean) -> Unit)? = null
    private var lastActivity: Activity? = null

    var isDisplayingAd: Boolean = false
        private set
    var canShowAppOpen: Boolean = true
    private var isActivityPaused: Boolean = false

    // Check if cached ad is still fresh (within hours)
    private val isAdAvailable: Boolean
        get() = startOpenAd != null && !isExpired(4)

    /**
     * Should be called inside Activity.onPause()
     */
    fun onPause() {
        isActivityPaused = true
        Log.d(TAG, "Activity paused → deferring ad show")
    }

    /**
     * Should be called inside Activity.onResume()
     */
    fun onResume(activity: Activity) {
        isActivityPaused = false
        lastActivity = activity
        Log.d(TAG, "Activity resumed")

        // If ad was pending when paused → show it now
        if (pendingShowCallback != null && isAdAvailable) {
            Log.d(TAG, "Resuming pending AppOpenAd show")
            showAppOpenAdForSplash(activity, pendingShowCallback)
            pendingShowCallback = null
        }
    }

    /**
     * Show splash AppOpenAd if available
     */
    fun showAppOpenAdForSplash(
        activity: Activity,
        callback: ((showed: Boolean, dismissed: Boolean, failed: Boolean) -> Unit)? = null
    ) {
        if (isActivityPaused) {
            Log.i(TAG, "Activity is paused, deferring ad show")
            pendingShowCallback = callback
            lastActivity = activity
            return
        }
        if (isDisplayingAd) {
            Log.i(TAG, "Already displaying an AppOpenAd")
            callback?.invoke(false, false, true)
            return
        }
        if (!isAdAvailable) {
            Log.i(TAG, "No AppOpenAd available to show")
            callback?.invoke(false, false, true)
            return
        }
        if (!canShowAppOpen) {
            Log.i(TAG, "Ad blocked by canShowAppOpen=false")
            callback?.invoke(false, false, true)
            return
        }

        startOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                Dialogs.dismissProgressDialogloading()
                startOpenAd = null
                isDisplayingAd = false
                callback?.invoke(false, true, false)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Ad failed to show: $adError")
                startOpenAd = null
                isDisplayingAd = false
                callback?.invoke(false, false, true)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad shown")
                Dialogs.showProgressDialogloading(activity)
                isDisplayingAd = true
                callback?.invoke(true, false, false)
            }
        }

        startOpenAd?.show(activity)
    }

    /**
     * Load an AppOpenAd
     */
    fun loadAppOpenAd(
        activity: Activity,
        withCallback: Boolean = false,
        callback: ((loaded: Boolean, failed: Boolean) -> Unit)? = null
    ) {
        val adUnit = if (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            "ca-app-pub-3940256099942544/9257395921" // Google test ID
        } else {
            RemoteConfigManager.getSplashAppOpenAdId()
        }

        if (!RemoteConfigManager.isAllAdsEnabled() || !RemoteConfigManager.isSplashAppOpenEnabled() || !Ads.showadsinapp) {
            Log.i(TAG, "AppOpenAd disabled by RemoteConfig or ads disabled")
            if (withCallback) callback?.invoke(false, true)
            return
        }

        if (isAdAvailable) {
            Log.d(TAG, "Ad already available, skipping load")
            if (withCallback) callback?.invoke(true, false)
            return
        }

        if (isLoading) {
            Log.d(TAG, "Already loading, skipping new load")
            return
        }

        isLoading = true
        AppOpenAd.load(
            activity,
            adUnit,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    ad.setImmersiveMode(true)
                    Log.d(TAG, "AppOpenAd loaded")
                    startOpenAd = ad
                    adLoadingTime = Date().time
                    isLoading = false
                    if (withCallback) callback?.invoke(true, false)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "AppOpenAd failed: $error")
                    startOpenAd = null
                    isLoading = false
                    if (withCallback) callback?.invoke(false, true)
                }
            }
        )
    }

    private fun isExpired(hours: Long): Boolean {
        val diff = Date().time - adLoadingTime
        return diff > hours * 3600 * 1000
    }

    fun clear() {
        startOpenAd = null
        isDisplayingAd = false
        isLoading = false
        isActivityPaused = false
        pendingShowCallback = null
    }
}
