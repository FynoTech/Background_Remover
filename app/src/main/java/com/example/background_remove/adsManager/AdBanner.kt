package com.example.background_remove.adsManager

import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.FrameLayout
import com.facebook.shimmer.ShimmerFrameLayout

import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError


object AdBanner {

    private var shimmerEffect: Boolean = false

    private var shimmerBackgroundColor: String? = null
    private var callback: ((loaded: Boolean, failed: Boolean) -> Unit)? = null
    var banneradloaded=false
    fun shimmerEffect(effect: Boolean): AdBanner {
        shimmerEffect = effect
        return this
    }


    fun shimmerBackgroundColor(color: String): AdBanner {
        shimmerBackgroundColor = color
        return this
    }

    fun callback(callback: ((loaded: Boolean, failed: Boolean) -> Unit)?): AdBanner {
        AdBanner.callback = callback
        return this
    }
    fun loadbannersplash(
        ctx: Activity,
        bannerAdContainer: FrameLayout,
        id: String,
        Collapsible: Boolean,
        top: Boolean,
        shimmerFrameLayout: ShimmerFrameLayout
    ) {
        // view visibility gone if there is no internet
        if (!Ads.isNetworkAvailableWithoutPremium(ctx)) {
            bannerAdContainer.visibility = View.GONE
            return
        }

        bannerAdContainer.visibility = View.VISIBLE
        bannerAdContainer.minimumHeight = 110
        bannerAdContainer.layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT

        val adview = AdView(ctx)


        adview.adUnitId = id


        val adRequest: AdRequest = when (Collapsible) {
            false -> AdRequest.Builder().build()
            true -> {
                val extras = Bundle()
                if (top) {
                    extras.putString("collapsible", "top")
                } else {
                    extras.putString("collapsible", "bottom")
                }
                AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
            }

            else -> {
                AdRequest.Builder().build()
            }

        }

        val adSize: AdSize? = getAdSize(ctx)
        adview.setAdSize(adSize!!)
        adview.loadAd(adRequest)

        adview.adListener = object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                bannerAdContainer.visibility = View.GONE
                callback?.invoke(false, true)
                banneradloaded =false
            }

            override fun onAdLoaded() {
                bannerAdContainer.addView(adview)
                banneradloaded =true
                bannerAdContainer.layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT
                shimmerFrameLayout.stopShimmer()
                shimmerFrameLayout.visibility = View.INVISIBLE
                callback?.invoke(true, false)

            }
        }
    }
    fun load(
        ctx: Activity,
        adview: AdView,
        bannerAdContainer: FrameLayout,
        id: String,
        bannerAdType: Boolean,
        top: Boolean,
        shimmerFrameLayout: ShimmerFrameLayout
    ) {
        // view visibility gone if there is no internet
        if (!isOnline(ctx)) {
            bannerAdContainer.visibility = View.GONE
            return
        }

        bannerAdContainer.visibility = View.VISIBLE
        bannerAdContainer.minimumHeight = 110
        bannerAdContainer.layoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT
        bannerAdContainer.removeAllViews()



        adview.adUnitId = id


        val adRequest: AdRequest = when (bannerAdType) {
            false -> AdRequest.Builder().build()
            true -> {
                val extras = Bundle()
                if (top) {
                    extras.putString("collapsible", "top")
                } else {
                    extras.putString("collapsible", "bottom")
                }
                AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
            }

            else -> {
                AdRequest.Builder().build()
            }

        }

        val adSize: AdSize? = getAdSize(ctx)
        adview.setAdSize(adSize!!)
        adview.loadAd(adRequest)

        adview.adListener = object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                bannerAdContainer.visibility = View.GONE
                callback?.invoke(false, true)
                banneradloaded =false
            }

            override fun onAdLoaded() {
                banneradloaded =true
                bannerAdContainer.layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT
                bannerAdContainer.removeAllViews()
                shimmerFrameLayout.stopShimmer()
                shimmerFrameLayout.visibility = View.INVISIBLE
                bannerAdContainer.addView(adview)
                callback?.invoke(true, false)

            }
        }
    }

    private fun getAdSize(activity: Activity): AdSize {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    fun loadBannerAdsplash(
        ctx: Activity,
        bannerAdContainer: FrameLayout,
        id: String,
        bannerAdType: Boolean,shimmerFrameLayout: ShimmerFrameLayout,top:Boolean=false
    ): AdBanner {
        loadbannersplash(ctx, bannerAdContainer, id, bannerAdType,top,shimmerFrameLayout)
        return AdBanner
    }
    fun loadBannerAd(
        ctx: Activity,
        adview: AdView,
        bannerAdContainer: FrameLayout,
        id: String,
        bannerAdType: Boolean,shimmerFrameLayout: ShimmerFrameLayout,top:Boolean=false
    ): AdBanner {
        load(ctx, adview, bannerAdContainer, id, bannerAdType,top,shimmerFrameLayout)
        return AdBanner
    }
}