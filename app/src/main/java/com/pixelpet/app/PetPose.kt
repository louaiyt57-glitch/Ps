package com.pixelpet.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.cos

/**
 * وضعية الشخصية في لحظة معينة. كل القيم بوحدات الشبكة البكسلية (unit) أو بالدرجات.
 * الزوايا: 0 = الذراع متدلية، موجب = للخارج/للأعلى، سالب = نحو الجسم (لحمل الأغراض).
 */
class Pose {
    var dx = 0f; var dy = 0f            // إزاحة الجسم (dy موجب = للأسفل)
    var lean = 0f                       // ميلان الجسم (درجات، موجب = نحو اتجاه النظر)
    var headTilt = 0f                   // ميلان الرأس
    var look = 0f; var lookY = 0f       // اتجاه النظر (-1..1)
    var armL = 0f; var armR = 0f        // زاويتا الذراعين
    var legL = 0f; var legR = 0f        // رفع الرجلين
    var legSpread = 0f                  // تباعد الرجلين
    var side = false                    // true = منظر جانبي (للمشي والركض)
    var swing = 0f                      // تأرجح الرجلين للأمام/الخلف في المنظر الجانبي
    var squash = 1f                     // انضغاط الجسم رأسياً
    var lieAmt = 0f                     // 0 = واقف ... 1 = مستلقٍ على السرير
    var sit = false                     // جلوس
    var face: Face? = null              // تعبير يتجاوز تعبير الحالة
    var chew = 0f                       // مضغ (حركة الفم)
    var propT = 0f                      // معامل الغرض (قضمات/رمية/تقليب...)
    var blush = 0f                      // احمرار الوجه 0..1

    fun reset() {
        dx = 0f; dy = 0f; lean = 0f; headTilt = 0f; look = 0f; lookY = 0f
        armL = 0f; armR = 0f; legL = 0f; legR = 0f; legSpread = 0f
        squash = 1f; lieAmt = 0f; sit = false; face = null; side = false; swing = 0f
        chew = 0f; propT = 0f; blush = 0f
    }
}

object PetPose {
    private const val HOLD = -52f   // وضع حمل الأغراض أمام الصدر

    private fun kf(t: Float, vararg k: Pair<Float, Float>): Float {
        if (t <= k.first().first) return k.first().second
        for (i in 1 until k.size) {
            val (t1, v1) = k[i]
            if (t <= t1) {
                val (t0, v0) = k[i - 1]
                return v0 + (v1 - v0) * ((t - t0) / (t1 - t0))
            }
        }
        return k.last().second
    }

    private fun smooth(x: Float): Float { val c = x.coerceIn(0f, 1f); return c * c * (3 - 2 * c) }

    /** يملأ [p] بوضعية الحالة [s] عند الزمن [tMs] من بدايتها. */
    fun compute(s: PetState, tMs: Long, durMs: Long, p: Pose) {
        p.reset()
        val t = tMs / 1000f
        val prog = (tMs.toFloat() / durMs).coerceIn(0f, 1f)
        p.side = s.motion != Motion.NONE   // أي حركة تنقل = منظر جانبي

        when (s.anim) {
            // ───── المشي والتنقل ─────
            Anim.WALK -> {
                val w = sin(t * 6f)
                p.swing = w * 2.4f
                p.dy = -abs(w) * 0.7f
                p.legL = max(0f, w) * 1.6f; p.legR = max(0f, -w) * 1.6f
                p.armL = 8f + w * 18f; p.armR = 8f - w * 18f
            }
            Anim.RUN -> {
                val w = sin(t * 11f)
                p.swing = w * 3.8f
                p.dy = -abs(w) * 1.3f; p.lean = 14f
                p.legL = max(0f, w) * 3.2f; p.legR = max(0f, -w) * 3.2f
                p.armL = 20f + w * 40f; p.armR = 20f - w * 40f
            }
            Anim.SNEAK -> {
                val w = sin(t * 3f)
                p.swing = w * 1.5f
                p.dy = 1.2f; p.squash = 0.93f; p.lean = 7f
                p.legL = max(0f, w) * 1.2f; p.legR = max(0f, -w) * 1.2f
                p.armL = 25f; p.armR = 25f; p.look = sin(t * 1.3f) * 0.6f
            }
            Anim.JUMP -> {
                val u = t % 1.1f
                val h = if (u < 0.8f) 6f * sin(Math.PI.toFloat() * u / 0.8f) else 0f
                p.dy = -h
                p.squash = if (u >= 0.8f) 0.9f else 1f
                val up = h > 0.5f
                p.armL = if (up) 155f else 20f; p.armR = p.armL
            }
            Anim.WAVE -> {
                p.armR = 140f + sin(t * 10f) * 25f; p.armL = 6f
                p.headTilt = sin(t * 2f) * 3f
            }
            Anim.LOOK -> {
                val l = sin(t * 1.7f)
                p.look = l; p.headTilt = l * 5f
            }
            Anim.TRIP -> {
                p.lean = kf(t, 0f to 0f, 0.9f to 0f, 1.3f to 55f, 1.55f to 88f, 2.8f to 88f, 3.6f to 15f, 4.0f to 0f)
                p.dy = p.lean / 88f * 2.4f
                if (t in 1.0f..1.7f) { p.armL = 110f + sin(t * 30f) * 30f; p.armR = p.armL } else { p.armL = 12f; p.armR = 12f }
                if (t in 1.1f..2.8f) p.face = Face.SURPRISED else if (t >= 2.8f) p.face = Face.SAD
                if (t < 0.9f) { val w = sin(t * 7f); p.legL = max(0f, w) * 1.5f; p.legR = max(0f, -w) * 1.5f }
            }
            Anim.CLIMB -> {
                val w = sin(t * 3.2f)
                p.swing = w * 2.5f
                p.armR = 165f + w * 6f            // الذراع القريبة من الحافة ممسكة بها
                p.armL = 110f - w * 22f
                p.legL = max(0f, w) * 2.5f; p.legR = max(0f, -w) * 2.5f
                p.look = 1f; p.headTilt = 4f
            }

            // ───── النوم والاستيقاظ ─────
            Anim.YAWN -> {
                if (t < 2.2f) {
                    p.armL = kf(t, 0f to 5f, 0.6f to 165f, 1.8f to 165f, 2.2f to 20f); p.armR = p.armL
                    p.lean = -5f; p.face = Face.YAWN
                } else {
                    p.armR = -128f + sin(t * 6f) * 8f; p.face = Face.CLOSED; p.headTilt = 4f
                }
            }
            Anim.SLEEP -> {
                p.lieAmt = 1f; p.dy = sin(t * 1.6f) * 0.25f; p.face = Face.CLOSED
            }
            Anim.WAKE_JUMP -> {
                p.dy = -5f * sin(Math.PI.toFloat() * min(t / 0.55f, 1f))
                p.armL = if (t < 1.4f) 160f else 15f; p.armR = p.armL
                p.look = if (t > 1.5f) sin(t * 6f) else 0f
            }
            Anim.STRETCH -> {
                val up = if (prog < 0.7f) 170f + sin(t * 3f) * 5f else 20f
                p.armL = up; p.armR = up
                p.lean = if (prog < 0.7f) -6f else 0f
                p.squash = 1f + (if (prog < 0.7f) 0.04f * sin(t * 4f) else 0f)
            }
            Anim.DROWSY -> {
                val lie = smooth((t - 1.4f) / 2.2f)
                p.lieAmt = lie
                p.face = if (lie > 0.6f) Face.CLOSED else Face.SLEEPY
                p.headTilt = sin(t * 3f) * 7f * (1f - lie)
                p.armL = 10f; p.armR = 10f
            }
            Anim.SLEEP_SHIVER -> {
                p.lieAmt = 1f; p.face = Face.CLOSED
                p.dx = sin(t * 38f) * 0.3f; p.dy = sin(t * 3f) * 0.3f
            }
            Anim.SIT_BED -> {
                p.sit = true; p.dy = -4.2f
                p.armR = -135f + sin(t * 7f) * 14f; p.armL = 8f
                p.headTilt = 5f + sin(t * 2f) * 3f
                p.face = if (t < 3.2f) Face.SLEEPY else Face.NORMAL
            }

            // ───── العمل والدراسة والتقنية ─────
            Anim.TYPE -> {
                p.sit = true
                p.armL = HOLD + sin(t * 15f) * 7f; p.armR = HOLD + cos(t * 15f) * 7f
                p.headTilt = 2f; p.lookY = 0.3f
            }
            Anim.THINK -> {
                p.armR = -125f; p.armL = 8f
                p.headTilt = sin(t * 1.5f) * 5f; p.look = 0.6f; p.lookY = -0.6f
            }
            Anim.IDEA -> {
                if (t < 1.0f) { p.face = Face.THINK; p.armR = -125f; p.look = 0.5f }
                else {
                    val h = 3f * sin(Math.PI.toFloat() * min((t - 1f) / 0.5f, 1f))
                    p.dy = -h; p.armR = 160f; p.armL = 10f; p.face = Face.HAPPY
                }
            }
            Anim.READ -> {
                p.armL = HOLD - 4f; p.armR = HOLD - 4f
                p.headTilt = 6f; p.lookY = 0.7f; p.propT = (t * 0.55f) % 1f
            }
            Anim.WRITE -> {
                p.armR = HOLD + sin(t * 18f) * 6f; p.armL = HOLD + 6f
                p.headTilt = 7f; p.lookY = 0.7f; p.lean = 4f
            }
            Anim.PHONE -> {
                p.armL = -68f; p.armR = -68f
                p.headTilt = 5f; p.lookY = 0.6f
            }
            Anim.HAMMER -> {
                p.armR = -72f + sin(t * 13f) * 12f; p.armL = -36f
                p.lean = 4f; p.dy = 0.8f; p.legSpread = 0.6f
                p.propT = sin(t * 13f)
            }
            Anim.CHEER -> {
                p.armL = 160f + sin(t * 12f) * 14f; p.armR = 160f - sin(t * 12f) * 14f
                p.dy = -abs(sin(t * 5f)) * 3f
            }
            Anim.SLUMP -> {
                p.sit = true; p.lean = 20f; p.headTilt = 14f
                p.armL = HOLD; p.armR = HOLD
                p.dy = 0.6f + sin(t * 1.4f) * 0.3f
            }

            // ───── الشتاء والثلج ─────
            Anim.SHOW -> {
                p.armL = if ((t % 3f) < 1.8f) 75f + sin(t * 3f) * 6f else 140f + sin(t * 10f) * 20f
                p.armR = 8f; p.look = -1f; p.dy = -abs(sin(t * 2.5f)) * 0.6f
            }
            Anim.PUSH -> {
                val w = sin(t * 4f)
                p.swing = w * 2f
                p.armL = -72f; p.armR = -72f; p.lean = 16f
                p.legL = max(0f, w) * 1.5f; p.legR = max(0f, -w) * 1.5f
                p.propT = t
            }
            Anim.SHIVER -> {
                p.dx = sin(t * 46f) * 0.35f; p.squash = 0.96f
                p.armL = -96f; p.armR = -96f
                p.chew = abs(sin(t * 30f))
            }
            Anim.SCARF -> {
                if (t < 2.6f) { p.armL = -116f + sin(t * 6f) * 8f; p.armR = -116f - sin(t * 6f) * 8f; p.face = Face.NORMAL }
                else { p.armL = 12f; p.armR = 12f; p.face = Face.HAPPY }
                p.headTilt = 3f
            }
            Anim.THROW -> {
                val c = t % 2.4f
                p.armR = kf(c, 0f to 10f, 0.9f to 155f, 1.1f to -35f, 1.6f to -35f, 2.2f to 10f)
                p.lean = -kf(c, 0f to 0f, 0.9f to 10f, 1.1f to 0f)
                p.armL = 10f
                p.propT = if (c in 0.95f..2.15f) (c - 0.95f) / 1.2f else 0f
                p.face = if (c > 1.1f) Face.LAUGH else Face.NORMAL
            }
            Anim.LOOK_UP -> {
                p.lookY = -1f; p.headTilt = -7f
                p.armR = 95f + sin(t * 2.2f) * 12f; p.armL = 8f
            }
            Anim.UMBRELLA -> {
                p.armR = 18f; p.armL = 8f
                p.dx = sin(t * 1.5f) * 0.2f
            }

            // ───── الطعام والشراب ─────
            Anim.SIP -> {
                val c = t % 3f
                val up = kf(c, 0f to 0f, 0.7f to 0f, 1.4f to 1f, 2.0f to 1f, 2.7f to 0f)
                p.armR = HOLD + (-118f - HOLD) * up; p.armL = -30f
                p.propT = up
                p.face = if (up > 0.9f) Face.HAPPY else Face.NORMAL
                p.headTilt = -3f * up
            }
            Anim.EAT -> {
                val c = t % 2.0f
                val up = kf(c, 0f to 0f, 0.5f to 0f, 1.0f to 1f, 1.7f to 1f, 2.0f to 0f)
                p.armR = HOLD + (-118f - HOLD) * up; p.armL = -30f
                p.chew = if (up > 0.95f) abs(sin(t * 16f)) else 0f
                p.propT = prog   // يكبر الجزء المأكول مع الوقت
            }
            Anim.BELLY -> {
                p.armL = -58f + sin(t * 8f) * 6f; p.armR = -58f - sin(t * 8f) * 6f
                p.lean = 3f; p.dx = if ((t % 1.6f) < 0.25f) sin(t * 50f) * 0.3f else 0f
            }
            Anim.RELAX -> {
                p.sit = true; p.lean = -5f
                p.armL = -140f; p.armR = -140f
                p.dy = sin(t * 1.3f) * 0.3f; p.face = if (t > 2f) Face.CLOSED else Face.HAPPY
            }

            // ───── المشاعر ─────
            Anim.HAPPY -> {
                p.dy = -abs(sin(t * 4f)) * 1.6f
                p.armL = 32f + sin(t * 8f) * 12f; p.armR = 32f - sin(t * 8f) * 12f
                p.headTilt = sin(t * 4f) * 4f
            }
            Anim.ANGRY -> {
                p.dx = sin(t * 30f) * 0.22f; p.lean = 5f
                p.armL = 8f; p.armR = 8f; p.blush = 0.8f
            }
            Anim.SHY -> {
                p.headTilt = 10f; p.look = -0.6f; p.lookY = 0.8f
                p.armL = -46f + sin(t * 5f) * 5f; p.armR = -46f - sin(t * 5f) * 5f
                p.dx = sin(t * 2f) * 0.4f; p.blush = 1f
            }
            Anim.LAUGH -> {
                p.dy = sin(t * 28f) * 0.35f; p.headTilt = -8f + sin(t * 14f) * 3f
                p.armL = -60f; p.armR = -60f
            }
            Anim.SIT_SAD -> {
                p.sit = true; p.headTilt = 10f; p.lookY = 0.8f
                p.armL = -26f; p.armR = -26f; p.dy = sin(t * 1.2f) * 0.2f
            }
            Anim.SHOCK -> {
                p.dy = -kf(t, 0f to 0f, 0.12f to 3.2f, 0.4f to 0f)
                p.lean = -8f
                p.armL = 92f + sin(t * 24f) * 5f; p.armR = 92f - sin(t * 24f) * 5f
                p.dx = if (t > 0.4f) sin(t * 35f) * 0.15f else 0f
            }
            Anim.PROUD -> {
                p.lean = -3f; p.headTilt = -4f; p.legSpread = 1.2f
                p.armL = 38f; p.armR = 38f; p.dy = sin(t * 1.5f) * 0.25f
            }

            // ───── التفاعلات والأنشطة ─────
            Anim.ASK -> {
                p.dy = sin(t * 2f) * 0.3f; p.headTilt = sin(t * 1.3f) * 4f
                p.armL = 8f; p.armR = 8f; p.look = sin(t * 0.9f) * 0.3f
            }
            Anim.DANCE -> {
                val w = sin(t * 5f)
                p.dx = w * 2f; p.lean = w * 8f; p.headTilt = -w * 6f
                p.armL = 105f + sin(t * 10f) * 40f; p.armR = 105f - sin(t * 10f) * 40f
                p.legL = max(0f, w) * 1.8f; p.legR = max(0f, -w) * 1.8f
                p.dy = -abs(w) * 0.8f
            }
            Anim.EXERCISE -> {
                val u = t % 1.2f
                val up = u < 0.55f
                p.dy = if (up) -4.5f * sin(Math.PI.toFloat() * u / 0.55f) else 0f
                p.armL = if (up) 160f else 8f; p.armR = p.armL
                p.legSpread = if (up) 2.2f else 0f
                p.squash = if (!up) 0.92f else 1f
            }
            Anim.WAVE_SOFT -> {
                p.armR = 125f + sin(t * 5f) * 18f; p.armL = 8f
                p.headTilt = sin(t * 2f) * 3f
            }
        }
    }
}
