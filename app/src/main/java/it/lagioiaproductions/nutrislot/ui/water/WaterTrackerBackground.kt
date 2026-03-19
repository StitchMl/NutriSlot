package it.lagioiaproductions.nutrislot.ui.water

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AnimatedWaterBackground(
    progress: Float,
    isGoalReached: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "water_waves")

    val phaseBack by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_back"
    )

    val phaseMid by infiniteTransition.animateFloat(
        initialValue = (PI / 3f).toFloat(),
        targetValue = ((2f * PI) + (PI / 3f)).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_mid"
    )

    val phaseFront by infiniteTransition.animateFloat(
        initialValue = (PI / 1.6f).toFloat(),
        targetValue = ((2f * PI) + (PI / 1.6f)).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_front"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (isGoalReached) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        WaterTrackerColors.HeaderBlue,
                        WaterTrackerColors.BlueLight,
                        WaterTrackerColors.Blue,
                        WaterTrackerColors.Teal
                    ),
                    startY = 0f,
                    endY = height
                )
            )

            drawWave(
                color = Color.White.copy(alpha = 0.18f),
                baseY = height * 0.22f,
                amplitude = 18.dp.toPx(),
                wavelength = width / 1.4f,
                phase = phaseBack
            )

            drawWave(
                color = WaterTrackerColors.BlueLight.copy(alpha = 0.38f),
                baseY = height * 0.33f,
                amplitude = 22.dp.toPx(),
                wavelength = width / 1.15f,
                phase = phaseMid
            )

            drawWave(
                color = WaterTrackerColors.Blue.copy(alpha = 0.55f),
                baseY = height * 0.46f,
                amplitude = 28.dp.toPx(),
                wavelength = width / 0.95f,
                phase = phaseFront
            )
        } else {
            drawRect(color = Color.White)

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        WaterTrackerColors.HeaderBlue,
                        WaterTrackerColors.HeaderTeal
                    )
                ),
                topLeft = Offset.Zero,
                size = Size(width, 18.dp.toPx())
            )

            val waterBaseY = height * (0.60f - (progress * 0.17f))

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        WaterTrackerColors.Blue,
                        WaterTrackerColors.Teal
                    ),
                    startY = waterBaseY - 90f,
                    endY = height
                ),
                topLeft = Offset(0f, waterBaseY - 90f),
                size = Size(width, height - (waterBaseY - 90f))
            )

            drawWave(
                color = WaterTrackerColors.BlueLight.copy(alpha = 0.92f),
                baseY = waterBaseY - 18f,
                amplitude = 20.dp.toPx(),
                wavelength = width / 1.3f,
                phase = phaseBack
            )

            drawWave(
                color = WaterTrackerColors.Blue.copy(alpha = 0.88f),
                baseY = waterBaseY + 18f,
                amplitude = 26.dp.toPx(),
                wavelength = width / 1.05f,
                phase = phaseMid
            )

            drawWave(
                color = WaterTrackerColors.BlueDark.copy(alpha = 0.97f),
                baseY = waterBaseY + 56f,
                amplitude = 30.dp.toPx(),
                wavelength = width / 0.92f,
                phase = phaseFront
            )
        }
    }
}

private fun DrawScope.drawWave(
    color: Color,
    baseY: Float,
    amplitude: Float,
    wavelength: Float,
    phase: Float
) {
    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, baseY)

        var x = 0f
        while (x <= size.width) {
            val normalized = (x / wavelength) * (2f * PI.toFloat())
            val y = baseY + sin(normalized + phase) * amplitude
            lineTo(x, y)
            x += 8f
        }

        lineTo(size.width, size.height)
        close()
    }

    drawPath(
        path = path,
        color = color
    )
}