package com.pixelpet.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * حركة خاصة لكل تسريحة ولكل زي، كل واحدة دالة مستقلة بالشكل:
 *     fun اسم(t: Float, prog: Float, p: Pose)
 * t = الزمن بالثواني منذ بداية الحالة، prog = التقدم من 0 إلى 1، p = الوضعية التي تُعدَّل.
 *
 * زوايا الذراعين (منظر أمامي): موجب = للخارج/للأعلى (180 = رأسياً للأعلى)،
 * سالب = نحو الجسم (حوالي -60 = اليد عند الصدر، -125 = اليد عند الذقن).
 * hairSwing = تمايل خصلات الشعر المتدلية.
 */
object Actions {

    private fun sm(x: Float) = PetPose.smooth(x)
    private const val PI_F = 3.1415927f

    // ═══════════════ حركات التسريحات ═══════════════

    /** قصير: تمرير اليد على الشعر لتنفيشه. */
    fun hairRuffle(t: Float, prog: Float, p: Pose) {
        val a = sin(t * 11f)
        p.armR = 168f + a * 8f
        p.armL = 8f
        p.headTilt = 5f + a * 2f
        p.hairSwing = a * 0.9f
        p.face = Face.HAPPY
    }

    /** طويل: هزّ الرأس ليتطاير الشعر ثم يعود ينساب. */
    fun hairFlip(t: Float, prog: Float, p: Pose) {
        val k = (t % 2.8f) / 2.8f
        val tilt = when {
            k < 0.25f -> -12f * sm(k / 0.25f)
            k < 0.45f -> -12f + 24f * sm((k - 0.25f) / 0.2f)
            else -> 12f * (1f - sm((k - 0.45f) / 0.35f))
        }
        p.headTilt = tilt
        p.hairSwing = -tilt * 0.45f + sin(t * 14f) * 0.4f * (if (k in 0.25f..0.7f) 1f else 0f)
        p.lean = -tilt * 0.15f
        p.armL = 14f
        p.armR = 14f
        p.face = Face.HAPPY
    }

    /** ذيل حصان: قفزات مرحة والذيل يتأرجح. */
    fun ponyHop(t: Float, prog: Float, p: Pose) {
        val h = abs(sin(t * 5f))
        p.dy = -h * 3.5f
        p.hairSwing = sin(t * 5f + 1f) * 2.4f
        p.armL = 35f + sin(t * 10f) * 10f
        p.armR = p.armL
        p.legL = h * 0.8f
        p.legR = h * 0.8f
        p.face = Face.HAPPY
    }

    /** ضفيرة: الإمساك بالضفيرة ولفّها بين الأصابع. */
    fun braidTwirl(t: Float, prog: Float, p: Pose) {
        val a = sin(t * 4f)
        p.armR = -12f + a * 8f
        p.armL = 8f
        p.headTilt = 6f
        p.hairSwing = a * 1.3f
        p.face = if (t > 1f) Face.HAPPY else Face.NORMAL
    }

    /** كعكة: رفع اليدين لتثبيت الكعكة. */
    fun bunAdjust(t: Float, prog: Float, p: Pose) {
        val a = sin(t * 9f)
        p.armL = 160f + a * 6f
        p.armR = 160f - a * 6f
        p.headTilt = sin(t * 2f) * 2f
        p.face = Face.HAPPY
    }

    /** مجعّد: ارتداد الشعر المجعد مع كل قفزة. */
    fun curlyBounce(t: Float, prog: Float, p: Pose) {
        val h = abs(sin(t * 6f))
        p.dy = -h * 2.5f
        p.squash = 1f - 0.05f * (1f - h)
        p.hairSwing = sin(t * 6f) * 1.6f
        p.armL = 25f
        p.armR = 25f
        p.legL = h * 0.7f
        p.legR = h * 0.7f
        p.face = Face.HAPPY
    }

    /** موهوك: هزّ الرأس بقوة على إيقاع الروك. */
    fun mohawkBang(t: Float, prog: Float, p: Pose) {
        val a = sin(t * 9f)
        p.headTilt = a * 18f
        p.lean = a * 5f
        p.hairSwing = a * 1.2f
        p.armL = 120f + sin(t * 9f + 1f) * 15f
        p.armR = p.armL
        p.dy = -abs(a) * 0.5f
        p.face = Face.HAPPY
    }

    /** أشواك: وقفة واثقة بيدين على الخصر وشعر يهتز. */
    fun spikyProud(t: Float, prog: Float, p: Pose) {
        p.armL = 38f
        p.armR = 38f
        p.lean = -3f
        p.headTilt = -4f
        p.legSpread = 1f
        p.dy = sin(t * 1.5f) * 0.25f
        p.hairSwing = sin(t * 12f) * 0.4f
        p.face = Face.HAPPY
    }

    // ═══════════════ حركات الملابس ═══════════════

    /** معطف: تزرير الأزرار وهو يرتجف من البرد. */
    fun coatButton(t: Float, prog: Float, p: Pose) {
        val a = sin(t * 7f)
        p.armL = -75f + a * 8f
        p.armR = -75f - a * 8f
        p.headTilt = 5f
        p.lookY = 0.7f
        p.dx = sin(t * 40f) * 0.15f
        p.face = if (t > 3.5f) Face.HAPPY else Face.NORMAL
    }

    /** قميص: استعراض العضلات بالتناوب بين الذراعين. */
    fun tshirtFlex(t: Float, prog: Float, p: Pose) {
        val first = (t % 2f) < 1f
        val up = 125f + sin(t * 12f) * 5f
        p.armR = if (first) up else 25f
        p.armL = if (first) 25f else up
        p.headTilt = if (first) 4f else -4f
        p.legSpread = 1.4f
        p.lean = -3f
        p.dy = -abs(sin(t * 3f)) * 0.4f
        p.face = Face.HAPPY
    }

    /** هودي: رفع القلنسوة فوق الرأس ثم إنزالها. */
    fun hoodiePull(t: Float, prog: Float, p: Pose) {
        p.hoodUp = sm((prog - 0.18f) / 0.08f) * (1f - sm((prog - 0.78f) / 0.08f))
        val reachUp = max(1f - sm(abs(prog - 0.2f) / 0.1f), 1f - sm(abs(prog - 0.82f) / 0.1f))
        p.armR = 15f + 153f * reachUp
        p.armL = 12f
        p.headTilt = p.hoodUp * 4f
        p.face = if (p.hoodUp > 0.5f) Face.HAPPY else Face.NORMAL
    }

    /** بدلة: تعديل ربطة العنق ثم تسوية السترة. */
    fun suitTie(t: Float, prog: Float, p: Pose) {
        if (t < 2.4f) {
            p.armR = -112f + sin(t * 6f) * 5f
            p.armL = 8f
            p.lookY = 0.6f
            p.headTilt = 4f
            p.face = Face.NORMAL
        } else {
            val s = sin((t - 2.4f) * 5f)
            p.armL = -62f + s * 8f
            p.armR = -62f - s * 8f
            p.lean = -2f
            p.face = Face.HAPPY
        }
    }

    /** فستان: الدوران حول النفس مرتين وهو يتسع. */
    fun dressTwirl(t: Float, prog: Float, p: Pose) {
        val k = ((t - 0.5f) / 2.4f).coerceIn(0f, 1f)
        p.spin = 720f * sm(k)
        p.flare = sm(minOf(k * 4f, 1f)) * (1f - sm((k - 0.85f) / 0.15f))
        val spinning = k > 0f && k < 1f
        p.armL = if (spinning) 62f else 12f
        p.armR = p.armL
        p.dy = -2f * sin(k * PI_F)
        p.face = Face.HAPPY
    }

    /** تنورة: انحناءة تحية رشيقة (كيرتسي) مع إمساك التنورة. */
    fun skirtCurtsy(t: Float, prog: Float, p: Pose) {
        val bend = when {
            prog < 0.2f -> sm(prog / 0.2f)
            prog < 0.7f -> 1f
            else -> 1f - sm((prog - 0.7f) / 0.2f)
        }
        p.squash = 1f - 0.14f * bend
        p.headTilt = 9f * bend
        p.lean = 4f * bend
        p.armL = 8f + 30f * bend
        p.armR = p.armL
        p.legSpread = 0.5f * bend
        p.face = Face.HAPPY
    }

    /** ثوب: اليد على الصدر ثم التلويح بالسلام. */
    fun thobeSalam(t: Float, prog: Float, p: Pose) {
        if (t < 1.8f) {
            p.armR = -135f
            p.armL = 8f
            p.headTilt = 8f * sm(t / 0.5f)
        } else {
            p.armR = 140f + sin(t * 9f) * 22f
            p.armL = 8f
            p.headTilt = sin(t * 2f) * 3f
        }
        p.face = Face.HAPPY
    }

    /** رياضي: الركض في المكان بحماس. */
    fun sportJog(t: Float, prog: Float, p: Pose) {
        val w = sin(t * 10f)
        p.legL = max(0f, w) * 3f
        p.legR = max(0f, -w) * 3f
        p.armL = 40f + w * 45f
        p.armR = 40f - w * 45f
        p.dy = -abs(w)
        p.lean = 6f
        p.hairSwing = w * 0.6f
        p.face = Face.NORMAL
    }

    /** بيجاما: احتضان الوسادة والنعاس. */
    fun pajamaHug(t: Float, prog: Float, p: Pose) {
        p.armL = -60f
        p.armR = -60f
        p.dx = sin(t * 1.5f) * 0.5f
        p.headTilt = 10f
        p.lean = 3f
        p.face = if (t < 2.5f) Face.SLEEPY else Face.CLOSED
    }

    /** صيفي: التروّح بمروحة ورقية من شدة الحر. */
    fun summerFan(t: Float, prog: Float, p: Pose) {
        p.armR = -115f + sin(t * 14f) * 10f
        p.armL = 10f
        p.headTilt = -4f
        p.dy = sin(t * 3f) * 0.2f
        p.face = Face.NORMAL
    }
}
