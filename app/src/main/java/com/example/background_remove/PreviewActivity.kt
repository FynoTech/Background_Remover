package com.example.background_remove

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import com.example.background_remove.databinding.ActivityPreviewBinding
import com.example.background_remove.utils.setSafeClickListener
import com.example.background_remove.utils.setupStatusBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PreviewActivity : BaseActivity() {

    private lateinit var binding: ActivityPreviewBinding
    private var filePath: String? = null
    private var isImage: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPreviewBinding.inflate(layoutInflater)
        enableEdgeToEdge()
       
        setupStatusBar(window, Color.BLACK)
        setContentView(binding.root)

     /*  var party= Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
            position = Position.Relative(0.5, 0.3),
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
        )
        binding.konfettiView.start(party)*/
        // Receive data
        filePath = intent.getStringExtra("pdf_path")
        val fileName = intent.getStringExtra("file_name")
        isImage = intent.getBooleanExtra("isImage", false)
        binding.tvFilePath.text = filePath
        binding.tvFileName.text = fileName ?: "File"

        try {// ✅ Preview
            filePath?.let {
                val file = File(it)
                if (file.exists()) {
                    if (isImage) {
                        binding.imgPreview.setImageURI(Uri.fromFile(file))
                    } else {
                        binding.imgPreview.setImageURI(Uri.parse(intent.getStringExtra("image_path")))
                    }
                }
            }
        } catch (e: Exception) {

        }

        // ✅ Close button
        binding.close.setSafeClickListener(this) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }

        // ✅ Open button
        binding.btnOpen.setSafeClickListener(this) {
            filePath?.let {
                val file = File(it)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)

                    // ✅ Detect correct MIME type
                    val type = when {
                        file.name.endsWith(".jpg", true) || file.name.endsWith(
                            ".jpeg",
                            true
                        ) -> "image/jpeg"

                        file.name.endsWith(".png", true) -> "image/png"
                        else -> "application/pdf"
                    }

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, type)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    startActivity(Intent.createChooser(intent, "Open with"))
                }
            }
        }


        // ✅ Share button
        binding.btnShare.setSafeClickListener(this) {
            filePath?.let {
                val file = File(it)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)

                    // ✅ Detect correct MIME type
                    val type = when {
                        file.name.endsWith(".jpg", true) || file.name.endsWith(
                            ".jpeg",
                            true
                        ) -> "image/jpeg"

                        file.name.endsWith(".png", true) -> "image/png"
                        else -> "application/pdf"
                    }

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        this.type = type
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share File"))
                }
            }
        }
    }
    fun isPdfFile(file: File): Boolean {
        return file.extension.equals("pdf", ignoreCase = true)
    }
    fun pdfToBitmaps(
        context: Context,
        pdfFile: File,
        onResult: (Boolean, List<Bitmap>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val bitmaps = mutableListOf<Bitmap>()
            var success = false

            try {
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                    val renderer = PdfRenderer(pfd)

                    for (i in 0 until 1) {
                        val page = renderer.openPage(i)

                        val bitmap = Bitmap.createBitmap(
                            page.width,
                            page.height,
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bitmap)

                        page.close()
                    }
                    renderer.close()
                    success = bitmaps.isNotEmpty()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            withContext(Dispatchers.Main) {
                onResult(success, bitmaps)
            }
        }
    }
        override fun onBackPressed() {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
}
