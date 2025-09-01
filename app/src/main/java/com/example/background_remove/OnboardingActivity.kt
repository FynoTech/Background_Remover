package com.example.background_remove

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.example.background_remove.adapters.OnboardingAdapter
import com.example.background_remove.databinding.ActivityOnboardingBinding
import com.example.background_remove.utils.OnboardingPage
import com.example.background_remove.utils.setupStatusBar


class OnboardingActivity : BaseActivity() {
    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        setupStatusBar(window, Color.BLACK)

     /*   val adBetween1and2 = RemoteConfigManager.isOnboardingNativeFull1Enabled()
        val adBetween2and3 = RemoteConfigManager.isOnboardingNativeFull2Enabled()
        if(RemoteConfigManager.isOnboardingNativeEnabled()){
            loadnativead()
        }*/
        val pages = mutableListOf<OnboardingPage>()

        // Slide 1
        pages.add(
            OnboardingPage.Content(
                title = getString(R.string.remove_background),
                description = getString(R.string.instantly_erase_distractions_and_highlight_your_subject),
                imageRes = R.drawable.firstimage,
                indiREs = R.drawable.firstindi

            )
        )

       /* if (adBetween1and2) {
            pages.add(OnboardingPage.NativeAdPage(position = "1-2"))
        }*/

        // Slide 2
        pages.add(
            OnboardingPage.Content(
                title = getString(R.string.change_background),
                description = getString(R.string.give_your_subject_a_fresh_new_look_in_seconds),
                imageRes = R.drawable.secimage,
                indiREs = R.drawable.secondindi

            )
        )

       /* if (adBetween2and3) {
            pages.add(OnboardingPage.NativeAdPage(position = "2-3"))
        }*/

        // Slide 3
        pages.add(
            OnboardingPage.Content(
                title = getString(R.string.remove_object),
                description = getString(R.string.erase_unwanted_elements_and_keep_the_focus_on_your_subject),
                imageRes = R.drawable.thirdiamge,
                indiREs = R.drawable.thirdindi

            )
        )
        binding.viewPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val page = pages[position]
               /* if (page is OnboardingPage.NativeAdPage) {
                    binding.cvAdContainerNative.visibility = View.GONE
                } else {
                    binding.cvAdContainerNative.visibility = View.VISIBLE
                }*/
            }
        })
        val adapter = OnboardingAdapter(this, pages)
        binding.viewPager.adapter = adapter
    }
 /*   private fun loadnativead(){
        NativeAdManager.loadAndShowNativeAd(
            activity = this,
            adUnitId = RemoteConfigManager.getOnBoardingNativeAdId(),
            container = binding.cvAdContainerNative,
            style = NativeAdManager.NativeStyle.ADVANCE,binding.loadingLayoutNative
        ) { loaded, failed, fromCache ->
            Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
            if(failed){
                binding.cvAdContainerNative.visibility= View.GONE
            }
        }


    }*/
    override fun onPause() {
        super.onPause()
      //  Ads.setPaused(true,this)

    }

    override fun onResume() {
        super.onResume()
      //  Ads.setPaused(false,this)


    }
}
