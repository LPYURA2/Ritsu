/*
 * Original, procedural placeholder illustration created for Ritsu Android.
 * No downloaded character artwork, models, fonts, or third-party textures.
 * The final character design is deliberately not established by this asset.
 */
package dev.ritsu.android.avatar

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

enum class AvatarMood { CALM, HAPPY, THINKING, SLEEPING, CONCERNED }

/** Renderer input. A future rig/Live2D renderer can consume the same core-derived pose. */
@Immutable
data class AvatarPose(
    val mood: AvatarMood,
    val eyeOpenness: Float = 1f,
    val breath: Float = 0f,
    val hairSway: Float = 0f,
)

/** No independent avatar mood timer: the application supplies the core's state. */
@Composable
fun RitsuAvatar(
    mood: AvatarMood,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val context = LocalContext.current
    fun motionEnabled(): Boolean = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }.getOrDefault(true)
    var systemMotion by remember(context) { mutableStateOf(motionEnabled()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { systemMotion = motionEnabled() }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer,
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    val enabled = animate && systemMotion
    // Keep a static renderer available for previews, accessibility, and reduced motion.
    val phase = if (enabled) {
        val transition = rememberInfiniteTransition(label = "ritsu-idle")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
            label = "breath-and-blink",
        )
        value
    } else 0f
    val blink = if (enabled && phase in 0.76f..0.81f) {
        (abs(phase - 0.785f) / 0.025f).coerceIn(0f, 1f)
    } else 1f
    val pose = AvatarPose(
        mood = mood,
        eyeOpenness = if (mood == AvatarMood.SLEEPING) 0f else blink,
        breath = if (enabled) sin(phase * 2f * PI).toFloat() else 0f,
        hairSway = if (enabled) sin(phase * 2f * PI + 0.5f).toFloat() else 0f,
    )
    val description = when (mood) {
        AvatarMood.CALM -> "Рицу: спокойная"
        AvatarMood.HAPPY -> "Рицу: радостная"
        AvatarMood.THINKING -> "Рицу: задумчивая"
        AvatarMood.SLEEPING -> "Рицу: отдыхает"
        AvatarMood.CONCERNED -> "Рицу: обеспокоена"
    }
    Canvas(modifier.semantics { contentDescription = description }) {
        val factor = min(size.width / 300f, size.height / 340f)
        translate((size.width - 300f * factor) / 2f, (size.height - 340f * factor) / 2f) {
            scale(factor, factor, pivot = Offset.Zero) { drawRitsu(pose) }
        }
    }
}

private val Hair = Color(0xFF393442)
private val HairLight = Color(0xFF51495E)
private val HairHighlight = Color(0xFF71687D)
private val Skin = Color(0xFFF1D6C4)
private val SkinLight = Color(0xFFFFE8D7)
private val Outline = Color(0xFF453C49)
private val Lilac = Color(0xFFB3A4C2)
private val Ivory = Color(0xFFF0EDE5)

private fun DrawScope.drawRitsu(pose: AvatarPose) {
    val sway = pose.hairSway * 2.8f
    val breath = pose.breath * 1.2f
    // A quiet halo and ground shadow keep the portrait legible on an ivory surface.
    drawCircle(Color(0xFFE8E2EB), radius = 119f, center = Offset(150f, 151f))
    drawCircle(Color(0xFFD7CFDC), radius = 130f, center = Offset(150f, 151f), style = Stroke(0.7f))
    drawOval(Color(0x142E263B), Offset(66f, 320f), Size(169f, 14f))
    drawSparkle(44f, 111f, 5f, Color(0xFFAAA084))
    drawSparkle(260f, 191f, 4f, Color(0xFFAAA084))
    drawCircle(Color(0xFFB8ADBF), 2f, Offset(244f, 76f))
    translate(top = breath) {
        // Back hair: the asymmetry and separate lower tips provide a useful future rig seam.
        drawPath(path {
            moveTo(145f, 29f)
            cubicTo(76f, 20f, 62f, 69f, 62f, 127f)
            cubicTo(62f, 196f, 40f + sway, 264f, 55f + sway, 320f)
            cubicTo(60f, 333f, 72f, 326f, 79f, 309f)
            cubicTo(89f, 329f, 108f, 326f, 114f, 314f)
            lineTo(200f, 314f)
            cubicTo(211f, 332f, 226f, 325f, 229f, 308f)
            cubicTo(239f, 328f, 253f + sway, 330f, 249f + sway, 310f)
            cubicTo(230f + sway, 239f, 245f, 181f, 237f, 112f)
            cubicTo(232f, 55f, 211f, 20f, 145f, 29f)
            close()
        }, Brush.linearGradient(listOf(HairLight, Hair, Color(0xFF302E3B)), Offset(65f, 40f), Offset(229f, 310f)))
        drawPath(path {
            moveTo(85f, 106f); cubicTo(70f, 167f, 88f + sway, 263f, 64f + sway, 312f)
            moveTo(216f, 111f); cubicTo(230f, 178f, 210f + sway, 266f, 239f + sway, 313f)
        }, HairHighlight.copy(alpha = 0.48f), style = Stroke(2f, cap = StrokeCap.Round))

        // Neck and softly rounded shoulders.
        drawPath(path {
            moveTo(132f, 170f); lineTo(130f, 205f)
            cubicTo(115f, 211f, 82f, 210f, 70f, 240f)
            lineTo(64f, 319f)
            quadraticTo(150f, 339f, 237f, 319f)
            lineTo(228f, 240f)
            cubicTo(218f, 215f, 185f, 211f, 171f, 205f)
            lineTo(170f, 170f); close()
        }, Skin)
        drawPath(path {
            moveTo(132f, 175f); quadraticTo(152f, 194f, 170f, 176f)
            lineTo(170f, 189f); quadraticTo(150f, 203f, 132f, 189f); close()
        }, Color(0xFFD5AE9E).copy(alpha = 0.5f))
        drawPath(path {
            moveTo(123f, 205f)
            cubicTo(111f, 213f, 76f, 208f, 65f, 245f)
            lineTo(60f, 319f)
            quadraticTo(150f, 341f, 241f, 319f)
            lineTo(233f, 245f)
            cubicTo(222f, 211f, 187f, 213f, 177f, 205f)
            quadraticTo(151f, 229f, 123f, 205f); close()
        }, Brush.verticalGradient(listOf(Ivory, Color(0xFFDCD6E0)), 210f, 330f))
        drawPath(path {
            moveTo(123f, 205f); lineTo(150f, 225f); lineTo(128f, 246f)
            lineTo(109f, 216f); close()
            moveTo(177f, 205f); lineTo(150f, 225f); lineTo(173f, 246f)
            lineTo(191f, 216f); close()
        }, Color(0xFFFFFCF5))
        drawPath(path {
            moveTo(150f, 231f); lineTo(150f, 326f)
            moveTo(90f, 263f); lineTo(87f, 324f)
            moveTo(211f, 263f); lineTo(215f, 324f)
        }, Color(0xFFB9B0C2), style = Stroke(1.1f, cap = StrokeCap.Round))
        // Muted lilac bow and a small gold pendant.
        drawPath(path {
            moveTo(149f, 230f); cubicTo(139f, 222f, 131f, 222f, 132f, 235f)
            cubicTo(132f, 246f, 144f, 237f, 149f, 235f); close()
            moveTo(152f, 230f); cubicTo(165f, 220f, 172f, 225f, 168f, 237f)
            cubicTo(165f, 244f, 156f, 237f, 152f, 235f); close()
            moveTo(147f, 235f); lineTo(139f, 260f); lineTo(151f, 255f); lineTo(156f, 263f)
            lineTo(154f, 234f); close()
        }, Color(0xFF9C8AAE))
        drawOval(Color(0xFFC5AC77), Offset(146f, 227f), Size(9f, 12f))
        drawOval(Color(0xFFDBCAA7), Offset(148f, 229f), Size(5f, 7f))

        drawOval(Skin, Offset(94f, 117f), Size(15f, 28f))
        drawOval(Skin, Offset(192f, 117f), Size(15f, 28f))
        // Face silhouette with a gentle jaw rather than an exaggerated chibi proportion.
        drawPath(path {
            moveTo(101f, 100f)
            cubicTo(101f, 61f, 124f, 47f, 152f, 49f)
            cubicTo(189f, 48f, 202f, 72f, 201f, 112f)
            cubicTo(201f, 151f, 186f, 177f, 154f, 187f)
            cubicTo(128f, 181f, 108f, 163f, 102f, 140f)
            quadraticTo(99f, 121f, 101f, 100f); close()
        }, Brush.linearGradient(listOf(SkinLight, Skin), Offset(123f, 66f), Offset(175f, 183f)))

        val concerned = pose.mood == AvatarMood.CONCERNED
        val thinking = pose.mood == AvatarMood.THINKING
        drawPath(path {
            moveTo(111f, if (concerned) 105f else 102f)
            quadraticTo(123f, if (concerned) 99f else 98f, 134f, 103f)
            moveTo(167f, if (thinking) 102f else 103f)
            quadraticTo(179f, if (concerned) 99f else 98f, 191f, if (concerned) 105f else 102f)
        }, HairLight, style = Stroke(2f, cap = StrokeCap.Round))
        drawEye(121f, 120f, pose.eyeOpenness, lookingAside = thinking, happy = pose.mood == AvatarMood.HAPPY)
        drawEye(179f, 120f, pose.eyeOpenness, lookingAside = thinking, happy = pose.mood == AvatarMood.HAPPY)
        drawOval(Color(0xFFE6A79F).copy(alpha = 0.27f), Offset(107f, 139f), Size(25f, 9f))
        drawOval(Color(0xFFE6A79F).copy(alpha = 0.27f), Offset(171f, 139f), Size(25f, 9f))
        drawPath(path {
            moveTo(151f, 133f); quadraticTo(149f, 142f, 153f, 144f)
        }, Color(0xFFC8A394), style = Stroke(1.1f, cap = StrokeCap.Round))
        when (pose.mood) {
            AvatarMood.HAPPY -> {
                drawPath(path {
                    moveTo(140f, 158f); quadraticTo(151f, 163f, 163f, 157f)
                    quadraticTo(153f, 176f, 142f, 161f); close()
                }, Color(0xFF9C6569))
                drawPath(path { moveTo(143f, 159f); quadraticTo(151f, 163f, 160f, 159f) },
                    Color(0xFFFFF5EB), style = Stroke(2f, cap = StrokeCap.Round))
            }
            AvatarMood.CONCERNED -> drawPath(path {
                moveTo(143f, 163f); quadraticTo(151f, 158f, 160f, 163f)
            }, Color(0xFFA77477), style = Stroke(1.5f, cap = StrokeCap.Round))
            AvatarMood.THINKING -> drawPath(path {
                moveTo(147f, 160f); quadraticTo(152f, 162f, 157f, 159f)
            }, Color(0xFFA77477), style = Stroke(1.5f, cap = StrokeCap.Round))
            else -> drawPath(path {
                moveTo(142f, 158f); quadraticTo(152f, 165f, 162f, 158f)
            }, Color(0xFFA77477), style = Stroke(1.5f, cap = StrokeCap.Round))
        }
        // Separate foreground locks allow a future layered sprite or skeletal renderer.
        drawPath(path {
            moveTo(150f, 34f)
            cubicTo(106f, 25f, 77f, 54f, 77f, 108f)
            cubicTo(75f, 174f, 81f + sway, 220f, 67f + sway, 274f)
            cubicTo(94f + sway, 254f, 103f, 221f, 101f, 187f)
            lineTo(97f, 114f)
            cubicTo(111f, 108f, 130f, 90f, 139f, 66f)
            cubicTo(137f, 87f, 124f, 96f, 116f, 100f)
            cubicTo(137f, 98f, 150f, 86f, 154f, 65f)
            cubicTo(160f, 87f, 178f, 98f, 194f, 104f)
            cubicTo(200f, 155f, 192f, 231f, 226f + sway, 274f)
            cubicTo(215f + sway, 229f, 227f, 172f, 224f, 115f)
            cubicTo(226f, 53f, 197f, 26f, 150f, 34f); close()
        }, Brush.linearGradient(listOf(HairLight, Hair), Offset(100f, 37f), Offset(199f, 218f)))
        drawPath(path {
            moveTo(136f, 43f); cubicTo(105f, 42f, 88f, 66f, 88f, 98f)
            moveTo(164f, 43f); cubicTo(192f, 43f, 210f, 65f, 213f, 94f)
            moveTo(88f, 132f); cubicTo(87f, 186f, 93f + sway, 220f, 79f + sway, 251f)
            moveTo(211f, 135f); cubicTo(213f, 179f, 204f + sway, 220f, 219f + sway, 251f)
        }, HairHighlight.copy(alpha = 0.62f), style = Stroke(1.7f, cap = StrokeCap.Round))
        drawPath(path {
            moveTo(101f, 60f); cubicTo(108f, 51f, 119f, 47f, 126f, 46f)
        }, Color(0xFFABA0B6).copy(alpha = 0.45f), style = Stroke(3f, cap = StrokeCap.Round))
        // An unbranded, original two-pin hair ornament.
        drawLine(Color(0xFFC4B084), Offset(205f, 87f), Offset(217f, 100f), 3f, StrokeCap.Round)
        drawLine(Color(0xFFC4B084), Offset(202f, 96f), Offset(213f, 108f), 3f, StrokeCap.Round)
    }
}

private fun DrawScope.drawEye(
    cx: Float,
    cy: Float,
    openness: Float,
    lookingAside: Boolean,
    happy: Boolean,
) {
    if (openness < 0.14f) {
        drawPath(path {
            moveTo(cx - 14f, cy)
            quadraticTo(cx, cy + if (happy) -5f else 6f, cx + 14f, cy)
        }, Outline, style = Stroke(2.2f, cap = StrokeCap.Round))
        return
    }
    val height = 20f * openness
    val top = cy - height / 2f
    drawPath(path {
        moveTo(cx - 14f, cy - 2f)
        cubicTo(cx - 8f, top - 4f, cx + 8f, top - 4f, cx + 14f, cy - 2f)
        quadraticTo(cx + 10f, cy + height / 2f, cx, cy + height / 2f)
        quadraticTo(cx - 11f, cy + height / 2f, cx - 14f, cy - 2f)
        close()
    }, Color(0xFFFFF9F0))
    val pupilX = cx + if (lookingAside) 3f else 0f
    drawOval(Brush.verticalGradient(listOf(Color(0xFF61536F), Lilac), top, top + height),
        Offset(pupilX - 7.2f, top + 0.5f), Size(14.4f, height - 0.5f))
    drawOval(Color(0xFF3C354A), Offset(pupilX - 3.1f, top + 1.5f), Size(6.2f, height * 0.65f))
    drawCircle(Color(0xFFFFFCF5), 2.5f * openness, Offset(pupilX - 3f, cy - 4f * openness))
    drawCircle(Color(0xFFEAE3EC), 1.1f * openness, Offset(pupilX + 3.2f, cy + 4f * openness))
    drawPath(path {
        moveTo(cx - 15f, cy - 3f)
        cubicTo(cx - 8f, top - 4f, cx + 8f, top - 4f, cx + 15f, cy - 2f)
        moveTo(cx - 14f, cy - 3f); lineTo(cx - 17f, cy - 6f)
    }, Outline, style = Stroke(2.4f, cap = StrokeCap.Round))
    drawPath(path {
        moveTo(cx - 10f, cy + height / 2f - 1f)
        quadraticTo(cx, cy + height / 2f + 2f, cx + 10f, cy + height / 2f - 1f)
    }, Color(0xFFBE9894), style = Stroke(0.8f, cap = StrokeCap.Round))
}

private fun DrawScope.drawSparkle(x: Float, y: Float, radius: Float, color: Color) {
    drawPath(path {
        moveTo(x, y - radius); quadraticTo(x + 1f, y - 1f, x + radius, y)
        quadraticTo(x + 1f, y + 1f, x, y + radius)
        quadraticTo(x - 1f, y + 1f, x - radius, y)
        quadraticTo(x - 1f, y - 1f, x, y - radius); close()
    }, color)
}

private inline fun path(block: Path.() -> Unit): Path = Path().apply(block)
