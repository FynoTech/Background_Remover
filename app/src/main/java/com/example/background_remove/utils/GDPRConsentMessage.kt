package com.example.background_remove.utils

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm.OnConsentFormDismissedListener
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.analytics.FirebaseAnalytics

import java.util.EnumMap


class GDPRConsentMessage private constructor(context: Context) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: FormError?)
    }

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() =
            consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gatherConsent(
        activity: Activity,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        val debugSettings =
            ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                // .addTestDeviceHashedId("65BAA29BAEF3C4660714DDED592FE809")
                .addTestDeviceHashedId("033D34F4888A70243B1620A9331EBD2B")
                .build()


        val params =
            ConsentRequestParameters.Builder()./*setConsentDebugSettings(debugSettings).*/build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    onConsentGatheringCompleteListener.consentGatheringComplete(formError)

                    val consentMap: MutableMap<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> =
                        EnumMap(FirebaseAnalytics.ConsentType::class.java)

                    when (consentInformation.consentStatus) {
                        ConsentInformation.ConsentStatus.REQUIRED -> {
                            // If consent is required but not given, deny all.
                            consentMap[FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE] = FirebaseAnalytics.ConsentStatus.DENIED
                            consentMap[FirebaseAnalytics.ConsentType.AD_STORAGE] = FirebaseAnalytics.ConsentStatus.DENIED
                            consentMap[FirebaseAnalytics.ConsentType.AD_USER_DATA] = FirebaseAnalytics.ConsentStatus.DENIED
                            consentMap[FirebaseAnalytics.ConsentType.AD_PERSONALIZATION] = FirebaseAnalytics.ConsentStatus.DENIED
                        }
                        ConsentInformation.ConsentStatus.OBTAINED -> {
                            // If consent is given, grant all.
                            consentMap[FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE] = FirebaseAnalytics.ConsentStatus.GRANTED
                            consentMap[FirebaseAnalytics.ConsentType.AD_STORAGE] = FirebaseAnalytics.ConsentStatus.GRANTED
                            consentMap[FirebaseAnalytics.ConsentType.AD_USER_DATA] = FirebaseAnalytics.ConsentStatus.GRANTED
                            consentMap[FirebaseAnalytics.ConsentType.AD_PERSONALIZATION] = FirebaseAnalytics.ConsentStatus.GRANTED
                        }
                        else -> {
                            // Default to denied if status is unknown or not required.
                            consentMap[FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE] = FirebaseAnalytics.ConsentStatus.DENIED
                            consentMap[FirebaseAnalytics.ConsentType.AD_STORAGE] = FirebaseAnalytics.ConsentStatus.DENIED
                            consentMap[FirebaseAnalytics.ConsentType.AD_USER_DATA] = FirebaseAnalytics.ConsentStatus.DENIED
                            consentMap[FirebaseAnalytics.ConsentType.AD_PERSONALIZATION] = FirebaseAnalytics.ConsentStatus.DENIED
                        }
                    }

                    FirebaseAnalytics.getInstance(activity).setConsent(consentMap)
                }
            },
            { requestConsentError ->
                onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError)
            }
        )
    }

    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    }

    companion object {
        @Volatile
        private var instance: GDPRConsentMessage? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance ?: GDPRConsentMessage(context).also { instance = it }
                }
    }
}
