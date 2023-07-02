package ru.netology.statsview.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.withStyledAttributes
import ru.netology.statsview.R
import ru.netology.statsview.utils.AndroidUtils
import kotlin.math.min
import kotlin.random.Random

const val TYPE_ROTATION = 0
const val TYPE_SEQUENTIAL = 1
const val TYPE_BIDIRECTIONAL = 2

class StatsView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : View(context, attributeSet, defStyleAttr, defStyleRes) {
    private var radius = 0F
    private var center = PointF()
    private var oval = RectF()

    private var lineWidth = AndroidUtils.dp(context, 5F).toFloat()
    private var textSize = AndroidUtils.dp(context, 20F).toFloat()
    private var colors = emptyList<Int>()

    private var startFrom = -90F
    private var animationType = 0
    private var progress = 0F
    private var valueAnimator: ValueAnimator? = null

    init {
        context.withStyledAttributes(attributeSet, R.styleable.StatsView) {
            textSize = getDimension(R.styleable.StatsView_textSize, textSize)
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth)

            colors = listOf(
                getColor(R.styleable.StatsView_color1, generateRandomColor()),
                getColor(R.styleable.StatsView_color2, generateRandomColor()),
                getColor(R.styleable.StatsView_color3, generateRandomColor()),
                getColor(R.styleable.StatsView_color4, generateRandomColor()),
            )
            animationType = getInteger(R.styleable.StatsView_animationType, 1)
        }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = lineWidth
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    @SuppressLint("ResourceAsColor")
    private val paintEmpty = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        strokeWidth = lineWidth
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = R.color.empty_color
        alpha = 10
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = this@StatsView.textSize
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    var data: List<Float> = emptyList()
        set(value) {
            field = value
            update()
        }

    private fun getAngle(datum: Float) = (datum / data.sum() * 360F)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = min(w, h) / 2F - lineWidth
        center = PointF(w / 2F, h / 2F)
        oval = RectF(
            center.x - radius, center.y - radius,
            center.x + radius, center.y + radius
        )
    }

    override fun onDraw(canvas: Canvas) {

        if (data.isEmpty()) {
            return
        }
        canvas.drawArc(oval, startFrom, 360F, false, paintEmpty)

        when (animationType) {
            TYPE_ROTATION -> {
                rotate(canvas)
            }

            TYPE_SEQUENTIAL -> {
                sequent(canvas)
            }

            TYPE_BIDIRECTIONAL -> {
                bidirectional(canvas)
            }
        }

        canvas.drawText(
            "%.2f%%".format(progress * 100),
            center.x,
            center.y + textPaint.textSize / 4,
            textPaint
        )
    }

    private fun update() {
        valueAnimator?.let {
            it.removeAllListeners()
            it.cancel()
        }
        progress = 0F

        valueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
            addUpdateListener { anim ->
                progress = anim.animatedValue as Float
                invalidate()
            }
            duration = 3000
            interpolator = LinearInterpolator()
        }.also {
            it.start()
        }
    }

    private fun rotate(canvas: Canvas) {
        var startAngle = -90F + progress * 360
        var firstArcColor = 0
        var firstAngle = 0F
        data.forEachIndexed { index, datum ->
            val angle = getAngle(datum)
            paint.color = colors.getOrElse(index) { generateRandomColor() }
            if (index == 0) {
                firstArcColor = paint.color
                firstAngle = angle
            }
            canvas.drawArc(oval, startAngle, angle * progress, false, paint)
            startAngle += angle
        }

        paint.color = firstArcColor
        canvas.drawArc(oval, startAngle, firstAngle / 100, false, paint)
    }


    private fun sequent(canvas: Canvas) {
        var startFrom = -90F
        val maxAngle = 360 * progress + startFrom

        for ((index, datum) in data.withIndex()) {

            if (startFrom > maxAngle) return

            val angle = getAngle(datum)
            val sweepAngle = min(angle, maxAngle - startFrom)

            paint.color = colors.getOrNull(index) ?: generateRandomColor()
            canvas.drawArc(oval, startFrom, sweepAngle, false, paint)
            startFrom += angle
        }
    }


    private fun bidirectional(canvas: Canvas) {
        data.forEachIndexed { index, datum ->
            val angle = getAngle(datum)
            paint.color = colors.getOrElse(index) { generateRandomColor() }
            canvas.drawArc(
                oval,
                startFrom + (angle / 2) - (angle * progress / 2),
                angle * progress,
                false,
                paint
            )
            startFrom += angle
        }
    }

    private fun generateRandomColor() = Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
}

