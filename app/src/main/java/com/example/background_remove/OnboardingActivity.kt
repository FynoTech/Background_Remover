package com.example.background_remove

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.example.background_remove.adapters.OnboardingAdapter
import com.example.background_remove.adsManager.AdBanner
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.adsManager.NativeAdManager
import com.example.background_remove.utils.OnboardingPage
import com.example.background_remove.utils.Preferences
import com.example.background_remove.utils.ProgressUtils
import com.example.background_remove.utils.RemoteConfigManager
import com.example.background_remove.utils.setSafeClickListenertools
import com.example.background_remove.utils.setupStatusBar
import com.photo.editor.background.eraser.bgeraser.R
import com.photo.editor.background.eraser.bgeraser.databinding.ActivityOnboardingBinding


class OnboardingActivity : BaseActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var pages: MutableList<OnboardingPage>
    private var shouldRenderSharedBottomAd: Boolean = false

    companion object {
        var fromsettings: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        setupStatusBar(window, Color.BLACK)
        fromsettings = intent.getBooleanExtra("fromsettings", false)

        val adBetween1and2 =
            RemoteConfigManager.isIntroFullscreenNativeEnabled() &&
                RemoteConfigManager.isIntroLargeNative1Enabled() &&
                !Ads.isPremiumUser(this)
        val adBetween2and3 =
            RemoteConfigManager.isIntroFullscreenNativeEnabled() &&
                RemoteConfigManager.isIntroLargeNative2Enabled() &&
                !Ads.isPremiumUser(this)

        pages = mutableListOf(
            OnboardingPage.Content(
                title = getString(R.string.remove_background),
                description = getString(R.string.instantly_erase_distractions_and_highlight_your_subject),
                imageRes = R.drawable.firstimage,
                indiREs = 0,
                screenNumber = 1,
                showTop = RemoteConfigManager.isIntroTopEnabled(1),
                showBottom = RemoteConfigManager.isIntroBottomEnabled(1),
            ),
        )
        if (adBetween1and2) pages.add(OnboardingPage.NativeAdPage("1-2"))
        pages.add(
            OnboardingPage.Content(
                title = getString(R.string.change_background),
                description = getString(R.string.give_your_subject_a_fresh_new_look_in_seconds),
                imageRes = R.drawable.secimage,
                indiREs = 0,
                screenNumber = 2,
                showTop = RemoteConfigManager.isIntroTopEnabled(2),
                showBottom = RemoteConfigManager.isIntroBottomEnabled(2),
            ),
        )
        if (adBetween2and3) pages.add(OnboardingPage.NativeAdPage("2-3"))
        pages.add(
            OnboardingPage.Content(
                title = getString(R.string.remove_object),
                description = getString(R.string.erase_unwanted_elements_and_keep_the_focus_on_your_subject),
                imageRes = R.drawable.thirdiamge,
                indiREs = 0,
                screenNumber = 3,
                showTop = RemoteConfigManager.isIntroTopEnabled(3),
                showBottom = RemoteConfigManager.isIntroBottomEnabled(3),
            ),
        )

        pages.forEachIndexed { index, page ->
            if (page is OnboardingPage.Content) {
                val resName = if (pages.size == 3) {
                    when (index) {
                        0 -> "firstindi"
                        1 -> "secondindi"
                        else -> "thirdindi"
                    }
                } else {
                    "indi${index + 1}"
                }
                pages[index] = page.copy(indiREs = resources.getIdentifier(resName, "drawable", packageName))
            }
        }

        binding.viewPager.adapter = OnboardingAdapter(this, pages)
        binding.viewPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageUi(position)
            }
        })

        binding.btnSkip.setSafeClickListenertools {
            handleAction(isSkip = true)
        }
        binding.btnNext.setSafeClickListenertools {
            handleAction(isSkip = false)
        }

        updatePageUi(0)
    }

    private fun updatePageUi(position: Int) {
        val page = pages[position]
        when (pages.size) {
            5 -> binding.ivindicator.setImageDrawable(
                ContextCompat.getDrawable(this, resources.getIdentifier("indi${position + 1}", "drawable", packageName)),
            )
            else -> {
                val drawable = when (position) {
                    0 -> R.drawable.firstindi
                    1 -> R.drawable.secondindi
                    else -> R.drawable.thirdindi
                }
                binding.ivindicator.setImageDrawable(ContextCompat.getDrawable(this, drawable))
            }
        }

        if (page is OnboardingPage.NativeAdPage) {
            binding.layout.visibility = View.GONE
            hideSharedAd()
            return
        }
        page as OnboardingPage.Content
        binding.layout.visibility = View.VISIBLE
        binding.btnSkip.visibility = View.GONE

        val isLast = position == pages.lastIndex
        if (isLast) {
            val showGetStarted = RemoteConfigManager.isIntroGetStartedEnabled(page.screenNumber)
            binding.btnNext.visibility = if (showGetStarted) View.VISIBLE else View.GONE
            if (!showGetStarted) {
                goToPermissionScreen()
                return
            }
        } else {
            val showContinue = RemoteConfigManager.isIntroContinueEnabled(page.screenNumber)
            binding.btnNext.visibility = if (showContinue) View.VISIBLE else View.GONE
            if (!showContinue) {
                moveToNextPage()
                return
            }
        }
        showSharedAdForScreen(page.screenNumber)
    }

    private fun showSharedAdForScreen(screenNumber: Int) {
        if (!RemoteConfigManager.isIntroAdPlacementEnabled(screenNumber) || !Ads.isNetworkAvailableWithoutPremium(this)) {
            hideSharedAd()
            return
        }
        shouldRenderSharedBottomAd = true
        if (RemoteConfigManager.isOnboardingBannerEnabled()) {
            binding.cvAdContainerbanner.visibility = View.VISIBLE
            binding.cvAdContainernative.visibility = View.GONE
            loadbannerad()
        } else if (RemoteConfigManager.isOnboardingNativeEnabled()) {
            binding.cvAdContainernative.visibility = View.VISIBLE
            binding.cvAdContainerbanner.visibility = View.GONE
            loadnativead()
        } else {
            hideSharedAd()
        }
    }

    private fun hideSharedAd() {
        shouldRenderSharedBottomAd = false
        binding.cvAdContainernative.visibility = View.GONE
        binding.cvAdContainerbanner.visibility = View.GONE
    }

    private fun handleAction(isSkip: Boolean) {
        val contentPage = pages.getOrNull(binding.viewPager.currentItem) as? OnboardingPage.Content ?: return
        val isLastPage = binding.viewPager.currentItem == pages.lastIndex
        val shouldShowInterstitial = if (isSkip) {
            RemoteConfigManager.isIntroSkipInterstitialEnabled(contentPage.screenNumber)
        } else {
            RemoteConfigManager.isIntroContinueInterstitialEnabled(contentPage.screenNumber)
        }
        if (!shouldShowInterstitial) {
            continueOnboardingFlow(isLastPage)
            return
        }

        ProgressUtils.show(this, getString(R.string.loading_ad_))
        val placement = if (isSkip) "intro_skip_${contentPage.screenNumber}" else "intro_continue_${contentPage.screenNumber}"
        Ads.loadAndShowInterstitial(
            activity = this,
            adUnitId = RemoteConfigManager.getOnBoardingInterstitialId(),
            placement = placement,
            useAnyCachedFallback = true,
            showDelayMs = 0,
        ) { _, failed, _, dismissed ->
            if (failed || dismissed) {
                ProgressUtils.dismiss()
                continueOnboardingFlow(isLastPage)
            }
        }
    }

    private fun continueOnboardingFlow(isLastPage: Boolean) {
        if (isLastPage) {
            goToPermissionScreen()
        } else {
            moveToNextPage()
        }
    }

    private fun moveToNextPage() {
        val next = binding.viewPager.currentItem + 1
        if (next <= pages.lastIndex) {
            binding.viewPager.setCurrentItem(next, true)
        }
    }
    private fun goToPermissionScreen() {
        ProgressUtils.dismiss()
        if (!OnboardingActivity.fromsettings) {
            Preferences.setBoolValue(this, "firsttime", true)
            val mainIntent = Intent(this, MainActivity::class.java)
            startActivity(mainIntent)
            finish()
        } else {
            finish()
        }
    }
    private fun loadbannerad(){
        if (!shouldRenderSharedBottomAd) {
            binding.cvAdContainerbanner.visibility = View.GONE
            return
        }
        binding.cvAdContainerbanner.visibility=View.VISIBLE
        AdBanner.loadBannerAdsplash(this,binding.cvAdContainerbanner,RemoteConfigManager.getOnBoardingBannerId(),false,binding.loadingLayoutbanner,true)
            .callback {
                    loaded, failed ->
                if (!shouldRenderSharedBottomAd) {
                    binding.cvAdContainerbanner.visibility = View.GONE
                    return@callback
                }
                if(failed){
                    binding.cvAdContainerbanner.visibility= View.GONE
                }

            }
    }
    private fun loadnativead(){
        if (!shouldRenderSharedBottomAd) {
            binding.cvAdContainernative.visibility = View.GONE
            return
        }
        NativeAdManager.loadAndShowNativeAd(
            activity = this,
            adUnitId = RemoteConfigManager.getOnBoardingNativeAdId(),
            container = binding.cvAdContainernative,
            style = RemoteConfigManager.onboardingnativelayout(),binding.loadingLayoutnative
        ) { loaded, failed, fromCache ->
            if (!shouldRenderSharedBottomAd) {
                binding.cvAdContainernative.visibility = View.GONE
                return@loadAndShowNativeAd
            }
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
