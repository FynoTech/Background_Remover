package com.example.background_remove.utils

import android.util.Log
import org.json.JSONObject

/**
 * Remote Config `ads_control` JSON. Integer flags: 1 = on, 0 = off.
 * Sub-objects omitted → callers fall back to legacy flat RC keys for that screen.
 */
internal data class SplashAdsControl(
    val banner: Int,
    val interstitial: Int,
    val native: Int,
    val nativeLayout: Int,
    val nativeBottom: Int,
)

internal data class HomeSection(
    val banner: Int,
    val native: Int,
    val nativeLayout: Int,
    val nativeBottom: Int,
    val bannerFailNative: Int,
    val nativeFailBanner: Int,
)

internal data class SlidedrawerSection(
    val banner: Int,
    val native: Int,
    val nativeLayout: Int,
    val nativeBottom: Int,
)

internal data class ResultSection(
    val banner: Int,
    val native: Int,
    val nativeLayout: Int,
    val nativeBottom: Int,
)

internal data class EditorSection(
    val banner: Int,
    val native: Int,
)

internal data class CropperSection(
    val banner: Int,
    val native: Int,
    val nativeLayout: Int,
    val nativeBottom: Int,
)

internal data class LanguageSection(
    val banner: Int,
    val native: Int,
    val nativeLayout: Int,
    val nativeBottom: Int,
    val interstitial: Int,
)

internal data class CounterSection(
    val interstitial: Int,
    val count: Int,
    val durationSec: Int,
)

internal data class RewardedSection(
    val ad: Int,
    val auto: Int,
)

internal data class OnboardingSection(
    val banner: Int,
    val native: Int,
    val nativeLayout: Int,
    val nativeBottom: Int,
    val nativeFull1: Int,
    val nativeFull2: Int,
    val interstitial: Int,
)

internal data class ExitSection(
    val banner: Int,
    val native: Int,
    val nativeLayout: Int,
)

internal data class IntroSection(
    val topOnOff: Int,
    val bottomOnOff: Int,
    val skipOnOff: Int,
    val continueOnOff: Int,
    val adOnOff: Int,
    val getStartedOnOff: Int,
    val skipInterstitialOnOff: Int,
    val continueInterstitialOnOff: Int,
)

internal data class AdsControlRoot(
    val appOpen: Int,
    /** Master switch: 1 = all ad placements may show (subject to sub-flags), 0 = no ads. */
    val allAdsOnOff: Int,
    /** Optional non-ad flow flag: 1 show language, 0 skip to onboarding. */
    val languageScreenOnOff: Int?,
    /** Optional non-ad flow flag: 1 show interstitial on language done, 0 skip ad. */
    val languageDoneInterstitial: Int?,
    /** Optional home-exit interstitial gate. */
    val homeExitInterstitialOnOff: Int?,
    /** Optional app-open resume gate: 1 show app-open on resume, 0 skip. */
    val resumeAppOpenOnOff: Int?,
    /** Optional: 1 show Privacy Policy entry, 0 hide (flat RC fallback if null). */
    val privacyPolicyOnOff: Int?,
    /** Optional non-empty URL when set in JSON (flat RC fallback if null). */
    val privacyPolicyUrl: String?,
    /** Optional CTA/button color for all native ads (hex like #FF512F). */
    val nativeButtonColor: String?,
    val splashAppOpen: Int,
    val splash: SplashAdsControl?,
    val home: HomeSection?,
    val slidedrawer: SlidedrawerSection?,
    val result: ResultSection?,
    val editor: EditorSection?,
    val cropper: CropperSection?,
    val language: LanguageSection?,
    val counter: CounterSection?,
    val rewarded: RewardedSection?,
    val onboarding: OnboardingSection?,
    val exit: ExitSection?,
    val introFullscreenNative: Int?,
    val introLargeNative1OnOff: Int?,
    val introLargeNative2OnOff: Int?,
    val intro1: IntroSection?,
    val intro2: IntroSection?,
    val intro3: IntroSection?,
    /** Null if `force_update_version` absent at root — use legacy RC. */
    val forceUpdateVersion: Int?,
)

internal object AdsControlJson {

    private const val TAG = "AdsControl"

    /**
     * Reads0/1 flags from JSON when the value is number, boolean (`true`/`false`), or string (`"1"`/`"0"`).
     * [JSONObject.optInt] alone treats booleans/strings incorrectly (often 0).
     * @return null only if [key] is missing or JSON null.
     */
    private fun optOptionalOnOff(o: JSONObject, key: String): Int? {
        if (!o.has(key) || o.isNull(key)) return null
        return when (val v = o.opt(key)) {
            is Boolean -> if (v) 1 else 0
            is Int -> if (v == 1) 1 else 0
            is Long -> if (v == 1L) 1 else 0
            is Double -> if (v == 1.0) 1 else 0
            is String -> when (v.trim().lowercase()) {
                "1", "true", "yes", "on" -> 1
                else -> 0
            }
            else -> try {
                if (o.getInt(key) == 1) 1 else 0
            } catch (_: Exception) {
                0
            }
        }
    }

    /** 0/1 when key present; [default] when missing or JSON null (boolean-safe; [optInt] is not). */
    private fun optOnOffWithDefault(o: JSONObject, key: String, default: Int): Int {
        if (!o.has(key) || o.isNull(key)) return default
        return optOptionalOnOff(o, key) ?: default
    }

    fun parse(raw: String): AdsControlRoot? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val root = JSONObject(trimmed)
            val splash = root.optJSONObject("splash")?.let(::parseSplash)
            val home = root.optJSONObject("home")?.let(::parseHome)
            val slidedrawer = root.optJSONObject("slidedrawer")?.let(::parseSlidedrawer)
            val result = root.optJSONObject("result")?.let(::parseResult)
            val editor = root.optJSONObject("editor")?.let(::parseEditor)
            val cropper = root.optJSONObject("cropper")?.let(::parseCropper)
            val language = root.optJSONObject("language")?.let(::parseLanguage)
            val counter = root.optJSONObject("counter")?.let(::parseCounter)
            val rewarded = root.optJSONObject("rewarded")?.let(::parseRewarded)
            val onboarding = root.optJSONObject("onboarding")?.let(::parseOnboarding)
            val exit = root.optJSONObject("exit")?.let(::parseExit)
            val intro1 = root.optJSONObject("intro1")?.let(::parseIntroSection)
            val intro2 = root.optJSONObject("intro2")?.let(::parseIntroSection)
            val intro3 = root.optJSONObject("intro3")?.let(::parseIntroSection)
            val forceUpdate =
                if (root.has("force_update_version")) root.optInt("force_update_version") else null

            AdsControlRoot(
                appOpen = optOnOffWithDefault(root, "app_open", 1),
                allAdsOnOff = optOnOffWithDefault(root, "all_ads_onoff", 1),
                languageScreenOnOff = optOptionalOnOff(root, "language_screen_onoff"),
                languageDoneInterstitial = optOptionalOnOff(root, "language_done_interstitial"),
                homeExitInterstitialOnOff = optOptionalOnOff(root, "home_exit_interstitial_onoff"),
                resumeAppOpenOnOff = optOptionalOnOff(root, "resume_appopen_onoff"),
                privacyPolicyOnOff = optOptionalOnOff(root, "privacy_policy_onoff"),
                privacyPolicyUrl = if (root.has("privacy_policy_url")) {
                    root.optString("privacy_policy_url").trim().ifEmpty { null }
                } else null,
                nativeButtonColor = if (root.has("native_button_color")) {
                    root.optString("native_button_color").trim().ifEmpty { null }
                } else null,
                splashAppOpen = optOnOffWithDefault(root, "splash_app_open", 0),
                splash = splash,
                home = home,
                slidedrawer = slidedrawer,
                result = result,
                editor = editor,
                cropper = cropper,
                language = language,
                counter = counter,
                rewarded = rewarded,
                onboarding = onboarding,
                exit = exit,
                introFullscreenNative = optOptionalOnOff(root, "intro_fullscreen_native"),
                introLargeNative1OnOff = optOptionalOnOff(root, "introlargenative1_onoff"),
                introLargeNative2OnOff = optOptionalOnOff(root, "introlargenative2_onoff"),
                intro1 = intro1,
                intro2 = intro2,
                intro3 = intro3,
                forceUpdateVersion = forceUpdate,
            ).also {
                Log.d(TAG, "ads_control parsed (splash=${it.splash != null}, home=${it.home != null}, … force=$forceUpdate)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "ads_control parse failed, using legacy RC keys", e)
            null
        }
    }

    private fun parseSplash(o: JSONObject) = SplashAdsControl(
        banner = optOnOffWithDefault(o, "banner", 1),
        interstitial = optOnOffWithDefault(o, "interstitial", 1),
        native = optOnOffWithDefault(o, "native", 1),
        nativeLayout = optOnOffWithDefault(o, "native_layout", 1),
        nativeBottom = optOnOffWithDefault(o, "native_bottom", 1),
    )

    private fun parseHome(o: JSONObject) = HomeSection(
        banner = optOnOffWithDefault(o, "banner", 1),
        native = optOnOffWithDefault(o, "native", 1),
        nativeLayout = optOnOffWithDefault(o, "native_layout", 1),
        nativeBottom = optOnOffWithDefault(o, "native_bottom", 1),
        bannerFailNative = optOnOffWithDefault(o, "banner_fail_native_onoff", 0),
        nativeFailBanner = optOnOffWithDefault(o, "native_fail_banner_onoff", 0),
    )

    private fun parseSlidedrawer(o: JSONObject) = SlidedrawerSection(
        banner = optOnOffWithDefault(o, "banner", 1),
        native = optOnOffWithDefault(o, "native", 1),
        nativeLayout = optOnOffWithDefault(o, "native_layout", 1),
        nativeBottom = optOnOffWithDefault(o, "native_bottom", 1),
    )

    private fun parseResult(o: JSONObject) = ResultSection(
        banner = optOnOffWithDefault(o, "banner", 1),
        native = optOnOffWithDefault(o, "native", 1),
        nativeLayout = optOnOffWithDefault(o, "native_layout", 1),
        nativeBottom = optOnOffWithDefault(o, "native_bottom", 1),
    )

    private fun parseEditor(o: JSONObject) = EditorSection(
        banner = optOnOffWithDefault(o, "banner", 1),
        native = optOnOffWithDefault(o, "native", 1),
    )

    private fun parseCropper(o: JSONObject) = CropperSection(
        banner = optOnOffWithDefault(o, "banner", 1),
        native = optOnOffWithDefault(o, "native", 1),
        nativeLayout = optOnOffWithDefault(o, "native_layout", 1),
        nativeBottom = optOnOffWithDefault(o, "native_bottom", 1),
    )

    private fun parseLanguage(o: JSONObject) = LanguageSection(
        banner = optOnOffWithDefault(o, "banner", 1),
        native = optOnOffWithDefault(o, "native", 1),
        nativeLayout = optOnOffWithDefault(o, "native_layout", 1),
        nativeBottom = optOnOffWithDefault(o, "native_bottom", 1),
        interstitial = optOnOffWithDefault(o, "interstitial", 1),
    )

    private fun parseCounter(o: JSONObject) = CounterSection(
        interstitial = optOnOffWithDefault(o, "interstitial", 1),
        count = o.optInt("count", 3),
        durationSec = o.optInt("duration_sec", 0),
    )

    private fun parseRewarded(o: JSONObject) = RewardedSection(
        ad = optOnOffWithDefault(o, "ad", 1),
        auto = optOnOffWithDefault(o, "auto", 1),
    )

    private fun parseOnboarding(o: JSONObject) = OnboardingSection(
        banner = optOnOffWithDefault(o, "banner", 1),
        native = optOnOffWithDefault(o, "native", 1),
        nativeLayout = optOnOffWithDefault(o, "native_layout", 1),
        nativeBottom = optOnOffWithDefault(o, "native_bottom", 1),
        nativeFull1 = optOnOffWithDefault(o, "native_full_1", 1),
        nativeFull2 = optOnOffWithDefault(o, "native_full_2", 1),
        interstitial = optOnOffWithDefault(o, "interstitial", 1),
    )

    private fun parseExit(o: JSONObject) = ExitSection(
        banner = optOnOffWithDefault(o, "banner", 1),
        native = optOnOffWithDefault(o, "native", 1),
        nativeLayout = optOnOffWithDefault(o, "native_layout", 1),
    )

    private fun parseIntroSection(o: JSONObject) = IntroSection(
        topOnOff = optOnOffWithDefault(o, "intro_top_onoff", 1),
        bottomOnOff = optOnOffWithDefault(o, "intro_bottom_onoff", 1),
        skipOnOff = optOnOffWithDefault(o, "intro_skip_onoff", 1),
        continueOnOff = optOnOffWithDefault(o, "intro_continue_onoff", 1),
        adOnOff = optOnOffWithDefault(o, "intro_ad_onoff", 1),
        getStartedOnOff = optOnOffWithDefault(o, "intro_get_started_onoff", 1),
        skipInterstitialOnOff = optOnOffWithDefault(o, "intro_skip_interstitial_onoff", 0),
        continueInterstitialOnOff = optOnOffWithDefault(o, "intro_continue_interstitial_onoff", 0),
    )
}
