package com.example.background_remove

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.example.background_remove.databinding.ActivitySettingsBinding
import com.example.background_remove.utils.Dialogs
import com.example.background_remove.utils.setSafeClickListener
import com.example.background_remove.utils.setupStatusBar


class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        enableEdgeToEdge()
       
        setupStatusBar(window, Color.BLACK)
        setContentView(binding.root)

        binding.back.setSafeClickListener(this) {
            finish()
        }

        binding.changelanguage.setSafeClickListener(this) {
            val intent = Intent(this, LanguageActivity::class.java)
            startActivity(intent)
        }
        val versionName = BuildConfig.VERSION_NAME
        binding.textverison.text = "${getString(R.string.version)} $versionName"

        binding.share.setSafeClickListener(this) {
            try {
                val myIntent = Intent(Intent.ACTION_SEND)
                myIntent.type = "text/plain"

                val shareBody = """
            Let me recommend you this application
            
            PDF Reader
            
            https://play.google.com/store/apps/details?id=$packageName
        """.trimIndent()

                val shareSub = "PDF Reader App"

                myIntent.putExtra(Intent.EXTRA_SUBJECT, shareSub)
                myIntent.putExtra(Intent.EXTRA_TEXT, shareBody)

                startActivity(Intent.createChooser(myIntent, "Share using"))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.unable_to_share), Toast.LENGTH_SHORT).show()
            }
        }
        binding.privacy.setSafeClickListener(this) {
            try {
                val url = "https://zafiradesigns.com/privacy-policy-ai-pdf-reader/"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {

            }
        }

        binding.howtouse.setSafeClickListener(this){
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }

        binding.rateus.setSafeClickListener(this) {

            Dialogs.showRateUsDialog(this)
        }
    }

    override fun onBackPressed() {
        finish()
    }
}