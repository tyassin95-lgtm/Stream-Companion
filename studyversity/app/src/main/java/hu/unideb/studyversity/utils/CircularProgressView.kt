package hu.unideb.studyversity.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import hu.unideb.studyversity.R

/** Ring-style progress indicator used on the mood game screens. */
class CircularProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()
    private val startAngle = -90f
    private val maxAngle = 360f
    private var maxProgress = 100
    private var angle = 0f
    private var diameter = 0f
    var initialProgress = 0f
        private set

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.CircleProgress)
            initialProgress = a.getFloat(R.styleable.CircleProgress_progress, 0f)
            a.recycle()
            setProgress(initialProgress)
        }
    }

    private fun updateRect() {
        val s = backgroundPaint.strokeWidth
        rect.set(s, s, diameter - s, diameter - s)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawArc(rect, startAngle, maxAngle, false, backgroundPaint)
        canvas.drawArc(rect, startAngle, angle, false, progressPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        diameter = minOf(w, h).toFloat()
        updateRect()
    }

    fun setMaxProgress(max: Int) { maxProgress = max }
    fun setProgress(progress: Float) { angle = maxAngle / maxProgress * progress; invalidate() }
    fun setProgressBackgroundColor(color: Int) { backgroundPaint.color = color; invalidate() }
    fun setProgressColor(color: Int) { progressPaint.color = color; invalidate() }
    fun setProgressWidth(width: Float) {
        progressPaint.strokeWidth = width; backgroundPaint.strokeWidth = width; updateRect(); invalidate()
    }
    fun setRounded(rounded: Boolean) {
        progressPaint.strokeCap = if (rounded) Paint.Cap.ROUND else Paint.Cap.BUTT; invalidate()
    }
}
