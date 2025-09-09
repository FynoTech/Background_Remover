package com.example.background_remove

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import com.example.background_remove.adsManager.AdBanner
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.adsManager.NativeAdManager

import com.example.background_remove.utils.ProgressUtils
import com.example.background_remove.utils.RemoteConfigManager
import com.example.background_remove.utils.ResetGradientColor
import com.example.background_remove.utils.setGradientColor
import com.example.background_remove.utils.setSafeClickListener
import com.example.background_remove.utils.setupStatusBar
import com.photo.editor.background.eraser.bgeraser.R
import com.photo.editor.background.eraser.bgeraser.databinding.ActivityPreviewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class PreviewActivity : BaseActivity() {

    private lateinit var binding: ActivityPreviewBinding
    private var withWatermark: Boolean = true
    private var lastSavedUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPreviewBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setupStatusBar(window, Color.BLACK)
        setContentView(binding.root)

        binding.afterImage.setImageBitmap(ProgressUtils.bitmap)
        binding.beforeImage.setImageBitmap(ProgressUtils.beforebitmap)



        if (RemoteConfigManager.isResultBannerEnabled()) {
            binding.cvAdContainerbanner.visibility = View.VISIBLE
            loadbannerad()
        } else if (RemoteConfigManager.isResultNativeEnabled()) {
            binding.cvAdContainernative.visibility = View.VISIBLE
            loadnativead()
        }

        binding.imagewithwatermark.setSafeClickListener(this) {
            withWatermark = true
            binding.imagewithwatermark.background =
                resources.getDrawable(R.drawable.selected_border)
            binding.imagewithoutwatermark.background =
                resources.getDrawable(R.drawable.unselected_border)
            binding.watermarktext.setGradientColor()
            binding.nowatermarktext.ResetGradientColor()
        }
        binding.imagewithoutwatermark.setSafeClickListener(this) {
            withWatermark = false
            binding.imagewithoutwatermark.background =
                resources.getDrawable(R.drawable.selected_border)
            binding.imagewithwatermark.background =
                resources.getDrawable(R.drawable.unselected_border)
            binding.watermarktext.ResetGradientColor()
            binding.nowatermarktext.setGradientColor()
        }

        binding.share.visibility = View.GONE

        // ✅ Close button
        binding.back.setSafeClickListener(this) {
            if (lastSavedUri != null) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finishAffinity()
            } else {
                finish()
            }
        }

        // ✅ Save button
        binding.btnSave.setSafeClickListener(this,check = RemoteConfigManager.isRewardedAdEnabled(), type = "rewarded") {
            showSaveDialog()
        }

        // ✅ Share button
        binding.share.setSafeClickListener(this) {
            if (lastSavedUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, lastSavedUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Image"))
            } else {
                showToast(getString(R.string.please_save_the_image_first_before_sharing))
            }
        }
    }


        // ✅ Dialog for save options
    private fun showSaveDialog() {

            if (RemoteConfigManager.isRewardedAdEnabled() && !withWatermark) {
                ProgressUtils.show(this,getString(R.string.loading_ad_))
                Ads.loadAndShowRewarded(
                    this,
                    RemoteConfigManager.getRewaredAdId(),
                    showDelayMs = 0L
                ) { loaded, failed, showed, dismissed, rewarded ->
                    when {
                        loaded -> {
                            ProgressUtils.dismiss()
                        }
                        failed -> {
                            ProgressUtils.dismiss()
                            saveImage(withWatermark)
                        }
                        showed -> {
                            saveImage(withWatermark)

                        }
                        dismissed -> {

                        }
                        rewarded -> {

                        }
                    }
                }
            } else {
                saveImage(withWatermark)
            }


        }
    private fun loadbannerad(){
        binding.cvAdContainerbanner.visibility=View.VISIBLE
        AdBanner.loadBannerAdsplash(this,binding.cvAdContainerbanner,RemoteConfigManager.getResultBannerAdId(),false,binding.loadingLayoutbanner,true)
            .callback {
                    loaded, failed ->
                if(failed){
                    binding.cvAdContainerbanner.visibility= View.GONE
                }

            }
    }
    private fun loadnativead(){
        binding.cvAdContainernative.visibility=View.VISIBLE
        NativeAdManager.loadAndShowNativeAd(
            activity = this,
            adUnitId = RemoteConfigManager.getResultNativeAdId(),
            container = binding.cvAdContainernative,
            style = RemoteConfigManager.resultnativelayout(),binding.loadingLayoutnative
        ) { loaded, failed, fromCache ->
            Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
            if(failed){
                binding.cvAdContainernative.visibility= View.GONE
            }
        }


    }

    private fun saveImage(withWatermark: Boolean) {
        val originalBitmap = ProgressUtils.bitmap
        if (originalBitmap != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val finalBitmap = if (withWatermark) {
                        addWatermark(originalBitmap, getString(R.string.ai_background_remover))
                    } else {
                        originalBitmap
                    }

                    lastSavedUri = saveBitmapToDownloads(this@PreviewActivity, finalBitmap)

                    withContext(Dispatchers.Main) {
                        if (lastSavedUri != null) {
                            binding.share.visibility = View.VISIBLE
                            showToast(getString(R.string.image_saved_to_downloads))
                        } else {
                            showToast(getString(R.string.failed_to_save_image))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            showToast(getString(R.string.no_image_found))
        }
    }


    // ✅ Add watermark
    private fun addWatermark(original: Bitmap, watermarkText: String): Bitmap {
        val result = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            isAntiAlias = true
            setShadowLayer(2f, 0f, 2f, Color.DKGRAY)
        }
        val x = result.width * 0.05f
        val y = result.height * 0.98f
        canvas.drawText(watermarkText, x, y, paint)
        return result
    }

    // ✅ Save to Downloads (MediaStore for Android 10+, File for older)
    private fun saveBitmapToDownloads(context: Context, bitmap: Bitmap): Uri? {
        val filename = "IMG_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null
        var imageUri: Uri? = null

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                imageUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val image = File(downloadsDir, filename)
                fos = FileOutputStream(image)
                imageUri = Uri.fromFile(image)
            }
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            imageUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun Context.showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        if (lastSavedUri != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
        } else {
           finish()
        }

    }
}
