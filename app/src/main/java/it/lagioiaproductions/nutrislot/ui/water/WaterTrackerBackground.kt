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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AnimatedWaterBackground(
    progress: Float,
    isGoalReached: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "water_bg")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 1.4f,
        targetValue = (2f * PI).toFloat() + 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val phase3 by infiniteTransition.animateFloat(
        initialValue = 2.1f,
        targetValue = (2f * PI).toFloat() + 2.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val normalizedProgress = progress.coerceIn(0f, 1f)
        val colorProgress = if (isGoalReached) 1f else normalizedProgress

        val topColor = lerp(
            WaterTrackerColors.BackgroundTop,
            WaterTrackerColors.GoalReachedTop,
            colorProgress
        )
        val midColor = lerp(
            WaterTrackerColors.Background,
            WaterTrackerColors.GoalReachedMid,
            colorProgress
        )
        val bottomColor = lerp(
            WaterTrackerColors.Background,
            WaterTrackerColors.GoalReachedBottom,
            colorProgress
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(topColor, midColor, bottomColor)
            ),
            size = Size(width, height)
        )

        val backWaveBase = height * (0.88f - normalizedProgress * 0.42f)
        val midWaveBase = height * (0.94f - normalizedProgress * 0.38f)
        val frontWaveBase = height * (1.00f - normalizedProgress * 0.34f)

        drawWave(
            color = lerp(
                WaterTrackerColors.AccentBlueStrong.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.16f),
                colorProgress
            ),
            baseY = backWaveBase,
            amplitude = 20.dp.toPx(),
            wavelength = width / 1.35f,
            phase = phase1
        )

        drawWave(
            color = lerp(
                WaterTrackerColors.AccentBlue.copy(alpha = 0.10f),
                WaterTrackerColors.AccentBlue.copy(alpha = 0.22f),
                colorProgress
            ),
            baseY = midWaveBase,
            amplitude = 28.dp.toPx(),
            wavelength = width / 1.10f,
            phase = phase2
        )

        drawWave(
            color = lerp(
                WaterTrackerColors.AccentBlueDeep.copy(alpha = 0.14f),
                WaterTrackerColors.AccentBlueStrong.copy(alpha = 0.26f),
                colorProgress
            ),
            baseY = frontWaveBase,
            amplitude = 34.dp.toPx(),
            wavelength = width / 0.92f,
            phase = phase3
        )
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

    drawPath(path = path, color = color)
}