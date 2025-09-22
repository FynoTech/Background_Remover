package com.example.background_remove

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import com.example.background_remove.adsManager.Ads
import kotlin.math.abs
import java.util.*

class DrawView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // --- Paints ---
    private val erasePaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = Color.TRANSPARENT
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 60f
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val repairPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 60f
        xfermode = null
    }
    var onsizechange=false
    private val previewPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = erasePaint.strokeWidth
        color = Color.argb(160, 255, 255, 255)
    }

    private var currentPaint: Paint = erasePaint
    private var background: Int = Color.TRANSPARENT

    // --- Bitmaps ---
    private var savedImageBitmap: Bitmap? = null
    private var savedImageBitmapScaled: Bitmap? = null
    private var workingBitmap: Bitmap? = null
    private var bufferCanvas: Canvas? = null

    // --- Pan & Zoom ---
    private var scaleFactor = 1f
    private var minScale = 1f
    private var maxScale = 4f
    private var focusX = 0f
    private var focusY = 0f
    private var posX = 0f
    private var posY = 0f

    // Offset to center bitmap
    private var bitmapOffsetX = 0f
    private var bitmapOffsetY = 0f

    // --- Undo / Redo ---
    private val undoStack: ArrayDeque<Bitmap> = ArrayDeque()
    private val redoStack: ArrayDeque<Bitmap> = ArrayDeque()
    private val maxHistory = 20

    // --- Drawing ---
    private var livePath = Path()
    private var pathX = 0f
    private var pathY = 0f
    private val TOUCH_TOLERANCE = 4f

    // --- UI buttons ---
    private var undoButton: ImageView? = null
    private var redoButton: ImageView? = null

    // --- Actions ---
    enum class DrawViewAction { AUTO_SELFIE, MANUAL_CLEAR, MANUAL_REPAIR, ZOOM, CROP }
    private var currentAction: DrawViewAction = DrawViewAction.MANUAL_CLEAR

    // --- Gesture detectors ---
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val prevScale = scaleFactor
            scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(minScale, maxScale)

            val scaleChange = scaleFactor / prevScale
            posX = (posX - detector.focusX) * scaleChange + detector.focusX
            posY = (posY - detector.focusY) * scaleChange + detector.focusY

            invalidate()
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (scaleFactor > 1f) {
                posX -= distanceX
                posY -= distanceY
                invalidate()
                return true
            }
            return false
        }
    })

    // --- Public API ---
    fun setButtons(undoBtn: ImageView?, redoBtn: ImageView?) {
        undoButton = undoBtn
        redoButton = redoBtn
        resetZoom()
        updateButtons()
    }

    fun setStrokeWidth(px: Int) {
        erasePaint.strokeWidth = px.toFloat()
        repairPaint.strokeWidth = px.toFloat()
        previewPaint.strokeWidth = px.toFloat()
    }

    fun setAction(action: DrawViewAction) {
        currentAction = action
        currentPaint = if (action == DrawViewAction.MANUAL_REPAIR) repairPaint else erasePaint
        previewPaint.color = if (action == DrawViewAction.MANUAL_REPAIR)
            Color.argb(180, 0, 0, 0) else Color.argb(160, 255, 255, 255)
        invalidate()
    }

    fun setBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            savedImageBitmap = null
            savedImageBitmapScaled = null
            workingBitmap = null
            bufferCanvas = null
            clearHistory()
            invalidate()
            return
        }

        savedImageBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (width > 0 && height > 0) {
            savedImageBitmapScaled = scaleBitmapToFitView(savedImageBitmap!!, width, height)
            workingBitmap = savedImageBitmapScaled?.copy(Bitmap.Config.ARGB_8888, true)
            bufferCanvas = Canvas(workingBitmap!!)
            clearHistory()
        }
        invalidate()
    }

    fun setColor(transparent: Int) {
        background = transparent
        invalidate()
    }

    fun getBitmap(): Bitmap? {
        workingBitmap ?: return null
        val finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        canvas.drawColor(background)
        canvas.save()
        canvas.translate(posX + bitmapOffsetX, posY + bitmapOffsetY)
        canvas.scale(scaleFactor, scaleFactor, focusX, focusY)
        workingBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        canvas.restore()
        return finalBitmap
    }

    fun resetZoom() {
        scaleFactor = 1f
        posX = 0f
        posY = 0f
        invalidate()
    }

    fun beforeAutoSelfie() { pushHistorySnapshot() }

    fun afterAutoSelfie(segmented: Bitmap) {
        savedImageBitmapScaled = scaleBitmapToFitView(segmented, width, height)
        workingBitmap = savedImageBitmapScaled?.copy(Bitmap.Config.ARGB_8888, true)
        bufferCanvas = Canvas(workingBitmap!!)
        updateButtons()
        invalidate()
    }

    // --- Override ---
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (onsizechange) {
            bitmapOffsetX = ((w - (workingBitmap?.width ?: 0)) / 2f).coerceAtLeast(0f)
            bitmapOffsetY = ((h - (workingBitmap?.height ?: 0)) / 2f).coerceAtLeast(0f)
            Log.d("abcd", "onSizeChanged ignored")
            return
        }
        Log.d("abcd", "onSizeChanged: ")
        savedImageBitmap?.let {
            savedImageBitmapScaled = scaleBitmapToFitView(it, w, h)
            workingBitmap = savedImageBitmapScaled?.copy(Bitmap.Config.ARGB_8888, true)
            bufferCanvas = Canvas(workingBitmap!!)
            clearHistory()
            onsizechange=true
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setBackgroundColor(background)

        canvas.save()
        canvas.translate(posX, posY)
        canvas.scale(scaleFactor, scaleFactor, focusX, focusY)
        workingBitmap?.let { canvas.drawBitmap(it, bitmapOffsetX, bitmapOffsetY, null) }
        if (!livePath.isEmpty) canvas.drawPath(livePath, previewPaint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (workingBitmap == null) return super.onTouchEvent(event)

        val x = event.x
        val y = event.y

        when (currentAction) {
            DrawViewAction.ZOOM -> {
                scaleDetector.onTouchEvent(event)
                gestureDetector.onTouchEvent(event)
                return true
            }

            DrawViewAction.MANUAL_CLEAR, DrawViewAction.MANUAL_REPAIR -> {
                val bitmapX = (x - posX - bitmapOffsetX) / scaleFactor
                val bitmapY = (y - posY - bitmapOffsetY) / scaleFactor

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        parent.requestDisallowInterceptTouchEvent(true)
                        pathX = bitmapX
                        pathY = bitmapY
                        livePath.reset()
                        livePath.moveTo(bitmapX, bitmapY)
                        pushHistorySnapshot()
                        invalidate()
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = abs(bitmapX - pathX)
                        val dy = abs(bitmapY - pathY)
                        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                            livePath.quadTo(pathX, pathY, (bitmapX + pathX) / 2f, (bitmapY + pathY) / 2f)
                            pathX = bitmapX
                            pathY = bitmapY
                        }
                        invalidate()
                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        livePath.lineTo(pathX, pathY)
                        applyPathToBitmap(Path(livePath))
                        livePath.reset()
                        updateButtons()
                        invalidate()
                        parent.requestDisallowInterceptTouchEvent(false)
                        return true
                    }
                }
            }

            else -> return super.onTouchEvent(event)
        }
        return true
    }

    // --- Undo/Redo ---
    private fun pushHistorySnapshot() {
        workingBitmap?.let {
            val snap = it.copy(Bitmap.Config.ARGB_8888, true)
            undoStack.addLast(snap)
            if (undoStack.size > maxHistory) undoStack.removeFirst()
            redoStack.clear()
            updateButtons()
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            workingBitmap?.let { redoStack.addLast(it.copy(Bitmap.Config.ARGB_8888, true)) }
            val prev = undoStack.removeLast()
            workingBitmap = prev.copy(Bitmap.Config.ARGB_8888, true)
            bufferCanvas = Canvas(workingBitmap!!)
            updateButtons()
            invalidate()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            workingBitmap?.let { undoStack.addLast(it.copy(Bitmap.Config.ARGB_8888, true)) }
            val nxt = redoStack.removeLast()
            workingBitmap = nxt.copy(Bitmap.Config.ARGB_8888, true)
            bufferCanvas = Canvas(workingBitmap!!)
            updateButtons()
            invalidate()
        }
    }

    private fun updateButtons() {
        undoButton?.isEnabled = undoStack.isNotEmpty()
        redoButton?.isEnabled = redoStack.isNotEmpty()
    }

    private fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        updateButtons()
    }

    // --- Bitmap helpers ---
    private fun scaleBitmapToFitView(bitmap: Bitmap, viewWidth: Int, viewHeight: Int): Bitmap {
        val srcWidth = bitmap.width
        val srcHeight = bitmap.height
        val scale = minOf(viewWidth.toFloat() / srcWidth, viewHeight.toFloat() / srcHeight)
        val scaledWidth = (srcWidth * scale).toInt()
        val scaledHeight = (srcHeight * scale).toInt()
        bitmapOffsetX = (viewWidth - scaledWidth) / 2f
        bitmapOffsetY = (viewHeight - scaledHeight) / 2f
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }

    private fun applyPathToBitmap(path: Path) {
        if (workingBitmap == null) return
        bufferCanvas ?: run { bufferCanvas = Canvas(workingBitmap!!) }

        val translatedPath = Path(path)
        val matrix = Matrix()
        matrix.setTranslate(-bitmapOffsetX, -bitmapOffsetY)
        translatedPath.transform(matrix)

        when (currentAction) {
            DrawViewAction.MANUAL_CLEAR -> bufferCanvas!!.drawPath(translatedPath, erasePaint)

            DrawViewAction.MANUAL_REPAIR -> {
                val src = savedImageBitmapScaled ?: return
                val w = workingBitmap!!.width
                val h = workingBitmap!!.height

                val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val mCanvas = Canvas(mask)
                val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    strokeWidth = repairPaint.strokeWidth
                    color = Color.WHITE
                }
                mCanvas.drawPath(translatedPath, mPaint)

                val restored = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val rCanvas = Canvas(restored)
                rCanvas.drawBitmap(src, 0f, 0f, null)
                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }
                rCanvas.drawBitmap(mask, 0f, 0f, maskPaint)
                bufferCanvas!!.drawBitmap(restored, 0f, 0f, null)

                mask.recycle()
                restored.recycle()
            }

            else -> {}
        }
    }
}
