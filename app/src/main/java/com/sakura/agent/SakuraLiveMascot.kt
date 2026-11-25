package com.sakura.agent

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val Hair = Color(0xFFF6A5C2)
private val HairDeep = Color(0xFFE574A4)
private val Skin = Color(0xFFFFE2D1)
private val Dress = Color(0xFF5A4472)
private val Eye = Color(0xFF3A2945)
private val Mouth = Color(0xFFB7436A)
private val Glow = Color(0xFFFF9CC6)
private val Petal = Color(0xFFFFD4E3)
private val Pollen = Color(0xFFFFD466)

/** Vector mascot: eyes blink and look around, mouth follows speech, hands wave. */
@Composable
internal fun SakuraLiveMascot(size: Dp, motion: Boolean, thinking: Boolean, speaking: Boolean, event: String) {
    val transition = rememberInfiniteTransition(label = "Sakura viva")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5600, easing = LinearEasing), RepeatMode.Restart),
        label = "ciclo de expresiones"
    )
    val p = if (motion) phase else 0.12f
    val description = "Sakura: ${MascotExpressions.mood(event)}" +
        if (speaking) ", hablando" else if (thinking) ", pensando" else ""
    Canvas(Modifier.size(size).semantics { contentDescription = description }) {
        drawSakura(size.toPx(), p, thinking, speaking, event)
    }
}

private fun DrawScope.drawSakura(s: Float, phase: Float, thinking: Boolean, speaking: Boolean, event: String) {
    val cx = s * 0.5f
    val cy = s * 0.43f
    val r = s * 0.29f

    drawCircle(color = Glow, radius = s * 0.47f, center = Offset(cx, cy), alpha = 0.14f)

    // Hair petals behind the head, swaying gently with the expression cycle.
    val sway = MascotExpressions.hairSway(phase, thinking) * s
    for (i in 0 until 10) {
        val a = Math.toRadians(i * 36.0).toFloat()
        val px = cx + cos(a) * r * 0.94f + sway
        val py = cy + sin(a) * r * 0.94f + sway * 0.35f
        drawCircle(HairDeep, radius = r * 0.33f, center = Offset(px, py))
    }

    // Body and sleeves.
    drawOval(Dress, topLeft = Offset(cx - s * 0.21f, cy + r * 0.62f), size = Size(s * 0.42f, s * 0.36f))
    drawOval(Petal, topLeft = Offset(cx - s * 0.05f, cy + r * 0.76f), size = Size(s * 0.10f, s * 0.13f))

    val wave = MascotExpressions.handWave(phase, thinking)
    val bounce = if (MascotExpressions.happy(event)) abs(wave) * s * 0.02f else 0f
    val leftHand = Offset(cx - r * 1.04f, cy + r * 0.96f - wave * s * 0.07f - bounce)
    val rightHand = Offset(cx + r * 1.04f, cy + r * 0.96f + wave * s * 0.07f - bounce)
    drawLine(Skin, Offset(cx - r * 0.72f, cy + r * 0.70f), leftHand, strokeWidth = s * 0.052f)
    drawLine(Skin, Offset(cx + r * 0.72f, cy + r * 0.70f), rightHand, strokeWidth = s * 0.052f)
    drawCircle(Skin, radius = s * 0.055f, center = leftHand)
    drawCircle(Skin, radius = s * 0.055f, center = rightHand)

    // Head, face and fringe.
    drawCircle(Hair, radius = r, center = Offset(cx, cy))
    drawCircle(Skin, radius = r * 0.79f, center = Offset(cx, cy + r * 0.06f))
    for (i in -2..2) {
        val px = cx + i * r * 0.30f
        drawCircle(Hair, radius = r * 0.30f, center = Offset(px, cy - r * 0.62f + abs(i.toFloat()) * r * 0.05f))
    }

    // Eyes: two whites, moving pupils, optional blink and a small highlight.
    val blink = MascotExpressions.blinkWithDouble(phase)
    val asleep = event == "Dormir" && MascotExpressions.sleepingEyes(phase)
    val eyeScale = if (blink || asleep) 0.10f else 1f
    val gx = MascotExpressions.gazeX(phase) * r * 0.11f
    val gy = MascotExpressions.gazeY(phase) * r * 0.08f
    for (ex in listOf(cx - r * 0.30f, cx + r * 0.30f)) {
        val eyeCenter = Offset(ex, cy + r * 0.02f)
        drawOval(Color.White, topLeft = Offset(eyeCenter.x - r * 0.16f, eyeCenter.y - r * 0.17f * eyeScale),
            size = Size(r * 0.32f, r * 0.34f * eyeScale))
        if (!blink && !asleep) {
            val pupil = Offset(ex + gx, eyeCenter.y + r * 0.04f + gy)
            drawCircle(Eye, radius = r * 0.125f, center = pupil)
            drawCircle(Color.White, radius = r * 0.038f, center = Offset(pupil.x - r * 0.04f, pupil.y - r * 0.05f))
        }
    }
    if (thinking) {
        drawLine(Eye, Offset(cx - r * 0.46f, cy - r * 0.25f), Offset(cx - r * 0.16f, cy - r * 0.31f), strokeWidth = s * 0.014f)
    }
    val blush = MascotExpressions.blush(event, phase)
    if (blush > 0f) {
        drawCircle(Glow, radius = r * 0.10f, center = Offset(cx - r * 0.48f, cy + r * 0.26f), alpha = blush)
        drawCircle(Glow, radius = r * 0.10f, center = Offset(cx + r * 0.48f, cy + r * 0.26f), alpha = blush)
    }

    // Mouth: small smile when idle, animated ellipse while speaking.
    val open = MascotExpressions.mouthOpen(phase, speaking)
    if (open > 0.22f) {
        drawOval(Mouth, topLeft = Offset(cx - r * 0.14f, cy + r * 0.30f), size = Size(r * 0.28f, r * 0.42f * open))
    } else {
        drawArc(Mouth, 12f, 156f, false, topLeft = Offset(cx - r * 0.16f, cy + r * 0.24f),
            size = Size(r * 0.32f, r * 0.22f), style = Stroke(width = s * 0.018f))
    }

    // Floating pollen: three small orbiting particles around the silhouette.
    for (i in 0 until 3) {
        val a = MascotExpressions.pollenAngle(i, phase)
        val orbit = r * (1.12f + 0.08f * i)
        drawCircle(Pollen, radius = s * 0.012f, alpha = 0.55f,
            center = Offset(cx + cos(a) * orbit, cy + sin(a) * orbit * 0.86f))
    }

    // Same Sakura, different focus: a small lens when searching, a page when reading an attachment.
    when (event) {
        "Buscando" -> {
            val lens = Offset(cx - r * 0.86f, cy - r * 0.80f)
            drawCircle(Color.White, radius = s * 0.065f, center = lens, style = Stroke(width = s * 0.018f), alpha = 0.90f)
            drawLine(Color.White, Offset(lens.x + s * 0.045f, lens.y + s * 0.045f),
                Offset(lens.x + s * 0.085f, lens.y + s * 0.085f), strokeWidth = s * 0.022f, alpha = 0.90f)
        }
        "Leyendo" -> {
            val page = Offset(cx - r * 0.98f, cy - r * 0.86f)
            drawRect(Color.White, topLeft = page, size = Size(s * 0.11f, s * 0.15f), alpha = 0.88f)
            repeat(3) { i -> drawLine(Dress, Offset(page.x + s * 0.02f, page.y + s * (0.04f + i * 0.03f)),
                Offset(page.x + s * 0.09f, page.y + s * (0.04f + i * 0.03f)), strokeWidth = s * 0.008f) }
        }
    }

    drawBlossom(cx + r * 0.82f, cy - r * 0.78f, s * 0.055f)

    // Extra offline gestures. All are finite, deterministic and pause with `motion`.
    when (event) {
        "Corazón" -> {
            val pulse = 0.9f + 0.2f * MascotExpressions.heartBeat(phase)
            val hx = cx + r * 0.95f
            val hy = cy - r * 0.95f
            val hs = s * 0.035f * pulse
            val heart = Path().apply {
                moveTo(hx, hy + hs)
                cubicTo(hx - hs * 1.6f, hy - hs * 0.8f, hx - hs * 0.6f, hy - hs * 1.6f, hx, hy - hs * 0.6f)
                cubicTo(hx + hs * 0.6f, hy - hs * 1.6f, hx + hs * 1.6f, hy - hs * 0.8f, hx, hy + hs)
                close()
            }
            drawPath(heart, Glow, alpha = 0.92f)
        }
        "Dormir" -> {
            repeat(3) { i ->
                val zy = cy - r * (0.88f + i * 0.12f)
                val zx = cx + r * (0.82f + i * 0.08f)
                drawLine(Petal, Offset(zx, zy), Offset(zx + s * 0.05f, zy), strokeWidth = s * 0.012f, alpha = 0.9f)
                drawLine(Petal, Offset(zx + s * 0.05f, zy), Offset(zx, zy - s * 0.035f), strokeWidth = s * 0.012f, alpha = 0.9f)
                drawLine(Petal, Offset(zx, zy - s * 0.035f), Offset(zx + s * 0.05f, zy - s * 0.035f), strokeWidth = s * 0.012f, alpha = 0.9f)
            }
        }
        "Girar" -> {
            rotate(MascotExpressions.spin(phase) * 14f, pivot = Offset(cx, cy)) {
                drawBlossom(cx + r * 0.95f, cy - r * 0.85f, s * 0.05f)
            }
        }
        "Celebrar" -> {
            repeat(6) { i ->
                val a = MascotExpressions.pollenAngle(i, phase)
                val radius = r * (1.18f + 0.05f * i)
                drawCircle(Petal, radius = s * 0.016f, alpha = 0.85f,
                    center = Offset(cx + cos(a) * radius, cy + sin(a) * radius * 0.9f))
            }
        }
    }
}

private fun DrawScope.drawBlossom(cx: Float, cy: Float, radius: Float) {
    for (i in 0 until 5) {
        val a = Math.toRadians(i * 72.0).toFloat()
        drawCircle(Petal, radius = radius * 0.62f,
            center = Offset(cx + cos(a) * radius * 0.62f, cy + sin(a) * radius * 0.62f))
    }
    drawCircle(Pollen, radius = radius * 0.38f, center = Offset(cx, cy))
}
