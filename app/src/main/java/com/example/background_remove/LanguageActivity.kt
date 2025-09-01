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
import com.example.background_remove.databinding.ActivityLanguageBinding

import com.example.background_remove.utils.LanguageModel
import com.example.background_remove.utils.Preferences

import com.example.background_remove.utils.setSafeClickListener
import com.example.background_remove.utils.setSafeClickListenertools
import com.example.background_remove.utils.setupStatusBar
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
            LanguageModel("English", "en", R.drawable.ic_english),
            LanguageModel("French", "fr", R.drawable.ic_french),
            LanguageModel("Arabic", "ar", R.drawable.ic_arabic),
            LanguageModel("Spanish", "es", R.drawable.ic_spanish),
            LanguageModel("Portugese", "pt", R.drawable.ic_portuguese),
            LanguageModel("Hindi", "hi", R.drawable.ic_hindi),
            LanguageModel("Russian", "ru", R.drawable.ic_russian),
            LanguageModel("Persian", "fa", R.drawable.ic_persian),
            LanguageModel("Uzbek", "uz", R.drawable.ic_uzbek),
            LanguageModel("Indonesian", "id", R.drawable.ic_indonesian),
            LanguageModel("African", "af", R.drawable.ic_southafrica),
            LanguageModel("Vietnam", "vi", R.drawable.ic_vietnam),
            LanguageModel("Turkish", "tr", R.drawable.ic_turkish),
            LanguageModel("Thai", "th", R.drawable.ic_thai),

            )


        val pref = getSharedPreferences("Languages", MODE_PRIVATE)
        languageCode = pref.getString("languageCode", "")

        binding.apply.visibility = View.GONE

        val mLayoutManager = GridLayoutManager(this,2,GridLayoutManager.VERTICAL,false)
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


    /*    if(RemoteConfigManager.isLanguageNativeEnabled()){
            loadnativead()
        }
        else{
            binding.cvAdContainerNative.visibility= View.GONE
        }
        if(RemoteConfigManager.isLanguageBannerEnabled()){
            loadbannerad()
        }else{
            binding.cvAdContainerBanner.visibility= View.GONE
        }*/


        binding.back.setSafeClickListener(this) {
            finish()
        }
        // Switch to the main thread to update the UI


        binding.apply.setSafeClickListenertools {



            if (languageCode != "") {
                Log.d("abcd",languageCode.toString())
                val editor: SharedPreferences.Editor = pref.edit()
                editor.putString("languageCode", languageCode)
                editor.putInt("languageCodeposition", position)
                editor.apply()
             /*   if (RemoteConfigManager.isLanguageInterstitialEnabled()) {
                    ProgressUtils.show(this,getString(R.string.loading_ad_))
                    Ads.loadAndShowInterstitial(
                        activity = this,
                        adUnitId = RemoteConfigManager.getLanguageInterstitialAdId(),
                        placement = "language",
                        useAnyCachedFallback = true,
                        showDelayMs = 0
                    ) { loaded, failed, showed, dismissed ->
                        when {
                            failed     ->
                            {
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
                            dismissed  -> {
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
                            loaded     -> { *//* optional: ad ready *//* }
                            showed     ->
                            {

                            }
                        }
                    }
                } else {*/
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







            } else {
                Toast.makeText(this, getString(R.string.Choose_Language), Toast.LENGTH_SHORT).show()
            }


        }
    }
/*
 private fun loadbannerad(){
     AdBanner.loadBannerAdsplash(this,binding.cvAdContainerBanner,RemoteConfigManager.getLanguageBannerAdId(),true,binding.loadingLayoutBanner,true)
         .callback {
                 loaded, failed ->
             if(failed){
                 binding.cvAdContainerBanner.visibility= View.GONE
             }

         }
 }
private fun loadnativead(){
    NativeAdManager.loadAndShowNativeAd(
        activity = this,
        adUnitId = RemoteConfigManager.getLanguageNativeAdId(),
        container = binding.cvAdContainerNative,
        style = NativeAdManager.NativeStyle.ADVANCE,binding.loadingLayoutNative
    ) { loaded, failed, fromCache ->
        Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
        if(failed){
            binding.cvAdContainerNative.visibility= View.GONE
        }
    }


}*/
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
