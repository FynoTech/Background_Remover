package com.example.background_remove

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

import com.example.autobackgroundremover.OnBackgroundChangeListener
import com.example.background_remove.databinding.ActivityEditorBinding
import com.example.background_remove.databinding.ActivityMainBinding
import com.example.background_remove.utils.setSafeClickListenertools
import com.example.background_remove.utils.setupStatusBar


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
        setupBottomNavigation()




        binding.done.setSafeClickListenertools {
            binding.done.setImageBitmap(binding.drawView.getBitmap())
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
        binding.drawView.setLoadingModal(binding.loadingView)
    }

    private fun setupUndoRedo() {
        binding.undo.isEnabled = false
        binding.redo.isEnabled = false

        binding.undo.setSafeClickListenertools { binding.drawView.undo() }
        binding.redo.setSafeClickListenertools { binding.drawView.redo() }

        binding.drawView.setButtons(binding.undo, binding.redo)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavView.selectedItemId = R.id.manual_clear_button
        binding.bottomNavView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.auto_selfie -> {
                    autoSelfie()
                    true
                }
                R.id.manual_clear_button -> {
                    binding.drawView.setAction(DrawView.DrawViewAction.MANUAL_CLEAR)
                    binding.manualClearSettingsLayout.visibility = android.view.View.VISIBLE
                    true
                }
                R.id.manual_repair_button -> {
                    binding.drawView.setAction(DrawView.DrawViewAction.MANUAL_REPAIR)
                    binding.manualClearSettingsLayout.visibility = android.view.View.VISIBLE
                    true
                }
                R.id.crop_button -> {
                    imageUri?.let { uri -> startCrop(uri) }
                    true
                }
                R.id.zoom_button -> {
                    binding.drawView.setAction(DrawView.DrawViewAction.ZOOM)
                    binding.manualClearSettingsLayout.visibility = android.view.View.INVISIBLE
                    true
                }
                else -> false
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
    private fun autoSelfie() {
        binding.drawView.beforeAutoSelfie()
        BackgroundRemover.process(
            binding.drawView.getBitmap()!!,
            true,
            object : OnBackgroundChangeListener {
                override fun onSuccess(bitmap: Bitmap) {
                    binding.drawView.afterAutoSelfie(bitmap)
                }

                override fun onFailed(exception: Exception) {
                    Toast.makeText(this@EditorActivity, "Error Occurred", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onBackPressed() {
       finish()

    }


    private fun setDrawViewBitmap(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        binding.drawView.setBitmap(bitmap)
    }
}
