package com.pixelpet.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/** الإشارات والحركات المتاحة لعيدان الشخصيات. كل واحدة دالة وضعية بدون أي كلام. */
enum class SAnim(val front: Boolean = false, val loop: Boolean = true, val snappy: Boolean = false) {
    IDLE, WALK, RUN, WAVE(true), JUMP(false, false), CHEER(true), DANCE(true), MINE, POINT,
    SHRUG(true), CLAP(true), BOW(false, false), SIT, SLEEP, SCARED(true), LAUGH(true), THINK,
    FLEX(true), FALL(false, false), SALUTE(true), THUMBS(true), FACEPALM, BECKON(true), STOP(true),
    HEART(true), PEACE(true), KISS(true), HIGHFIVE, HUG, PUSH, CARRIED(true), GETUP(false, false),
    SPIN(true), NOD, LOOKAROUND, TIRED,
    // ── قتال ──
    FSTANCE, SHUFFLE, WINDUP(false, false, true), LUNGE(false, false, true), SLASH(false, false, true),
    KICK(false, false, true), HIT(false, false, true), BLOCK(false, false, true), FLYBACK(false, false, true),
    KOED, CHARGE, BEAM(false, false, true), SLAM(false, false, true), VICTORY, SPINATK(true, true, true),
    // ── سينمائية: ترقّب ثم ضربة ثم متابعة، وحركات استعراض الجسد ──
    PROWL, TENSE, BACKFLIP(false, false, true), DODGE(false, false, true), COMBO(false, false, true),
    SLIDE(false, false, true), HANDSTAND, MEDITATE, PAT(true), RISE(false, false)
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

            SAnim.FSTANCE, SAnim.SHUFFLE -> {
                val b = w(t, 1.7f)
                val sh = if (a == SAnim.SHUFFLE) w(t, 4.2f) * 9f else 0f
                p.lean = 9f + b * 1.2f; p.nHa = 28f + sh; p.nKb = 26f; p.fHa = -22f - sh; p.fKb = 22f
                p.nSa = 78f + b * 3f; p.nEb = 92f; p.fSa = 66f; p.fEb = 104f
            }
            SAnim.WINDUP -> {
                val u = ease(t / 0.3f)
                p.nSa = 165f * u + 78f * (1f - u); p.nEb = 25f * u + 92f * (1f - u)
                p.fSa = 66f * (1f - u) + 40f * u; p.fEb = 100f
                p.lean = 9f - 19f * u; p.nHa = 22f; p.nKb = 30f; p.fHa = -18f; p.fKb = 26f
            }
            SAnim.LUNGE -> {
                p.nSa = 150f; p.nEb = 10f; p.fSa = -30f; p.fEb = 10f
                p.lean = 26f; p.nHa = 42f; p.nKb = 0f; p.fHa = -38f; p.fKb = 34f; p.lift = 2f
            }
            SAnim.SLASH -> {
                val u = ease(t / 0.2f)
                p.nSa = 165f - 115f * u; p.nEb = 25f * (1f - u); p.fSa = 40f + 40f * u; p.fEb = 60f
                p.lean = -6f + 30f * u; p.nHa = 36f; p.nKb = 8f; p.fHa = -28f; p.fKb = 24f
            }
            SAnim.KICK -> {
                val u = sin(3.1416f * (t / 0.42f).coerceIn(0f, 1f))
                p.nHa = 104f * u; p.nKb = 55f * (1f - u); p.fHa = -6f; p.fKb = 8f
                p.lean = -16f * u; p.nSa = 60f * u; p.fSa = -35f * u; p.nEb = 30f; p.fEb = 30f
            }
            SAnim.HIT -> {
                val k = 1f - 0.6f * ease(t / 0.5f)
                p.lean = -30f * k; p.head = -26f * k; p.nSa = 135f * k; p.fSa = 95f * k; p.nEb = 30f; p.fEb = 30f
                p.nKb = 22f * k; p.nHa = -8f * k; p.fHa = 10f * k
            }
            SAnim.BLOCK -> {
                p.nSa = 108f; p.nEb = 118f; p.fSa = 102f; p.fEb = 112f; p.lean = -5f
                p.nHa = 26f; p.nKb = 24f; p.fHa = -26f; p.fKb = 20f
                p.dx = if (t < 0.18f) w(t, 22f) * 0.8f else 0f
            }
            SAnim.FLYBACK -> {
                val u = (t / 0.8f).coerceIn(0f, 1f)
                p.lift = 4f * u * (1f - u) * 17f
                p.rot = -90f * ease(u) - 28f * sin(3.1416f * u)
                p.nSa = 150f; p.fSa = 120f; p.nHa = 24f; p.fHa = -16f; p.nKb = 20f; p.dx = 9f * u
            }
            SAnim.KOED -> {
                p.rot = -90f; p.dx = 9f; p.nSa = 100f; p.fSa = 70f; p.nHa = 14f; p.fHa = -10f
                p.nKb = 16f; p.fKb = 30f; p.lean = w(t, 0.5f) * 1.2f
            }
            SAnim.CHARGE -> {
                p.nHa = 30f; p.nKb = 56f; p.fHa = -28f; p.fKb = 50f; p.lean = 14f
                p.nSa = 26f; p.nEb = 40f; p.fSa = 18f; p.fEb = 40f; p.dx = w(t, 26f) * 0.8f
            }
            SAnim.BEAM -> {
                val k = ease(t / 0.15f)
                p.lean = -10f * k + w(t, 20f) * 1.4f; p.nSa = 92f * k; p.fSa = 95f * k; p.nEb = -2f; p.fEb = -2f
                p.nHa = 38f; p.fHa = -32f; p.nKb = 10f; p.fKb = 20f; p.dx = w(t, 27f) * 0.6f
            }
            SAnim.SLAM -> {
                val u = (t / 0.62f).coerceIn(0f, 1f)
                p.lift = 30f * sin(3.1416f * u)
                val down = ease((u - 0.72f) / 0.28f)
                p.nSa = 172f - 118f * down; p.nEb = 8f; p.fSa = 150f - 90f * down; p.fEb = 20f
                p.nHa = 40f; p.nKb = 62f * (1f - down); p.fHa = -10f; p.fKb = 70f * (1f - down)
                p.lean = -6f + 34f * down
            }
            SAnim.VICTORY -> {
                val b = w(t, 1.3f)
                p.nSa = 172f; p.nEb = 4f; p.fSa = 24f; p.fEb = 20f; p.lean = -7f
                p.lift = max(0f, b) * 3f; p.nHa = 10f; p.fHa = -10f
            }
            SAnim.SPINATK -> {
                val b = w(t, 7f)
                p.nSa = 90f; p.fSa = 90f; p.nEb = 0f; p.fEb = 0f; p.lift = 5f
                p.nHa = 20f; p.fHa = -20f; p.nKb = 30f; p.fKb = 30f; p.front = b > 0f
            }
            SAnim.TIRED -> {
                val b = w(t, 0.5f)
                p.lean = 10f + b * 2f; p.head = 14f; p.nSa = -2f; p.fSa = -6f; p.nEb = 5f; p.fEb = 5f
                p.nHa = 4f; p.fHa = -4f
            }

            // ── حركات سينمائية: المعنى من الجسد وحده (ترقّب → ضربة → متابعة) ──
            SAnim.PROWL -> { // تسلل منخفض بطيء: ترقّب قبل الغارة
                val b = w(t, 1.1f)
                p.lean = 18f + b * 3f; p.nHa = 34f + b * 10f; p.nKb = 58f; p.fHa = -30f; p.fKb = 64f
                p.nSa = 30f - b * 8f; p.nEb = 70f; p.fSa = 22f + b * 8f; p.fEb = 82f
                p.dx = w(t, 2.2f) * 1.2f
            }
            SAnim.TENSE -> { // قوة مشدودة: قبضتان ترتجفان، رقبة مفتوحة
                val s = w(t, 11f)
                p.nSa = 96f; p.fSa = 96f; p.nEb = 128f + s * 4f; p.fEb = 128f - s * 4f
                p.lean = 6f; p.nHa = 20f; p.fHa = -20f; p.nKb = 16f; p.fKb = 16f
            }
            SAnim.BACKFLIP -> { // شقلبة خلفية كاملة حول الورك
                val u = (t / 0.62f).coerceIn(0f, 1f)
                p.rot = -360f * ease(u)
                p.lift = 22f * sin(3.1416f * u)
                p.nSa = 150f * (1f - u) + 20f * u; p.fSa = p.nSa
                p.nKb = 70f * (1f - abs(2f * u - 1f)); p.fKb = p.nKb
                p.nHa = 18f; p.fHa = -18f
            }
            SAnim.DODGE -> { // تلقاء سريع للخلف: خطوة واحدة ملتوية
                val u = (t / 0.28f).coerceIn(0f, 1f)
                val k = sin(3.1416f * u)
                p.lean = -34f * k; p.dx = -10f * k
                p.nSa = 60f * k + 20f * (1f - k); p.fSa = 40f * k
                p.nEb = 80f; p.fEb = 90f; p.nHa = 24f; p.fKb = 40f * k + 12f
                p.head = -10f * k
            }
            SAnim.COMBO -> { // ضربتان متتاليتان: وجهة ثم عرضية
                val u = (t / 0.5f).coerceIn(0f, 1f)
                val swing = if (u < 0.5f) ease(u * 4f) else ease((1f - u) * 4f)
                val v = if (u < 0.5f) 1f else 0.6f
                p.nSa = 150f - 110f * swing * v; p.nEb = 24f * (1f - swing)
                p.fSa = 40f + 50f * swing * v; p.lean = -4f + 26f * swing
                p.nHa = 34f; p.nKb = 10f; p.fHa = -26f; p.fKb = 26f
            }
            SAnim.SLIDE -> { // انزلاق على الورك بقدم ممدودة
                p.rot = -68f; p.dx = 6f
                p.fHa = 78f; p.fKb = 4f; p.nHa = 30f; p.nKb = 66f
                p.nSa = 30f; p.fSa = 130f; p.fEb = 40f; p.lean = -6f
            }
            SAnim.HANDSTAND -> { // وقوف على اليدين: جسم مقلوب ثابت مع توازن طفيف
                p.rot = 180f; p.nSa = 172f; p.fSa = 172f; p.nEb = 4f; p.fEb = 4f
                p.nHa = 6f + w(t, 1.3f) * 3f; p.fHa = -6f - w(t, 1.3f) * 3f
                p.head = -6f
            }
            SAnim.MEDITATE -> { // جلوس التأمل: أرجل متشابكة ويانعة على الركبتين
                p.nHa = 96f; p.fHa = 96f; p.nKb = 108f; p.fKb = 108f
                p.nSa = 26f; p.nEb = 58f; p.fSa = 22f; p.fEb = 58f
                p.lean = 1f + w(t, 0.3f) * 0.8f; p.head = -4f + w(t, 0.3f) * 2f
            }
            SAnim.PAT -> { // ربّة على الكتف: يد قريبة تنخفض بلطف بإيقاع
                val b = w(t, 2.6f) * 0.5f + 0.5f
                p.nSa = 96f; p.nEb = 96f - 14f * b; p.fSa = 4f
                p.lean = 7f; p.head = 5f; p.nHa = 6f; p.fHa = -4f
            }
            SAnim.RISE -> { // نهوض بطولي: يبدأ راكعاً ويرتفع مع فتح الصدر
                val u = ease(t / 0.9f)
                p.lean = 34f * (1f - u) - 6f * u; p.head = 12f * (1f - u) - 8f * u
                p.nSa = 60f * (1f - u) + 10f * u; p.fSa = 55f * (1f - u) - 6f * u
                p.nEb = 40f * (1f - u) + 6f; p.fEb = p.nEb
                p.nHa = 40f * (1f - u) + 4f * u; p.nKb = 60f * (1f - u)
                p.fHa = -40f * (1f - u) - 4f * u; p.fKb = 60f * (1f - u)
                p.lift = 1.5f * sin(3.1416f * u)
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
