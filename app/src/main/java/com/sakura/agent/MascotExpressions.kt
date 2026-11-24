package com.sakura.agent

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Deterministic expressions for the offline vector mascot. Kept free of Android and
 * Compose imports so the timing rules can be tested on the JVM.
 */
object MascotExpressions {
    private const val TAU = 6.2831853f

    fun mood(event: String): String = when (event) {
        "Saludo" -> "saludando"
        "Saltito" -> "feliz"
        "Pensando" -> "pensando"
        "Lista" -> "contenta"
        "Pausa" -> "en pausa"
        "Error" -> "preocupada"
        "Buscando" -> "buscando"
        "Leyendo" -> "leyendo"
        else -> "contigo"
    }

    fun blink(phase: Float): Boolean = phase in 0.765f..0.795f

    /** Occasional double blink; deterministic so it can be tested on the JVM. */
    fun blinkWithDouble(phase: Float): Boolean = blink(phase) || phase in 0.205f..0.225f

    /** Small hair sway; gentler while thinking. */
    fun hairSway(phase: Float, thinking: Boolean): Float =
        sin(phase * TAU).toFloat() * if (thinking) 0.035f else 0.07f

    /** Cheek colour intensity: only for happy events. */
    fun blush(event: String, phase: Float): Float =
        if (happy(event)) 0.30f + 0.10f * (1f + sin(phase * TAU * 2f)).toFloat() / 2f else 0f

    /** Orbit angle for the three floating pollen particles. */
    fun pollenAngle(index: Int, phase: Float): Float =
        phase * TAU * (1f + index * 0.18f) + index * (TAU / 3f)

    fun gazeX(phase: Float): Float = sin(phase * TAU).toFloat() * 0.30f
    fun gazeY(phase: Float): Float = cos(phase * TAU * 2f).toFloat() * 0.10f

    /** 0.45..1.0 while speaking; a small idle smile otherwise. */
    fun mouthOpen(phase: Float, speaking: Boolean): Float =
        if (speaking) 0.45f + 0.55f * abs(sin(phase * TAU * 5f)).toFloat()
        else 0.10f + 0.04f * (1f + sin(phase * TAU)).toFloat()

    fun handWave(phase: Float, thinking: Boolean): Float =
        if (thinking) sin(phase * TAU * 2f).toFloat() * 0.35f
        else sin(phase * TAU * 3f).toFloat()

    fun happy(event: String): Boolean = event == "Saludo" || event == "Saltito" || event == "Lista"
}
