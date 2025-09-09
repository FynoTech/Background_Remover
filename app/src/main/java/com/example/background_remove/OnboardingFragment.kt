package com.example.background_remove

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.utils.ProgressUtils
import com.example.background_remove.utils.RemoteConfigManager
import com.example.background_remove.utils.setSafeClickListenertools
import com.photo.editor.background.eraser.bgeraser.R
import com.photo.editor.background.eraser.bgeraser.databinding.FragmentOnboardingBinding


class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESC = "arg_desc"
        private const val ARG_IMAGE = "arg_image"
        private const val ARG_INDI = "arg_indi"

        fun newInstance(
            title: String,
            description: String,
            imageRes: Int,
            indiRes: Int

        ): OnboardingFragment {
            val fragment = OnboardingFragment()
            val args = Bundle().apply {
                putString(ARG_TITLE, title)
                putString(ARG_DESC, description)
                putInt(ARG_IMAGE, imageRes)
                putInt(ARG_INDI, indiRes)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            tvTitle.text = arguments?.getString(ARG_TITLE)
            tvDesc.text = arguments?.getString(ARG_DESC)
            ivImage.setImageResource(arguments?.getInt(ARG_IMAGE) ?: 0)
            ivindicator.setImageResource(arguments?.getInt(ARG_INDI) ?: 0)
            btnNext.setSafeClickListenertools{
                val viewPager = activity?.findViewById<ViewPager2>(
                    R.id.viewPager
                )
                viewPager?.let {
                    if (it.currentItem < (it.adapter?.itemCount?.minus(1) ?: 0)) {
                        it.currentItem += 1
                    } else {
                        // last page -> finish onboarding
                        if (RemoteConfigManager.isOnboardingInterstitalEnabled()) {
                            ProgressUtils.show(requireContext(),getString(R.string.loading_ad_))
                            Ads.loadAndShowInterstitial(
                                activity = requireActivity(),
                                adUnitId = RemoteConfigManager.getOnBoardingInterstitialId(),
                                placement = "onboarding",
                                useAnyCachedFallback = true,
                                showDelayMs = 0
                            ) { loaded, failed, showed, dismissed ->
                                when {
                                    failed     ->
                                    {
                                        goToPermissionScreen()
                                    }
                                    dismissed  -> {  goToPermissionScreen()
                                    }
                                    loaded     -> { /* optional: ad ready */ }
                                    showed     ->
                                    {

                                    }
                                }
                            }
                        } else {
                            // If disabled → skip ads and go directly
                            goToPermissionScreen()
                        }
                    }
                }

            }
        }
    }
    private fun goToPermissionScreen() {
        ProgressUtils.dismiss()
        if (!OnboardingActivity.fromsettings) {
            val mainIntent = Intent(requireContext(), MainActivity::class.java)
            startActivity(mainIntent)
            activity?.finish()
        } else {
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
