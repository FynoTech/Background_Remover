package com.example.background_remove

import ColorAdapter
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

import com.example.autobackgroundremover.OnBackgroundChangeListener
import com.example.background_remove.adsManager.AdBanner
import com.example.background_remove.adsManager.Ads
import com.example.background_remove.adsManager.NativeAdManager


import com.example.background_remove.utils.Dialogs
import com.example.background_remove.utils.Dialogs.colors
import com.example.background_remove.utils.Dialogs.showQuitDialog
import com.example.background_remove.utils.Preferences
import com.example.background_remove.utils.ProgressUtils
import com.example.background_remove.utils.RemoteConfigManager

import com.example.background_remove.utils.ResetGradientColor
import com.example.background_remove.utils.ResetGradientDrawableColors

import com.example.background_remove.utils.setGradientColor
import com.example.background_remove.utils.setGradientDrawableColors
import com.example.background_remove.utils.setSafeClickListener
import com.example.background_remove.utils.setSafeClickListenertools
import com.example.background_remove.utils.setupStatusBar
import com.photo.editor.background.eraser.bgeraser.R
import com.photo.editor.background.eraser.bgeraser.databinding.ActivityEditorBinding


class EditorActivity : BaseActivity() {

    private lateinit var binding: ActivityEditorBinding
    private var imageUri: Uri? = null

    private val cropImage = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            // Use the cropped image URI.
            val croppedImageUri = result.uriContent
            setDrawViewBitmap(croppedImageUri!!)

        } else {
            // An error occurred.
            val exception = result.error
            // Handle the error.
        }

    }
    private var selectedLayout: LinearLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        enableEdgeToEdge()

        setupStatusBar(window, Color.BLACK)
        setContentView(binding.root)
        Log.d("abcd", "onCreate:${intent.getStringExtra("IMAGE_URI").toString()} ")
        setDrawViewBitmap(Uri.parse(intent.getStringExtra("IMAGE_URI").toString()))
        setupStrokeBar()
        setupUndoRedo()




        binding.done.setSafeClickListener(this) {
            ProgressUtils.bitmap=binding.drawView.getBitmap()
            val intent = Intent(this, PreviewActivity::class.java)
            startActivity(intent)


        }
        binding.back.setSafeClickListener(this) {
            showQuitDialog(
                context = this,
                title = getString(R.string.quit),
                onConfirm = {
                    finish() // close the activity
                },
                onCancel = {
                    Toast.makeText(this, getString(R.string.cancelled), Toast.LENGTH_SHORT).show()
                }
            )
        }

        val adapter = ColorAdapter(colors) { clickedColor ->
            if(clickedColor==null){
                binding.drawView.background=ContextCompat.getDrawable(this,R.drawable.pngpg)
                binding.drawView.setColor(Color.TRANSPARENT)
            }else{
                binding.drawView.setColor(clickedColor)
            }

        }
        val recyclerView = findViewById<RecyclerView>(R.id.colorRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter
        // Auto Removal click
        binding.layoutAutoRemoval.setSafeClickListener(this,check = RemoteConfigManager.isRewardedAutoAdEnabled(), type = "rewarded") {
            if (!RemoteConfigManager.isRewardedAutoAdEnabled()) {
                if (!autocheck) {
                    autoSelfie()

                } else {
                    binding.manualClearSettingsLayout.visibility = android.view.View.GONE
                    binding.colorlayout.visibility = android.view.View.VISIBLE
                    selectItem(binding.layoutAutoRemoval)
                    Toast.makeText(this, getString(R.string.already_removed), Toast.LENGTH_SHORT).show()
                }
            } else {
                ProgressUtils.show(this,getString(R.string.loading_ad_))
                Ads.loadAndShowRewarded(
                    this,
                    RemoteConfigManager.getRewardedAutoAdId(),
                    showDelayMs = 0L
                ) { loaded, failed, showed, dismissed, rewarded ->
                    when {
                        loaded -> {
                            ProgressUtils.dismiss()
                        }
                        failed -> {
                            ProgressUtils.dismiss()
                            if (!autocheck) {
                                autoSelfie()

                            } else {
                                binding.manualClearSettingsLayout.visibility = android.view.View.GONE
                                binding.colorlayout.visibility = android.view.View.VISIBLE
                                selectItem(binding.layoutAutoRemoval)
                                Toast.makeText(this, getString(R.string.already_removed), Toast.LENGTH_SHORT).show()
                            }
                        }
                        showed -> {


                        }
                        dismissed -> {
                            if (!autocheck) {
                                autoSelfie()

                            } else {
                                binding.manualClearSettingsLayout.visibility = android.view.View.GONE
                                binding.colorlayout.visibility = android.view.View.VISIBLE
                                selectItem(binding.layoutAutoRemoval)
                                Toast.makeText(this, getString(R.string.already_removed), Toast.LENGTH_SHORT).show()
                            }
                        }
                        rewarded -> {

                        }
                    }
                }
            }
        }

        // Manual click
        binding.layoutManual.setSafeClickListener(this) {
            binding.drawView.setAction(DrawView.DrawViewAction.MANUAL_CLEAR)
            binding.manualClearSettingsLayout.visibility = android.view.View.VISIBLE
            binding.colorlayout.visibility = android.view.View.GONE
            selectItem(binding.layoutManual)
        }

        // Crop click
        binding.layoutCrop.setSafeClickListener(this) {
            imageUri?.let { uri -> startCrop(uri) }
            binding.manualClearSettingsLayout.visibility = android.view.View.GONE
            binding.colorlayout.visibility = android.view.View.GONE
            selectItem(binding.layoutCrop)
        }

        // Repair click
        binding.layoutRepair.setSafeClickListener(this) {
            binding.drawView.setAction(DrawView.DrawViewAction.MANUAL_REPAIR)
            binding.manualClearSettingsLayout.visibility = android.view.View.VISIBLE
            binding.colorlayout.visibility = android.view.View.GONE
            selectItem(binding.layoutRepair)
        }
        if (RemoteConfigManager.isEditorBannerEnabled()) {
            binding.cvAdContainerbanner.visibility = View.VISIBLE
            loadbannerad()
        } else if (RemoteConfigManager.isEditorNativeEnabled()) {
            binding.cvAdContainernative.visibility = View.VISIBLE
            loadnativead()
        }
        // Zoom click
        binding.layoutZoom.setSafeClickListener(this) {
            binding.drawView.setAction(DrawView.DrawViewAction.ZOOM)
            binding.manualClearSettingsLayout.visibility = android.view.View.GONE
            binding.colorlayout.visibility = android.view.View.GONE
            selectItem(binding.layoutZoom)
        }


        selectItem(binding.layoutManual)
        binding.colorlayout.visibility = android.view.View.GONE

    }


    private fun loadbannerad(){
        binding.cvAdContainerbanner.visibility=View.VISIBLE
        AdBanner.loadBannerAdsplash(this,binding.cvAdContainerbanner,RemoteConfigManager.getEditorBannerAdId(),false,binding.loadingLayoutbanner,true)
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
            adUnitId = RemoteConfigManager.getEditorNativeAdId(),
            container = binding.cvAdContainernative,
            style = 2,binding.loadingLayoutnative
        ) { loaded, failed, fromCache ->
            Log.d("Ads", "Native Ad loaded=$loaded failed=$failed fromCache=$fromCache")
            if(failed){
                binding.cvAdContainernative.visibility= View.GONE
            }
        }


    }
    private fun setupStrokeBar() {
        binding.strokeBar.max = 150
        binding.strokeBar.progress = 20
        binding.drawView.setStrokeWidth(binding.strokeBar.progress)

        binding.strokeBar.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                binding.drawView.setStrokeWidth(seekBar!!.progress)

            }
        })

    }

    private fun setupUndoRedo() {
        binding.undo.isEnabled = false
        binding.redo.isEnabled = false

        binding.undo.setSafeClickListenertools { binding.drawView.undo() }
        binding.redo.setSafeClickListenertools { binding.drawView.redo() }

        binding.drawView.setButtons(binding.undo, binding.redo)
    }


    private fun selectItem(layout: LinearLayout) {
        // Reset previous selection
        selectedLayout?.let { prev ->
            when (prev.id) {
                R.id.layoutAutoRemoval ->
                {
                    binding.autoRemoval.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.autor_icon_unselect))
                    binding.textAutoRemoval.ResetGradientColor()
                }
                R.id.layoutManual -> {
                    binding.manual.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.erase_icon))
                    binding.textManual.ResetGradientColor()
                }
                R.id.layoutCrop -> {
                    binding.crop.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.crop_icon))
                    binding.textCrop.ResetGradientColor()
                }
                R.id.layoutRepair -> {
                    binding.repair.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.repair_icon))
                    binding.textRepair.ResetGradientColor()
                }
                R.id.layoutZoom -> {
                    binding.zoom.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.zoom_icon))
                    binding.textZoom.ResetGradientColor()
                }
            }
        }


        selectedLayout = layout
        selectedLayout?.let { prev ->
            when (prev.id) {
                R.id.layoutAutoRemoval ->
                {
                    binding.autoRemoval.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.auto_icon))
                    binding.textAutoRemoval.setGradientColor()
                }
                R.id.layoutManual -> {
                    binding.manual.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.manual_icon_select))
                    binding.textManual.setGradientColor()
                }
                R.id.layoutCrop -> {
                    binding.crop.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.crop_icon_select))
                    binding.textCrop.setGradientColor()
                }
                R.id.layoutRepair -> {
                    binding.repair.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.repair_icon_select))
                    binding.textRepair.setGradientColor()
                }
                R.id.layoutZoom -> {
                    binding.zoom.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.zoom_icon_select))
                    binding.textZoom.setGradientColor()
                }
            }
        }


    }


    private fun startCrop(uri: Uri) {
        cropImage.launch(
            CropImageContractOptions(uri,
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON
                )
            )
        )
    }
    var autocheck=false
    var remvoedbitmap:Bitmap?=null
    private fun autoSelfie() {
        binding.drawView.setAction(DrawView.DrawViewAction.AUTO_SELFIE)
        ProgressUtils.show(this, getString(R.string.auto_remove_is_processing))
        binding.drawView.beforeAutoSelfie()
        BackgroundRemover.process(
            binding.drawView.getBitmap()!!,
            true,
            object : OnBackgroundChangeListener {
                override fun onSuccess(bitmap: Bitmap) {
                    remvoedbitmap=bitmap
                    binding.drawView.afterAutoSelfie(bitmap)
                    autocheck=true
                    ProgressUtils.dismiss()
                }

                override fun onFailed(exception: Exception) {
                    Toast.makeText(this@EditorActivity,
                        getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
                    ProgressUtils.dismiss()

                }
            })
    }

    override fun onBackPressed() {
        showQuitDialog(
            context = this,
            title = getString(R.string.quit),
            onConfirm = {
                finish() // close the activity
            },
            onCancel = {
                Toast.makeText(this, getString(R.string.cancelled), Toast.LENGTH_SHORT).show()
            }
        )


    }


    private fun setDrawViewBitmap(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        ProgressUtils.beforebitmap=bitmap
        binding.drawView.setBitmap(bitmap)
    }
}
