package com.example.background_remove.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import com.example.background_remove.adsManager.Ads

import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

fun Activity.setupStatusBar(window: Window, black: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // Android 15+
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setBackgroundColor(Color.WHITE)

            // Adjust padding to avoid overlap
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // remove LIGHT_STATUS_BAR so icons/text stay white
        window.decorView.systemUiVisibility = 0
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    }
}


fun Activity.hideKeyboard() {
    val imm = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    //Find the currently focused view, so we can grab the correct window token from it.
    var view = this.currentFocus
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
        view = View(this)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}



fun Context.backgroundCrop(drawableID: Drawable, reqWidth: Int, reqHeight: Int): Bitmap? {
    val originalBitmap = drawableID.toBitmap(reqWidth, reqHeight)
    val centerX = originalBitmap.width / 2
    val centerY = originalBitmap.height / 2

    val scaledWidth = centerX - (reqWidth / 2)
    val scaledHeight = centerY - (reqHeight / 2)
    return Bitmap.createBitmap(originalBitmap, scaledWidth, scaledHeight, reqWidth, reqHeight)
}




var lastGlobalClickTime = 0L // Global variable to track last click time for all buttons
fun TextView.setGradientColor() {
    val paint = this.paint
    val width = paint.measureText(this.text.toString())
    val textShader: Shader = LinearGradient(
        0f, 0f, width, this.textSize,
        intArrayOf(
            Color.parseColor("#FFFF512F"),   // start color
            Color.parseColor("#FFDD2476")// end color
        ),
        null,
        Shader.TileMode.CLAMP
    )
    this.paint.shader = textShader
    this.invalidate() // refresh the TextView
}
fun TextView.ResetGradientColor() {
    val paint = this.paint
    val width = paint.measureText(this.text.toString())
    val textShader: Shader = LinearGradient(
        0f, 0f, width, this.textSize,
        intArrayOf(
            Color.parseColor("#595656"),   // start color
            Color.parseColor("#595656")// end color
        ),
        null,
        Shader.TileMode.CLAMP
    )
    this.paint.shader = textShader
    this.invalidate() // refresh the TextView
}
fun ImageView.setGradientDrawableColors() {
    val width = drawable.intrinsicWidth
    val height = drawable.intrinsicHeight

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw gradient
    val paint = Paint()
    paint.shader = LinearGradient(
        0f, 0f, width.toFloat(), height.toFloat(),
        Color.parseColor("#FFFF512F"), Color.parseColor("#FFDD2476"),
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    // Draw original drawable as mask
    drawable.mutate().let { original ->
        original.setBounds(0, 0, width, height)
        original.draw(canvas)
    }

    this.setImageDrawable(BitmapDrawable(resources, bitmap))
}
fun ImageView.ResetGradientDrawableColors() {
    val width = drawable.intrinsicWidth
    val height = drawable.intrinsicHeight

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw gradient
    val paint = Paint()
    paint.shader = LinearGradient(
        0f, 0f, width.toFloat(), height.toFloat(),
        Color.parseColor("#595656"), Color.parseColor("#595656"),
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    // Draw original drawable as mask
    drawable.mutate().let { original ->
        original.setBounds(0, 0, width, height)
        original.draw(canvas)
    }

    this.setImageDrawable(BitmapDrawable(resources, bitmap))
}
fun View.setSafeClickListener(activity: Context,check:Boolean=false,type:String="",interval: Long = 500, onSafeClick: (View) -> Unit) {
    this.setOnClickListener { view ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGlobalClickTime > interval) {
            lastGlobalClickTime = currentTime

            // Animate alpha 1 -> 0.5 -> 1
            view.animate()
                .alpha(0.5f)
                .setDuration(100)
                .withEndAction {
                    view.animate()
                        .alpha(1f)
                        .setDuration(100)
                        .start()
                }
                .start()

            var check1=false
            if (Ads.isNetworkAvailableWithoutPremium(activity)) {
                try {
                    Ads.showInterstitialWithCounter(
                        activity = activity as Activity,check,type // or requireActivity() in Fragment
                    ) { loaded, failed, showed, dismissed ->


                            Log.e("AdsManager", "⏰ Timeout interstitial$loaded ${showed} ($failed)")
                            if (dismissed || failed) {
                                onSafeClick(view)
                                Log.e("AdsManager", "⏰ clicked")
                            }
                            check1=true

                    }
                } catch (e: Exception) {

                }
            } else {
                onSafeClick(view)
            }
        }
    }
}


fun View.setSafeClickListenertools(interval: Long = 500, onSafeClick: (View) -> Unit) {
    this.setOnClickListener { view ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastGlobalClickTime > interval) {
            lastGlobalClickTime = currentTime

            // Animate alpha 1 -> 0.5 -> 1
            view.animate()
                .alpha(0.5f)
                .setDuration(100)
                .withEndAction {
                    view.animate()
                        .alpha(1f)
                        .setDuration(100)
                        .start()
                }
                .start()


            onSafeClick(view)

        }
    }
}

@SuppressLint("ClickableViewAccessibility")
fun View.setOnSwipeTouchListener(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
) {
    val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 70
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null || e2 == null) return false

            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            return if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight?.invoke()
                    } else {
                        onSwipeLeft?.invoke()
                    }
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    })

    setOnTouchListener { _, event ->
        gestureDetector.onTouchEvent(event)
    }
}

// Safe click extension for BottomNavigationView
fun BottomNavigationView.setSafeNavigationListener(
    delayMillis: Long = 400L,
    listener: (MenuItem) -> Boolean
) {
    var isClickable = true

    this.setOnNavigationItemSelectedListener { item ->
        if (isClickable) {
            // Disable further clicks
            isClickable = false
            val result = listener(item) // Trigger the actual click listener

            // Re-enable clicks after delay
            CoroutineScope(Dispatchers.Main).launch {
                delay(delayMillis)
                isClickable = true
            }

            result
        } else {
            false
        }
    }
}



fun delay(milliseconds: Long = 500L, runnable: Runnable): Handler {
    val handler = Handler(Looper.getMainLooper())
    handler.postDelayed(runnable, milliseconds)
    return handler
}

fun Context.getDrawableFromUri( uri: Uri): Drawable? {
    return try {
        val inputStream = contentResolver.openInputStream(uri)
        val drawable = Drawable.createFromStream(inputStream, uri.toString())
        inputStream?.close()
        drawable
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun View.getBitmapDrawableFromView(): Drawable {
    // Create a bitmap with the same size as the view
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Bind a canvas to the bitmap
    val canvas = Canvas(bitmap)

    // Draw the view onto the canvas
    draw(canvas)

    return BitmapDrawable(resources, bitmap)
}



fun View.animateViewIn() {
    visibility = View.VISIBLE
    translationX = -width.toFloat()
    this.animate()
        .translationX(0f)
        .setDuration(350)
        .start()
}

fun View.animateViewOut() {
    this.animate()
        .translationX(-width.toFloat())
        .setDuration(350)
        .withEndAction {
            visibility = View.INVISIBLE
        }
        .start()
}
