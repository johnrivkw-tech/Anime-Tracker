package com.example.animetracker.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animetracker.ui.theme.Bone
import com.example.animetracker.ui.theme.Charcoal
import com.example.animetracker.ui.theme.CharcoalHigh
import com.example.animetracker.ui.theme.Pulse
import com.example.animetracker.ui.theme.Smoke
import kotlin.math.sin

/**
 * Shown for the brief window between app launch and the initial home feed
 * load finishing, so the person never sees a half-loaded Scaffold with a
 * live (but empty) bottom nav bar and blank tabs underneath it.
 *
 * An original ship silhouette rocking on animated waves — not a
 * reproduction of any existing show's ship/character design.
 */
@Composable
fun SplashScreen() {
    // A single looping clock drives both the wave scroll and the ship's
    // bob/tilt, so everything stays in sync with itself.
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave-phase"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawMoon()
                drawShip(time)
                drawWaveLayer(
                    phase = time,
                    color = CharcoalHigh,
                    amplitude = 14f,
                    heightFraction = 0.62f,
                    speed = 1f
                )
                drawWaveLayer(
                    phase = time,
                    color = Charcoal,
                    amplitude = 10f,
                    heightFraction = 0.7f,
                    speed = 1.6f
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Vizora",
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.Medium,
                    fontSize = 44.sp,
                    color = Bone
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Loading your anime...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** A dim Pulse-tinted moon sitting high in the sky for a bit of atmosphere. */
private fun DrawScope.drawMoon() {
    drawCircle(
        color = Pulse.copy(alpha = 0.35f),
        radius = size.minDimension * 0.11f,
        center = Offset(size.width * 0.74f, size.height * 0.22f)
    )
}

/**
 * One layer of a repeating sine-wave "water" band. Multiple layers at
 * different speeds/amplitudes/colors give a simple parallax feel.
 */
private fun DrawScope.drawWaveLayer(
    phase: Float,
    color: androidx.compose.ui.graphics.Color,
    amplitude: Float,
    heightFraction: Float,
    speed: Float
) {
    val baseline = size.height * heightFraction
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, baseline)
        val step = 8f
        var x = 0f
        while (x <= size.width) {
            val y = baseline + amplitude * sin((x / 60f) + phase * speed)
            lineTo(x, y)
            x += step
        }
        lineTo(size.width, size.height)
        close()
    }
    drawPath(path, color = color)
}

/**
 * A minimal original ship silhouette: hull, mast, single sail, riding the
 * front wave layer. Bobs vertically and tilts slightly with the same clock
 * driving the waves so it reads as "sitting in the water" rather than
 * floating independently of it.
 */
private fun DrawScope.drawShip(phase: Float) {
    val baseline = size.height * 0.62f
    val bob = 6f * sin(phase * 1f)
    val centerX = size.width * 0.5f
    val shipY = baseline + bob - 18f

    val hullWidth = size.width * 0.28f
    val hullHeight = 22f

    // Hull: a simple rounded trapezoid.
    val hull = Path().apply {
        moveTo(centerX - hullWidth / 2, shipY)
        lineTo(centerX + hullWidth / 2, shipY)
        lineTo(centerX + hullWidth / 2 - 14f, shipY + hullHeight)
        lineTo(centerX - hullWidth / 2 + 14f, shipY + hullHeight)
        close()
    }
    drawPath(hull, color = Smoke)

    // Mast.
    drawLine(
        color = Bone,
        start = Offset(centerX, shipY),
        end = Offset(centerX, shipY - 60f),
        strokeWidth = 4f
    )

    // Sail: a single triangle, tilting gently with the same phase as the bob.
    val tilt = 6f * sin(phase * 1f + 1.2f)
    val sail = Path().apply {
        moveTo(centerX, shipY - 58f)
        lineTo(centerX + 34f + tilt, shipY - 30f)
        lineTo(centerX, shipY - 6f)
        close()
    }
    drawPath(sail, color = Bone.copy(alpha = 0.9f))
}
