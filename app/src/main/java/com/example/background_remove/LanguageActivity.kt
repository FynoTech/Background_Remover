package com.example.background_remove


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.GridLayoutManager
import com.example.background_remove.adapters.LanguageAdapter
import com.example.background_remove.adsManager.AdBanner
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.adsManager.NativeAdManager


import com.example.background_remove.utils.LanguageModel
import com.example.background_remove.utils.Preferences
import com.example.background_remove.utils.ProgressUtils
import com.example.background_remove.utils.RemoteConfigManager

import com.example.background_remove.utils.setSafeClickListener
import com.example.background_remove.utils.setSafeClickListenertools
import com.example.background_remove.utils.setupStatusBar
import com.photo.editor.background.eraser.bgeraser.R
import com.photo.editor.background.eraser.bgeraser.databinding.ActivityLanguageBinding
import java.util.Locale

class LanguageActivity : BaseActivity(), LanguageAdapter.NotifyLanguage {
    private lateinit var binding: ActivityLanguageBinding
    var adapter: LanguageAdapter? = null
    var languageCode: String? = null
    var position = 0

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        val view: View = binding.root
        setContentView(view)
        enableEdgeToEdge()
        setupStatusBar(window, Color.BLACK)


        val languages = listOf(
            LanguageModel("English", "en", R.drawable.english),
            LanguageModel("Hindi", "hi", R.drawable.india),
            LanguageModel("Arabic", "ar", R.drawable.arabic),
            LanguageModel("Portuguese", "pt", R.drawable.brazil),
            LanguageModel("German", "de", R.drawable.german),
            LanguageModel("Spanish", "es", R.drawable.spanish),
            LanguageModel("Russian", "ru", R.drawable.russian),
            LanguageModel("Chinese", "zh", R.drawable.chinese),

        )


        val pref = getSharedPreferences("Languages", MODE_PRIVATE)
        languageCode = pref.getString("languageCode", "")

        if (languageCode=="") {
            binding.apply.visibility = View.GONE
        }

        val mLayoutManager = GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false)
        Log.d(
            "abcd", Preferences.getBoolValue(
                this@LanguageActivity,
                "firsttime"
            ).toString()
        )
        if (Preferences.getBoolValue(
                this@LanguageActivity,
                "firsttime"
            )
        ) {
            binding.back.visibility = View.VISIBLE
        } else {
            binding.back.visibility = View.GONE
        }

        binding.languagerecycler.layoutManager = mLayoutManager
        adapter = LanguageAdapter(this, languages, this)
        binding.languagerecycler.adapter = adapter
        adapter?.SelectedLanguage(languageCode, pref.getInt("languageCodeposition", -1))


        if (RemoteConfigManager.isLanguageBannerEnabled()) {
            binding.cvAdContainerbanner.visibility = View.VISIBLE
            loadbannerad()
        } else if (RemoteConfigManager.isLanguageNativeEnabled()) {
            binding.cvAdContainernative.visibility = View.VISIBLE
            loadnativead()
        }


        binding.back.setSafeClickListener(this) {
            finish()
        }
        // Switch to the main thread to update the UI


        binding.apply.setSafeClickListenertools {


            if (languageCode != "") {
                Log.d("abcd", languageCode.toString())
                val editor: SharedPreferences.Editor = pref.edit()
                editor.putString("languageCode", languageCode)
                editor.putInt("languageCodeposition", position)
                editor.apply()
                if (RemoteConfigManager.isLanguageInterstitialEnabled()) {
                    ProgressUtils.show(this, getString(R.string.loading_ad_))
                    Ads.loadAndShowInterstitial(
                        activity = this,
                        adUnitId = RemoteConfigManager.getLanguageInterstitialAdId(),
                        placement = "language",
                        useAnyCachedFallback = true,
                        showDelayMs = 0
                    ) { loaded, failed, showed, dismissed ->
                        when {
                            failed -> {
                                ProgressUtils.dismiss()
                                if (Preferences.getBoolValue(
                                        this@LanguageActivity,
                                        "firsttime"
                                    )
                                ) {
                                    val mainIntent = Intent(this, MainActivity::class.java)
                                    startActivity(mainIntent)
                                    finish()
                                } else {
                                    val mainIntent = Intent(this, OnboardingActivity::class.java)
                                    startActivity(mainIntent)
                                    finish()
                                }
                                Preferences.setBoolValue(this@LanguageActivity, "firsttime", true)
                            }

                            dismissed -> {
                                ProgressUtils.dismiss()
                                if (Preferences.getBoolValue(
                                        this@LanguageActivity,
                                        "firsttime"
                                    )
                                ) {
                                    val mainIntent = Intent(this, MainActivity::class.java)
                                    startActivity(mainIntent)
                                    finish()
                                } else {
                                    val mainIntent = Intent(this, OnboardingActivity::class.java)
                                    startActivity(mainIntent)
                                    finish()
                                }
                                Preferences.setBoolValue(this@LanguageActivity, "firsttime", true)
                            }

                            loaded -> { /* optional: ad ready */
                            }

                            showed -> {

                            }
                        }
                    }
                } else {
                    if (Preferences.getBoolValue(
                            this@LanguageActivity,
                            "firsttime"
                        )
                    ) {
                        val mainIntent = Intent(this, MainActivity::class.java)
                        startActivity(mainIntent)
                        finish()
                    } else {
                        val mainIntent = Intent(this, OnboardingActivity::class.java)
                        startActivity(mainIntent)
                        finish()
                    }
                    Preferences.setBoolValue(this@LanguageActivity, "firsttime", true)
                }


            } else {
                Toast.makeText(this, getString(R.string.Choose_Language), Toast.LENGTH_SHORT).show()
            }


        }

    }
    private fun loadbannerad(){
        binding.cvAdContainerbanner.visibility=View.VISIBLE
        AdBanner.loadBannerAdsplash(this,binding.cvAdContainerbanner,RemoteConfigManager.getLanguageBannerAdId(),false,binding.loadingLayoutbanner,true)
            .callback {
                    loaded, failed ->
                if(failed){
                    binding.cvAdContainerbanner.visibility= View.GONE
                }

            }
    }
    private fun loadnativead(){
        binding.cvAdContainernative.visibility=View.VISIBLE
        NativeAdManager.loadAndShowNativeAd(
            activity = this,
            adUnitId = RemoteConfigManager.getLanguageNativeAdId(),
            container = binding.cvAdContainernative,
            style = RemoteConfigManager.languagenativelayout(),binding.loadingLayoutnative
        ) { loaded, failed, fromCache ->
            Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
            if(failed){
                binding.cvAdContainernative.visibility= View.GONE
            }
        }


    }
    private fun updateTextViewWithLocale(
        context: Context,
        textView: TextView,
        stringResId: Int,
        locale: Locale
    ) {
        val localizedResources = getLocalizedResources(context, locale)
        val localizedString = localizedResources.getString(stringResId)
        textView.text = localizedString
    }

    private fun getLocalizedResources(context: Context, desiredLocale: Locale): Resources {
        var conf = context.resources.configuration
        conf = Configuration(conf)
        conf.setLocale(desiredLocale)
        val localizedContext = context.createConfigurationContext(conf)
        return localizedContext.resources
    }
    override fun onBackPressed() {

        if (Preferences.getBoolValue(this@LanguageActivity, "firsttime")) {
            finish()
        } else {

        }

    }


    @SuppressLint("ResourceType")
    override fun notifyLanguage(flag: Int, name: String?, code: String?, position: Int) {
        binding.apply.visibility = View.VISIBLE
        this.position = position
        languageCode = code
        adapter?.SelectedLanguage(code, position)
    }


}
