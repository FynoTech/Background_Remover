package com.example.background_remove

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import com.example.background_remove.adapters.OnboardingAdapter
import com.example.background_remove.adsManager.AdBanner
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.adsManager.NativeAdManager
import com.example.background_remove.utils.OnboardingPage
import com.example.background_remove.utils.RemoteConfigManager
import com.example.background_remove.utils.setupStatusBar
import com.photo.editor.background.eraser.bgeraser.R
import com.photo.editor.background.eraser.bgeraser.databinding.ActivityOnboardingBinding


class OnboardingActivity : BaseActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    companion object{
        var fromsettings:Boolean=false
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        setupStatusBar(window, Color.BLACK)
        fromsettings = intent.getBooleanExtra("fromsettings", false)
        val adBetween1and2 = RemoteConfigManager.isOnboardingNativeFull1Enabled() && !Ads.isPremiumUser(this)
        val adBetween2and3 = RemoteConfigManager.isOnboardingNativeFull2Enabled() && !Ads.isPremiumUser(this)
        if (RemoteConfigManager.isOnboardingBannerEnabled()) {
            binding.cvAdContainerbanner.visibility=View.VISIBLE
            loadbannerad()
        }else if(RemoteConfigManager.isOnboardingNativeEnabled()){
            binding.cvAdContainernative.visibility=View.VISIBLE
            loadnativead()
        }
        val pages = mutableListOf<OnboardingPage>()

        pages.add(
            OnboardingPage.Content(
            title = getString(R.string.remove_background),
            description = getString(R.string.instantly_erase_distractions_and_highlight_your_subject),
            imageRes = R.drawable.firstimage,
            indiREs = 0 // temp, will fill later

        ))

        if (adBetween1and2) pages.add(OnboardingPage.NativeAdPage("1-2"))

// Slide 2
        pages.add(
            OnboardingPage.Content(
            title = getString(R.string.change_background),
            description = getString(R.string.give_your_subject_a_fresh_new_look_in_seconds),
            imageRes = R.drawable.secimage,
            indiREs = 0
        ))

        if (adBetween2and3) pages.add(OnboardingPage.NativeAdPage("2-3"))

// Slide 3
        pages.add(
            OnboardingPage.Content(
            title = getString(R.string.remove_object),
            description = getString(R.string.erase_unwanted_elements_and_keep_the_focus_on_your_subject),
            imageRes = R.drawable.thirdiamge,
            indiREs = 0
        ))

        val totalPages = pages.size
        if (totalPages == 3) {
            // No ads → fixed indicators
            pages[0] = (pages[0] as OnboardingPage.Content).copy(indiREs = R.drawable.firstindi)
            pages[1] = (pages[1] as OnboardingPage.Content).copy(indiREs = R.drawable.secondindi)
            pages[2] = (pages[2] as OnboardingPage.Content).copy(indiREs = R.drawable.thirdindi)
        } else {
            // Ads present → continuous indi1..indiN
            pages.forEachIndexed { index, page ->
                if (page is OnboardingPage.Content) {
                    val resName = "indi${index + 1}" // indi1..indi4..indi5
                    val resId = resources.getIdentifier(resName, "drawable", packageName)
                    pages[index] = page.copy(indiREs = resId)
                }
            }
        }
        binding.viewPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val page = pages[position]
                if (page is OnboardingPage.NativeAdPage) {
                    binding.cvAdContainernative.visibility = View.INVISIBLE
                    binding.cvAdContainerbanner.visibility = View.INVISIBLE
                } else {
                    if (Ads.isNetworkAvailableWithoutPremium(this@OnboardingActivity)) {
                        if (RemoteConfigManager.isOnboardingBannerEnabled()) {
                            binding.cvAdContainerbanner.visibility=View.VISIBLE

                        }else if(RemoteConfigManager.isOnboardingNativeEnabled()){
                            binding.cvAdContainernative.visibility=View.VISIBLE

                        }
                    }

                }
            }
        })
        val adapter = OnboardingAdapter(this, pages)
        binding.viewPager.adapter = adapter
    }

    private fun loadbannerad(){
        binding.cvAdContainerbanner.visibility=View.VISIBLE
        AdBanner.loadBannerAdsplash(this,binding.cvAdContainerbanner,RemoteConfigManager.getOnBoardingBannerId(),false,binding.loadingLayoutbanner,true)
            .callback {
                    loaded, failed ->
                if(failed){
                    binding.cvAdContainerbanner.visibility= View.GONE
                }

            }
    }
    private fun loadnativead(){
        NativeAdManager.loadAndShowNativeAd(
            activity = this,
            adUnitId = RemoteConfigManager.getOnBoardingNativeAdId(),
            container = binding.cvAdContainernative,
            style = RemoteConfigManager.onboardingnativelayout(),binding.loadingLayoutnative
        ) { loaded, failed, fromCache ->
            Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
            if(failed){
                binding.cvAdContainernative.visibility= View.GONE
            }
        }


    }
    override fun onPause() {
        super.onPause()
        Ads.setPaused(true,this)

    }

    override fun onResume() {
        super.onResume()
        Ads.setPaused(false,this)


    }
}
