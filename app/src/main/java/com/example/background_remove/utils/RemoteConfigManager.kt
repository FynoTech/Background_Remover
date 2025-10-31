package com.example.background_remove.utils

import android.util.Log
import com.google.firebase.Firebase

import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.photo.editor.background.eraser.bgeraser.BuildConfig


object RemoteConfigManager {

    private val remoteConfig = Firebase.remoteConfig

    // --- AdMob Test IDs ---
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/2014213617"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"

    fun init() {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
    }

    fun fetchAndActivate(onComplete: (() -> Unit)? = null) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("RemoteConfig", "✅ Fetch successful")
                } else {
                    Log.w("RemoteConfig", "❌ Fetch failed", task.exception)
                }
                onComplete?.invoke()
            }
    }

    // 🔹 Generic helpers
    private fun getAdId(key: String, testId: String): String {
        return if (BuildConfig.DEBUG) testId else remoteConfig.getString(key).ifEmpty { testId }
    }

    private fun getBoolean(key: String, default: Boolean = true): Boolean {
        return remoteConfig.getBoolean(key).takeIf { remoteConfig.getKeysByPrefix("").contains(key) } ?: default
    }

    private fun getNumber(key: String, default: Long = 0): Int {
        return (remoteConfig.getLong(key).takeIf { remoteConfig.getKeysByPrefix("").contains(key) } ?: default).toInt()
    }

    // 🔹 Ad IDs
    fun getAppOpenAdId(): String = getAdId("App_Open", TEST_APP_OPEN)
    fun getSplashAppOpenAdId(): String = getAdId("Splash_AppOpen", TEST_APP_OPEN)
    fun getSplashBannerAdId(): String = getAdId("Splash_Banner", TEST_BANNER)
    fun getSplashInterstitialAdId(): String = getAdId("Splash_Interstitial", TEST_INTERSTITIAL)
    fun getSplashNativeAdId(): String = getAdId("Splash_Native", TEST_NATIVE)

    fun getHomeNativeAdId(): String = getAdId("Home_Native", TEST_NATIVE)
    fun getHomeBannerAdId(): String = getAdId("Home_Banner", TEST_BANNER)



    fun getSlidedrawerNativeAdId(): String = getAdId("Slidedrawer_Native", TEST_NATIVE)
    fun getSlidedrawerBannerAdId(): String = getAdId("Slidedrawer_Banner", TEST_BANNER)

    fun getResultNativeAdId(): String = getAdId("Result_Native", TEST_NATIVE)
    fun getCropperNativeAdId(): String = getAdId("Cropper_Native", TEST_NATIVE)
    fun getEditorNativeAdId(): String = getAdId("Editor_Native", TEST_NATIVE)
    fun getResultBannerAdId(): String = getAdId("Result_Banner", TEST_BANNER)
    fun getCropperBannerAdId(): String = getAdId("Cropper_Banner", TEST_BANNER)
    fun getEditorBannerAdId(): String = getAdId("Editor_Banner", TEST_BANNER)
    fun resultnativelayout()= getNumber("result_native_layout")
    fun croppernativelayout()= getNumber("cropper_native_layout")

    fun getCounterInterstitialAdId(): String = getAdId("Counter_Interstitial", TEST_INTERSTITIAL)
    fun getRewardedAutoAdId(): String = getAdId("Rewarded_Auto", TEST_REWARDED)

    fun getRewaredAdId(): String = getAdId("Rewarded_Ad", TEST_REWARDED)

    fun getOnBoardingNativeAdId(): String = getAdId("Onboarding_Native", TEST_NATIVE)
    fun getOnBoardingBannerId(): String = getAdId("Onboarding_Banner", TEST_BANNER)
    fun getOnBoardingInterstitialId(): String = getAdId("Onboarding_Interstitial", TEST_INTERSTITIAL)
    fun getOnBoardingNativeAdIdFull1(): String = getAdId("Onboarding_Native_ad_full_1", TEST_NATIVE)
    fun getOnBoardingNativeAdIdFull2(): String = getAdId("Onboarding_Native_ad_full_2", TEST_NATIVE)


    fun getLanguageInterstitialAdId(): String = getAdId("Language_Interstitial", TEST_INTERSTITIAL)
    fun getLanguageBannerAdId(): String = getAdId("Language_Banner", TEST_BANNER)
    fun getLanguageNativeAdId(): String = getAdId("Language_Native", TEST_NATIVE)

    // 🔹 Enable/disable toggles
    fun isSplashInterstitialEnabled() = getBoolean("splash_interstital_enable")

    fun isLanguageInterstitialEnabled() = getBoolean("language_interstital_enable")
    fun isCounterInterstitialEnabled() = getBoolean("counter_interstital_enable")



    fun isSplashBannerEnabled() = getBoolean("splash_banner_enable")
    fun isLanguageBannerEnabled() = getBoolean("language_banner_enable")


    fun isHomeBannerEnabled() = getBoolean("home_banner_enable")

    fun isSidedrawerBannerEnabled() = getBoolean("sidedrawer_banner_enable")
    fun isResultBannerEnabled() = getBoolean("result_banner_enable")
    fun iscropperBannerEnabled() = getBoolean("cropper_banner_enable")

    fun isEditorBannerEnabled() = getBoolean("editor_banner_enable")



    fun isHomeNativeEnabled() = getBoolean("Home_Native_enable")

    fun isEditorNativeEnabled() = getBoolean("Editor_Native_enable")
    fun isLanguageNativeEnabled() = getBoolean("Language_native_enable")
    fun isSplashNativeEnabled() = getBoolean("splash_native_enable")

    fun isSideDrawerNativeEnabled() = getBoolean("sidedrawer_native_enable")
    fun isResultNativeEnabled() = getBoolean("result_native_enable")
    fun iscropperNativeEnabled() = getBoolean("cropper_native_enable")


    fun homenativelayout()= getNumber("home_native_layout")
    fun languagenativelayout()= getNumber("language_native_layout")

    fun slidenativelayout()= getNumber("slidedrawer_native_layout")
    fun splashnativelayout()= getNumber("splash_native_layout")

    fun onboardingnativelayout()= getNumber("onboarding_native_layout")



    fun isRewardedAdEnabled() = getBoolean("rewarded_enable")

    fun isRewardedAutoAdEnabled() = getBoolean("rewarded_Auto_enable")


    fun getCounterInterstitialCount(): Int = getNumber("Counter_Interstitial_count", 3)
    fun getForceUpdate(): Int = getNumber("force_update_version", 0)

    fun isOnboardingNativeEnabled() = getBoolean("Onboarding_native_enable")

    fun isOnboardingBannerEnabled() = getBoolean("Onboarding_banner_enable")
    fun isOnboardingInterstitalEnabled() = getBoolean("Onboarding_interstital_enable")
    fun isOnboardingNativeFull1Enabled() = getBoolean("Onboarding_nativefull_1_enable")
    fun isOnboardingNativeFull2Enabled() = getBoolean("Onboarding_nativefull_2_enable")


    fun isAppOpenEnabled() = getBoolean("App_Open_enable")
    fun isSplashAppOpenEnabled() = getBoolean("splash_open_ad_enable")
}
