package com.example.background_remove

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.background_remove.databinding.FragmentNativeAdBinding


class NativeAdFragment : Fragment() {

    private var _binding: FragmentNativeAdBinding? = null
    private val binding get() = _binding!!
    var failed1=false
    var failed2=false
    companion object {
        private const val ARG_POSITION = "arg_position"

        fun newInstance(position: String): NativeAdFragment {
            val fragment = NativeAdFragment()
            val args = Bundle().apply {
                putString(ARG_POSITION, position)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private var position: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        position = arguments?.getString(ARG_POSITION)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNativeAdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun onResume() {
        super.onResume()
        if(position=="1-2" && failed1){
            moveToNextPage()

        }else if(position=="2-3"  && failed2){
            moveToNextPage()

        }
    }
    /*fun loadnative1(){
        NativeAdManager.loadAndShowNativeAd(
            activity = requireActivity(),
            adUnitId = RemoteConfigManager.getOnBoardingNativeFull1AdId(),
            container = binding.cvAdContainerNative,
            style = NativeAdManager.NativeStyle.FULL,binding.loadingLayoutNative
        ) { loaded, failed, fromCache ->
            Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
            if(failed){
                binding.cvAdContainerNative.visibility= View.GONE
                moveToNextPage()
            }
        }
    }
    fun loadnative2(){
        NativeAdManager.loadAndShowNativeAd(
            activity = requireActivity(),
            adUnitId = RemoteConfigManager.getOnBoardingNativeFull2AdId(),
            container = binding.cvAdContainerNative,
            style = NativeAdManager.NativeStyle.FULL,binding.loadingLayoutNative
        ) { loaded, failed, fromCache ->
            Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
            if(failed){
                binding.cvAdContainerNative.visibility= View.GONE
                moveToNextPage()
            }
        }
    }*/
    private fun moveToNextPage() {
        val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager)
        viewPager?.let {
            if (it.currentItem < (it.adapter?.itemCount ?: 0) - 1) {
                it.currentItem += 1
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

