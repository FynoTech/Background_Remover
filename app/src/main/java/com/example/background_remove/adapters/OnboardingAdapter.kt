package com.example.background_remove.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.background_remove.NativeAdFragment
import com.example.background_remove.utils.OnboardingPage

import com.example.background_remove.OnboardingFragment


class OnboardingAdapter(
    fa: FragmentActivity,
    private val pages: List<OnboardingPage>
) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        return when (val page = pages[position]) {
            is OnboardingPage.Content -> {
                OnboardingFragment.newInstance(
                    title = page.title,
                    description = page.description,
                    imageRes = page.imageRes,
                    indiRes = page.indiREs,
                    screenNumber = page.screenNumber,
                    showTop = page.showTop,
                    showBottom = page.showBottom,

                )
            }
            is OnboardingPage.NativeAdPage -> {
                NativeAdFragment.newInstance(page.position)
            }
        }
    }
}
