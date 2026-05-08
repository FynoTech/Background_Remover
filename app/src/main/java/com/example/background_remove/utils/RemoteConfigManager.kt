package com.example.background_remove.utils

import android.util.Log
import com.google.firebase.Firebase

import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.photo.editor.background.eraser.bgeraser.BuildConfig


object RemoteConfigManager {

    private val remoteConfig = Firebase.remoteConfig

    private const val KEY_ADS_CONTROL = "ads_control"

    /** Non-null after valid `ads_control` JSON parse; null → legacy flat RC keys. */
    private var adsControl: AdsControlRoot? = null

    // --- AdMob Test IDs ---
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/2014213617"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
    // Exit placements: use provided production IDs as release fallback.
    private const val EXIT_BANNER_FALLBACK = "ca-app-pub-1031204555702774/1323905740"
    private const val EXIT_NATIVE_FALLBACK = "ca-app-pub-1031204555702774/1796893471"
    private const val EXIT_INTERSTITIAL_FALLBACK = "ca-app-pub-1031204555702774/2315768224"

    fun init() {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        refreshAdsControlFromRemote()
    }

    fun fetchAndActivate(onComplete: (() -> Unit)? = null) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("RemoteConfig", "✅ Fetch successful")
                } else {
                    Log.w("RemoteConfig", "❌ Fetch failed", task.exception)
                }
                refreshAdsControlFromRemote()
                onComplete?.invoke()
            }
    }

    private fun rcHasKey(key: String): Boolean =
        remoteConfig.getKeysByPrefix("").contains(key)

    private fun refreshAdsControlFromRemote() {
        val raw =
            if (rcHasKey(KEY_ADS_CONTROL)) remoteConfig.getString(KEY_ADS_CONTROL).trim() else ""
        adsControl = AdsControlJson.parse(raw)
    }

    private fun flagEnabled(value: Int): Boolean = value == 1

    private fun adsGloballyAllowed(): Boolean {
        adsControl?.let { return flagEnabled(it.allAdsOnOff) }
        return getNumber("all_ads_onoff", 1) == 1
    }

    /** `false` when `all_ads_onoff` is 0 (JSON or flat RC key). Force update still uses [getForceUpdate]. */
    fun isAllAdsEnabled(): Boolean = adsGloballyAllowed()

    private fun adFlag(jsonBlock: () -> Boolean?, legacy: () -> Boolean): Boolean {
        if (!adsGloballyAllowed()) return false
        return jsonBlock() ?: legacy()
    }

    // 🔹 Generic helpers
    private fun getAdId(key: String, testId: String): String {
        return if (BuildConfig.DEBUG) testId
        else if (!rcHasKey(key)) testId
        else remoteConfig.getString(key).ifEmpty { testId }
    }

    /**
     * Reads ad unit from the first non-empty key in [keys].
     * Useful when RC naming changed but old keys must continue working.
     */
    private fun getAdIdFromAny(keys: Array<String>, fallbackId: String): String {
        for (key in keys) {
            if (!rcHasKey(key)) continue
            val id = remoteConfig.getValue(key).asString().trim()
            if (id.isNotEmpty()) return id
        }
        return fallbackId
    }

    private fun getBoolean(key: String, default: Boolean = true): Boolean {
        if (!rcHasKey(key)) return default
        val raw = remoteConfig.getValue(key).asString().trim().lowercase()
        return when (raw) {
            "1", "true", "yes", "on" -> true
            "0", "false", "no", "off" -> false
            else -> default
        }
    }

    private fun getNumber(key: String, default: Long = 0): Int {
        if (!rcHasKey(key)) return default.toInt()
        val raw = remoteConfig.getValue(key).asString().trim()
        return raw.toLongOrNull()?.toInt() ?: default.toInt()
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
    fun getExitNativeAdId(): String =
        getAdIdFromAny(
            arrayOf("exit_native", "Exit_Native"),
            if (BuildConfig.DEBUG) TEST_NATIVE else EXIT_NATIVE_FALLBACK,
        )
    fun getCropperNativeAdId(): String = getAdId("Cropper_Native", TEST_NATIVE)
    fun getEditorNativeAdId(): String = getAdId("Editor_Native", TEST_NATIVE)
    fun getResultBannerAdId(): String = getAdId("Result_Banner", TEST_BANNER)
    fun getExitBannerAdId(): String =
        getAdIdFromAny(
            arrayOf("exit_banner", "Exit_Banner"),
            if (BuildConfig.DEBUG) TEST_BANNER else EXIT_BANNER_FALLBACK,
        )
    fun getCropperBannerAdId(): String = getAdId("Cropper_Banner", TEST_BANNER)
    fun getEditorBannerAdId(): String = getAdId("Editor_Banner", TEST_BANNER)
    fun resultnativelayout() =
        adsControl?.result?.nativeLayout ?: getNumber("result_native_layout")
    fun croppernativelayout() =
        adsControl?.cropper?.nativeLayout ?: getNumber("cropper_native_layout")

    fun getCounterInterstitialAdId(): String = getAdId("Counter_Interstitial", TEST_INTERSTITIAL)

    /** Exit interstitial unit: dedicated key, then other interstitial IDs, then Google test ID. */
    fun getHomeExitInterstitialAdId(): String {
        val keys = arrayOf(
            "exist_interstitial",
            "exit_interstitial",
            "Home_Exit_Interstitial",
            "Counter_Interstitial",
            "Splash_Interstitial",
            "Language_Interstitial",
            "Onboarding_Interstitial",
        )
        return getAdIdFromAny(
            keys,
            if (BuildConfig.DEBUG) TEST_INTERSTITIAL else EXIT_INTERSTITIAL_FALLBACK,
        )
    }
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
    fun isSplashInterstitialEnabled() = adFlag(
        { adsControl?.splash?.let { flagEnabled(it.interstitial) } },
        { getBoolean("splash_interstital_enable") },
    )

    fun isLanguageInterstitialEnabled() = adFlag(
        { adsControl?.language?.let { flagEnabled(it.interstitial) } },
        { getBoolean("language_interstital_enable") },
    )

    fun isCounterInterstitialEnabled() = adFlag(
        { adsControl?.counter?.let { flagEnabled(it.interstitial) } },
        { getBoolean("counter_interstital_enable") },
    )



    fun isSplashBannerEnabled() = adFlag(
        { adsControl?.splash?.let { flagEnabled(it.banner) } },
        { getBoolean("splash_banner_enable") },
    )

    fun isLanguageBannerEnabled() = adFlag(
        { adsControl?.language?.let { flagEnabled(it.banner) } },
        { getBoolean("language_banner_enable") },
    )


    fun isHomeBannerEnabled() = adFlag(
        { adsControl?.home?.let { flagEnabled(it.banner) } },
        { getBoolean("home_banner_enable") },
    )

    fun isSidedrawerBannerEnabled() = adFlag(
        { adsControl?.slidedrawer?.let { flagEnabled(it.banner) } },
        { getBoolean("sidedrawer_banner_enable") },
    )

    fun isResultBannerEnabled() = adFlag(
        { adsControl?.result?.let { flagEnabled(it.banner) } },
        { getBoolean("result_banner_enable") },
    )

    fun isExitBannerEnabled() = adFlag(
        { adsControl?.exit?.let { flagEnabled(it.banner) } },
        { getBoolean("exit_banner_enable") },
    )

    fun iscropperBannerEnabled() = adFlag(
        { adsControl?.cropper?.let { flagEnabled(it.banner) } },
        { getBoolean("cropper_banner_enable") },
    )

    fun isEditorBannerEnabled() = adFlag(
        { adsControl?.editor?.let { flagEnabled(it.banner) } },
        { getBoolean("editor_banner_enable") },
    )



    fun isHomeNativeEnabled() = adFlag(
        { adsControl?.home?.let { flagEnabled(it.native) } },
        { getBoolean("Home_Native_enable") },
    )

    fun isHomeBannerFailNativeEnabled(): Boolean =
        adsControl?.home?.let { flagEnabled(it.bannerFailNative) }
            ?: (getNumber("home_banner_fail_native_onoff", 0) == 1)

    fun isHomeNativeFailBannerEnabled(): Boolean =
        adsControl?.home?.let { flagEnabled(it.nativeFailBanner) }
            ?: (getNumber("home_native_fail_banner_onoff", 0) == 1)

    fun isEditorNativeEnabled() = adFlag(
        { adsControl?.editor?.let { flagEnabled(it.native) } },
        { getBoolean("Editor_Native_enable") },
    )

    fun isLanguageNativeEnabled() = adFlag(
        { adsControl?.language?.let { flagEnabled(it.native) } },
        { getBoolean("Language_native_enable") },
    )

    fun isSplashNativeEnabled() = adFlag(
        { adsControl?.splash?.let { flagEnabled(it.native) } },
        { getBoolean("splash_native_enable") },
    )

    fun isSideDrawerNativeEnabled() = adFlag(
        { adsControl?.slidedrawer?.let { flagEnabled(it.native) } },
        { getBoolean("sidedrawer_native_enable") },
    )

    fun isResultNativeEnabled() = adFlag(
        { adsControl?.result?.let { flagEnabled(it.native) } },
        { getBoolean("result_native_enable") },
    )

    fun isExitNativeEnabled() = adFlag(
        { adsControl?.exit?.let { flagEnabled(it.native) } },
        { getBoolean("exit_native_enable") },
    )

    fun iscropperNativeEnabled() = adFlag(
        { adsControl?.cropper?.let { flagEnabled(it.native) } },
        { getBoolean("cropper_native_enable") },
    )


    fun homenativelayout() =
        adsControl?.home?.nativeLayout ?: getNumber("home_native_layout")

    fun languagenativelayout() =
        adsControl?.language?.nativeLayout ?: getNumber("language_native_layout")

    fun slidenativelayout() =
        adsControl?.slidedrawer?.nativeLayout ?: getNumber("slidedrawer_native_layout")

    fun splashnativelayout() =
        adsControl?.splash?.nativeLayout ?: getNumber("splash_native_layout")

    fun exitnativelayout() =
        adsControl?.exit?.nativeLayout ?: getNumber("exit_native_layout")

    fun splashNativeBottom(): Int =
        adsControl?.splash?.nativeBottom ?: 1

    fun homeNativeBottom(): Int =
        adsControl?.home?.nativeBottom ?: 1

    fun slidedrawerNativeBottom(): Int =
        adsControl?.slidedrawer?.nativeBottom ?: 1

    fun resultNativeBottom(): Int =
        adsControl?.result?.nativeBottom ?: 1

    fun cropperNativeBottom(): Int =
        adsControl?.cropper?.nativeBottom ?: 1

    fun languageNativeBottom(): Int =
        adsControl?.language?.nativeBottom ?: 1

    fun onboardingNativeBottom(): Int =
        adsControl?.onboarding?.nativeBottom ?: 1

    fun onboardingnativelayout() =
        adsControl?.onboarding?.nativeLayout ?: getNumber("onboarding_native_layout")



    fun isRewardedAdEnabled() = adFlag(
        { adsControl?.rewarded?.let { flagEnabled(it.ad) } },
        { getBoolean("rewarded_enable") },
    )

    fun isRewardedAutoAdEnabled() = adFlag(
        { adsControl?.rewarded?.let { flagEnabled(it.auto) } },
        { getBoolean("rewarded_Auto_enable") },
    )


    fun getCounterInterstitialCount(): Int =
        adsControl?.counter?.count ?: getNumber("Counter_Interstitial_count", 3)

    /** Counter gate delay in seconds. JSON: `counter.duration_sec`, flat fallback key below. */
    fun getCounterInterstitialDurationSec(): Int =
        adsControl?.counter?.durationSec ?: getNumber("Counter_Interstitial_duration_sec", 0)

    fun getForceUpdate(): Int =
        adsControl?.forceUpdateVersion ?: getNumber("force_update_version", 0)

    fun isOnboardingNativeEnabled() = adFlag(
        { adsControl?.onboarding?.let { flagEnabled(it.native) } },
        { getBoolean("Onboarding_native_enable") },
    )

    fun isOnboardingBannerEnabled() = adFlag(
        { adsControl?.onboarding?.let { flagEnabled(it.banner) } },
        { getBoolean("Onboarding_banner_enable") },
    )

    fun isOnboardingInterstitalEnabled() = adFlag(
        { adsControl?.onboarding?.let { flagEnabled(it.interstitial) } },
        { getBoolean("Onboarding_interstital_enable") },
    )

    /**
     * Fullscreen intro-native controls are standardized to root keys only:
     * `intro_fullscreen_native`, `introlargenative1_onoff`, `introlargenative2_onoff`.
     */
    fun isOnboardingNativeFull1Enabled(): Boolean = isIntroLargeNative1Enabled()

    fun isOnboardingNativeFull2Enabled(): Boolean = isIntroLargeNative2Enabled()


    fun isAppOpenEnabled() = adFlag(
        { adsControl?.let { flagEnabled(it.appOpen) } },
        { getBoolean("App_Open_enable") },
    )

    fun isSplashAppOpenEnabled() = adFlag(
        { adsControl?.let { flagEnabled(it.splashAppOpen) } },
        { getBoolean("splash_open_ad_enable") },
    )

    /** `language_screen_onoff` from ads_control (if present) else flat RC key. */
    fun isLanguageScreenEnabled(): Boolean {
        adsControl?.languageScreenOnOff?.let { return it == 1 }
        return getNumber("language_screen_onoff", 1) == 1
    }

    /**
     * Flat RC `language_done_interstitial` (0/1). Interstitial on language Done.
     * If key absent, falls back to [isLanguageInterstitialEnabled]. Respects [isAllAdsEnabled].
     */
    fun isLanguageDoneInterstitialEnabled(): Boolean {
        if (!adsGloballyAllowed()) return false
        adsControl?.languageDoneInterstitial?.let { return it == 1 }
        val key = "language_done_interstitial"
        if (remoteConfig.getKeysByPrefix("").contains(key)) {
            return getNumber(key, 0) == 1
        }
        return isLanguageInterstitialEnabled()
    }

    /** Resume AppOpen switch (JSON preferred, flat fallback). */
    fun isResumeAppOpenEnabled(): Boolean {
        if (!adsGloballyAllowed()) return false
        adsControl?.resumeAppOpenOnOff?.let { return it == 1 }
        return getNumber("resume_appopen_onoff", 1) == 1
    }

    /** 1 = show Privacy Policy entry; 0 = hide. JSON `ads_control` root or flat `privacy_policy_onoff`. */
    fun isPrivacyPolicyEnabled(): Boolean {
        adsControl?.privacyPolicyOnOff?.let { return it == 1 }
        return getNumber("privacy_policy_onoff", 0) == 1
    }

    /** Remote privacy URL; empty if unset. Prefer JSON root `privacy_policy_url` when present. */
    fun getPrivacyPolicyUrl(): String {
        adsControl?.privacyPolicyUrl?.let { return it }
        return if (rcHasKey("privacy_policy_url")) remoteConfig.getString("privacy_policy_url").trim() else ""
    }

    /**
     * Native CTA/button color for all native placements.
     * Priority: `ads_control.native_button_color` -> flat `native_button_color`.
     * Expected value: hex color string (e.g. `#FF512F` or `#AARRGGBB`).
     */
    fun getNativeButtonColor(): String {
        adsControl?.nativeButtonColor?.let { return it }
        return if (rcHasKey("native_button_color")) remoteConfig.getString("native_button_color").trim() else ""
    }

    /**
     * Exit interstitial gate. `ads_control` root `home_exit_interstitial_onoff` wins when present.
     * If absent from JSON: flat RC `home_exit_interstitial_onoff` when that key exists; otherwise OFF.
     */
    fun isHomeExitInterstitialEnabled(): Boolean {
        if (!adsGloballyAllowed()) return false
        adsControl?.homeExitInterstitialOnOff?.let { return it == 1 }
        val flat = "home_exit_interstitial_onoff"
        return if (remoteConfig.getKeysByPrefix("").contains(flat)) {
            getNumber(flat, 0) == 1
        } else {
            false
        }
    }

    private fun isIntroFlagEnabled(screen: Int, suffix: String, default: Int = 1): Boolean {
        return getNumber("intro_${screen}_${suffix}", default.toLong()) == 1
    }

    private fun introFromJson(screen: Int): IntroSection? = when (screen) {
        1 -> adsControl?.intro1
        2 -> adsControl?.intro2
        3 -> adsControl?.intro3
        else -> null
    }

    fun isIntroTopEnabled(screen: Int): Boolean =
        introFromJson(screen)?.let { flagEnabled(it.topOnOff) } ?: isIntroFlagEnabled(screen, "top_onoff", 1)

    fun isIntroBottomEnabled(screen: Int): Boolean =
        introFromJson(screen)?.let { flagEnabled(it.bottomOnOff) } ?: isIntroFlagEnabled(screen, "bottom_onoff", 1)

    fun isIntroSkipEnabled(screen: Int): Boolean =
        introFromJson(screen)?.let { flagEnabled(it.skipOnOff) } ?: isIntroFlagEnabled(screen, "skip_onoff", 1)

    fun isIntroContinueEnabled(screen: Int): Boolean =
        introFromJson(screen)?.let { flagEnabled(it.continueOnOff) } ?: isIntroFlagEnabled(screen, "continue_onoff", 1)

    fun isIntroAdPlacementEnabled(screen: Int): Boolean =
        introFromJson(screen)?.let { flagEnabled(it.adOnOff) } ?: isIntroFlagEnabled(screen, "ad_onoff", 1)

    fun isIntroGetStartedEnabled(screen: Int): Boolean =
        introFromJson(screen)?.let { flagEnabled(it.getStartedOnOff) } ?: isIntroFlagEnabled(screen, "get_started_onoff", 1)

    fun isIntroSkipInterstitialEnabled(screen: Int): Boolean {
        if (!adsGloballyAllowed()) return false
        introFromJson(screen)?.let { return flagEnabled(it.skipInterstitialOnOff) }
        return isIntroFlagEnabled(screen, "skip_interstitial_onoff", 0)
    }

    fun isIntroContinueInterstitialEnabled(screen: Int): Boolean {
        if (!adsGloballyAllowed()) return false
        introFromJson(screen)?.let { return flagEnabled(it.continueInterstitialOnOff) }
        return isIntroFlagEnabled(screen, "continue_interstitial_onoff", 0)
    }

    fun isIntroFullscreenNativeEnabled(): Boolean =
        adsControl?.introFullscreenNative?.let { flagEnabled(it) } ?: (getNumber("intro_fullscreen_native", 0) == 1)

    fun isIntroLargeNative1Enabled(): Boolean =
        adsControl?.introLargeNative1OnOff?.let { flagEnabled(it) } ?: (getNumber("introlargenative1_onoff", 0) == 1)

    fun isIntroLargeNative2Enabled(): Boolean =
        adsControl?.introLargeNative2OnOff?.let { flagEnabled(it) } ?: (getNumber("introlargenative2_onoff", 0) == 1)
}
