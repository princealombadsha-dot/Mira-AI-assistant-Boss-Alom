package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.NeonGreen
import com.example.voice.VoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HolographicAiOrb(
    voiceState: VoiceState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val rippleWave by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    val primaryColor = when (voiceState) {
        VoiceState.IDLE -> EmeraldPrimary
        VoiceState.LISTENING -> NeonGreen
        VoiceState.THINKING -> CyberCyan
        VoiceState.SPEAKING -> EmeraldTertiary
        VoiceState.RESTING -> Color(0xFF6B7280)
        VoiceState.ERROR -> Color(0xFFEF4444)
    }

    val stateLabel = when (voiceState) {
        VoiceState.IDLE -> "প্রস্তুত (Idle)"
        VoiceState.LISTENING -> "শুনছি... (Listening)"
        VoiceState.THINKING -> "ভাবছি... (Thinking)"
        VoiceState.SPEAKING -> "বলছি... (Speaking)"
        VoiceState.RESTING -> "বিশ্রামে (Resting)"
        VoiceState.ERROR -> "ত্রুটি (Error)"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .clickable { onClick() }
                .testTag("ai_orb_button")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 4f) * pulseScale + (amplitude * 24.dp.toPx())

                // Ripple outer glow rings
                if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                    drawCircle(
                        color = primaryColor.copy(alpha = (0.35f - (rippleWave - 0.5f) * 0.3f).coerceAtLeast(0f)),
                        radius = baseRadius * rippleWave,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Rotating orbital rings
                val ringRadius = baseRadius * 1.35f
                val rad = Math.toRadians(rotationAngle.toDouble())
                for (i in 0 until 4) {
                    val angleOffset = rad + (i * Math.PI / 2)
                    val x = center.x + (ringRadius * cos(angleOffset)).toFloat()
                    val y = center.y + (ringRadius * 0.45f * sin(angleOffset)).toFloat()
                    drawCircle(
                        color = CyberCyan.copy(alpha = 0.85f),
                        radius = 3.5.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                // Rotating soundwave arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(primaryColor, CyberCyan, Color.Transparent, primaryColor)
                    ),
                    startAngle = rotationAngle,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Holographic Orb Core with multi-stop radial gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            primaryColor.copy(alpha = 0.85f),
                            primaryColor.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * 1.15f
                    ),
                    radius = baseRadius,
                    center = center
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = CircleShape,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = stateLabel,
                color = primaryColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun RealtimeAudioVisualizer(
    amplitude: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 18
    val infiniteTransition = rememberInfiniteTransition(label = "viz")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = size.width / (barCount * 1.8f)
            val maxHeight = size.height
            val activeMult = if (isActive) (0.35f + amplitude * 1.8f).coerceAtMost(1f) else 0.1f

            for (i in 0 until barCount) {
                val x = (i * size.width) / barCount + barWidth / 2f
                val waveFactor = sin(phase + i * 0.45f).toFloat() * 0.35f + 0.65f
                val h = (maxHeight * waveFactor * activeMult).coerceAtLeast(4.dp.toPx())
                val topY = (size.height - h) / 2f

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonGreen, CyberCyan)
                    ),
                    topLeft = Offset(x, topY),
                    size = androidx.compose.ui.geometry.Size(barWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}

/**
 * BD-NEXUS Dynamic 3D Audio-Reactive Sphere Visualizer
 * Features interactive drag-to-orbit gesture control, continuous 3D rotation,
 * real-time audio amplitude vertex displacement, and depth-shaded particle projection.
 */
@Composable
fun Interactive3DAudioSphereVisualizer(
    amplitude: Float,
    voiceState: VoiceState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    var manualYaw by remember { mutableFloatStateOf(0f) }
    var manualPitch by remember { mutableFloatStateOf(0.2f) }

    val infiniteTransition = rememberInfiniteTransition(label = "nexus_3d_sphere")
    val autoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto_rot"
    )

    val breathingWave by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    val coreColor = when (voiceState) {
        VoiceState.IDLE -> EmeraldPrimary
        VoiceState.LISTENING -> NeonGreen
        VoiceState.THINKING -> CyberCyan
        VoiceState.SPEAKING -> EmeraldTertiary
        VoiceState.RESTING -> Color(0xFF64748B)
        VoiceState.ERROR -> Color(0xFFEF4444)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    manualYaw += dragAmount.x * 0.012f
                    manualPitch = (manualPitch + dragAmount.y * 0.012f).coerceIn(-1.3f, 1.3f)
                }
            }
            .clickable { onClick() }
            .testTag("interactive_3d_sphere_visualizer")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension * 0.32f) * breathingWave + (amplitude * 28.dp.toPx())
            val focalLength = size.minDimension * 0.85f

            val currentYaw = Math.toRadians((autoRotation + Math.toDegrees(manualYaw.toDouble())).toDouble())
            val currentPitch = manualPitch.toDouble()

            val cosYaw = cos(currentYaw)
            val sinYaw = sin(currentYaw)
            val cosPitch = cos(currentPitch)
            val sinPitch = sin(currentPitch)

            // Inner core holographic ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        coreColor.copy(alpha = 0.65f),
                        coreColor.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.88f
                ),
                radius = baseRadius * 0.88f,
                center = center
            )

            // 3D Point Cloud on sphere shell: Latitudes & Longitudes
            data class ProjectedPoint(val x: Float, val y: Float, val z: Float, val pointRadius: Float, val alpha: Float)
            val points = mutableListOf<ProjectedPoint>()

            val latSteps = 10
            val lonSteps = 16

            for (i in 1 until latSteps) {
                val phi = (i.toDouble() / latSteps - 0.5) * Math.PI // -pi/2 to pi/2
                val cosPhi = cos(phi)
                val sinPhi = sin(phi)

                for (j in 0 until lonSteps) {
                    val theta = (j.toDouble() / lonSteps) * 2.0 * Math.PI // 0 to 2pi

                    // Audio ripple displacement
                    val displacement = 1.0 + (amplitude * 0.45f * sin(theta * 3.0 + phi * 4.0 + currentYaw)).coerceIn(-0.3, 0.6)
                    val r = baseRadius * displacement

                    // Sphere coordinates
                    val x0 = r * cosPhi * cos(theta)
                    val y0 = r * sinPhi
                    val z0 = r * cosPhi * sin(theta)

                    // Rotate Y (Yaw)
                    val x1 = x0 * cosYaw - z0 * sinYaw
                    val z1 = x0 * sinYaw + z0 * cosYaw

                    // Rotate X (Pitch)
                    val y2 = y0 * cosPitch - z1 * sinPitch
                    val z2 = y0 * sinPitch + z1 * cosPitch

                    // Perspective 3D projection
                    val scale = (focalLength / (focalLength + z2)).toFloat().coerceIn(0.4f, 2.2f)
                    val projX = (center.x + x1 * scale).toFloat()
                    val projY = (center.y + y2 * scale).toFloat()

                    // Depth shading
                    val depthFactor = ((z2 / (baseRadius * 1.2)) + 1.0) / 2.0 // 0.0 (far) to 1.0 (near)
                    val alpha = (depthFactor * 0.85 + 0.15).coerceIn(0.12, 1.0).toFloat()
                    val pRadius = (2.2.dp.toPx() * scale).coerceAtLeast(1.5f)

                    points.add(ProjectedPoint(projX, projY, z2.toFloat(), pRadius, alpha))
                }
            }

            // Depth sorting (furthest first, closest last)
            points.sortBy { it.z }

            // Render projected 3D nodes
            points.forEach { pt ->
                val pColor = if (pt.z > 0) {
                    CyberCyan.copy(alpha = pt.alpha)
                } else {
                    coreColor.copy(alpha = pt.alpha * 0.7f)
                }
                drawCircle(
                    color = pColor,
                    radius = pt.pointRadius,
                    center = Offset(pt.x, pt.y)
                )
            }

            // Exterior equatorial audio ring
            val ringRadius = baseRadius * 1.28f + (amplitude * 20.dp.toPx())
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(NeonGreen, CyberCyan, EmeraldTertiary, NeonGreen)
                ),
                startAngle = (autoRotation * 1.5f) % 360f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius * 0.45f),
                size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 0.9f),
                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

