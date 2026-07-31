//package com.example.background_remove
//
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.content.Intent
//import android.graphics.Color
//import android.graphics.Rect
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import android.view.MotionEvent
//import android.view.View
//import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.FileProvider
//import com.canhub.cropper.CropImageContract
//import com.canhub.cropper.CropImageContractOptions
//import com.canhub.cropper.CropImageOptions
//import com.canhub.cropper.CropImageView
//
//import com.example.background_remove.adsManager.AdBanner
//import com.example.background_remove.adsManager.Ads
//import com.example.background_remove.adsManager.NativeAdManager
//import com.example.background_remove.billing.PremiumActivity
//
//import com.example.background_remove.utils.Dialogs
//import com.example.background_remove.utils.ProgressUtils
//import com.example.background_remove.utils.RemoteConfigManager
//import com.example.background_remove.utils.setSafeClickListener
//import com.example.background_remove.utils.setSafeClickListenertools
//import com.example.background_remove.utils.setupStatusBar
//import com.photo.editor.background.eraser.bgeraser.R
//import com.photo.editor.background.eraser.bgeraser.databinding.ActivityMainBinding
//import java.io.File
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//
//class MainActivity : BaseActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private var imageUri: Uri? = null
//    private var attemptedHomeBanner = false
//    private var attemptedHomeNative = false
//    private var homeBackExitAdInProgress = false
//    private val permissionPrefs by lazy {
//        getSharedPreferences("permission_prefs", MODE_PRIVATE)
//    }
//
//    private val cameraPermissionRequestedKey = "camera_permission_requested"
//
//    private val pickImageLauncher =
//        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
//            uri?.let {
//                imageUri = it
//                // Launch UCrop instead of directly loading
//                val intent = Intent(this, CropperActivity::class.java).apply {
//                    putExtra("image_uri", imageUri)
//                }
//                cropLauncher.launch(intent)
//            }
//        }
//    private val cropLauncher = registerForActivityResult(
//        ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        if (result.resultCode == Activity.RESULT_OK) {
//            val croppedUri = result.data?.getParcelableExtra<Uri>("cropped_uri")
//            croppedUri?.let {
//                val intent = Intent(this, EditorActivity::class.java)
//                intent.putExtra("IMAGE_URI", croppedUri.toString())
//                startActivity(intent)
//            }
//        }
//    }
//
//    private var cameraUri: Uri? = null
//    private val takePictureLauncher =
//        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
//            if (success) {
//                cameraUri?.let {  val intent = Intent(this, CropperActivity::class.java).apply {
//                    putExtra("image_uri", it)
//                }
//                    cropLauncher.launch(intent) }
//            }
//        }
////    private val permissionLauncher =
////        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
////            val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
////            val storageGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
////
////            if (cameraGranted ) {
////                launchCamera()
////            } else if (storageGranted) {
////                MyApp.skipNextResumeAppOpen = true
////                pickImageLauncher.launch(arrayOf("image/*"))
////            } else {
////                Toast.makeText(this,
////                    getString(R.string.permissions_are_required_to_continue), Toast.LENGTH_SHORT).show()
////            }
////        }
//
//    private val cameraPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
//
//            if (granted) {
//                launchCamera()
//                return@registerForActivityResult
//            }
//
//            val wasPreviouslyRequested =
//                permissionPrefs.getBoolean(cameraPermissionRequestedKey, false)
//
//            val permanentlyDenied =
//                wasPreviouslyRequested &&
//                        !shouldShowRequestPermissionRationale(
//                            android.Manifest.permission.CAMERA
//                        )
//
//            if (permanentlyDenied) {
//                showCameraSettingsDialog()
//            } else {
//                Toast.makeText(
//                    this,
//                    getString(R.string.camera_permission_required),
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//
//
//
//    @SuppressLint("ClickableViewAccessibility")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        enableEdgeToEdge()
//        Ads.shownow=true
//
//
//        setupStatusBar(window, Color.BLACK)
//        setContentView(binding.root)
//
//        loadHomeAds()
//        preloadHomeExitInterstitialIfEnabled()
//        binding.menu.setSafeClickListener(this) {
//            val intent = Intent(this, SettingsActivity::class.java)
//            startActivity(intent)
//        }
//        binding.premium.setSafeClickListenertools {
//            val intent = Intent(this, PremiumActivity::class.java)
//            startActivity(intent)
//        }
//
//
//        binding.gallery.setSafeClickListener(this) {
//            checkPermissionsAndOpenGallery()
//        }
//
//        binding.camera.setSafeClickListener(this) {
//            checkPermissionsAndOpenCamera()
//        }
//        binding.beforeImage.post {
//            val layoutWidth = binding.beforeImage.width
//            updateClip(layoutWidth / 2)
//        }
//
//        binding.sliderHandle.setOnTouchListener { v, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                   Log.d("abcd", "onTouch: ACTION_DOWN")
//                    true
//                }
//                MotionEvent.ACTION_MOVE -> {
//                    Log.d("abcd", "onTouch: ACTION_MOVe")
//                    val x = event.rawX.coerceIn(0f, binding.beforeImage.width.toFloat())
//                    updateClip(x.toInt())
//                    true
//                }
//                else -> false
//            }
//        }
//
//    }
//    private fun moveNativeAdToBottom() {
//        val parent = binding.root as androidx.constraintlayout.widget.ConstraintLayout
//        val constraintSet = androidx.constraintlayout.widget.ConstraintSet()
//        constraintSet.clone(parent)
//
//        // Reconnect buttonRow ABOVE the native ad
//        constraintSet.clear(binding.buttonsRow.id, androidx.constraintlayout.widget.ConstraintSet.BOTTOM)
//        constraintSet.connect(
//            binding.buttonsRow.id,
//            androidx.constraintlayout.widget.ConstraintSet.BOTTOM,
//            binding.cvAdContainerNative.id,
//            androidx.constraintlayout.widget.ConstraintSet.TOP
//        )
//        constraintSet.connect(
//            binding.buttonsRow.id,
//            androidx.constraintlayout.widget.ConstraintSet.TOP,
//            binding.comparisonLayout.id,
//            androidx.constraintlayout.widget.ConstraintSet.BOTTOM
//        )
//
//        // Reconnect native ad AT THE BOTTOM
//        constraintSet.clear(binding.cvAdContainerNative.id, androidx.constraintlayout.widget.ConstraintSet.TOP)
//        constraintSet.connect(
//            binding.cvAdContainerNative.id,
//            androidx.constraintlayout.widget.ConstraintSet.BOTTOM,
//            androidx.constraintlayout.widget.ConstraintSet.PARENT_ID,
//            androidx.constraintlayout.widget.ConstraintSet.BOTTOM
//        )
//
//        constraintSet.applyTo(parent)
//    }
//
//    private fun loadHomeAds() {
//        attemptedHomeBanner = false
//        attemptedHomeNative = false
//        binding.cvAdContainerBanner.visibility = View.GONE
//        binding.cvAdContainerNative.visibility = View.GONE
//
//        // Requirement: Home primary preference is Banner when banner_onoff = true.
//        if (RemoteConfigManager.isHomeBannerEnabled()) {
//            loadbannerad()
//        } else if (RemoteConfigManager.isHomeNativeEnabled()) {
//            loadnativead()
//        }
//    }
//
//    private fun loadbannerad(){
//        if (attemptedHomeBanner) return
//        attemptedHomeBanner = true
//        binding.cvAdContainerBanner.visibility= View.VISIBLE
//        AdBanner.loadBannerAdsplash(this,binding.cvAdContainerBanner,
//            RemoteConfigManager.getHomeBannerAdId(),false,binding.loadingLayoutBanner,false)
//            .callback {
//                    loaded, failed ->
//                if(failed){
//                    binding.cvAdContainerBanner.visibility= View.GONE
//                    if (!attemptedHomeNative &&
//                        RemoteConfigManager.isHomeNativeEnabled() &&
//                        RemoteConfigManager.isHomeBannerFailNativeEnabled()
//                    ) {
//                        loadnativead()
//                    }
//                }
//
//            }
//    }
//    private fun loadnativead(){
//        if (attemptedHomeNative) return
//        attemptedHomeNative = true
//        binding.cvAdContainerNative.visibility= View.VISIBLE
//        if (RemoteConfigManager.homenativelayout()==1) {
//            // Move native ad to bottom dynamically
//            moveNativeAdToBottom()
//        }
//        NativeAdManager.loadAndShowNativeAd(
//            activity = this,
//            adUnitId = RemoteConfigManager.getHomeNativeAdId(),
//            container = binding.cvAdContainerNative,
//            style = RemoteConfigManager.homenativelayout(),binding.loadingLayoutNative
//        ) { loaded, failed, fromCache ->
//            Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
//            if(failed){
//                binding.cvAdContainerNative.visibility= View.GONE
//                if (!attemptedHomeBanner &&
//                    RemoteConfigManager.isHomeBannerEnabled() &&
//                    RemoteConfigManager.isHomeNativeFailBannerEnabled()
//                ) {
//                    loadbannerad()
//                }
//            }
//        }
//
//
//    }
//
//    private fun launchCamera() {
//        try {
//            val photoFile: File? = createImageFile()
//            photoFile?.also {
//                cameraUri = FileProvider.getUriForFile(
//                    this,
//                    "${packageName}.fileprovider",
//                    it
//                )
//                MyApp.skipNextResumeAppOpen = true
//                takePictureLauncher.launch(cameraUri!!)
//            }
//        } catch (ex: Exception) {
//            Toast.makeText(this, getString(R.string.failed_to_create_image_file), Toast.LENGTH_SHORT).show()
//        }
//    }
//    private fun createImageFile(): File {
//        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//        // Save to internal cache directory
//        val storageDir: File = cacheDir
//        return File.createTempFile(
//            "JPEG_${timeStamp}_",
//            ".jpg",
//            storageDir
//        )
//    }
//    private fun updateClip(x: Int) {
//        // Clip Before image
//        binding.beforeImage.clipBounds = Rect(0, 0, x, binding.beforeImage.height)
//
//        // Move divider & handle
//        binding.dividerLine.translationX = x.toFloat()
//        binding.sliderHandle.translationX = x.toFloat() - binding.sliderHandle.width / 2
//    }
//    private fun checkPermissionsAndOpenGallery() {
//        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
//            // Below Android 10, request storage permission
//            permissionLauncher.launch(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE))
//        } else {
//            // Android 10+ no need
//            MyApp.skipNextResumeAppOpen = true
//            pickImageLauncher.launch(arrayOf("image/*"))
//        }
//    }
//
//    private fun checkPermissionsAndOpenCamera() {
//        val permission = android.Manifest.permission.CAMERA
//
//        when {
//            androidx.core.content.ContextCompat.checkSelfPermission(
//                this,
//                permission
//            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
//                launchCamera()
//            }
//
//            shouldShowRequestPermissionRationale(permission) -> {
//                showCameraPermissionRationaleDialog()
//            }
//
//            permissionPrefs.getBoolean(cameraPermissionRequestedKey, false) -> {
//                // Permission was already requested and Android no longer
//                // allows showing the permission popup.
//                showCameraSettingsDialog()
//            }
//
//            else -> {
//                requestCameraPermission()
//            }
//        }
//    }
//
//    private fun requestCameraPermission() {
//        permissionPrefs.edit()
//            .putBoolean(cameraPermissionRequestedKey, true)
//            .apply()
//
//        cameraPermissionLauncher.launch(
//            android.Manifest.permission.CAMERA
//        )
//    }
//
//    private fun showCameraPermissionRationaleDialog() {
//        androidx.appcompat.app.AlertDialog.Builder(this)
//            .setTitle(getString(R.string.camera_permission_title))
//            .setMessage(getString(R.string.camera_permission_message))
//            .setNegativeButton(getString(R.string.cancel), null)
//            .setPositiveButton(getString(R.string.allow)) { _, _ ->
//                requestCameraPermission()
//            }
//            .show()
//    }
//
//    private fun showCameraSettingsDialog() {
//        androidx.appcompat.app.AlertDialog.Builder(this)
//            .setTitle(getString(R.string.camera_permission_denied_title))
//            .setMessage(getString(R.string.camera_permission_denied_message))
//            .setNegativeButton(getString(R.string.cancel), null)
//            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
//                openAppSettings()
//            }
//            .show()
//    }
//
//    private fun openAppSettings() {
//        try {
//            MyApp.skipNextResumeAppOpen = true
//
//            val intent = Intent(
//                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//            ).apply {
//                data = Uri.fromParts("package", packageName, null)
//            }
//
//            startActivity(intent)
//        } catch (exception: Exception) {
//            Toast.makeText(
//                this,
//                getString(R.string.unable_to_open_settings),
//                Toast.LENGTH_SHORT
//            ).show()
//        }
//    }
////    private fun checkPermissionsAndOpenCamera() {
////        val permissions = mutableListOf(android.Manifest.permission.CAMERA)
////        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
////            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
////        }
////
////        if (permissions.isNotEmpty()) {
////            permissionLauncher.launch(permissions.toTypedArray())
////        } else {
////            launchCamera()
////        }
////    }
//
//    private fun preloadHomeExitInterstitialIfEnabled() {
//        if (!RemoteConfigManager.isHomeExitInterstitialEnabled()) return
//        if (!Ads.isNetworkAvailableWithoutPremium(this)) return
//        Ads.preloadInterstitial(
//            ctx = this,
//            adUnitId = RemoteConfigManager.getHomeExitInterstitialAdId(),
//            placement = "home_exit",
//        )
//    }
//
//    override fun onBackPressed() {
//        if (homeBackExitAdInProgress) return
//        if (RemoteConfigManager.isHomeExitInterstitialEnabled()) {
//            homeBackExitAdInProgress = true
//            ProgressUtils.show(this, getString(R.string.loading_ad_))
//            Ads.loadAndShowInterstitial(
//                activity = this,
//                adUnitId = RemoteConfigManager.getHomeExitInterstitialAdId(),
//                placement = "home_exit",
//                useAnyCachedFallback = true,
//                showDelayMs = 0L,
//            ) { loaded, failed, showed, dismissed ->
//                runOnUiThread {
//                    if (loaded || failed || showed || dismissed) {
//                        ProgressUtils.dismiss()
//                    }
//                    if (failed || dismissed) {
//                        homeBackExitAdInProgress = false
//                        startActivity(
//                            Intent(this@MainActivity, ExitActivity::class.java).putExtra(
//                                ExitActivity.EXTRA_FROM_HOME,
//                                true,
//                            ),
//                        )
//                    }
//                }
//            }
//        } else {
//            startActivity(
//                Intent(this, ExitActivity::class.java).putExtra(
//                    ExitActivity.EXTRA_FROM_HOME,
//                    true,
//                ),
//            )
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if(Ads.isPremiumUser(this)){
//            binding.premium.visibility= View.GONE
//        }
//
//    }
//
//
//
//}


package com.example.background_remove

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

import com.example.background_remove.adsManager.AdBanner
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.adsManager.NativeAdManager
import com.example.background_remove.billing.PremiumActivity

import com.example.background_remove.utils.Dialogs
import com.example.background_remove.utils.ProgressUtils
import com.example.background_remove.utils.RemoteConfigManager
import com.example.background_remove.utils.setSafeClickListener
import com.example.background_remove.utils.setSafeClickListenertools
import com.example.background_remove.utils.setupStatusBar
import com.photo.editor.background.eraser.bgeraser.R
import com.photo.editor.background.eraser.bgeraser.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageUri: Uri? = null
    private var attemptedHomeBanner = false
    private var attemptedHomeNative = false
    private var homeBackExitAdInProgress = false

    private val permissionPrefs by lazy {
        getSharedPreferences("permission_prefs", MODE_PRIVATE)
    }

    private val cameraPermissionRequestedKey = "camera_permission_requested"

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                imageUri = it
                // Launch UCrop instead of directly loading
                val intent = Intent(this, CropperActivity::class.java).apply {
                    putExtra("image_uri", imageUri)
                }
                cropLauncher.launch(intent)
            }
        }

    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val croppedUri = result.data?.getParcelableExtra<Uri>("cropped_uri")
            croppedUri?.let {
                val intent = Intent(this, EditorActivity::class.java)
                intent.putExtra("IMAGE_URI", croppedUri.toString())
                startActivity(intent)
            }
        }
    }

    private var cameraUri: Uri? = null

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraUri?.let {
                    val intent = Intent(this, CropperActivity::class.java).apply {
                        putExtra("image_uri", it)
                    }
                    cropLauncher.launch(intent)
                }
            }
        }

//    private val permissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: false
//            val storageGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
//
//            if (cameraGranted ) {
//                launchCamera()
//            } else if (storageGranted) {
//                MyApp.skipNextResumeAppOpen = true
//                pickImageLauncher.launch(arrayOf("image/*"))
//            } else {
//                Toast.makeText(this,
//                    getString(R.string.permissions_are_required_to_continue), Toast.LENGTH_SHORT).show()
//            }
//        }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                MyApp.skipNextResumeAppOpen = true
                pickImageLauncher.launch(arrayOf("image/*"))
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permissions_are_required_to_continue),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            if (granted) {
                launchCamera()
                return@registerForActivityResult
            }

            val wasPreviouslyRequested =
                permissionPrefs.getBoolean(cameraPermissionRequestedKey, false)

            val permanentlyDenied =
                wasPreviouslyRequested &&
                        !shouldShowRequestPermissionRationale(
                            android.Manifest.permission.CAMERA
                        )

            if (permanentlyDenied) {
                showCameraSettingsDialog()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.camera_permission_required),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        Ads.shownow = true

        setupStatusBar(window, Color.BLACK)
        setContentView(binding.root)

        loadHomeAds()
        preloadHomeExitInterstitialIfEnabled()

        binding.menu.setSafeClickListener(this) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.premium.setSafeClickListenertools {
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
                    val x = event.rawX.coerceIn(
                        0f,
                        binding.beforeImage.width.toFloat()
                    )
                    updateClip(x.toInt())
                    true
                }

                else -> false
            }
        }
    }

    private fun moveNativeAdToBottom() {
        val parent =
            binding.root as androidx.constraintlayout.widget.ConstraintLayout
        val constraintSet =
            androidx.constraintlayout.widget.ConstraintSet()

        constraintSet.clone(parent)

        // Reconnect buttonRow ABOVE the native ad
        constraintSet.clear(
            binding.buttonsRow.id,
            androidx.constraintlayout.widget.ConstraintSet.BOTTOM
        )

        constraintSet.connect(
            binding.buttonsRow.id,
            androidx.constraintlayout.widget.ConstraintSet.BOTTOM,
            binding.cvAdContainerNative.id,
            androidx.constraintlayout.widget.ConstraintSet.TOP
        )

        constraintSet.connect(
            binding.buttonsRow.id,
            androidx.constraintlayout.widget.ConstraintSet.TOP,
            binding.comparisonLayout.id,
            androidx.constraintlayout.widget.ConstraintSet.BOTTOM
        )

        // Reconnect native ad AT THE BOTTOM
        constraintSet.clear(
            binding.cvAdContainerNative.id,
            androidx.constraintlayout.widget.ConstraintSet.TOP
        )

        constraintSet.connect(
            binding.cvAdContainerNative.id,
            androidx.constraintlayout.widget.ConstraintSet.BOTTOM,
            androidx.constraintlayout.widget.ConstraintSet.PARENT_ID,
            androidx.constraintlayout.widget.ConstraintSet.BOTTOM
        )

        constraintSet.applyTo(parent)
    }

    private fun loadHomeAds() {
        attemptedHomeBanner = false
        attemptedHomeNative = false

        binding.cvAdContainerBanner.visibility = View.GONE
        binding.cvAdContainerNative.visibility = View.GONE

        // Requirement: Home primary preference is Banner when banner_onoff = true.
        if (RemoteConfigManager.isHomeBannerEnabled()) {
            loadbannerad()
        } else if (RemoteConfigManager.isHomeNativeEnabled()) {
            loadnativead()
        }
    }

    private fun loadbannerad() {
        if (attemptedHomeBanner) return

        attemptedHomeBanner = true
        binding.cvAdContainerBanner.visibility = View.VISIBLE

        AdBanner.loadBannerAdsplash(
            this,
            binding.cvAdContainerBanner,
            RemoteConfigManager.getHomeBannerAdId(),
            false,
            binding.loadingLayoutBanner,
            false
        ).callback { loaded, failed ->
            if (failed) {
                binding.cvAdContainerBanner.visibility = View.GONE

                if (
                    !attemptedHomeNative &&
                    RemoteConfigManager.isHomeNativeEnabled() &&
                    RemoteConfigManager.isHomeBannerFailNativeEnabled()
                ) {
                    loadnativead()
                }
            }
        }
    }

    private fun loadnativead() {
        if (attemptedHomeNative) return

        attemptedHomeNative = true
        binding.cvAdContainerNative.visibility = View.VISIBLE

        if (RemoteConfigManager.homenativelayout() == 1) {
            // Move native ad to bottom dynamically
            moveNativeAdToBottom()
        }

        NativeAdManager.loadAndShowNativeAd(
            activity = this,
            adUnitId = RemoteConfigManager.getHomeNativeAdId(),
            container = binding.cvAdContainerNative,
            style = RemoteConfigManager.homenativelayout(),
            binding.loadingLayoutNative
        ) { loaded, failed, fromCache ->

            Log.d(
                "Ads",
                "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache"
            )

            if (failed) {
                binding.cvAdContainerNative.visibility = View.GONE

                if (
                    !attemptedHomeBanner &&
                    RemoteConfigManager.isHomeBannerEnabled() &&
                    RemoteConfigManager.isHomeNativeFailBannerEnabled()
                ) {
                    loadbannerad()
                }
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

                MyApp.skipNextResumeAppOpen = true
                takePictureLauncher.launch(cameraUri!!)
            }
        } catch (ex: Exception) {
            Toast.makeText(
                this,
                getString(R.string.failed_to_create_image_file),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.getDefault()
            ).format(Date())

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
        binding.beforeImage.clipBounds =
            Rect(0, 0, x, binding.beforeImage.height)

        // Move divider & handle
        binding.dividerLine.translationX = x.toFloat()
        binding.sliderHandle.translationX =
            x.toFloat() - binding.sliderHandle.width / 2
    }

    private fun checkPermissionsAndOpenGallery() {
        if (
            android.os.Build.VERSION.SDK_INT <
            android.os.Build.VERSION_CODES.Q
        ) {
            val permission =
                android.Manifest.permission.READ_EXTERNAL_STORAGE

            val permissionGranted =
                androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (permissionGranted) {
                MyApp.skipNextResumeAppOpen = true
                pickImageLauncher.launch(arrayOf("image/*"))
            } else {
                storagePermissionLauncher.launch(permission)
            }
        } else {
            // Android 10+ no need
            MyApp.skipNextResumeAppOpen = true
            pickImageLauncher.launch(arrayOf("image/*"))
        }
    }

    private fun checkPermissionsAndOpenCamera() {
        val permission = android.Manifest.permission.CAMERA

        when {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }

            shouldShowRequestPermissionRationale(permission) -> {
                showCameraPermissionRationaleDialog()
            }

            permissionPrefs.getBoolean(
                cameraPermissionRequestedKey,
                false
            ) -> {
                // Permission was already requested and Android no longer
                // allows showing the permission popup.
                showCameraSettingsDialog()
            }

            else -> {
                requestCameraPermission()
            }
        }
    }

    private fun requestCameraPermission() {
        permissionPrefs.edit()
            .putBoolean(cameraPermissionRequestedKey, true)
            .apply()

        cameraPermissionLauncher.launch(
            android.Manifest.permission.CAMERA
        )
    }

    private fun showCameraPermissionRationaleDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.camera_permission_title))
            .setMessage(getString(R.string.camera_permission_message))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.allow)) { _, _ ->
                requestCameraPermission()
            }
            .show()
    }

    private fun showCameraSettingsDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(
                getString(R.string.camera_permission_denied_title)
            )
            .setMessage(
                getString(R.string.camera_permission_denied_message)
            )
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(
                getString(R.string.open_settings)
            ) { _, _ ->
                openAppSettings()
            }
            .show()
    }

    private fun openAppSettings() {
        try {
            MyApp.skipNextResumeAppOpen = true

            val intent = Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ).apply {
                data = Uri.fromParts(
                    "package",
                    packageName,
                    null
                )
            }

            startActivity(intent)
        } catch (exception: Exception) {
            Toast.makeText(
                this,
                getString(R.string.unable_to_open_settings),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

//    private fun checkPermissionsAndOpenCamera() {
//        val permissions = mutableListOf(android.Manifest.permission.CAMERA)
//        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
//            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
//        }
//
//        if (permissions.isNotEmpty()) {
//            permissionLauncher.launch(permissions.toTypedArray())
//        } else {
//            launchCamera()
//        }
//    }

    private fun preloadHomeExitInterstitialIfEnabled() {
        if (!RemoteConfigManager.isHomeExitInterstitialEnabled()) return
        if (!Ads.isNetworkAvailableWithoutPremium(this)) return

        Ads.preloadInterstitial(
            ctx = this,
            adUnitId = RemoteConfigManager.getHomeExitInterstitialAdId(),
            placement = "home_exit",
        )
    }

    override fun onBackPressed() {
        if (homeBackExitAdInProgress) return

        if (RemoteConfigManager.isHomeExitInterstitialEnabled()) {
            homeBackExitAdInProgress = true

            ProgressUtils.show(
                this,
                getString(R.string.loading_ad_)
            )

            Ads.loadAndShowInterstitial(
                activity = this,
                adUnitId = RemoteConfigManager.getHomeExitInterstitialAdId(),
                placement = "home_exit",
                useAnyCachedFallback = true,
                showDelayMs = 0L,
            ) { loaded, failed, showed, dismissed ->

                runOnUiThread {
                    if (loaded || failed || showed || dismissed) {
                        ProgressUtils.dismiss()
                    }

                    if (failed || dismissed) {
                        homeBackExitAdInProgress = false

                        startActivity(
                            Intent(
                                this@MainActivity,
                                ExitActivity::class.java
                            ).putExtra(
                                ExitActivity.EXTRA_FROM_HOME,
                                true,
                            ),
                        )
                    }
                }
            }
        } else {
            startActivity(
                Intent(
                    this,
                    ExitActivity::class.java
                ).putExtra(
                    ExitActivity.EXTRA_FROM_HOME,
                    true,
                ),
            )
        }
    }

    override fun onResume() {
        super.onResume()

        if (Ads.isPremiumUser(this)) {
            binding.premium.visibility = View.GONE
        }
    }
}