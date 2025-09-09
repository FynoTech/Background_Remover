package com.example.background_remove.utils

sealed class OnboardingPage {
    data class Content(
        val title: String,
        val description: String,
        val imageRes: Int,
        val indiREs: Int

    ) : OnboardingPage()

    data class NativeAdPage(val position: String) : OnboardingPage()
}