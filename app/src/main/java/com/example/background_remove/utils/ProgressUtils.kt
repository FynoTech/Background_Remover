package com.example.background_remove.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.example.background_remove.R


object ProgressUtils {

    private var dialog: Dialog? = null
/*
    fun show(context: Context, message: String) {
        if (dialog != null && dialog?.isShowing == true) return

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null)

        // Update message
        val tvMessage = view.findViewById<TextView>(R.id.tvSuccess)
        tvMessage.text = message

        // Lottie animation
        val animationView = view.findViewById<LottieAnimationView>(R.id.animationView)
        animationView.playAnimation()

        dialog = Dialog(context).apply {
            setContentView(view)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // ✅ no black corners
            window?.setDimAmount(0.5f) // ✅ dim background behind dialog

        }
        val width = (context.resources.displayMetrics.widthPixels * 0.75).toInt()
        dialog!!.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.show()
    }*/

    fun dismiss() {
        dialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        dialog = null
    }
}
