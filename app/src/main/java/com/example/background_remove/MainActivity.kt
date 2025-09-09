package com.example.background_remove

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.canhub.cropper.CropImageActivity
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

import com.example.autobackgroundremover.OnBackgroundChangeListener
import com.example.background_remove.adsManager.AdBanner
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.adsManager.NativeAdManager
import com.example.background_remove.billing.PremiumActivity

import com.example.background_remove.utils.Dialogs
import com.example.background_remove.utils.RemoteConfigManager
import com.example.background_remove.utils.setSafeClickListener
import com.example.background_remove.utils.setSafeClickListenertools
import com.example.background_remove.utils.setupStatusBar
import com.photo.editor.background.eraser.bgeraser.R
import com.photo.editor.background.eraser.bgeraser.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                // Launch UCrop instead of directly loading
                startCrop(it)
            }
        }

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // Use the cropped image URI.
            val croppedImageUri = result.uriContent
            val intent = Intent(this, EditorActivity::class.java)
            intent.putExtra("IMAGE_URI", croppedImageUri.toString())
            startActivity(intent)

        } else {
            // An error occurred.
            val exception = result.error
            // Handle the error.
        }

    }
    private var cameraUri: Uri? = null
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraUri?.let { startCrop(it) }
            }
        }
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
            val storageGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

            if (cameraGranted ) {
                launchCamera()
            } else if (storageGranted) {
                pickImageLauncher.launch(arrayOf("image/*"))
            } else {
                Toast.makeText(this, "Permissions are required to continue", Toast.LENGTH_SHORT).show()
            }
        }
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        Ads.shownow=true


        setupStatusBar(window, Color.BLACK)
        setContentView(binding.root)

        if(RemoteConfigManager.isHomeNativeEnabled()){
            loadnativead()
        }
        else{
            binding.cvAdContainerNative.visibility= View.GONE
        }
        if(RemoteConfigManager.isHomeBannerEnabled() ){
            loadbannerad()
        }else{
            binding.cvAdContainerBanner.visibility= View.GONE
        }
        binding.menu.setSafeClickListener(this) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        binding.premium.setSafeClickListener(this) {
            val intent = Intent(this, PremiumActivity::class.java)
            startActivity(intent)
        }


        binding.gallery.setSafeClickListener(this) {
            checkPermissionsAndOpenGallery()
        }

        binding.camera.setSafeClickListener(this) {
            checkPermissionsAndOpenCamera()
        }
        binding.beforeImage.post {
            val layoutWidth = binding.beforeImage.width
            updateClip(layoutWidth / 2)
        }

        binding.sliderHandle.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                   Log.d("abcd", "onTouch: ACTION_DOWN")
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    Log.d("abcd", "onTouch: ACTION_MOVe")
                    val x = event.rawX.coerceIn(0f, binding.beforeImage.width.toFloat())
                    updateClip(x.toInt())
                    true
                }
                else -> false
            }
        }

    }
    private fun loadbannerad(){
        binding.cvAdContainerBanner.visibility= View.VISIBLE
        AdBanner.loadBannerAdsplash(this,binding.cvAdContainerBanner,
            RemoteConfigManager.getHomeBannerAdId(),false,binding.loadingLayoutBanner,false)
            .callback {
                    loaded, failed ->
                if(failed){
                    binding.cvAdContainerBanner.visibility= View.GONE
                }

            }
    }
    private fun loadnativead(){
        binding.cvAdContainerNative.visibility= View.VISIBLE
        NativeAdManager.loadAndShowNativeAd(
            activity = this,
            adUnitId = RemoteConfigManager.getHomeNativeAdId(),
            container = binding.cvAdContainerNative,
            style = RemoteConfigManager.homenativelayout(),binding.loadingLayoutNative
        ) { loaded, failed, fromCache ->
            Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
            if(failed){
                binding.cvAdContainerNative.visibility= View.GONE
            }
        }


    }

    private fun launchCamera() {
        try {
            val photoFile: File? = createImageFile()
            photoFile?.also {
                cameraUri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    it
                )
                takePictureLauncher.launch(cameraUri!!)
            }
        } catch (ex: Exception) {
            Toast.makeText(this, getString(R.string.failed_to_create_image_file), Toast.LENGTH_SHORT).show()
        }
    }
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        // Save to internal cache directory
        val storageDir: File = cacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }
    private fun updateClip(x: Int) {
        // Clip Before image
        binding.beforeImage.clipBounds = Rect(0, 0, x, binding.beforeImage.height)

        // Move divider & handle
        binding.dividerLine.translationX = x.toFloat()
        binding.sliderHandle.translationX = x.toFloat() - binding.sliderHandle.width / 2
    }
    private fun checkPermissionsAndOpenGallery() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            // Below Android 10, request storage permission
            permissionLauncher.launch(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE))
        } else {
            // Android 10+ no need
            pickImageLauncher.launch(arrayOf("image/*"))
        }
    }

    private fun checkPermissionsAndOpenCamera() {
        val permissions = mutableListOf(android.Manifest.permission.CAMERA)
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            launchCamera()
        }
    }

    override fun onBackPressed() {
        Dialogs.showQuitDialog(
            context = this,
            title = getString(R.string.quit),
            onConfirm = {
                System.exit(0)
                finishAffinity()
            },
            onCancel = {
                Toast.makeText(this, getString(R.string.cancelled), Toast.LENGTH_SHORT).show()
            }
        )

    }

    override fun onResume() {
        super.onResume()
        if(Ads.isPremiumUser(this)){
            binding.premium.visibility= View.GONE
        }

    }
    private fun startCrop(uri: Uri) {
        cropImage.launch(

            CropImageContractOptions(uri,
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    toolbarColor = Color.parseColor("#FFDD2476")



                )

            )
        )
    }


}
