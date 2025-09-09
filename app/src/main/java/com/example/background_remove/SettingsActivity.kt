package com.example.background_remove

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.example.background_remove.adsManager.AdBanner
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.adsManager.NativeAdManager
import com.example.background_remove.billing.PremiumActivity

import com.example.background_remove.utils.Dialogs
import com.example.background_remove.utils.RemoteConfigManager
import com.example.background_remove.utils.setSafeClickListener
import com.example.background_remove.utils.setSafeClickListenertools
import com.example.background_remove.utils.setupStatusBar
import com.photo.editor.background.eraser.bgeraser.BuildConfig
import com.photo.editor.background.eraser.bgeraser.R
import com.photo.editor.background.eraser.bgeraser.databinding.ActivitySettingsBinding


class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        enableEdgeToEdge()
       
        setupStatusBar(window, Color.BLACK)
        setContentView(binding.root)

        binding.back.setSafeClickListenertools  {
            finish()
        }
        if(RemoteConfigManager.isSideDrawerNativeEnabled()){
            loadnativead()
        }
        else{
            binding.cvAdContainernative.visibility= View.GONE
        }
        if(RemoteConfigManager.isSidedrawerBannerEnabled() && !RemoteConfigManager.isSideDrawerNativeEnabled() ){
            loadbannerad()
        }else{
            binding.cvAdContainerbanner.visibility= View.GONE
        }
        binding.language.setSafeClickListener(this) {
            val intent = Intent(this, LanguageActivity::class.java)
            startActivity(intent)
        }
        val versionName = BuildConfig.VERSION_NAME
        binding.textverison.text = "${getString(R.string.version)} $versionName"

        binding.share.setSafeClickListener(this) {
            try {
                val myIntent = Intent(Intent.ACTION_SEND)
                myIntent.type = "text/plain"

                val shareBody = """
            Let me recommend you this application
            
            Background Eraser - Photo Editor
            
            https://play.google.com/store/apps/details?id=$packageName
        """.trimIndent()

                val shareSub = "Background Eraser - Photo Editor"

                myIntent.putExtra(Intent.EXTRA_SUBJECT, shareSub)
                myIntent.putExtra(Intent.EXTRA_TEXT, shareBody)

                startActivity(Intent.createChooser(myIntent, "Share using"))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.unable_to_share), Toast.LENGTH_SHORT).show()
            }
        }
        binding.privacy.setSafeClickListener(this) {
            try {
                val url = "https://www.termsfeed.com/live/c153f142-bc22-498b-9ddc-95c43aab5f0c"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {

            }
        }
        binding.terms.setSafeClickListener(this) {
            try {
                val url = "https://www.termsfeed.com/live/6d21e7b7-3230-4651-a76d-3c50e0189523"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {

            }
        }

        binding.howtouse.setSafeClickListener(this){
            val intent = Intent(this, OnboardingActivity::class.java)
            intent.putExtra("fromsettings",true)
            startActivity(intent)
        }
        binding.premium.setSafeClickListener(this){
            val intent = Intent(this, PremiumActivity::class.java)
            startActivity(intent)
        }

        binding.rateus.setSafeClickListener(this) {

            Dialogs.showRateUsDialog(this)
        }
    }
    private fun loadbannerad(){
        binding.cvAdContainerbanner.visibility= View.VISIBLE
        AdBanner.loadBannerAdsplash(this,binding.cvAdContainerbanner,RemoteConfigManager.getSlidedrawerBannerAdId(),false,binding.loadingLayoutbanner,false)
            .callback {
                    loaded, failed ->
                if(failed){
                    binding.cvAdContainerbanner.visibility= View.GONE
                }

            }
    }
    private fun loadnativead(){
        binding.cvAdContainernative.visibility= View.VISIBLE
        NativeAdManager.loadAndShowNativeAd(
            activity = this,
            adUnitId = RemoteConfigManager.getSlidedrawerNativeAdId(),
            container = binding.cvAdContainernative,
            style = RemoteConfigManager.slidenativelayout(),binding.loadingLayoutnative
        ) { loaded, failed, fromCache ->
            Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
            if(failed){
                binding.loadingLayoutnative.visibility= View.GONE
            }
        }


    }

    override fun onResume() {
        super.onResume()
        if(Ads.isPremiumUser(this)){
            binding.premium.visibility= View.GONE
        }
    }
    override fun onBackPressed() {
        finish()
    }
}