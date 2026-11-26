package com.pixelpet.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/** الإشارات والحركات المتاحة لعيدان الشخصيات. كل واحدة دالة وضعية بدون أي كلام. */
enum class SAnim(val front: Boolean = false, val loop: Boolean = true) {
    IDLE, WALK, RUN, WAVE(true), JUMP(false, false), CHEER(true), DANCE(true), MINE, POINT,
    SHRUG(true), CLAP(true), BOW(false, false), SIT, SLEEP, SCARED(true), LAUGH(true), THINK,
    FLEX(true), FALL(false, false), SALUTE(true), THUMBS(true), FACEPALM, BECKON(true), STOP(true),
    HEART(true), PEACE(true), KISS(true), HIGHFIVE, HUG, PUSH, CARRIED(true), GETUP(false, false),
    SPIN(true), NOD, LOOKAROUND, TIRED
}

object StickAnims {
    private const val TAU = 6.2831853f
    private fun w(t: Float, hz: Float, ph: Float = 0f) = sin((t * hz + ph) * TAU)
    private fun ease(x: Float): Float { val c = x.coerceIn(0f, 1f); return c * c * (3f - 2f * c) }

    /** يضبط الوضعية للحركة [a] عند الزمن t (بالثواني منذ بداية الحركة). */
    fun apply(a: SAnim, t: Float, p: SPose) {
        p.reset()
        p.front = a.front
        when (a) {
            SAnim.IDLE -> {
                val b = w(t, 0.5f)
                p.nSa = 4f + b * 2f; p.fSa = -4f - b * 2f; p.nEb = 8f; p.fEb = 8f
                p.nHa = 3f; p.fHa = -3f; p.lean = b * 0.8f
            }
            SAnim.WALK -> walk(t, 1.1f, 28f, 22f, 1.2f, p)
            SAnim.RUN -> {
                walk(t, 2.1f, 48f, 55f, 1.8f, p)
                p.lean = 14f; p.nEb = 80f; p.fEb = 80f
                p.lift = max(0f, w(t, 4.2f)) * 3f
            }
            SAnim.WAVE -> {
                val e = ease(t / 0.3f)
                p.nSa = 150f * e + 4f; p.nEb = 0f + w(t, 2.6f) * 28f * e; p.thumb = false
                p.fSa = 5f; p.lean = 0f; p.nHa = 3f; p.fHa = -3f
            }
            SAnim.JUMP -> {
                val u = (t / 0.7f).coerceIn(0f, 1f)
                val h = 4f * u * (1f - u) * 15f
                p.lift = h; p.nSa = 140f * (1f - abs(2 * u - 1f)) + 10f; p.fSa = p.nSa
                p.nHa = 25f * (1f - u); p.nKb = 50f; p.fHa = -10f; p.fKb = 40f
                p.lean = -4f
            }
            SAnim.CHEER -> {
                val b = w(t, 2.2f)
                p.nSa = 160f + b * 12f; p.fSa = 160f - b * 12f; p.nEb = 10f; p.fEb = 10f
                p.lift = max(0f, b) * 7f; p.nKb = 12f * max(0f, -b); p.fKb = p.nKb
                p.nHa = 6f; p.fHa = -6f
            }
            SAnim.DANCE -> {
                val b = w(t, 1.8f); val c = w(t, 0.9f)
                p.nSa = 95f + b * 55f; p.fSa = 95f - b * 55f; p.nEb = -30f * b; p.fEb = 30f * b
                p.nHa = c * 18f; p.fHa = -c * 18f; p.nKb = 20f * max(0f, c); p.fKb = 20f * max(0f, -c)
                p.lean = c * 6f; p.head = -c * 10f; p.lift = abs(b) * 4f
            }
            SAnim.MINE -> {
                val sw = w(t, 1.6f)
                val u = (sw + 1f) / 2f
                p.nSa = 40f + 150f * u * u; p.nEb = 20f - 40f * (1f - u); p.fSa = p.nSa - 8f; p.fEb = p.nEb
                p.lean = 6f + 12f * (1f - u); p.nHa = 14f; p.fHa = -10f; p.nKb = 10f
            }
            SAnim.POINT -> {
                val e = ease(t / 0.25f)
                p.nSa = 90f * e + 5f; p.nEb = 0f; p.fSa = 6f; p.lean = 3f; p.nHa = 3f; p.fHa = -3f
            }
            SAnim.SHRUG -> {
                val e = ease(t / 0.25f); val b = w(t, 2f) * 0.5f + 0.5f
                p.nSa = 45f * e; p.fSa = 45f * e; p.nEb = 105f * e; p.fEb = 105f * e
                p.lift = 1.5f * e * b; p.head = 8f * w(t, 1f)
            }
            SAnim.CLAP -> {
                val c = w(t, 3.2f) * 0.5f + 0.5f
                p.nSa = 80f; p.fSa = 80f; p.nEb = 62f + 28f * (1f - c); p.fEb = 62f + 28f * (1f - c)
                p.lift = c * 1.2f
            }
            SAnim.BOW -> {
                val u = (t / 1.3f).coerceIn(0f, 1f)
                val k = sin(u * 3.1416f)
                p.lean = 62f * k; p.head = 20f * k; p.nSa = 20f * k; p.fSa = 15f * k
                p.nHa = -8f * k; p.fHa = 8f * k
            }
            SAnim.SIT -> {
                p.nHa = 88f; p.fHa = 88f; p.nKb = 85f; p.fKb = 85f
                p.nSa = 35f; p.nEb = 30f; p.fSa = 25f; p.fEb = 30f
                p.lean = 3f + w(t, 0.4f)
            }
            SAnim.SLEEP -> {
                p.rot = -90f; p.dx = 9f; p.nSa = 20f; p.fSa = 10f; p.nHa = 6f; p.fHa = -4f
                p.nKb = 18f; p.fKb = 24f; p.lean = 0f
            }
            SAnim.SCARED -> {
                val s = w(t, 9f) * 1.2f
                p.nSa = 120f; p.fSa = 120f; p.nEb = 100f; p.fEb = 100f
                p.nHa = 12f; p.fHa = -12f; p.nKb = 25f; p.fKb = 25f; p.dx = s; p.lean = -6f
            }
            SAnim.LAUGH -> {
                val b = w(t, 4f)
                p.lean = -8f + b * 6f; p.nSa = 40f; p.fSa = 40f; p.nEb = 70f; p.fEb = 70f
                p.lift = max(0f, b) * 2.5f; p.head = -12f
            }
            SAnim.THINK -> {
                p.nSa = 95f; p.nEb = 135f; p.fSa = 50f; p.fEb = 100f; p.head = 8f + w(t, 0.6f) * 3f
                p.lean = 3f; p.nHa = 4f; p.fHa = -4f
            }
            SAnim.FLEX -> {
                val e = ease(t / 0.3f); val b = w(t, 1.8f) * 0.5f + 0.5f
                p.nSa = 90f * e; p.fSa = 90f * e; p.nEb = (150f + 8f * b) * e; p.fEb = (150f + 8f * b) * e
                p.nHa = 10f; p.fHa = -10f
            }
            SAnim.FALL -> {
                val u = ease(t / 0.55f)
                p.rot = -90f * u; p.nSa = 150f * u; p.fSa = 110f * u; p.nHa = 20f * u; p.fHa = -15f * u
                p.nKb = 20f * u; p.dx = 9f * u
            }
            SAnim.SALUTE -> {
                val e = ease(t / 0.2f)
                p.nSa = 135f * e; p.nEb = 100f * e; p.fSa = 4f; p.nHa = 3f; p.fHa = -3f
            }
            SAnim.THUMBS -> {
                val e = ease(t / 0.2f); val b = w(t, 2f)
                p.nSa = 85f * e; p.nEb = 80f * e; p.fSa = 4f; p.thumb = true; p.lift = max(0f, b) * 1.5f
            }
            SAnim.FACEPALM -> {
                p.nSa = 110f; p.nEb = 130f; p.fSa = 6f; p.head = 14f; p.lean = 5f
                p.dx = w(t, 0.7f) * 0.5f
            }
            SAnim.BECKON -> {
                p.nSa = 100f; p.nEb = 20f + w(t, 2.4f) * 45f; p.fSa = 5f
            }
            SAnim.STOP -> {
                val e = ease(t / 0.2f)
                p.nSa = 100f * e; p.nEb = 5f; p.fSa = 4f; p.lean = -3f * e
            }
            SAnim.HEART -> {
                val b = w(t, 1.2f) * 0.5f + 0.5f
                p.nSa = 150f; p.fSa = 150f; p.nEb = 80f + 10f * b; p.fEb = 80f + 10f * b
            }
            SAnim.PEACE -> {
                val e = ease(t / 0.2f)
                p.nSa = 150f * e; p.nEb = 20f; p.fSa = 4f; p.head = -6f * e
            }
            SAnim.KISS -> {
                val u = (t / 1f).coerceIn(0f, 1f)
                p.nSa = 120f * (1f - u * 0.3f); p.nEb = 100f - 70f * u; p.head = -6f; p.lean = -3f
                p.lift = if (u > 0.5f) 2f else 0f
            }
            SAnim.HIGHFIVE -> {
                val e = ease(t / 0.25f)
                p.nSa = 165f * e; p.nEb = 0f; p.fSa = 4f; p.lean = 4f; p.nHa = 4f; p.fHa = -4f
            }
            SAnim.HUG -> {
                val e = ease(t / 0.4f)
                p.nSa = 80f * e; p.fSa = 80f * e; p.nEb = 30f * e; p.fEb = 30f * e; p.lean = 8f * e
                p.nHa = 6f; p.fHa = -6f
            }
            SAnim.PUSH -> {
                p.lean = 22f; p.nSa = 90f; p.fSa = 88f; p.nEb = 0f; p.fEb = 0f
                p.nHa = 30f + w(t, 2f) * 4f; p.fHa = -22f; p.nKb = 8f; p.fKb = 25f
            }
            SAnim.CARRIED -> {
                val s = w(t, 1.6f)
                p.nSa = 165f + s * 10f; p.fSa = 165f - s * 10f; p.nHa = s * 10f; p.fHa = -s * 10f
                p.nKb = 20f; p.fKb = 25f; p.dx = 0f; p.lift = 0f
            }
            SAnim.GETUP -> {
                val u = ease(t / 0.6f)
                p.dx = 9f * (1f - u); p.rot = -90f * (1f - u); p.nSa = 40f * (1f - u); p.fSa = 20f * (1f - u)
                p.lean = 12f * sin(u * 3.1416f)
            }
            SAnim.SPIN -> {
                val b = w(t, 1.6f)
                p.nSa = 90f; p.fSa = 90f; p.nEb = 0f; p.fEb = 0f; p.lift = 3f + abs(b) * 5f
                p.nHa = 8f; p.fHa = -8f
                p.front = b > 0f // التبديل يعطي إحساس الدوران
            }
            SAnim.NOD -> {
                val b = w(t, 2f)
                p.head = 12f * b; p.nSa = 4f; p.fSa = -4f; p.nEb = 8f; p.fEb = 8f
            }
            SAnim.LOOKAROUND -> {
                val b = w(t, 0.7f)
                p.head = 10f * b; p.nSa = 5f; p.fSa = -3f; p.nEb = 20f; p.fEb = 15f; p.dx = b * 0.8f
                p.nSa = 100f * max(0f, -b) * 0f + p.nSa
            }
            SAnim.TIRED -> {
                val b = w(t, 0.5f)
                p.lean = 10f + b * 2f; p.head = 14f; p.nSa = -2f; p.fSa = -6f; p.nEb = 5f; p.fEb = 5f
                p.nHa = 4f; p.fHa = -4f
            }
        }
    }

    private fun walk(t: Float, hz: Float, legAmp: Float, kneeAmp: Float, armK: Float, p: SPose) {
        val ph = w(t, hz)
        val ph2 = w(t, hz, 0.25f)
        val ph3 = w(t, hz, 0.5f)
        p.nHa = legAmp * ph; p.fHa = -legAmp * ph
        p.nKb = kneeAmp * max(0f, -w(t, hz, 0.25f) * 0f + sin((t * hz + 0.25f) * TAU))
        p.fKb = kneeAmp * max(0f, -sin((t * hz + 0.25f) * TAU))
        p.nSa = -legAmp * 0.8f * ph * armK; p.fSa = legAmp * 0.8f * ph * armK
        p.nEb = 20f + 14f * max(0f, ph2); p.fEb = 20f + 14f * max(0f, -ph2)
        p.lean = 4f; p.lift = 0f
    }
}
