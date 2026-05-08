package com.example.background_remove.adsManager

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.background_remove.utils.RemoteConfigManager
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.photo.editor.background.eraser.bgeraser.R


object NativeAdManager {

    private var cachedNativeAd: NativeAd? = null
    private var lastAdUnit: String? = null

    enum class NativeStyle {
        ADVANCE, SMALL, FULL
    }

    /**
     * Load and show Native Ad with style param
     */
    fun loadAndShowNativeAd(
        activity: Activity,
        adUnitId: String,
        container: View, // should be FrameLayout or LinearLayout
        style: Int,
        shimmerFrameLayout: ShimmerFrameLayout,
        callback: ((loaded: Boolean, failed: Boolean, fromCache: Boolean) -> Unit)? = null
    ) {
        val parent = container as? ViewGroup ?: return
        if (!RemoteConfigManager.isAllAdsEnabled()) {
            callback?.invoke(false, true, false)
            return
        }
        if(!Ads.isNetworkAvailableWithoutPremium(activity)){
            callback?.invoke(false, true, false)
            return
        }
        // If cached ad → show immediately
        if (cachedNativeAd != null ) {
            Log.d("NativeAdManager", "Showing cached native ad")
            inflateAdView(activity, cachedNativeAd!!, parent, style, shimmerFrameLayout)
            callback?.invoke(true, false, true)
            cachedNativeAd = null
            return
        }
        if(style==0){
            shimmerFrameLayout.removeAllViews()
            val inflater = LayoutInflater.from(activity)
            val nativeLayout = inflater.inflate(R.layout.native_small_button_bottom,shimmerFrameLayout, false)
            shimmerFrameLayout.addView(nativeLayout)
        }else if(style==1){
            shimmerFrameLayout.removeAllViews()
            val inflater = LayoutInflater.from(activity)
            val nativeLayout = inflater.inflate(R.layout.native_advance_button_bottom,shimmerFrameLayout, false)
            shimmerFrameLayout.addView(nativeLayout)
        }else if(style==2){
            shimmerFrameLayout.removeAllViews()
            val inflater = LayoutInflater.from(activity)
            val nativeLayout = inflater.inflate(R.layout.native_small_inline,shimmerFrameLayout, false)
            shimmerFrameLayout.addView(nativeLayout)
        }else if(style==4){
            shimmerFrameLayout.removeAllViews()
            val inflater = LayoutInflater.from(activity)
            val nativeLayout = inflater.inflate(R.layout.full_screen_native,shimmerFrameLayout, false)
            shimmerFrameLayout.addView(nativeLayout)
        }

        shimmerFrameLayout.startShimmer()
        shimmerFrameLayout.visibility = View.VISIBLE


        val adOptions = NativeAdOptions.Builder()
            .setRequestCustomMuteThisAd(true)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .build()

        val adLoader = AdLoader.Builder(activity, adUnitId)
            .forNativeAd { nativeAd ->
                Log.d("NativeAdManager", "✅ Loaded: ${nativeAd.headline}")
                cachedNativeAd = nativeAd
                lastAdUnit = adUnitId

                inflateAdView(activity, nativeAd, parent, style, shimmerFrameLayout)
                callback?.invoke(true, false, false)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("NativeAdManager", "❌ Failed: ${error.message}")
                    shimmerFrameLayout.stopShimmer()
                    shimmerFrameLayout.visibility = View.GONE
                    parent.visibility = View.GONE
                    callback?.invoke(false, true, false)
                }
            })
            .withNativeAdOptions(adOptions)
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * Inflate ad layout based on style
     */
    private fun inflateAdView(
        activity: Activity,
        nativeAd: NativeAd,
        container: ViewGroup,
        style: Int,
        shimmerFrameLayout: ShimmerFrameLayout
    ) {
        container.removeAllViews()

        val layoutRes = when (style) {
            1 -> R.layout.native_advance_button_bottom
            0 -> R.layout.native_small_button_bottom
            2 -> R.layout.native_small_inline
            4 -> R.layout.full_screen_native
            else -> {
                R.layout.native_small_button_bottom}
        }

        val adView = LayoutInflater.from(activity).inflate(layoutRes, container, false) as NativeAdView
        populateNativeAd(nativeAd, adView)

        shimmerFrameLayout.stopShimmer()
        shimmerFrameLayout.visibility = View.GONE

        container.addView(adView)
        container.visibility = View.VISIBLE
        cachedNativeAd = null
    }

    /**
     * Populate views with ad content
     */
    private fun populateNativeAd(nativeAd: NativeAd, adView: NativeAdView) {
        try {
            adView.mediaView = adView.findViewById(R.id.ad_media)
            adView.headlineView = adView.findViewById(R.id.ad_headline)
            adView.bodyView = adView.findViewById(R.id.ad_body)
            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
            adView.iconView = adView.findViewById(R.id.ad_app_icon)
            adView.priceView = adView.findViewById(R.id.ad_price)
            adView.storeView = adView.findViewById(R.id.ad_store)
            adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

            (adView.headlineView as TextView).text = nativeAd.headline
            adView.mediaView?.mediaContent = nativeAd.mediaContent

            if (nativeAd.body == null) {
                adView.bodyView?.visibility = View.INVISIBLE
            } else {
                adView.bodyView?.visibility = View.VISIBLE
                (adView.bodyView as TextView).text = nativeAd.body
            }

            if (nativeAd.callToAction == null) {
                adView.callToActionView?.visibility = View.INVISIBLE
            } else {
                adView.callToActionView?.visibility = View.VISIBLE
                (adView.callToActionView as TextView).text = nativeAd.callToAction
                applyNativeCtaColor(adView.callToActionView)
            }

            if (nativeAd.icon == null) {
                adView.iconView?.visibility = View.GONE
            } else {
                (adView.iconView as ImageView).setImageDrawable(nativeAd.icon?.drawable)
                adView.iconView?.visibility = View.VISIBLE
            }

            if (nativeAd.price == null) {
                adView.priceView?.visibility = View.INVISIBLE
            } else {
                adView.priceView?.visibility = View.VISIBLE
                (adView.priceView as TextView).text = nativeAd.price
            }

            if (nativeAd.store == null) {
                adView.storeView?.visibility = View.INVISIBLE
            } else {
                adView.storeView?.visibility = View.VISIBLE
                (adView.storeView as TextView).text = nativeAd.store
            }

            if (nativeAd.advertiser == null) {
                adView.advertiserView?.visibility = View.INVISIBLE
            } else {
                adView.advertiserView?.visibility = View.VISIBLE
                (adView.advertiserView as TextView).text = nativeAd.advertiser
            }

            adView.setNativeAd(nativeAd)

            val vc = nativeAd.mediaContent?.videoController

            if (vc?.hasVideoContent() == true) {
                vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {
                    override fun onVideoEnd() {
                        super.onVideoEnd()
                        // Custom logic after video ends
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NativeAdManager", "populateNativeAd failed: ${e.message}")
        }
    }

    private fun applyNativeCtaColor(callToActionView: View?) {
        val target = callToActionView as? TextView ?: return
        val colorValue = RemoteConfigManager.getNativeButtonColor()
        if (colorValue.isBlank()) return
        val parsed = try {
            Color.parseColor(colorValue)
        } catch (_: Exception) {
            Log.w("NativeAdManager", "Invalid native_button_color: $colorValue")
            return
        }
        target.backgroundTintList = ColorStateList.valueOf(parsed)
    }
}
