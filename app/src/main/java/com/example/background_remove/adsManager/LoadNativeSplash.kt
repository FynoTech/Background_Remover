package com.example.background_remove.adsManager
import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.photo.editor.background.eraser.bgeraser.R



object LoadNativeSplash {

    var nativeAdSplash: NativeAd? = null
    var adloaded=false


    fun loadNativeAd(
        adUnit: String = "",
        adContainer: FrameLayout,
        context: Activity,
        adtype: Boolean = true, loadingLayout: ShimmerFrameLayout
    ) {
        if(adtype){
            val inflater = LayoutInflater.from(context)
            val nativeLayout =
                inflater.inflate(R.layout.native_small_button_bottom,loadingLayout, false)

            adContainer.removeAllViews()
            // Optional: Clear existing views in the parent layout
            loadingLayout.removeAllViews()

            // Add the inflated layout to the parent
            loadingLayout.addView(nativeLayout)
            adContainer.addView(loadingLayout)

            // Optionally make it visible
            nativeLayout.visibility = View.VISIBLE
        }

            val videoOptions = VideoOptions.Builder().setStartMuted(true).build()
            val adOptions = NativeAdOptions.Builder()
                .setRequestCustomMuteThisAd(true)
                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                .setVideoOptions(videoOptions).build()

            val builder = AdLoader.Builder(context, adUnit)


                builder.forNativeAd { loadedNativeAd ->
                   nativeAdSplash =loadedNativeAd
                    Log.d("NativeAd15splash", "Ad loaded successfully ${loadedNativeAd.headline}.")

            }

            val adLoader = builder.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    if (loadAdError.code == 3)
                        adloaded =true
                    Log.e("NativeAdsplash", "Failed to load ad: ${loadAdError.message}")
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    showNativeAd(adContainer,context,adtype,loadingLayout)
                    adloaded =true
                    Log.d("NativeAd12splash", "Ad ${context::class.simpleName} type splash")
                    Log.d("NativeAd12splash", "Ad loaded successfully.")
                }
            }).withNativeAdOptions(adOptions).build()

            adLoader.loadAd(AdRequest.Builder().build())

    }



    fun showNativeAd(
        adContainer: FrameLayout,
        context: Activity,
      adtype: Boolean = true, loadingLayout: ShimmerFrameLayout
    ) {
        Log.d("NativeAd15splash","show ${adtype}")
        val nativeAdToShow = nativeAdSplash
        adContainer.visibility = View.VISIBLE
        if(adtype){
            val inflater = LayoutInflater.from(context)
            val nativeLayout = inflater.inflate(R.layout.native_small_button_bottom,loadingLayout, false)

            // Optional: Clear existing views in the parent layout
            loadingLayout.removeAllViews()

            // Add the inflated layout to the parent
            loadingLayout.addView(nativeLayout)

            // Optionally make it visible
            nativeLayout.visibility = View.VISIBLE
        }

        if (nativeAdToShow != null) {
            var adView =   context.layoutInflater.inflate(R.layout.native_small_button_bottom, null) as NativeAdView



            populateNativeAd(nativeAdToShow, adView,adtype)
            Log.d("NativeAd15splash",nativeAdToShow.headline.toString())

            adContainer.removeAllViews()
            adContainer.addView(adView)
            adContainer.invalidate()


            nativeAdSplash =null


        } else {
            Log.e("NativeAd", "Native ad for splash is not loaded yet.")
        }
    }

    private fun populateNativeAd(
        nativeAd: NativeAd,
        adView: NativeAdView,
        switch: Boolean,
    ) {

        try {
            (adView.findViewById<View>(R.id.icon) as ImageView).setImageDrawable(nativeAd.icon?.drawable)
        } catch (e: Exception) {
            Log.e("NativeAd", "Error loading ad icon: ${e.message}")
        }

        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)

        (adView.headlineView as TextView?)?.text = nativeAd.headline
        (adView.bodyView as TextView?)?.apply {
            text = nativeAd.body ?: ""
            visibility = if (nativeAd.body.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        }
        (adView.callToActionView as Button?)?.apply {
            text = nativeAd.callToAction ?: ""
            visibility = if (nativeAd.callToAction.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        }
        (adView.starRatingView as RatingBar?)?.apply {
            rating = nativeAd.starRating?.toFloat() ?: 0f
        }

        adView.setNativeAd(nativeAd)
    }
}
