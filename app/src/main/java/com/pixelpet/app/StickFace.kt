package com.pixelpet.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** 35 تعبيراً للوجه (من ورقة التعبيرات) + تعبير الحب. */
enum class SFace {
    HAPPY, SAD, ANGRY, CONFUSED, PLEASED,
    PAIN, SCARED, SERIOUS, SILLY, FLIRTY,
    NERVOUS, TIRED, SHOCKED, IRRITATED, WTF,
    RAGE, DISGUSTED, CONFIDENT, DRUNK, CONCERNED,
    CURIOUS, POUTY, SICK, LAUGHING, SHAME,
    INCREDULOUS, BORED, SNOOTY, COLD, CALM,
    CRYING, DEVIOUS, PENSIVE, EXCITED, TRIUMPH,
    LOVE, SLEEPING
}

/** وصف تعبير: شكل العين، انغلاق الجفن، ميل الجفن، الحاجب، الفم، علامة إضافية. */
private class FD(
    val eye: Int = E_OPEN, val lid: Float = 0f, val tilt: Float = 0f, val size: Float = 1f,
    val brow: Float = 0f, val browY: Float = 0f, val mouth: Int = 0, val mark: Int = 0,
    val pupil: Float = 0.5f, val look: Float = 0f, val lookY: Float = 0.1f,
    val asym: Boolean = false, val eye2: Int = -1, val size2: Float = 1f, val lid2: Float = -1f, val brow2: Float = 99f
)

private const val E_OPEN = 0; private const val E_UP = 1; private const val E_DN = 2; private const val E_LINE = 3
private const val E_GT = 4; private const val E_X = 5; private const val E_SPIRAL = 6; private const val E_STAR = 7
private const val E_HEART = 8; private const val E_DOT = 9; private const val E_TEAR = 10; private const val E_WIDE = 11

private const val M_NONE = 0; private const val M_SMILE = 1; private const val M_FROWN = 2; private const val M_O = 3
private const val M_FLAT = 4; private const val M_WAVY = 5; private const val M_GRIN = 6; private const val M_SMIRK = 7

private const val K_NONE = 0; private const val K_SWEAT = 1; private const val K_BLUSH = 2; private const val K_VEIN = 3
private const val K_SPARK = 4; private const val K_TEARS = 5; private const val K_BAGS = 6; private const val K_GREEN = 7
private const val K_SHADE = 8; private const val K_COLD = 9

object StickFaces {
    private val T = HashMap<SFace, FD>().apply {
        put(SFace.HAPPY, FD(E_UP, mouth = M_SMILE))
        put(SFace.SAD, FD(lid = 0.3f, tilt = -20f, brow = -20f, browY = -0.2f, mouth = M_FROWN, lookY = 0.3f))
        put(SFace.ANGRY, FD(lid = 0.4f, tilt = 22f, brow = 24f, mouth = M_FROWN))
        put(SFace.CONFUSED, FD(size = 1.2f, size2 = 0.8f, asym = true, brow = 0f, brow2 = -16f, browY = -0.3f, mouth = M_WAVY))
        put(SFace.PLEASED, FD(E_UP, brow = -6f, browY = -0.4f, mouth = M_SMILE, mark = K_BLUSH))
        put(SFace.PAIN, FD(E_GT, mouth = M_O, mark = K_SWEAT, brow = -16f))
        put(SFace.SCARED, FD(E_WIDE, size = 1.25f, pupil = 0.22f, brow = -12f, browY = -0.5f, mouth = M_WAVY, mark = K_SWEAT))
        put(SFace.SERIOUS, FD(lid = 0.35f, tilt = 6f, brow = 8f, mouth = M_FLAT))
        put(SFace.SILLY, FD(E_DOT, size = 1.3f, size2 = 0.7f, asym = true, mouth = M_SMIRK))
        put(SFace.FLIRTY, FD(lid = 0.42f, tilt = -6f, brow = -6f, browY = -0.5f, mouth = M_SMIRK, mark = K_BLUSH, pupil = 0.6f))
        put(SFace.NERVOUS, FD(size = 1.2f, pupil = 0.28f, brow = -14f, mouth = M_WAVY, mark = K_SWEAT, look = -0.4f))
        put(SFace.TIRED, FD(lid = 0.6f, mouth = M_FLAT, mark = K_BAGS, lookY = 0.3f))
        put(SFace.SHOCKED, FD(E_WIDE, size = 1.3f, pupil = 0.2f, brow = -8f, browY = -0.7f, mouth = M_O))
        put(SFace.IRRITATED, FD(lid = 0.45f, tilt = 12f, brow = 14f, mouth = M_FLAT, mark = K_VEIN))
        put(SFace.WTF, FD(E_DOT, size = 0.9f, size2 = 1.15f, lid2 = 0.4f, asym = true, brow = 14f, brow2 = -16f, mouth = M_WAVY))
        put(SFace.RAGE, FD(lid = 0.5f, tilt = 32f, brow = 34f, mouth = M_GRIN, mark = K_VEIN, pupil = 0.35f))
        put(SFace.DISGUSTED, FD(lid = 0.55f, tilt = -4f, brow = 6f, mouth = M_WAVY, mark = K_GREEN))
        put(SFace.CONFIDENT, FD(E_DN, mouth = M_SMIRK, mark = K_SPARK))
        put(SFace.DRUNK, FD(E_SPIRAL, mouth = M_WAVY, mark = K_BLUSH))
        put(SFace.CONCERNED, FD(size = 1.1f, brow = -18f, browY = -0.3f, mouth = M_WAVY, mark = K_SHADE))
        put(SFace.CURIOUS, FD(size = 1.25f, brow = 0f, brow2 = -14f, browY = -0.5f, asym = true, mouth = M_SMILE, lookY = -0.2f, pupil = 0.55f))
        put(SFace.POUTY, FD(lid = 0.4f, tilt = 6f, brow = 6f, mouth = M_FROWN, mark = K_BLUSH, lookY = 0.3f))
        put(SFace.SICK, FD(lid = 0.4f, tilt = -8f, mouth = M_WAVY, mark = K_GREEN, brow = -8f))
        put(SFace.LAUGHING, FD(E_GT, mouth = M_GRIN, mark = K_TEARS))
        put(SFace.SHAME, FD(size = 0.9f, pupil = 0.3f, brow = -10f, mouth = M_WAVY, mark = K_SHADE, lookY = 0.4f))
        put(SFace.INCREDULOUS, FD(lid = 0.5f, tilt = 8f, brow = 10f, size2 = 1.2f, lid2 = 0f, brow2 = -16f, asym = true, mouth = M_FLAT))
        put(SFace.BORED, FD(lid = 0.55f, mouth = M_FLAT))
        put(SFace.SNOOTY, FD(lid = 0.42f, tilt = -4f, brow = -10f, browY = -0.4f, mouth = M_FROWN, look = 0.9f))
        put(SFace.COLD, FD(E_DOT, size = 1.1f, mouth = M_WAVY, mark = K_COLD, brow = -8f))
        put(SFace.CALM, FD(mouth = M_SMILE))
        put(SFace.CRYING, FD(E_TEAR, brow = -20f, browY = -0.3f, mouth = M_O, mark = K_TEARS))
        put(SFace.DEVIOUS, FD(lid = 0.38f, tilt = 18f, brow = 22f, pupil = 0.35f, mouth = M_SMIRK))
        put(SFace.PENSIVE, FD(lid = 0.3f, brow = -5f, mouth = M_FLAT, look = 0.5f, lookY = -0.3f))
        put(SFace.EXCITED, FD(E_STAR, mouth = M_GRIN, mark = K_SPARK))
        put(SFace.TRIUMPH, FD(lid = 0.4f, tilt = 10f, brow = 14f, brow2 = -10f, asym = true, mouth = M_GRIN, mark = K_SPARK))
        put(SFace.LOVE, FD(E_HEART, mouth = M_SMILE, mark = K_BLUSH))
        put(SFace.SLEEPING, FD(E_LINE, mouth = M_NONE))
    }

    // شخصية افتراضية لكل لون في الوقت العادي
    private val base = arrayOf(SFace.CONFIDENT, SFace.CALM, SFace.HAPPY, SFace.CURIOUS, SFace.DEVIOUS)

    /** يختار التعبير المناسب لحالة الشخصية: الرمز أولاً ثم الحركة. */
    fun pick(a: SAnim, e: SEmote, id: Int): SFace {
        when (a) {
            SAnim.KOED -> return SFace.DRUNK
            SAnim.FLYBACK -> return SFace.PAIN
            SAnim.HIT -> return SFace.PAIN
            SAnim.SLEEP -> return SFace.SLEEPING
            else -> {}
        }
        when (e) {
            SEmote.EXCL -> return if (a == SAnim.SCARED) SFace.SCARED else SFace.SHOCKED
            SEmote.QUEST -> return SFace.CONFUSED
            SEmote.ANGER -> return if (a == SAnim.CHARGE || a == SAnim.FSTANCE) SFace.RAGE else SFace.ANGRY
            SEmote.SWEAT -> return if (a == SAnim.TIRED) SFace.TIRED else SFace.NERVOUS
            SEmote.HEART -> return if (a == SAnim.KISS || a == SAnim.HEART) SFace.LOVE else SFace.PLEASED
            SEmote.STAR -> return if (a == SAnim.VICTORY || a == SAnim.FLEX) SFace.TRIUMPH else SFace.EXCITED
            SEmote.BULB -> return SFace.EXCITED
            SEmote.NOTE -> return SFace.HAPPY
            SEmote.ZZZ -> return SFace.SLEEPING
            SEmote.CHECK -> return SFace.CONFIDENT
            else -> {}
        }
        return when (a) {
            SAnim.WAVE, SAnim.DANCE, SAnim.PEACE -> SFace.HAPPY
            SAnim.CHEER, SAnim.HIGHFIVE -> SFace.EXCITED
            SAnim.LAUGH -> SFace.LAUGHING
            SAnim.SCARED -> SFace.SCARED
            SAnim.SHRUG -> SFace.CONFUSED
            SAnim.FACEPALM -> SFace.SHAME
            SAnim.KISS, SAnim.HEART -> SFace.LOVE
            SAnim.THINK -> SFace.PENSIVE
            SAnim.FLEX, SAnim.VICTORY -> SFace.TRIUMPH
            SAnim.THUMBS, SAnim.SALUTE -> SFace.CONFIDENT
            SAnim.BOW, SAnim.HUG, SAnim.CLAP -> SFace.PLEASED
            SAnim.TIRED, SAnim.SIT -> SFace.TIRED
            SAnim.MINE, SAnim.PUSH, SAnim.RUN, SAnim.STOP, SAnim.POINT -> SFace.SERIOUS
            SAnim.FSTANCE, SAnim.SHUFFLE -> if (id % 2 == 0) SFace.SERIOUS else SFace.DEVIOUS
            SAnim.WINDUP, SAnim.LUNGE, SAnim.SLASH, SAnim.KICK, SAnim.SLAM -> SFace.ANGRY
            SAnim.CHARGE, SAnim.BEAM, SAnim.SPINATK -> SFace.RAGE
            SAnim.BLOCK -> SFace.IRRITATED
            SAnim.FALL, SAnim.CARRIED -> SFace.SHOCKED
            SAnim.GETUP -> SFace.SILLY
            SAnim.BECKON -> SFace.FLIRTY
            SAnim.LOOKAROUND, SAnim.NOD -> SFace.CURIOUS
            else -> base[id % 5]
        }
    }

    private val paint = Paint().apply { isAntiAlias = true; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()
    private val oval = RectF()
    private val ink = Color.parseColor("#20243A")
    private val white = Color.parseColor("#FFFFFF")

    /**
     * يرسم الوجه داخل دائرة الرأس. الإحداثيات محلية للرأس: +x للأمام، الوحدة = وحدة الشخصية.
     * [angle] ميل الرأس بالدرجات، [front] منظر أمامي، [blink] رمشة، [t] زمن للتحريك الخفيف.
     */
    fun draw(c: Canvas, face: SFace, hx: Float, hy: Float, angle: Float, r: Float, front: Boolean, blink: Boolean, t: Float, headCol: Int) {
        val f = T[face] ?: return
        c.save()
        c.translate(hx, hy)
        c.rotate(angle)
        val sc = r / 6.3f
        c.scale(sc, sc)
        val xc = if (front) 0f else 3.3f
        val sp = if (front) 3.5f else 3.5f
        val ey = -0.5f
        val closedAll = blink || face == SFace.SLEEPING

        // لون الوجه (مرض/برد) فوق الرأس
        if (f.mark == K_GREEN || f.mark == K_COLD) {
            paint.style = Paint.Style.FILL
            paint.color = if (f.mark == K_GREEN) Color.argb(90, 120, 200, 60) else Color.argb(80, 120, 190, 255)
            c.drawCircle(0f, 0f, 6.3f, paint)
        }

        for (i in 0..1) {
            val s = if (i == 0) -1f else 1f
            val x = xc + s * sp / 2f
            val second = i == 1 && f.asym
            val sz = if (second) f.size2 else f.size
            val lid = if (second && f.lid2 >= 0f) f.lid2 else f.lid
            val kind = if (second && f.eye2 >= 0) f.eye2 else f.eye
            val bAng = if (second && f.brow2 < 90f) f.brow2 else f.brow
            val kindUse = if (closedAll && (kind == E_OPEN || kind == E_WIDE || kind == E_TEAR || kind == E_DOT)) E_LINE else kind
            eye(c, kindUse, x, ey, sz, lid, f.tilt * -s * -1f * (if (i == 0) 1f else -1f) * -1f, f, s, t)
            if (!closedAll && bAng != 0f || (closedAll && bAng != 0f)) brow(c, x, ey - 1.9f * sz + f.browY - 0.8f, s, bAng)
        }
        mouth(c, f.mouth, xc, 2.7f, t)
        mark(c, f.mark, xc, ey, sp, t)
        c.restore()
    }

    private fun eye(c: Canvas, kind: Int, x: Float, y: Float, sz: Float, lid: Float, tiltIn: Float, f: FD, s: Float, t: Float) {
        // الميل: الطرف الداخلي (نحو مركز الوجه) ينزل للغاضب (tilt > 0)
        val tilt = f.tilt * (-s)       // s=-1 (يسار): الداخلي يمين → ميل موجب
        val a = 1.45f * sz; val b = 1.8f * sz
        paint.strokeWidth = 0.55f
        paint.style = Paint.Style.STROKE
        paint.color = ink
        when (kind) {
            E_UP -> { arc(c, x, y + 0.5f, a * 1.05f, b * 0.9f, true) }
            E_DN -> { arc(c, x, y - 0.1f, a * 1.05f, b * 0.9f, false) }
            E_LINE -> { paint.strokeWidth = 0.75f; c.drawLine(x - a, y + 0.3f, x + a, y + 0.3f, paint) }
            E_GT -> {
                paint.strokeWidth = 0.8f
                val d = a * 0.62f
                if (s < 0) { c.drawLine(x - d, y - d, x + d, y, paint); c.drawLine(x + d, y, x - d, y + d, paint) }
                else { c.drawLine(x + d, y - d, x - d, y, paint); c.drawLine(x - d, y, x + d, y + d, paint) }
            }
            E_X -> {
                paint.strokeWidth = 0.8f; val d = a * 0.9f
                c.drawLine(x - d, y - d, x + d, y + d, paint); c.drawLine(x + d, y - d, x - d, y + d, paint)
            }
            E_SPIRAL -> {
                paint.strokeWidth = 0.55f; path.reset()
                var ang = t * 8f * (if (s < 0) 1f else -1f)
                var rr = 0.15f
                path.moveTo(x + cos(ang) * rr, y + sin(ang) * rr)
                for (k in 0 until 26) { ang += 0.55f * (if (s < 0) 1f else -1f); rr += 0.07f * sz; path.lineTo(x + cos(ang) * rr, y + sin(ang) * rr) }
                c.drawPath(path, paint)
            }
            E_STAR -> {
                paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#FFEB3B")
                path.reset()
                for (k in 0 until 10) {
                    val ang = -1.5708f + k * 0.62832f; val rr = if (k % 2 == 0) 1.9f else 0.8f
                    val px = x + cos(ang) * rr; val py = y + sin(ang) * rr
                    if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close(); c.drawPath(path, paint)
                paint.style = Paint.Style.STROKE; paint.color = ink; paint.strokeWidth = 0.35f; c.drawPath(path, paint)
            }
            E_HEART -> {
                paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#F44336")
                val k = 1.45f * (1f + 0.1f * sin(t * 9f))
                path.reset()
                path.moveTo(x, y + 1.0f * k)
                path.cubicTo(x - 1.7f * k, y - 0.1f * k, x - 0.9f * k, y - 1.2f * k, x, y - 0.55f * k)
                path.cubicTo(x + 0.9f * k, y - 1.2f * k, x + 1.7f * k, y - 0.1f * k, x, y + 1.0f * k)
                path.close(); c.drawPath(path, paint)
            }
            else -> openEye(c, kind, x, y, a, b, lid, tilt, f)
        }
    }

    private fun arc(c: Canvas, x: Float, y: Float, w: Float, h: Float, up: Boolean) {
        paint.strokeWidth = 0.8f
        path.reset()
        val n = 8
        for (i in 0..n) {
            val th = 3.1416f * i / n
            val px = x - w + 2f * w * i / n
            val py = y + (if (up) -sin(th) else sin(th)) * h
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        c.drawPath(path, paint)
    }

    private fun openEye(c: Canvas, kind: Int, x: Float, y: Float, a: Float, b: Float, lid: Float, tilt: Float, f: FD) {
        val big = kind == E_WIDE
        val aa = if (big) a * 1.1f else a
        val bb = if (big) b * 1.15f else b
        val slope = tilt * 0.01745f * 1.1f
        // مضلع العين: قطع ناقص مقصوص من الأعلى بخط الجفن المائل
        path.reset()
        val n = 14
        val lidBase = -bb + 2f * bb * lid
        for (i in 0..n) {
            val th = 6.2832f * i / n
            val ex = cos(th) * aa; var ey2 = sin(th) * bb
            val lineY = lidBase + slope * ex
            if (ey2 < lineY) ey2 = lineY
            val px = x + ex; val py = y + ey2
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        paint.style = Paint.Style.FILL; paint.color = white; c.drawPath(path, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.4f; paint.color = ink; c.drawPath(path, paint)
        if (kind == E_DOT) {
            // عين صغيرة جداً (نقطة)
            paint.style = Paint.Style.FILL; paint.color = ink; c.drawCircle(x, y + 0.2f, a * 0.55f, paint)
            return
        }
        // الحدقة
        val visHalf = (bb - lidBase) / 2f
        val pr = minOf(bb * f.pupil * 0.95f, visHalf * 0.8f).coerceAtLeast(0.45f)
        val px = x + f.look * (aa - pr).coerceAtLeast(0f) * 0.8f
        val pyc = y + lidBase + visHalf + f.lookY * visHalf * 0.5f
        val py = pyc.coerceIn(y + lidBase + pr * 0.6f, y + bb - pr * 0.6f)
        paint.style = Paint.Style.FILL; paint.color = ink
        c.drawCircle(px, py, pr, paint)
        paint.color = white; c.drawCircle(px - pr * 0.3f, py - pr * 0.3f, pr * 0.28f, paint)
        // حد الجفن العلوي أثقل
        if (lid > 0.05f || abs(tilt) > 1f) {
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.55f; paint.color = ink
            c.drawLine(x - aa, lidBase - slope * aa, x + aa, lidBase + slope * aa, paint)
        }
        if (kind == E_TEAR) {
            paint.style = Paint.Style.FILL; paint.color = Color.argb(150, 80, 190, 255)
            c.drawCircle(x, y + bb * 0.65f, aa * 0.5f, paint)
        }
    }

    private fun brow(c: Canvas, x: Float, y: Float, s: Float, ang: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.55f; paint.color = ink
        val a = ang * 0.01745f * (-s)   // الطرف الداخلي ينزل عند الزاوية الموجبة
        val dx = 1.2f
        c.drawLine(x - dx, y - sin(a) * dx, x + dx, y + sin(a) * dx, paint)
    }

    private fun mouth(c: Canvas, m: Int, x: Float, y: Float, t: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.65f; paint.color = ink
        when (m) {
            M_SMILE -> { path.reset(); path.moveTo(x - 1.5f, y - 0.2f); path.quadTo(x, y + 1.5f, x + 1.5f, y - 0.2f); c.drawPath(path, paint) }
            M_FROWN -> { path.reset(); path.moveTo(x - 1.4f, y + 0.9f); path.quadTo(x, y - 0.6f, x + 1.4f, y + 0.9f); c.drawPath(path, paint) }
            M_FLAT -> c.drawLine(x - 1.3f, y + 0.4f, x + 1.3f, y + 0.4f, paint)
            M_WAVY -> {
                path.reset(); path.moveTo(x - 1.6f, y + 0.4f)
                path.lineTo(x - 0.8f, y - 0.1f); path.lineTo(x, y + 0.6f); path.lineTo(x + 0.8f, y - 0.1f); path.lineTo(x + 1.6f, y + 0.4f)
                c.drawPath(path, paint)
            }
            M_O -> { paint.style = Paint.Style.FILL; oval.set(x - 0.8f, y - 0.2f, x + 0.8f, y + 1.6f); c.drawOval(oval, paint) }
            M_GRIN -> {
                paint.style = Paint.Style.FILL; path.reset()
                path.moveTo(x - 1.9f, y - 0.2f); path.lineTo(x + 1.9f, y - 0.2f)
                path.quadTo(x + 1.5f, y + 2.1f, x, y + 2.1f); path.quadTo(x - 1.5f, y + 2.1f, x - 1.9f, y - 0.2f)
                path.close(); c.drawPath(path, paint)
                paint.color = Color.parseColor("#FFFFFF"); c.drawRect(x - 1.5f, y - 0.2f, x + 1.5f, y + 0.5f, paint)
            }
            M_SMIRK -> { path.reset(); path.moveTo(x - 1.3f, y + 0.5f); path.quadTo(x + 0.3f, y + 0.9f, x + 1.8f, y - 0.4f); c.drawPath(path, paint) }
        }
    }

    private fun mark(c: Canvas, k: Int, xc: Float, ey: Float, sp: Float, t: Float) {
        when (k) {
            K_BLUSH -> {
                paint.style = Paint.Style.FILL; paint.color = Color.argb(150, 255, 105, 135)
                oval.set(xc - sp - 1.1f, ey + 1.6f, xc - sp + 1.3f, ey + 2.8f); c.drawOval(oval, paint)
                oval.set(xc + sp - 1.3f, ey + 1.6f, xc + sp + 1.1f, ey + 2.8f); c.drawOval(oval, paint)
            }
            K_SWEAT -> {
                paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#4FC3F7")
                val dy = (t * 6f) % 2f * 0.5f
                path.reset(); path.moveTo(4.6f, -4.2f + dy); path.quadTo(6.0f, -2.2f + dy, 4.6f, -1.6f + dy); path.quadTo(3.2f, -2.2f + dy, 4.6f, -4.2f + dy)
                c.drawPath(path, paint)
            }
            K_VEIN -> {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.6f; paint.color = Color.parseColor("#E53935")
                val x = 3.6f; val y = -4.2f
                c.drawLine(x - 0.5f, y - 1.2f, x - 0.5f, y - 0.2f, paint); c.drawLine(x + 0.5f, y - 1.2f, x + 0.5f, y - 0.2f, paint)
                c.drawLine(x - 1.4f, y - 0.7f, x - 0.3f, y - 0.7f, paint); c.drawLine(x + 0.3f, y - 0.7f, x + 1.4f, y - 0.7f, paint)
            }
            K_SPARK -> {
                paint.style = Paint.Style.FILL; paint.color = Color.WHITE
                val x = xc + sp * 0.9f + 1.0f; val y = ey - 2.2f
                val g = 0.8f + 0.25f * sin(t * 8f)
                path.reset(); path.moveTo(x, y - 1.5f * g); path.lineTo(x + 0.4f, y - 0.4f); path.lineTo(x + 1.5f * g, y); path.lineTo(x + 0.4f, y + 0.4f)
                path.lineTo(x, y + 1.5f * g); path.lineTo(x - 0.4f, y + 0.4f); path.lineTo(x - 1.5f * g, y); path.lineTo(x - 0.4f, y - 0.4f); path.close()
                c.drawPath(path, paint)
            }
            K_TEARS -> {
                paint.style = Paint.Style.FILL; paint.color = Color.argb(200, 90, 200, 255)
                val dy = (t * 7f) % 2f
                oval.set(xc - sp / 2f - 0.5f, ey + 1.4f + dy * 0.4f, xc - sp / 2f + 0.5f, ey + 2.6f + dy * 0.4f); c.drawOval(oval, paint)
                oval.set(xc + sp / 2f - 0.5f, ey + 1.4f + dy * 0.4f, xc + sp / 2f + 0.5f, ey + 2.6f + dy * 0.4f); c.drawOval(oval, paint)
            }
            K_BAGS -> {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.4f; paint.color = Color.argb(160, 50, 40, 80)
                c.drawLine(xc - sp / 2f - 1f, ey + 2.0f, xc - sp / 2f + 1f, ey + 2.0f, paint)
                c.drawLine(xc + sp / 2f - 1f, ey + 2.0f, xc + sp / 2f + 1f, ey + 2.0f, paint)
            }
            K_SHADE -> {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.35f; paint.color = Color.argb(150, 40, 30, 70)
                for (i in 0 until 3) {
                    c.drawLine(xc - sp / 2f - 1f + i * 0.7f, ey + 1.5f, xc - sp / 2f - 1.2f + i * 0.7f, ey + 2.8f, paint)
                    c.drawLine(xc + sp / 2f - 0.4f + i * 0.7f, ey + 1.5f, xc + sp / 2f - 0.6f + i * 0.7f, ey + 2.8f, paint)
                }
            }
        }
    }
}
