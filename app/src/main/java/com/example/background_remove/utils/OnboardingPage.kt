package com.example.background_remove.utils

sealed class OnboardingPage {
    data class Content(
        val title: String,
        val description: String,
        val imageRes: Int,
        val indiREs: Int,
        val screenNumber: Int,
        val showTop: Boolean = true,
        val showBottom: Boolean = true,

    ) : OnboardingPage()

    data class NativeAdPage(val position: String) : OnboardingPage()
}