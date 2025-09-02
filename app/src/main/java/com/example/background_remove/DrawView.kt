package com.example.background_remove

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.abs

class DrawView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // live path shown while the user draws
    private var livePath = Path()

    // paints that are actually applied to the working bitmap
    private val erasePaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = Color.TRANSPARENT
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 60f
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) // erase -> transparent
    }

    private val repairPaint = Paint().apply {
        isAntiAlias = true
        isDither = true
        color = Color.BLACK // color isn't used directly for repair; we restore from saved image
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 60f
        xfermode = null
    }

    // preview paint used to show the stroke while drawing (so user sees it)
    private val previewPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = erasePaint.strokeWidth
        color = Color.argb(160, 255, 255, 255) // semi-transparent white for erase preview
    }

    private var currentPaint: Paint = erasePaint
    private var background: Int = Color.TRANSPARENT

    // Working and original bitmaps
    private var savedImageBitmap: Bitmap? = null         // original (immutable copy)
    private var savedImageBitmapScaled: Bitmap? = null   // original scaled to view size
    private var workingBitmap: Bitmap? = null            // ARGB_8888 buffer we edit
    private var bufferCanvas: Canvas? = null             // canvas for workingBitmap
    private var scaleFactor = 1f
    private var minScale = 1f
    private var maxScale = 4f
    private var focusX = 0f
    private var focusY = 0f
    private var posX = 0f
    private var posY = 0f

    // Undo / redo as bitmap snapshots for simplicity
    private val undoStack: ArrayDeque<Bitmap> = ArrayDeque()
    private val redoStack: ArrayDeque<Bitmap> = ArrayDeque()
    private val maxHistory = 20

    private var pathX = 0f
    private var pathY = 0f
    private val TOUCH_TOLERANCE = 4f
    private val COLOR_TOLERANCE = 20f

    private var undoButton: ImageView? = null
    private var redoButton: ImageView? = null


    enum class DrawViewAction {
        AUTO_SELFIE, MANUAL_CLEAR, MANUAL_REPAIR, ZOOM,CROP
    }

    private var currentAction: DrawViewAction = DrawViewAction.MANUAL_CLEAR

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
        // preview color: white when erasing, black when repairing
        previewPaint.color = if (action == DrawViewAction.MANUAL_REPAIR)
            Color.argb(180, 0, 0, 0) else Color.argb(160, 255, 255, 255)
        invalidate()
    }

    fun getBitmap(): Bitmap? {
        workingBitmap ?: return null

        // Create final output the same size as the View (not just working bitmap)
        val finalBitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(finalBitmap)

        // Background color
        canvas.drawColor(background)

        // Apply current transformations (pan + zoom)
        canvas.save()
        canvas.translate(posX, posY)
        canvas.scale(scaleFactor, scaleFactor, focusX, focusY)

        // Draw the working bitmap
        workingBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }




        canvas.restore()

        return finalBitmap
    }
    // --- Bitmap setup/resizing ---
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // If we already have an original saved bitmap, scale it into workingBitmap now
        savedImageBitmap?.let { original ->
            savedImageBitmapScaled = Bitmap.createScaledBitmap(original, w, h, true)
            savedImageBitmapScaled = savedImageBitmapScaled?.copy(Bitmap.Config.ARGB_8888, true)
            workingBitmap = savedImageBitmapScaled?.copy(Bitmap.Config.ARGB_8888, true)
            bufferCanvas = workingBitmap?.let { Canvas(it) }
            clearHistory() // new image resets history
            invalidate()
        }
    }
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val prevScale = scaleFactor
            scaleFactor = (scaleFactor * detector.scaleFactor).coerceIn(minScale, maxScale)

            // focus point keeps zoom centered around fingers
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

    /**
     * Provide the source bitmap (usually the loaded image). We'll keep a copy and scale it when view size is known.
     */
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

        // keep original (not necessarily view-sized)
        savedImageBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        // If view already measured, scale & prepare working bitmap now; otherwise wait for onSizeChanged.
        if (width > 0 && height > 0) {
            savedImageBitmapScaled = Bitmap.createScaledBitmap(savedImageBitmap!!, width, height, true)
            savedImageBitmapScaled = savedImageBitmapScaled?.copy(Bitmap.Config.ARGB_8888, true)
            workingBitmap = savedImageBitmapScaled?.copy(Bitmap.Config.ARGB_8888, true)
            bufferCanvas = workingBitmap?.let { Canvas(it) }
            clearHistory()
        }
        invalidate()
    }

    private fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
        updateButtons()
    }

    // --- Drawing / commit logic ---
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Needed for transparency
        setBackgroundColor(background)   // View itself transparent
        canvas.save()
        canvas.translate(posX, posY)
        canvas.scale(scaleFactor, scaleFactor, focusX, focusY)

        workingBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        if (!livePath.isEmpty) {
            canvas.drawPath(livePath, previewPaint)
        }

        canvas.restore()
    }


    private fun pushHistorySnapshot() {
        workingBitmap?.let {
            // keep snapshot copy
            val snap = it.copy(Bitmap.Config.ARGB_8888, true)
            undoStack.addLast(snap)
            if (undoStack.size > maxHistory) undoStack.removeFirst()
            // new action clears redo history
            redoStack.clear()
            updateButtons()
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            workingBitmap?.let {
                redoStack.addLast(it.copy(Bitmap.Config.ARGB_8888, true))
            }
            val prev = undoStack.removeLast()
            workingBitmap = prev.copy(Bitmap.Config.ARGB_8888, true)
            bufferCanvas = Canvas(workingBitmap!!)
            updateButtons()
            invalidate()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            workingBitmap?.let {
                undoStack.addLast(it.copy(Bitmap.Config.ARGB_8888, true))
            }
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

    // Touch handling: build live path, then commit to workingBitmap on up
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // ignore gestures (zoom) — caller toggles action to ZOOM when needed
        if (workingBitmap == null ) return super.onTouchEvent(event)
        if (currentAction == DrawViewAction.ZOOM) {
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            return true
        }
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                pathX = x
                pathY = y
                livePath.reset()
                livePath.moveTo(x, y)
                // before changing the bitmap, store snapshot for undo
                pushHistorySnapshot()
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(x - pathX)
                val dy = abs(y - pathY)

                 if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    livePath.quadTo(pathX, pathY, (x + pathX) / 2f, (y + pathY) / 2f)
                    pathX = x
                    pathY = y
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                livePath.lineTo(pathX, pathY)
                // commit livePath to workingBitmap
                applyPathToBitmap(Path(livePath)) // pass a copy
                livePath.reset()
                updateButtons()
                invalidate()
                parent.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    fun beforeAutoSelfie() {
        // Save current state for undo before applying ML segmentation
        pushHistorySnapshot()

    }
    fun resetZoom() {
        scaleFactor = 1f
        posX = 0f
        posY = 0f
        invalidate()
    }
    fun afterAutoSelfie(segmented: Bitmap) {
        // Replace working bitmap with new segmented result
        workingBitmap = segmented.copy(Bitmap.Config.ARGB_8888, true)
        bufferCanvas = Canvas(workingBitmap!!)


        updateButtons()
        invalidate()
    }

    /**
     * Commit the provided path to the working bitmap, using the current action:
     * - MANUAL_CLEAR: draw path with erasePaint (CLEAR xfermode)
     * - MANUAL_REPAIR: restore pixels from savedImageBitmapScaled under the path region
     */
    private fun applyPathToBitmap(path: Path) {
        if (workingBitmap == null) return
        bufferCanvas ?: run { bufferCanvas = Canvas(workingBitmap!!) }

        when (currentAction) {
            DrawViewAction.MANUAL_CLEAR -> {
                // Erase: draw path directly with CLEAR xfermode -> transparent pixels
                bufferCanvas!!.drawPath(path, erasePaint)
            }

            DrawViewAction.MANUAL_REPAIR -> {
                val src = savedImageBitmapScaled ?: return
                val w = workingBitmap!!.width
                val h = workingBitmap!!.height

                // Create mask bitmap with stroke (NOT fill) so stroke width is respected
                val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val mCanvas = Canvas(mask)
                val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeCap = Paint.Cap.ROUND
                    strokeWidth = repairPaint.strokeWidth
                    color = Color.WHITE
                }
                mCanvas.drawPath(path, mPaint)

                // Extract masked portion from original
                val restored = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val rCanvas = Canvas(restored)
                rCanvas.drawBitmap(src, 0f, 0f, null)
                val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                }
                rCanvas.drawBitmap(mask, 0f, 0f, maskPaint)

                // Apply restored stroke to working bitmap
                bufferCanvas!!.drawBitmap(restored, 0f, 0f, null)

                mask.recycle()
                restored.recycle()
            }

            else -> {
                // other actions not applied here
            }
        }
    }

    fun setColor(transparent: Int) {
        background=transparent
        invalidate()

    }


}
