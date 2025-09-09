package com.example.background_remove.adsManager

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
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
            adView.mediaView = adView.findViewById(R.id.ad_media) as? MediaView
            adView.bodyView = adView.findViewById(R.id.ad_body)
            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
            adView.iconView = adView.findViewById(R.id.icon)
            adView.starRatingView = adView.findViewById(R.id.ad_stars)

            val adHeadline: TextView = adView.findViewById(R.id.ad_headline)
            adHeadline.text = nativeAd.headline




            (adView.bodyView as? TextView)?.apply {
                text = nativeAd.body ?: ""
                visibility = if (nativeAd.body.isNullOrEmpty()) View.GONE else View.GONE
            }
            (adView.callToActionView as? Button)?.apply {
                text = nativeAd.callToAction ?: ""
                visibility = if (nativeAd.callToAction.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
            }
            val iconView = adView.iconView as? ImageView
            val iconDrawable = nativeAd.icon?.drawable

            if (iconDrawable != null) {
                iconView?.visibility = View.VISIBLE
                iconView?.setImageDrawable(iconDrawable)
            } else {
                iconView?.visibility = View.GONE
            }

            adView.setNativeAd(nativeAd)
        } catch (e: Exception) {
            Log.e("NativeAdManager", "populateNativeAd failed: ${e.message}")
        }
    }
}
