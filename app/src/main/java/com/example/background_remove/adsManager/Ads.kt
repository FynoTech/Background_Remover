package com.example.background_remove.adsManager

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.background_remove.MyApp
import com.example.background_remove.utils.PrefsManagerRemoteConfig
import com.example.background_remove.utils.ProgressUtils
import com.example.background_remove.utils.RemoteConfigManager

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.photo.editor.background.eraser.bgeraser.R



object Ads {

    // ---- Public flags ----
    var showadsinapp: Boolean = true
    var shownow: Boolean = true
    var acitvitycountcheck: Int = 0
    var isinterstitialvisible: Boolean = false
     const val KEY_IS_PURCHASED = "is_purchased"
     const val lifetimekey = "lifetime_subscription"
     const val weeklykey = "weekly_subscription"
     const val monthlykey = "monthly_subscription"
     const val yearlykey = "yearly_subscription"
     const val KEY_IS_PURCHASEDSubs = "is_purchased_subs"
     const val  purchasedsubs = "subscription_type"
    var license_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5CFur6ApkwmL44RVQuyepPlpzvQ6iI+fC6Uf41iSyotQdbZmyC48Hhdik0TxrupltiLFhMF+Im/ze6Qb7smnMv26PQwDFnRFEVe0ek5jZhPDEAWW07MKcZNxtWomGNRjem4VkhEVnFGN70yctZBbT9sr6JboeyPrg3avwdiD68fxHvsY3aNPM1n9d54muNlcd+q4i2p9rVF/5fDfLU5KvurtngPOSeZi+kt6f18wT+KBO7SEE1VkCrQzGCBrTPN7/PAHUAEwnvLx6zuRUrMRubQYMKzm4U3XKJvoweeF6swa6Y2kwdWmdX01yEGuwjGNqB/2CRhOrlcb/4/4ggUc4wIDAQAB"
    // Pause/visible state
    @Volatile var isPaused: Boolean = false
        private set

    var timeedout= false
    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading: Boolean = false

    // Store pending rewarded callback if paused
    private var pendingRewardedCallback: ((Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit)? = null
    private var pendingRewardedAd: RewardedAd? = null
    // ---- Internal state ----
    // Cache loaded ads per placement
    private val interstitialCache: MutableMap<String, InterstitialAd?> = mutableMapOf()

    // Track active loads to avoid duplicate parallel loads
    private val loadingPlacements: MutableSet<String> = mutableSetOf()

    // If an ad was requested while paused, we’ll try to show it on resume
    private val pendingShowPlacements: MutableMap<String, ((Boolean, Boolean, Boolean, Boolean) -> Unit)?> =
        mutableMapOf()

    fun setPaused(paused: Boolean, activity: Activity? = null) {
        isPaused = paused
        if (!paused && activity != null) {
            // Resume: replay all pending shows
            val pending = pendingShowPlacements.toMap() // copy
            pendingShowPlacements.clear()

            pending.forEach { (placement, callback) ->
                interstitialCache[placement]?.let { ad ->
                    internalShow(activity, ad, placement, callback)
                }
            }
            // Resume pending rewarded
            pendingRewardedAd?.let { ad ->
                internalShowRewarded(activity, ad, pendingRewardedCallback)
                pendingRewardedAd = null
                pendingRewardedCallback = null
            }
        }
    }

    // ---------- Preload ----------
    /**
     * Preload an interstitial for the given placement.
     * No UI shown here. Use later with loadAndShowInterstitial (which will use cache).
     */
    fun preloadInterstitial(
        ctx: Context,
        adUnitId: String,
        placement: String,
        onResult: ((success: Boolean) -> Unit)? = null
    ) {
        if (!isNetworkAvailableWithoutPremium(ctx)) {
            Log.d("AdsManager", "❌ Preload skipped: no network / premium user ($placement)")
            onResult?.invoke(false)
            return
        }
        if (interstitialCache[placement] != null) {
            Log.d("AdsManager", "✅ Already preloaded for $placement")
            onResult?.invoke(true)
            return
        }
        if (!loadingPlacements.add(placement)) {
            Log.d("AdsManager", "⏳ Already loading for $placement")
            onResult?.invoke(false)
            return
        }

        InterstitialAd.load(ctx, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.setImmersiveMode(true)
                    interstitialCache[placement] = ad
                    loadingPlacements.remove(placement)
                    Log.d("AdsManager", "✅ Preloaded interstitial ($placement)")
                    onResult?.invoke(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingPlacements.remove(placement)
                    interstitialCache[placement] = null
                    Log.e("AdsManager", "❌ Preload failed ($placement): $error")
                    onResult?.invoke(false)
                }
            }
        )
    }

    // ---------- Unified load & show ----------
    /**
     * Load (if needed) and show an interstitial for a placement.
     *
     * @param useAnyCachedFallback If true, will use ANY cached interstitial (from another placement)
     *                             when this placement has none (helps avoid reloading).
     * @param showDelayMs Optional delay before show (useful on Splash).
     * @param callback Fires on each stage:
     *                 loaded  -> (true,  false, false, false)
     *                 failed  -> (false, true,  false, false)
     *                 showed  -> (false, false, true,  false)
     *                 dismissed->(false, false, false, true)
     */
    fun loadAndShowInterstitial(
        activity: Activity,
        adUnitId: String,
        placement: String,
        useAnyCachedFallback: Boolean = true,
        showDelayMs: Long = 0L,
        callback: ((loaded: Boolean, failed: Boolean, showed: Boolean, dismissed: Boolean) -> Unit)? = null
    ) {
        if (!isNetworkAvailableWithoutPremium(activity)) {
            callback?.invoke(false, true, false, false)
            return
        }
        if(MyApp.isShowingrecently){
            callback?.invoke(false, true, false, false)
            MyApp.isShowingrecently=false
            return
        }
        isinterstitialvisible =true
        // 🔑 If any ad already cached → show it (no matter which placement)
        val cachedAd = interstitialCache[placement]
            ?: if (useAnyCachedFallback) interstitialCache.values.firstOrNull { it != null } else null

        if (cachedAd != null) {
            callback?.invoke(true, false, false, false)
            postToMain(showDelayMs) { internalShow(activity, cachedAd, placement, callback) }
            return
        }

        if (!loadingPlacements.add(placement)) {
            Log.d("AdsManager", "⏳ Already loading; will show when ready ($placement)")
            return
        }
        timeedout = false
        val timeoutHandler = runWithTimeout(10_000) {
            if (interstitialCache[placement] == null) {
                ProgressUtils.dismiss()
                isinterstitialvisible = false
                timeedout = true
                Log.e("AdsManager", "⏰ Timeout interstitial ($placement)")
                callback?.invoke(false, true, false, false)
            }
        }
        InterstitialAd.load(activity, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    timeoutHandler.removeCallbacksAndMessages(null)
                    ad.setImmersiveMode(true)
                    interstitialCache[placement] = ad
                    loadingPlacements.remove(placement)
                    callback?.invoke(true, false, false, false)


                    if (!timeedout) {
                        postToMain(showDelayMs) { internalShow(activity, ad, placement, callback) }
                    }

                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    timeoutHandler.removeCallbacksAndMessages(null)
                    loadingPlacements.remove(placement)
                    interstitialCache[placement] = null
                    isinterstitialvisible =false
                    Log.e("AdsManager", "❌ Load failed ($placement): $error")
                    callback?.invoke(false, true, false, false)
                }
            }
        )
    }

    fun showInterstitialWithCounter(
        activity: Activity,check:Boolean,type:String,
        callback: ((Boolean, Boolean, Boolean, Boolean) -> Unit)? = null
    ) {
        if (Ads.shownow) acitvitycountcheck++
        if(check){
            ProgressUtils.show(activity,activity.getString(R.string.loading_ad_))
            when (type) {

                "rewarded" -> {
                    callback?.invoke(false, false, false, true)
                }
            }
        }else{
            if (acitvitycountcheck >= RemoteConfigManager.getCounterInterstitialCount() && RemoteConfigManager.isCounterInterstitialEnabled()) {
                ProgressUtils.show(activity,activity.getString(R.string.loading_ad_))
                acitvitycountcheck = 0 // reset after showing

                loadAndShowInterstitial(activity, RemoteConfigManager.getCounterInterstitialAdId(), "counter", true, 0L, callback)

            } else {
                // Not yet threshold → just continue app flow, trigger dismissed callback
                callback?.invoke(false, false, false, true)
            }
        }

    }

    // ---------- Internal show with FULL callback coverage ----------
    private fun internalShow(
        activity: Activity,
        ad: InterstitialAd,
        placement: String,
        callback: ((Boolean, Boolean, Boolean, Boolean) -> Unit)?
    ) {
        if (isPaused) {
            Log.d("AdsManager", "⏸️ Paused; deferring show ($placement)")
            pendingShowPlacements[placement] = callback
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            Log.w("AdsManager", "❌ Activity destroyed, skipping interstitial ($placement)")
            Ads.isinterstitialvisible=false
          //  callback?.invoke(false, true, false, false)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isinterstitialvisible = true
                callback?.invoke(false, false, true, false)
                Log.d("AdsManager", "🎬 Interstitial showed ($placement)")
            }

            override fun onAdDismissedFullScreenContent() {
                isinterstitialvisible = false
                callback?.invoke(false, false, false, true)
                // 🔑 Remove this ad from ANY placement it was stored under
                interstitialCache.entries
                    .filter { it.value == ad }
                    .forEach { interstitialCache[it.key] = null }
                isinterstitialvisible =false
                acitvitycountcheck = 0
                ProgressUtils.dismiss()

                Log.d("AdsManager", "👋 Interstitial dismissed ($placement)")
            }
        }

        try {
            ad.show(activity)
        } catch (t: Throwable) {
            Log.e("AdsManager", "⚠️ Show threw exception ($placement): ${t.message}")
            isinterstitialvisible = false
            callback?.invoke(false, true, false, false)
        }
    }



    // ---------- Utilities ----------
    private fun postToMain(delayMs: Long = 0L, block: () -> Unit) {
        if (delayMs <= 0L) {
            Handler(Looper.getMainLooper()).post(block)
        } else {
            Handler(Looper.getMainLooper()).postDelayed(block, delayMs)
        }
    }
    private fun runWithTimeout(timeoutMs: Long = 10_000, onTimeout: () -> Unit): Handler {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ onTimeout() }, timeoutMs)
        return handler
    }

    fun isNetworkAvailableWithoutPremium(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        val connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val isPremium = PrefsManagerRemoteConfig.with(context).getBoolean(KEY_IS_PURCHASED, false)
        val isPremiumsubs = PrefsManagerRemoteConfig.with(context).getBoolean(KEY_IS_PURCHASEDSubs, false)
        return connected && showadsinapp && !isPremium && !isPremiumsubs
    }
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        val connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        return connected
    }

    fun isPremiumUser(context: Context): Boolean {
        return PrefsManagerRemoteConfig.with(context).getBoolean(KEY_IS_PURCHASED, false) || PrefsManagerRemoteConfig.with(context).getBoolean(
            KEY_IS_PURCHASEDSubs, false)
    }

    fun fireEvent(context: Context, name: String) {
        val eventName = name.trim()
        val params = Bundle().apply { putString(eventName, eventName) }
        FirebaseAnalytics.getInstance(context).logEvent(eventName, params)
        Log.d("AdsManager", "📊 Event fired: $eventName")
    }
    fun loadAndShowRewarded(
        activity: Activity,
        adUnitId: String,
        showDelayMs: Long = 0L,
        callback: ((loaded: Boolean, failed: Boolean, showed: Boolean, dismissed: Boolean, rewarded: Boolean) -> Unit)? = null
    ) {
        if (!isNetworkAvailableWithoutPremium(activity)) {
            callback?.invoke(false, true, false, false, false)
            return
        }
        if(MyApp.isShowingrecently){
            callback?.invoke(false, true, false, false, false)
            MyApp.isShowingrecently=false
            return
        }
        // ✅ Already loaded → show immediately
        rewardedAd?.let { ad ->
            ad.setImmersiveMode(true)
            callback?.invoke(true, false, false, false, false)
            postToMain(showDelayMs) { internalShowRewarded(activity, ad, callback) }
            return
        }
        isinterstitialvisible =true
        if (isRewardedLoading) {
            Log.d("AdsManager", "⏳ Rewarded ad already loading")
            return
        }
        timeedout = false
        val timeoutHandler = runWithTimeout(10_000) {
            if (rewardedAd == null) {
                isRewardedLoading = false
                timeedout = true
                ProgressUtils.dismiss()
                isinterstitialvisible = false
                Log.e("AdsManager", "⏰ Timeout rewarded")
                callback?.invoke(false, true, false, false, false)
            }
        }
        isRewardedLoading = true
        RewardedAd.load(activity, adUnitId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    timeoutHandler.removeCallbacksAndMessages(null)
                    rewardedAd = ad
                    isRewardedLoading = false
                    callback?.invoke(true, false, false, false, false)
                    postToMain(showDelayMs) { internalShowRewarded(activity, ad, callback) }
                    Log.d("AdsManager", "✅ Rewarded ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    timeoutHandler.removeCallbacksAndMessages(null)
                    rewardedAd = null
                    isRewardedLoading = false
                    callback?.invoke(false, true, false, false, false)
                    Log.e("AdsManager", "❌ Rewarded load failed: $error")
                }
            })
    }

    private fun internalShowRewarded(
        activity: Activity,
        ad: RewardedAd,
        callback: ((Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit)?
    ) {
        if (isPaused) {
            Log.d("AdsManager", "⏸️ Paused; deferring rewarded ad show")
            pendingRewardedAd = ad
            pendingRewardedCallback = callback
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isinterstitialvisible = true
                acitvitycountcheck =0
                callback?.invoke(false, false, true, false, false)
                Log.d("AdsManager", "🎬 Rewarded showed")
            }

            override fun onAdDismissedFullScreenContent() {
                isinterstitialvisible = false
                rewardedAd = null // clear cache
                callback?.invoke(false, false, false, true, false)
                Log.d("AdsManager", "👋 Rewarded dismissed")
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                rewardedAd = null
                isinterstitialvisible = false
                callback?.invoke(false, true, false, false, false)
                Log.e("AdsManager", "⚠️ Rewarded failed to show: $error")
            }
        }

        ad.show(activity) { rewardItem: RewardItem ->
            callback?.invoke(false, false, false, false, true)
            Log.d("AdsManager", "🏆 User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
    }
    fun clearPlacement(placement: String) {
        interstitialCache[placement] = null
        loadingPlacements.remove(placement)
        pendingShowPlacements.remove(placement)
    }

    fun clearAll() {
        interstitialCache.keys.toList().forEach { interstitialCache[it] = null }
        loadingPlacements.clear()
        pendingShowPlacements.clear()
        isinterstitialvisible = false
    }
}
