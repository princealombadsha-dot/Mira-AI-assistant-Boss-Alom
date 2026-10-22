package com.nexus.aiagent.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom AudioVisualizerView rendering a futuristic glowing sphere
 * and audio-reactive waveforms for the floating assistant overlay.
 */
class AudioVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class State {
        IDLE,
        LISTENING,
        THINKING,
        SPEAKING
    }

    private var currentState: State = State.IDLE
    private var audioAmplitude: Float = 0f
    private var rotationAngle: Float = 0f

    private val spherePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 3000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            rotationAngle = it.animatedValue as Float
            postInvalidateOnAnimation()
        }
    }

    init {
        animator.start()
    }

    fun setState(state: State) {
        currentState = state
        invalidate()
    }

    fun setRms(rmsdB: Float) {
        // Normalize typical rmsdB (approx -2dB to 10dB) into 0f..1f
        val norm = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        audioAmplitude = audioAmplitude * 0.7f + norm * 0.3f
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = (width.coerceAtMost(height) / 2f) * 0.45f
        val dynamicRadius = baseRadius + (audioAmplitude * baseRadius * 0.35f)

        // Colors based on state (#31d69b 0% to #090b14 80% for Myra Overlay Bubble)
        val (coreColor, glowColor, ringColor) = when (currentState) {
            State.IDLE -> Triple(Color.parseColor("#31D69B"), Color.parseColor("#31D69B"), Color.parseColor("#31D69B"))
            State.LISTENING -> Triple(Color.parseColor("#31D69B"), Color.parseColor("#00E676"), Color.parseColor("#69F0AE"))
            State.THINKING -> Triple(Color.parseColor("#00E5FF"), Color.parseColor("#00B0FF"), Color.parseColor("#18FFFF"))
            State.SPEAKING -> Triple(Color.parseColor("#FFD600"), Color.parseColor("#FF9100"), Color.parseColor("#FFE57F"))
        }

        // 1. Draw outer aura/box-shadow glow: 0 0 15px-30px rgba(49, 214, 155, 0.6)
        val outerRadius = dynamicRadius * 1.55f
        glowPaint.shader = RadialGradient(
            cx, cy, outerRadius,
            intArrayOf(Color.argb((150 * (0.4f + audioAmplitude * 0.4f)).toInt(), 49, 214, 155), Color.TRANSPARENT),
            floatArrayOf(0.4f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, outerRadius, glowPaint)

        // 2. Draw rotating orbital rings / wave nodes
        wavePaint.color = ringColor
        wavePaint.alpha = 180
        val numPoints = 16
        for (i in 0 until numPoints) {
            val angle = Math.toRadians((i * (360.0 / numPoints) + rotationAngle))
            val waveOffset = sin(angle * 3.0 + (rotationAngle * 0.1)).toFloat() * (12f * (audioAmplitude + 0.3f))
            val px = cx + (dynamicRadius + waveOffset) * cos(angle).toFloat()
            val py = cy + (dynamicRadius + waveOffset) * sin(angle).toFloat()
            canvas.drawCircle(px, py, 4f + audioAmplitude * 4f, wavePaint)
        }

        // 3. Draw floating-bubble surface: radial-gradient(circle, #31d69b 0%, #090b14 85%)
        spherePaint.shader = RadialGradient(
            cx, cy, dynamicRadius,
            intArrayOf(coreColor, Color.parseColor("#090B14")),
            floatArrayOf(0.0f, 0.85f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, dynamicRadius, spherePaint)

        // 4. Draw core-dot with #31d69b halo shadow
        val coreDotRadius = (15 * resources.displayMetrics.density) // 30px diameter
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, coreDotRadius * 1.5f,
                intArrayOf(Color.parseColor("#31D69B"), Color.TRANSPARENT),
                floatArrayOf(0.5f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, coreDotRadius * 1.5f, haloPaint)

        val coreDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }
        canvas.drawCircle(cx, cy, coreDotRadius, coreDotPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}
