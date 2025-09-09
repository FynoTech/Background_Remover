package com.example.background_remove.adsManager

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.example.background_remove.adsManager.enums.NativeAdIcon
import com.example.background_remove.adsManager.enums.NativeAdType
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.photo.editor.background.eraser.bgeraser.R


object AdNative {
    private var textColorTitle: String? = null
    private var textColorDescription: String? = null
    private var textColorButton: String? = null
    private var colorButton: String? = null
    private var backgroundColor: String? = null
    private var buttonRoundness: Int = 0
    private var adIcon: NativeAdIcon? = null
    private var callback: ((loaded: Boolean, failed: Boolean) -> Unit)? = null
    var nativeAd: NativeAd? = null
    var nativeAdfullscreen: NativeAd? = null




    fun textColorTitle(color: String): AdNative {
        textColorTitle = color
        return this
    }

    fun textColorDescription(color: String): AdNative {
        textColorDescription = color
        return this
    }

    fun textColorButton(color: String): AdNative {
        textColorButton = color
        return this
    }

    fun buttonRoundness(buttonround: Int): AdNative {
        buttonRoundness = buttonround
        return this
    }

    fun colorButton(color: String): AdNative {
        colorButton = color
        return this
    }

    fun backgroundColor(color: String): AdNative {
        backgroundColor = color
        return this
    }

    fun adIcon(icon: NativeAdIcon): AdNative {
        adIcon = icon
        return this
    }



    fun callback(callback: ((loaded: Boolean, failed: Boolean) -> Unit)?): AdNative {
        AdNative.callback = callback
        return this
    }
var nativeisloading=false
var nativeexitisloading=false
    fun load(
        ctx: Activity,
        nativeAdContainer: FrameLayout,
        id: String,
        nativeAdType: NativeAdType,
        switch: Boolean,
        loadingLayout: ShimmerFrameLayout
    ) {
        // view visibility gone if there is no internet

        if ((!isOnline(ctx) && !Ads.isNetworkAvailableWithoutPremium(ctx)) ) {
            nativeAdContainer.visibility = View.GONE
            return
        }
        if(nativeisloading){
            Handler(Looper.getMainLooper()).postDelayed({
               load(ctx, nativeAdContainer, id, nativeAdType, switch, loadingLayout)
            }, 2000)
        }else{
            if (nativeAd ==null) {
                nativeAdContainer.visibility = View.VISIBLE
                if(switch){
                    val inflater = LayoutInflater.from(ctx)
                    val nativeLayout = inflater.inflate(R.layout.native_advance_button_bottom,loadingLayout, false)

                    // Optional: Clear existing views in the parent layout
                    loadingLayout.removeAllViews()

                    // Add the inflated layout to the parent
                    loadingLayout.addView(nativeLayout)

                    // Optionally make it visible
                    nativeLayout.visibility = View.VISIBLE
                }else{
                    val inflater = LayoutInflater.from(ctx)
                    val nativeLayout = inflater.inflate(R.layout.native_small_button_bottom,loadingLayout, false)

                    // Optional: Clear existing views in the parent layout
                    loadingLayout.removeAllViews()

                    // Add the inflated layout to the parent
                    loadingLayout.addView(nativeLayout)

                    // Optionally make it visible
                    nativeLayout.visibility = View.VISIBLE
                }

                nativeisloading =true
                val builder = AdLoader.Builder(ctx, id)

                builder.forNativeAd { nativeAd ->
                    Log.e("546546546546","native laoding ${nativeAd.headline}")
                    AdNative.nativeAd =nativeAd
                    val nativeView: NativeAdView = when (switch) {
                        false -> {

                            ctx.layoutInflater.inflate(
                                R.layout.native_small_button_bottom,
                                null
                            ) as NativeAdView

                        }

                        true -> {

                            ctx.layoutInflater.inflate(
                                R.layout.native_advance_button_bottom,
                                null
                            ) as NativeAdView


                        }

                        else -> {
                            NativeAdView(ctx)
                        }
                    }
                    nativeAdView(
                        ctx,
                        nativeAd,
                        nativeView,
                        nativeAdType,
                        textColorTitle,
                        textColorDescription,
                        textColorButton,
                        backgroundColor,
                        colorButton,
                        adIcon,
                        buttonRoundness,switch
                    )
                    nativeAdContainer.removeAllViews()
                    nativeAdContainer.addView(nativeView)

                }

                val adLoader = builder.withAdListener(object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("abcd234234","native $nativeAd")
                        nativeAdContainer.layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT
                        callback?.invoke(true, false)
                        nativeisloading =false
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        Log.d("abcd234234","shown")
                        nativeAd =null
                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        nativeAdContainer.visibility = View.GONE
                        nativeisloading =false
                        Log.d("abcd1212", p0.responseInfo.toString())
                        callback?.invoke(false, true)
                    }
                }).build()
                adLoader.loadAd(AdManagerAdRequest.Builder().build())
            } else {
                Log.d("abcd234234","native laded $nativeAd")
                nativeAdContainer.visibility = View.VISIBLE
                nativeAdContainer.removeAllViews()
                if(switch){
                    val inflater = LayoutInflater.from(ctx)
                    val nativeLayout = inflater.inflate(R.layout.native_advance_button_bottom,loadingLayout, false)

                    // Optional: Clear existing views in the parent layout
                    loadingLayout.removeAllViews()

                    // Add the inflated layout to the parent
                    loadingLayout.addView(nativeLayout)

                    // Optionally make it visible
                    nativeLayout.visibility = View.VISIBLE
                }else{
                    val inflater = LayoutInflater.from(ctx)
                    val nativeLayout = inflater.inflate(R.layout.native_small_button_bottom,loadingLayout, false)

                    // Optional: Clear existing views in the parent layout
                    loadingLayout.removeAllViews()

                    // Add the inflated layout to the parent
                    loadingLayout.addView(nativeLayout)

                    // Optionally make it visible
                    nativeLayout.visibility = View.VISIBLE
                }
                val nativeView: NativeAdView = when (switch) {
                    false -> {

                        ctx.layoutInflater.inflate(
                            R.layout.native_small_button_bottom,
                            null
                        ) as NativeAdView

                    }

                   true -> {

                        ctx.layoutInflater.inflate(
                            R.layout.native_advance_button_bottom,
                            null
                        ) as NativeAdView


                    }

                    else -> {
                        NativeAdView(ctx)
                    }
                }
                nativeAdView(
                    ctx,
                    nativeAd!!,
                    nativeView,
                    nativeAdType,
                    textColorTitle,
                    textColorDescription,
                    textColorButton,
                    backgroundColor,
                    colorButton,
                    adIcon,
                    buttonRoundness,switch
                )
                nativeAdContainer.removeAllViews()
                nativeAdContainer.addView(nativeView)
                nativeAd =null
            }
        }

    }


    fun loadfullscreen(
        ctx: Activity,
        nativeAdContainer: FrameLayout,
        id: String,
        nativeAdType: NativeAdType
    ) {
        // view visibility gone if there is no internet

        if ((!isOnline(ctx) && !Ads.isNetworkAvailableWithoutPremium(ctx)) ) {
            nativeAdContainer.visibility = View.GONE
            return
        }

            if (nativeAdfullscreen ==null) {
                nativeAdContainer.visibility = View.VISIBLE

                val builder = AdLoader.Builder(ctx, id)
                Log.d("abcd234234","native laoding")
                builder.forNativeAd { nativeAd ->
                    Log.d("abcd234234","native laded ${nativeAd?.headline}")
                    nativeAdfullscreen =nativeAd
                    val nativeView: NativeAdView =  ctx.layoutInflater.inflate(
                        R.layout.full_screen_native,
                        null
                    ) as NativeAdView
                    nativeAdView(
                        ctx,
                        nativeAdfullscreen!!,
                        nativeView,
                        nativeAdType,
                        textColorTitle,
                        textColorDescription,
                        textColorButton,
                        backgroundColor,
                        colorButton,
                        adIcon,
                        buttonRoundness,true
                    )
                    nativeAdContainer.removeAllViews()
                    nativeAdContainer.addView(nativeView)

                }

                val adLoader = builder.withAdListener(object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("abcd234234","native full screen $nativeAdfullscreen")
                        nativeAdContainer.layoutParams.height = FrameLayout.LayoutParams.WRAP_CONTENT
                        callback?.invoke(true, false)

                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        Log.d("abcd234234","shown")

                    }

                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        nativeAdContainer.visibility = View.GONE
                        Log.d("abcd1212", p0.responseInfo.toString())
                        callback?.invoke(false, true)
                    }
                }).build()
                adLoader.loadAd(AdManagerAdRequest.Builder().build())
            } else {
                callback?.invoke(true, false)
                Log.d("abcd234234","native laded $nativeAdfullscreen")
                nativeAdContainer.visibility = View.VISIBLE
                nativeAdContainer.removeAllViews()

                val nativeView: NativeAdView =  ctx.layoutInflater.inflate(
                    R.layout.full_screen_native,
                    null
                ) as NativeAdView

                nativeAdView(
                    ctx,
                    nativeAdfullscreen!!,
                    nativeView,
                    nativeAdType,
                    textColorTitle,
                    textColorDescription,
                    textColorButton,
                    backgroundColor,
                    colorButton,
                    adIcon,
                    buttonRoundness,true
                )
                nativeAdContainer.removeAllViews()
                nativeAdContainer.addView(nativeView)

            }
        }




    private fun nativeAdView(
        context: Context,
        nativeAd: NativeAd,
        adView: NativeAdView,
        nativeAdType: NativeAdType,
        textColorTitle: String?,
        textColorDescription: String?,
        textColorButton: String?,
        backgroundColor: String?,
        colorButton: String?,
        adIcon: NativeAdIcon?,
        buttonRoundness: Int,
        switch: Boolean
    ) {


        if (nativeAdType== NativeAdType.NativeAdvance && switch) {
            adView.mediaView = adView.findViewById<View>(R.id.ad_media) as MediaView

            adView.mediaView!!.mediaContent = nativeAd.mediaContent
        }
        try {
            (adView.findViewById<View>(R.id.icon) as ImageView).setImageDrawable(nativeAd.icon!!.drawable)
        } catch (e: Exception) {

        }

        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        try {
            if( nativeAd.starRating!=null){
                (adView.starRatingView as RatingBar?)!!.rating = nativeAd.starRating!!.toFloat()
            }
        } catch (e: Exception) {

        }




        (adView.headlineView as TextView?)!!.text = nativeAd.headline
        (adView.headlineView as TextView?)!!.isSelected = true
        if (textColorTitle != null) (adView.headlineView as TextView?)!!.setTextColor(
            Color.parseColor(
                textColorTitle
            )
        )
        try {
            if(nativeAdType== NativeAdType.NativeAdvance && nativeAd.starRating!=null){
                (adView.starRatingView as RatingBar?)!!.rating = nativeAd.starRating!!.toFloat()
            }
        } catch (e: Exception) {

        }
        if (nativeAd.body == null) {
            adView.bodyView!!.visibility = View.INVISIBLE
        } else {
            adView.bodyView!!.visibility = View.VISIBLE
            (adView.bodyView as TextView?)!!.text = nativeAd.body
            if (textColorDescription != null) (adView.bodyView as TextView?)!!.setTextColor(
                Color.parseColor(
                    textColorDescription
                )
            )
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView!!.visibility = View.INVISIBLE
        } else {
            adView.callToActionView!!.visibility = View.VISIBLE
            (adView.callToActionView as TextView?)!!.text = nativeAd.callToAction

            val backgroundDrawable = GradientDrawable()
            backgroundDrawable.cornerRadius =
                buttonRoundness.dpToPx(context).toFloat() // Adjust the corner radius as needed
            if (colorButton != null) backgroundDrawable.setColor(Color.parseColor(colorButton))
            //   adView.callToActionView!!.background = backgroundDrawable // Set the custom background

            //if (colorButton != null) (adView.callToActionView as Button?)!!.setBackgroundColor(Color.parseColor(colorButton))
            if (textColorButton != null) (adView.callToActionView as TextView?)!!.setTextColor(
                Color.parseColor(
                    textColorButton
                )
            )
        }

        adView.setNativeAd(nativeAd)
    }

    private fun Int.dpToPx(context: Context): Int {
        val scale = context.resources.displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }

    fun loadNativeAd(
        ctx: Activity,
        nativeAdContainer: FrameLayout,
        id: String,
        nativeAdType: NativeAdType,
        switch: Boolean = true,
        loadingLayout: ShimmerFrameLayout
    ): AdNative {
        load(ctx, nativeAdContainer, id, nativeAdType,switch,loadingLayout)
        return this
    }
    fun loadNativeAdfullsrceen(
        ctx: Activity,
        nativeAdContainer: FrameLayout,
        id: String,
        nativeAdType: NativeAdType
    ): AdNative {
        loadfullscreen(ctx, nativeAdContainer, id, nativeAdType)
        return this
    }
}
