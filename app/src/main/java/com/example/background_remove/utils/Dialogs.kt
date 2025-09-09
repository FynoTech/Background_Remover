package com.example.background_remove.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.photo.editor.background.eraser.bgeraser.R


object Dialogs {

    val colors = listOf<Int?>(
        null, // First item is null
        Color.RED,
        Color.GREEN,
        Color.BLUE,
        Color.YELLOW,
        Color.MAGENTA,
        Color.CYAN,
        Color.LTGRAY,
        Color.DKGRAY,
        Color.BLACK,
        Color.parseColor("#FFA500"), // Orange
        Color.parseColor("#800080"), // Purple
        Color.parseColor("#00FF7F"), // Spring Green
        Color.parseColor("#FF1493"), // Deep Pink
        Color.parseColor("#1E90FF"), // Dodger Blue
        Color.parseColor("#FFD700")  // Gold
    )
    fun showQuitDialog(
        context: Context,
        title:String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_exit) // your XML above
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)




        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val texttitle = dialog.findViewById<TextView>(R.id.tvTitle)
        texttitle.text = title
        val btnOk = dialog.findViewById<Button>(R.id.btnOk)

        // Clicks
        btnCancel.setSafeClickListener(context) {
            dialog.dismiss()
            onCancel?.invoke()
        }

        btnOk.setSafeClickListener(context) {
            dialog.dismiss()
            onConfirm()
        }
        val width = (context.resources.displayMetrics.widthPixels * 0.80).toInt()
        dialog!!.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(true)
        dialog.show()
    }
    fun showRateUsDialog(context: Context) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.rate_us_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val ratingBar = dialog.findViewById<RatingBar>(R.id.ratingBar)
        val btnRate = dialog.findViewById<TextView>(R.id.btnRate)
        val imagerate = dialog.findViewById<ImageView>(R.id.imgEmoji)
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            when (rating.toInt()) {
                1 -> imagerate.setImageResource(R.drawable.emoji1) // angry/sad
                2 -> imagerate.setImageResource(R.drawable.emoji2) // meh
                3 -> imagerate.setImageResource(R.drawable.emoji3) // neutral
                4 -> imagerate.setImageResource(R.drawable.emoji4) // happy
                5 -> imagerate.setImageResource(R.drawable.emoji5) // super happy
            }
        }
        btnRate.setSafeClickListener (context){
            val rating = ratingBar.rating

            // If user gives good rating, go to Play Store
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + context.packageName)
                )
                intent.setPackage("com.android.vending") // ensure Play Store opens
                context.startActivity(intent)
            } catch (e: Exception) {
                // fallback if Play Store is not available
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + context.packageName)
                )
                context.startActivity(intent)
            }
            dialog.dismiss()
        }



        dialog.show()
    }
    private var dialogloading: Dialog? = null
    fun dismissProgressDialogloading() {
        try {
            dialogloading?.dismiss()
            dialogloading = null
        } catch (e: Exception) {

        }

    }

    fun showProgressDialogloading(context: Context): Boolean {
        // Inflate the custom layout
        if (context is Activity) {
            Log.d("ProgressDialog", "Dialog called from: ${context::class.java.simpleName}")
        } else {
            Log.d("ProgressDialog", "Context is not an Activity!")
            return false // Prevent showing on non-Activity contexts
        }

        // Create AlertDialog
        if (dialogloading == null) {
            dialogloading = Dialog(context, android.R.style.Theme_Light)
            dialogloading?.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialogloading?.setCancelable(false)

            dialogloading?.setContentView(R.layout.progress_dialog)

        }

        try {
            if (!(context as Activity).isFinishing && !(context as Activity).isDestroyed) {
                dialogloading?.show()
                return true
            }else{
                return false
            }

        } catch (e: Exception) {
            Log.d("abcd",e.message.toString())
            return false


        }


    }



}