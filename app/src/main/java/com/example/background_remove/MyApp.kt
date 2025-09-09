package com.example.background_remove

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.adsManager.Ads.isNetworkAvailableWithoutPremium
import com.example.background_remove.adsManager.Ads.isinterstitialvisible
import com.example.background_remove.utils.Dialogs
import com.example.background_remove.utils.RemoteConfigManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.firebase.FirebaseApp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.Date


class MyApp : Application(), Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver, CoroutineScope by MainScope() {
    var ispause1=false
    var showAd = false
    var isShowingAd = false
    private var currentActivity: Activity? = null
    private var appOpenAdManager: AppOpenAdManager? = null
    private var countDownTimer: CountDownTimer? = null


    companion object {
        private const val LOG_TAG = "AppOpenAdManager"

    }
    override fun onCreate(){
        super<Application>.onCreate()

        registerActivityLifecycleCallbacks(this)
        Ads.shownow=false

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        launch(Dispatchers.IO) {

            try {
                MobileAds.initialize(this@MyApp)
            } catch (e: Exception) {

            }

        }
        appOpenAdManager = AppOpenAdManager()

        FirebaseApp.initializeApp(this)


    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)


        if (Ads.shownow && ispause1 && !isinterstitialvisible && !isShowingAd && RemoteConfigManager.isAppOpenEnabled()) {
            Handler(Looper.getMainLooper()).postDelayed({

                currentActivity?.let {
                    if (Ads.isNetworkAvailableWithoutPremium(it)) {
                        startCountdown(15000)
                        val aa=   Dialogs.showProgressDialogloading(it)
                        showAd=true
                        Handler(Looper.getMainLooper()).postDelayed({

                            if (aa) {
                                ispause1=false
                                loadAd(it)
                            }
                        }, 250)

                    }
                }
            }, 250)

        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        ispause1=true


    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {
        // An ad activity is started when an ad is showing, which could be AdActivity class from Google
        // SDK or another activity class implemented by a third party mediation partner. Updating the
        // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
        // one that shows the ad.
        if (!isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(p0: Activity) {

    }

    override fun onActivityPaused(p0: Activity) {

    }

    override fun onActivityStopped(p0: Activity) {

    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {

    }

    override fun onActivityDestroyed(p0: Activity) {

    }

    fun loadAd(activity: Activity) {
        // We wrap the loadAd to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdManager!!.loadAd(activity)
    }

    fun showAdIfAvailable(
        activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener
    ) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        appOpenAdManager!!.showAdIfAvailable(activity, onShowAdCompleteListener)
    }
    private fun startCountdown(timeInMillis: Long) {
        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                Log.d("abcd", "onTick: ${millisUntilFinished}")
            }

            override fun onFinish() {

                Dialogs.dismissProgressDialogloading()
                showAd=false

            }

        }.start()
    }
    /**
     * Interface definition for a callback to be invoked when an app open ad is complete
     * (i.e. dismissed or fails to show).
     */
    interface OnShowAdCompleteListener {
        fun onShowAdComplete()
    }

    private inner class AppOpenAdManager {

        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false



        private var loadTime: Long = 0


        fun loadAd(context: Context) {

            if (RemoteConfigManager.isAppOpenEnabled() && Ads.showadsinapp && Ads.isNetworkAvailableWithoutPremium(context)) {
                Log.d("ghjhgjhgjhg", "loadAd: 1")
                // Do not load ad if there is an unused ad or one is already loading.
                if (isLoadingAd || isAdAvailable || appOpenAd != null) {
                    showAdIfAvailable(context as Activity)
                    return
                }
                isLoadingAd = true

                val request = AdRequest.Builder().build()
                AppOpenAd.load(
                    context,
                    RemoteConfigManager.getAppOpenAdId(),
                    request,
                    object : AppOpenAd.AppOpenAdLoadCallback() {

                        override fun onAdLoaded(ad: AppOpenAd) {
                            ad.setImmersiveMode(true)
                            appOpenAd = ad
                            isLoadingAd = false
                            loadTime = Date().time
                            countDownTimer?.cancel()
                            if (RemoteConfigManager.isAppOpenEnabled() && showAd) {
                                showAdIfAvailable(context as Activity)
                            }

                            Log.d(LOG_TAG, "onAdLoaded.")


                        }


                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            isLoadingAd = false
                            appOpenAd = null
                            countDownTimer?.cancel()
                            Dialogs.dismissProgressDialogloading()
                            Log.d(LOG_TAG, "onAdFailedToLoad: " + loadAdError.message)

                        }
                    })
            }
        }


        private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
            val dateDifference = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        private val isAdAvailable: Boolean
            private get() =// Ad references in the app open beta will time out after four hours, but this time limit
            // may change in future beta versions. For details, see:
                // https://support.google.com/admob/answer/9341964?hl=en
                appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)

        fun showAdIfAvailable(
            activity: Activity,
            onShowAdCompleteListener: OnShowAdCompleteListener =
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        // Empty because the user will go back to the activity that shows the ad.
                    }
                }
        ) {
            Log.d("ghjhgjhgjhg", "loadAd: 2")
            // If the app open ad is already showing, do not show the ad again.
            if (isShowingAd) {
                Log.d(LOG_TAG, "The app open ad is already showing.")
                return
            }
            Log.d("ghjhgjhgjhg", "loadAd: 3")
            // If the app open ad is not available yet, invoke the callback then load the ad.
            if (!isAdAvailable) {
                Log.d(LOG_TAG, "The app open ad is not ready yet.")
                onShowAdCompleteListener.onShowAdComplete()



                return
            }
            Log.d("ghjhgjhgjhg", "loadAd: 4")
            Log.d(LOG_TAG, "Will show ad.")
            appOpenAd!!.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    // Set the reference to null so isAdAvailable() returns false.
                    appOpenAd = null
                    Dialogs.dismissProgressDialogloading()
                    isShowingAd = false
                    Ads.acitvitycountcheck = 0
                    Log.d(LOG_TAG, "onAdDismissedFullScreenContent.")
                    onShowAdCompleteListener.onShowAdComplete()
                    // AppopenupdateLastAdShownTime()


                }


                override fun onAdFailedToShowFullScreenContent(adError: AdError) {

                    isShowingAd = false
                    Log.d(LOG_TAG, "onAdFailedToShowFullScreenContent: " + adError.message)
                    Dialogs.dismissProgressDialogloading()
                    onShowAdCompleteListener.onShowAdComplete()


                }


                override fun onAdShowedFullScreenContent() {

                    Log.d(LOG_TAG, "onAdShowedFullScreenContent.")

                }
            }

            Log.d("ghjhgjhgjhg", "showAdIfAvailable: " + isinterstitialvisible)
            if (!isinterstitialvisible && RemoteConfigManager.isAppOpenEnabled() && Ads.showadsinapp && isNetworkAvailableWithoutPremium(activity)) {
                isShowingAd = true
                appOpenAd!!.show(activity)
            }
        }


    }
}