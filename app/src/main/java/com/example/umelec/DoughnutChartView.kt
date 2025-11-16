package com.example.umelec

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class DoughnutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val votedColor = Color.parseColor("#31D0AA")
    private val notVotedColor = Color.parseColor("#515167")

    // Paint object for drawing the arcs
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        // ⭐️ Change 1: Increased thickness (e.g., from 50f to 70f)
        strokeWidth = 70f
        // ⭐️ Change 2: Confirmed non-rounded ends
        strokeCap = Paint.Cap.BUTT
    }

    private val rectF = RectF()

    var votedPercentage: Int = 55
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // Ensure the view is square to draw a perfect circle/doughnut
        val size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val stroke = paint.strokeWidth
        val size = min(width, height).toFloat()
        val centerX = width / 2f
        val centerY = height / 2f

        // Ensure the radius accounts for the stroke so the ring fits inside the view bounds
        val radius = size / 2f - stroke / 2f

        // Set the bounds for the circle (arc)
        rectF.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // 1. Calculate Arc Angles
        val votedSweepAngle = 360f * (votedPercentage / 100f)
        val notVotedSweepAngle = 360f - votedSweepAngle

        // 2. Draw Not Voted (Background) Arc
        paint.color = notVotedColor
        canvas.drawArc(rectF, 270f + votedSweepAngle, notVotedSweepAngle, false, paint)

        // 3. Draw Voted (Foreground) Arc
        paint.color = votedColor
        canvas.drawArc(rectF, 270f, votedSweepAngle, false, paint)
    }
}