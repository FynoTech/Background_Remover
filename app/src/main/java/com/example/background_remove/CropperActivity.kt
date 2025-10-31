package com.example.background_remove

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.background_remove.adsManager.AdBanner
import com.example.background_remove.adsManager.NativeAdManager
import com.example.background_remove.utils.ProgressUtils
import com.example.background_remove.utils.RemoteConfigManager
import com.example.background_remove.utils.setupStatusBar
import com.photo.editor.background.eraser.bgeraser.R
import com.photo.editor.background.eraser.bgeraser.databinding.ActivityCropperBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CropperActivity : BaseActivity() {

    private lateinit var binding: ActivityCropperBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropperBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        setupStatusBar(window, Color.BLACK)

        val imageUri = intent.getParcelableExtra<Uri>("image_uri")
        imageUri?.let { binding.cropImageView.setImageUriAsync(it) }
        if (RemoteConfigManager.iscropperBannerEnabled()) {
            binding.cvAdContainerbanner.visibility = View.VISIBLE
            loadbannerad()
        } else if (RemoteConfigManager.iscropperNativeEnabled()) {
            binding.cvAdContainernative.visibility = View.VISIBLE
            loadnativead()
        }
        setupButtons()

    }

    private fun setupButtons() = with(binding) {
        back.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        text.setOnClickListener {
            // Launch coroutine on Main thread
            ProgressUtils.show(this@CropperActivity, getString(R.string.cropping_image))
            lifecycleScope.launch {
                val croppedBitmap = cropImageView.croppedImage ?: return@launch

                // Save in background thread
                val croppedUri = withContext(Dispatchers.IO) {
                    saveBitmapToCache(croppedBitmap)
                }

                // Return result to caller
                ProgressUtils.dismiss()
                val result = Intent().apply { putExtra("cropped_uri", croppedUri) }
                setResult(Activity.RESULT_OK, result)
                finish()
            }
        }

        rotate.setOnClickListener { cropImageView.rotateImage(90) }
        flip.setOnClickListener { cropImageView.flipImageHorizontally() }
    }
    private fun loadbannerad(){
        binding.cvAdContainerbanner.visibility= View.VISIBLE
        AdBanner.loadBannerAdsplash(this,binding.cvAdContainerbanner,
            RemoteConfigManager.getCropperBannerAdId(),false,binding.loadingLayoutbanner,true)
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
            adUnitId = RemoteConfigManager.getCropperNativeAdId(),
            container = binding.cvAdContainernative,
            style = RemoteConfigManager.croppernativelayout(),binding.loadingLayoutnative
        ) { loaded, failed, fromCache ->
            Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
            if(failed){
                binding.cvAdContainernative.visibility= View.GONE
            }
        }


    }
    /** Save bitmap to cache directory (IO thread safe) */
    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val file = File(cacheDir, "cropped_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return Uri.fromFile(file)
    }
}
