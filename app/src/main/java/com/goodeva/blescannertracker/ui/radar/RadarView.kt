package com.goodeva.blescannertracker.ui.radar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.goodeva.blescannertracker.domain.model.SignalCategory
import kotlin.math.cos
import kotlin.math.sin

private const val RSSI_NEAR = -30f
private const val RSSI_FAR = -90f
private const val CENTER_GAP = 0.12f // jarak minimum titik dari pusat
private const val BLIP_ANGLE = -0.785398f // -45° (BLE tidak punya info arah)

fun SignalCategory.toColor(): Color = when (this) {
    SignalCategory.VERY_STRONG -> Color(0xFF00C853)
    SignalCategory.STRONG -> Color(0xFF64DD17)
    SignalCategory.FAIR -> Color(0xFFFFD600)
    SignalCategory.WEAK -> Color(0xFFFF9100)
    SignalCategory.VERY_WEAK -> Color(0xFFFF3D00)
    SignalCategory.LOST -> Color(0xFF9E9E9E)
}

/** 0 = dekat (RSSI -30), 1 = jauh (RSSI -90). */
private fun rssiToFraction(rssi: Int?): Float =
    if (rssi == null) 1f else ((RSSI_NEAR - rssi) / (RSSI_NEAR - RSSI_FAR)).coerceIn(0f, 1f)

/** Posisi radial (0..1 dari radius) dengan jarak minimum dari pusat. */
private fun radial(fraction: Float): Float = CENTER_GAP + (1f - CENTER_GAP) * fraction

@Composable
fun RadarView(
    signal: SignalCategory,
    smoothedRssi: Int?,
    status: TrackingStatus,
    modifier: Modifier = Modifier,
) {
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant

    val signalColor by animateColorAsState(signal.toColor(), tween(500), label = "signalColor")
    val target = when (status) {
        TrackingStatus.TRACKING -> rssiToFraction(smoothedRssi)
        TrackingStatus.LOST -> 1f
        TrackingStatus.SEARCHING -> 0f
    }
    val fraction by animateFloatAsState(target, tween(600), label = "fraction")

    val transition = rememberInfiniteTransition(label = "radar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "sweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearOutSlowInEasing)),
        label = "pulse",
    )

    Canvas(modifier = modifier) {
        val c = center
        val radius = size.minDimension / 2f

        drawCircle(color = backgroundColor, radius = radius, center = c)

        // Cincin: -50 dBm (±3 m), -70 dBm (±10 m), -90 dBm (±20 m+)
        listOf(1f / 3f, 2f / 3f, 1f).forEach { f ->
            drawCircle(
                color = ringColor,
                radius = radius * radial(f),
                center = c,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        drawLine(ringColor, Offset(c.x - radius, c.y), Offset(c.x + radius, c.y), 1.dp.toPx())
        drawLine(ringColor, Offset(c.x, c.y - radius), Offset(c.x, c.y + radius), 1.dp.toPx())

        // Sapuan radar
        rotate(degrees = sweep, pivot = c) {
            drawCircle(
                brush = Brush.sweepGradient(
                    0f to Color.Transparent,
                    1f to signalColor.copy(alpha = 0.35f),
                    center = c,
                ),
                radius = radius,
                center = c,
            )
        }

        drawCircle(color = ringColor, radius = 5.dp.toPx(), center = c) // posisi kita

        if (status != TrackingStatus.SEARCHING) {
            val r = radius * radial(fraction)
            val pos = Offset(c.x + r * cos(BLIP_ANGLE), c.y + r * sin(BLIP_ANGLE))
            if (status == TrackingStatus.TRACKING) {
                drawCircle(
                    color = signalColor.copy(alpha = (1f - pulse) * 0.5f),
                    radius = 10.dp.toPx() + 24.dp.toPx() * pulse,
                    center = pos,
                )
                drawCircle(color = signalColor, radius = 8.dp.toPx(), center = pos)
            } else {
                // LOST: lingkaran kosong abu-abu di tepi
                drawCircle(
                    color = signalColor,
                    radius = 8.dp.toPx(),
                    center = pos,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}