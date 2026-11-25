package com.pixelpet.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** ألوان الشخصية والأغراض */
object Col {
    val INK = Color.parseColor("#1B1B22")
    val HAIR = Color.parseColor("#1B1B22")
    val SKIN = Color.parseColor("#E8B48E")
    val SKIN_D = Color.parseColor("#C98F6B")
    val SCARF = Color.parseColor("#C62828")
    val SCARF_D = Color.parseColor("#8E1B1B")
    val COAT = Color.parseColor("#7A4A2B")
    val COAT_D = Color.parseColor("#5C3519")
    val PANTS = Color.parseColor("#3A2B22")
    val SHOE = Color.parseColor("#1E1E1E")
    val LENS = Color.parseColor("#E6EEF5")
    val MOUTH = Color.parseColor("#C2575A")
    val MOUTH_F = Color.parseColor("#D6455D")
    val MOUTH_M = Color.parseColor("#B5584F")
    val BEARD = Color.parseColor("#2B2522")
    val PANTS_D = Color.parseColor("#2A1E17")
    val BLUSH = Color.parseColor("#E8858A")
    val WHITE = Color.parseColor("#F7F7F7")
    val SNOW = Color.parseColor("#FFFFFF")
    val SNOW_D = Color.parseColor("#C9D6E6")
}

/**
 * يرسم الشخصية وأغراضها بوحدات "بكسل" (unit). نقطة الأصل = منتصف أسفل القدمين، والمحور y السالب للأعلى.
 * كل المستطيلات بلا تنعيم (Anti-alias) للحصول على مظهر بكسلي حاد.
 */
class PetRenderer(density: Float) {
    val u = 3.5f * density            // حجم الوحدة بالبكسل الفعلي
    val boxW = 130f * density         // عرض مربع الشخصية
    val boxH = 128f * density         // ارتفاع مربع الشخصية
    val feetX = 70f * density
    private val feetY = boxH - 5f * density

    lateinit var c: Canvas
    val paint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }
    val textPaint = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // مواقع اليدين بإحداثيات العالم (وحدات) لاستخدامها في المؤثرات
    var rhX = 0f; var rhY = 0f
    var lhX = 0f; var lhY = 0f
    /** ارتفاع الرأس/العنق الحالي بإحداثيات العالم، تستخدمه المؤثرات */
    var neckW = -14f
    var curDx = 0f

    /** مظهر الشخصية الحالي (جنس، تسريحة، ملابس، ألوان). */
    var look: Look = Look.DEFAULT

    private val props = PropPainter(this)
    private val fx = FxPainter(this)

    // ───── أدوات رسم ─────
    fun rect(x0: Float, y0: Float, x1: Float, y1: Float, color: Int, alpha: Int = 255) {
        paint.color = color
        paint.alpha = alpha
        c.drawRect(x0, y0, x1, y1, paint)
        paint.alpha = 255
    }

    /** قرص بكسلي (صفوف أفقية) */
    fun disc(cx: Float, cy: Float, r: Float, color: Int, alpha: Int = 255) {
        paint.color = color
        paint.alpha = alpha
        var y = -r + 0.5f
        while (y <= r) {
            val hw = sqrt(max(0f, r * r - y * y))
            val w = floor(hw * 2f + 0.5f) / 2f
            c.drawRect(cx - w, cy + y - 0.5f, cx + w, cy + y + 0.5f, paint)
            y += 1f
        }
        paint.alpha = 255
    }

    /** رسم رموز بكسلية من نص: الحرف 'X' = لون أساسي، 'o' = لون ثانوي */
    fun sprite(rows: Array<String>, x: Float, y: Float, px: Float, main: Int, second: Int = main, alpha: Int = 255) {
        for (j in rows.indices) {
            val row = rows[j]
            for (i in row.indices) {
                val ch = row[i]
                if (ch == 'X' || ch == 'o') {
                    rect(x + i * px, y + j * px, x + (i + 1) * px, y + (j + 1) * px, if (ch == 'X') main else second, alpha)
                }
            }
        }
    }

    fun outlineText(s: String, x: Float, y: Float, size: Float, color: Int, alpha: Int = 255) {
        textPaint.textSize = size
        textPaint.color = Color.argb((alpha * 0.55f).toInt(), 0, 0, 0)
        c.drawText(s, x + 0.25f, y + 0.25f, textPaint)
        textPaint.color = color
        textPaint.alpha = alpha
        c.drawText(s, x, y, textPaint)
        textPaint.alpha = 255
    }

    // ───── الرسم الرئيسي ─────
    /**
     * @param left,top زاوية مربع الشخصية على الـ Canvas
     */
    fun draw(canvas: Canvas, left: Float, top: Float, st: PetState, p: Pose, face: Face, facing: Int, t: Float) {
        c = canvas
        c.save()
        c.translate(left + feetX, top + feetY)
        c.scale(u, u)

        curDx = p.dx
        val legH = if (p.sit) 2.2f else 5f
        neckW = -legH - 9f + p.dy

        // 1) أغراض خلف الشخصية
        props.drawBehind(st.prop, p, t)

        // 2) الشخصية
        c.save()
        c.translate(p.dx, p.dy)
        if (p.lieAmt > 0f) {
            c.translate(12f * p.lieAmt, -8.2f * p.lieAmt)
            c.rotate(-90f * p.lieAmt)
        }
        var sx = facing.toFloat()
        if (p.spin != 0f) {                       // دوران حول المحور الرأسي (فستان يدور)
            var k = cos(p.spin * 0.0174533f)
            if (abs(k) < 0.12f) k = if (k < 0f) -0.12f else 0.12f
            sx *= k
        }
        c.scale(sx, p.squash)
        c.rotate(p.lean)
        body(p, face, t, st, facing)
        c.restore()

        // 3) أغراض أمام الشخصية ومؤثرات
        c.save()
        c.scale(facing.toFloat(), 1f)
        props.drawFront(st.prop, p, t)
        c.restore()
        fx.draw(st.fx, p, t, facing)

        c.restore()
    }

    private fun body(p: Pose, face: Face, t: Float, st: PetState, facing: Int) {
        val legH = if (p.sit) 2.2f else 5f
        val bb = -legH
        val neck = bb - 9f
        val sy = neck + 1.8f
        val o = look.outfit
        val x = OC(this, look, p, t, p.side, legH, bb, neck)

        // تمايل الشعر: تمايل خفيف دائم + ما تضيفه الحركة + ترحيل للخلف أثناء الجري
        val sway = p.hairSwing + sin(t * 2.1f) * 0.25f - (if (p.side) abs(p.swing) * 0.35f else 0f)
        val hc = HC(this, look, p.side, sway, t)

        if (p.side) arm(x, sy, p.armL, false, near = false)      // الذراع البعيدة خلف الجسم

        o.legs(x)
        o.torso(x)
        o.neckwear(x)

        c.save()
        c.translate(if (p.side) 0.4f else 0f, neck)
        c.rotate(p.headTilt)
        if (!p.side) c.translate(p.look * 0.8f, 0f)
        head(p, face, t, hc, x)
        c.restore()

        if (p.side) {
            arm(x, sy, p.armR, true, near = true)               // الذراع القريبة أمام الجسم
        } else {
            val ax = x.w + 0.8f
            c.save(); c.translate(-ax, sy); c.rotate(p.armL); armShape(x, true); c.restore()
            c.save(); c.translate(ax, sy); c.rotate(-p.armR); armShape(x, true); c.restore()
        }

        // مواقع اليدين لرسم الأغراض والمؤثرات
        val len = 6.2f
        val rad = Math.PI.toFloat() / 180f
        val ax = x.w + 0.8f
        val rLx: Float; val rRx: Float
        if (p.side) {
            rLx = len * sin(-p.armL * rad); rRx = len * sin(-p.armR * rad)
        } else {
            rLx = -ax - len * sin(p.armL * rad); rRx = ax + len * sin(p.armR * rad)
        }
        val rLy = sy + len * cos(p.armL * rad)
        val rRy = sy + len * cos(p.armR * rad)
        lhX = p.dx + facing * rLx; lhY = p.dy + rLy
        rhX = p.dx + facing * rRx; rhY = p.dy + rRy

        props.drawHeld(st.prop, rRx, rRy, rLx, rLy, p, t)
    }

    /** ذراع جانبية: الزاوية الموجبة = للأمام (+x). */
    private fun arm(x: OC, sy: Float, angle: Float, @Suppress("UNUSED_PARAMETER") mirror: Boolean, near: Boolean) {
        c.save()
        c.translate(0f, sy)
        c.rotate(-angle)
        armShape(x, near)
        c.restore()
    }

    /** شكل الذراع حسب نوع الكم: طويل / قصير / بلا كم. يُرسم من الكتف نحو الأسفل. */
    private fun armShape(x: OC, near: Boolean) {
        val o = look.outfit
        val sc = o.sleeveColor ?: x.main
        val scD = shade(sc, 0.74f)
        val skinC = if (near) look.skin else look.skinD
        val body = if (near) sc else scD
        when (o.sleeve) {
            Sleeve.LONG -> {
                rect(-1f, -0.8f, 1f, 5.2f, body)
                rect(-1f, 4.4f, 1f, 5.2f, if (near) scD else Col.INK)
                rect(-1f, 5.2f, 1f, 7.2f, skinC)
            }
            Sleeve.SHORT -> {
                rect(-1f, -0.8f, 1f, 2.4f, body)
                rect(-1f, 2.0f, 1f, 2.5f, scD)
                rect(-1f, 2.5f, 1f, 7.2f, skinC)
            }
            Sleeve.NONE -> rect(-1f, -0.8f, 1f, 7.2f, skinC)
        }
    }

    // ───── الرأس ─────
    private fun mouthCol(): Int = when {
        look.beard -> Col.MOUTH
        look.gender == Gender.FEMALE -> Col.MOUTH_F
        else -> Col.MOUTH_M
    }

    private fun head(p: Pose, face: Face, t: Float, hc: HC, x: OC) {
        Hair.draw(hc, look.hair, Layer.BACK)
        if (!p.side) {
            rect(-4f, -9.8f, 4f, -1f, look.skin)
            rect(-4f, -1.8f, 4f, -1f, look.skinD)
        } else {
            rect(-3.8f, -9.8f, 3.8f, -1f, look.skin)
            rect(-3.8f, -1.8f, 3.8f, -1f, look.skinD)
            rect(3.8f, -5.4f, 5f, -3.9f, look.skin)                   // الأنف
        }
        Hair.draw(hc, look.hair, Layer.TOP)
        if (p.hoodUp > 0.5f && look.outfit.hood) Hair.hood(hc, x.main, x.dark)
        if (p.side) headSideFace(p, face, t) else headFrontFace(p, face, t)
    }

    private fun headFrontFace(p: Pose, face: Face, t: Float) {
        val ey = -5.7f
        val female = look.gender == Gender.FEMALE
        val blink = face == Face.NORMAL && (t % 3.4f) < 0.12f
        eyeFront(-2f, ey, face, blink, p, -1)
        eyeFront(2f, ey, face, blink, p, 1)
        if (look.glasses) rect(-0.5f, ey - 0.3f, 0.5f, ey + 0.2f, Col.INK)
        browsFront(face, ey)

        val b = if (face == Face.SHY) max(p.blush, 0.9f) else if (female) max(p.blush, 0.35f) else p.blush
        if (b > 0f && face != Face.ANGRY) {
            val a = (b * 190).toInt()
            rect(-3.9f, ey + 1.6f, -2.3f, ey + 2.4f, Col.BLUSH, a)
            rect(2.3f, ey + 1.6f, 3.9f, ey + 2.4f, Col.BLUSH, a)
        }
        lowerFaceFront(face, p)
    }

    private fun eyeFront(cx: Float, ey: Float, face: Face, blink: Boolean, p: Pose, sideSign: Int) {
        val gl = look.glasses
        val big = face == Face.SURPRISED
        val e = if (big) 0.3f else 0f
        val hw = if (gl) 1.5f else 1.25f
        val hh = if (gl) 1.2f else 0.95f
        val x0 = cx - hw - e; val x1 = cx + hw + e
        val y0 = ey - hh - e; val y1 = ey + hh + e
        if (gl) rect(x0, y0, x1, y1, Col.INK)
        val m = if (gl) 0.5f else 0f
        val ix0 = x0 + m; val ix1 = x1 - m; val iy0 = y0 + m; val iy1 = y1 - m
        when {
            blink || face == Face.CLOSED || face == Face.YAWN -> {
                rect(ix0, iy0, ix1, iy1, look.skinD)
                rect(ix0, ey, ix1, ey + 0.5f, Col.INK)
            }
            face == Face.HAPPY || face == Face.LAUGH -> {
                rect(ix0, iy0, ix1, iy1, look.skinD)
                rect(ix0, ey + 0.1f, cx - 0.2f, ey + 0.6f, Col.INK)
                rect(cx - 0.3f, ey - 0.4f, cx + 0.3f, ey + 0.1f, Col.INK)
                rect(cx + 0.2f, ey + 0.1f, ix1, ey + 0.6f, Col.INK)
            }
            else -> {
                rect(ix0, iy0, ix1, iy1, Col.LENS)
                val ps = if (big) 0.7f else 0.9f
                val px = cx + p.look * 0.5f
                val py = ey + p.lookY * 0.3f + (if (face == Face.SAD) 0.3f else 0f)
                rect(px - ps / 2, py - ps / 2, px + ps / 2, py + ps / 2, Col.INK)
                if (face == Face.SLEEPY) rect(ix0, iy0, ix1, ey, look.skinD)
            }
        }
        if (look.gender == Gender.FEMALE) {          // رموش في الزاوية الخارجية
            val ox = if (sideSign < 0) x0 - 0.6f else x1 - 0.4f
            rect(ox, y0 - 0.5f, ox + 1f, y0 + 0.1f, Col.INK)
        }
    }

    private fun browsFront(face: Face, ey: Float) {
        val y = ey - 2.2f
        val b = look.hairD
        when (face) {
            Face.ANGRY -> {
                rect(-3.5f, y, -2.5f, y + 0.5f, b); rect(-2.5f, y + 0.3f, -1.5f, y + 0.8f, b); rect(-1.5f, y + 0.6f, -0.4f, y + 1.1f, b)
                rect(2.5f, y, 3.5f, y + 0.5f, b); rect(1.5f, y + 0.3f, 2.5f, y + 0.8f, b); rect(0.4f, y + 0.6f, 1.5f, y + 1.1f, b)
            }
            Face.SAD -> {
                rect(-3.5f, y + 0.6f, -2.5f, y + 1.1f, b); rect(-2.5f, y + 0.3f, -1.5f, y + 0.8f, b); rect(-1.5f, y, -0.4f, y + 0.5f, b)
                rect(2.5f, y + 0.6f, 3.5f, y + 1.1f, b); rect(1.5f, y + 0.3f, 2.5f, y + 0.8f, b); rect(0.4f, y, 1.5f, y + 0.5f, b)
            }
            Face.THINK -> {
                rect(-3.4f, y + 0.4f, -0.6f, y + 0.9f, b)
                rect(0.6f, y - 0.4f, 3.4f, y + 0.1f, b)
            }
            Face.SURPRISED -> {
                rect(-3.4f, y - 0.6f, -0.6f, y - 0.1f, b)
                rect(0.6f, y - 0.6f, 3.4f, y - 0.1f, b)
            }
            else -> if (!look.glasses) {                     // حاجبان رفيعان عندما لا توجد نظارة
                rect(-3.3f, y + 0.1f, -0.7f, y + 0.6f, b)
                rect(0.7f, y + 0.1f, 3.3f, y + 0.6f, b)
            }
        }
    }

    /** أسفل الوجه: لحية اختيارية، والفم بلون وردي. */
    private fun lowerFaceFront(face: Face, p: Pose) {
        if (look.beard) {
            val bc = shade(look.hairCol, 1.35f)
            rect(-4f, -3.9f, 4f, -1f, bc)
            rect(-4f, -5f, -3.2f, -3.9f, bc)
            rect(3.2f, -5f, 4f, -3.9f, bc)
            rect(-2.6f, -4.5f, 2.6f, -3.7f, bc)
        }
        val m = mouthCol()
        if (p.chew > 0.5f) { rect(-0.9f, -3.4f, 0.9f, -2.2f, m); return }
        when (face) {
            Face.HAPPY -> {
                rect(-1.4f, -3.3f, 1.4f, -2.7f, m)
                rect(-1.9f, -3.8f, -1.4f, -3.3f, m); rect(1.4f, -3.8f, 1.9f, -3.3f, m)
            }
            Face.LAUGH -> {
                rect(-1.8f, -3.9f, 1.8f, -1.8f, m)
                rect(-1f, -3.9f, 1f, -3.4f, Col.WHITE)
            }
            Face.YAWN -> rect(-1.3f, -4f, 1.3f, -1.5f, m)
            Face.SURPRISED -> rect(-0.7f, -3.6f, 0.7f, -2.2f, m)
            Face.SAD -> {
                rect(-1.2f, -3.2f, 1.2f, -2.7f, m)
                rect(-1.7f, -2.7f, -1.2f, -2.2f, m); rect(1.2f, -2.7f, 1.7f, -2.2f, m)
            }
            Face.ANGRY -> rect(-1.3f, -3.2f, 1.3f, -2.6f, m)
            Face.THINK -> rect(-0.3f, -3.2f, 1.5f, -2.8f, m)
            Face.SHY -> { rect(-1f, -3.2f, 0f, -2.8f, m); rect(0f, -3.5f, 1f, -3.1f, m) }
            else -> rect(-1f, -3.3f, 1f, -2.7f, m)
        }
    }

    // ───── وجه جانبي (يتجه نحو +x): عين واحدة، أنف، لحية اختيارية ─────
    private fun headSideFace(p: Pose, face: Face, t: Float) {
        val gl = look.glasses
        val ey = -5.7f
        val blink = face == Face.NORMAL && (t % 3.4f) < 0.12f
        val x0 = if (gl) 1.1f else 1.6f
        var x1 = if (gl) 4.4f else 3.9f
        var y0 = ey - (if (gl) 1.3f else 0.95f)
        var y1 = ey + (if (gl) 1.3f else 0.95f)
        if (face == Face.SURPRISED) { x1 += 0.3f; y0 -= 0.3f; y1 += 0.3f }
        if (gl) {
            rect(-3.5f, ey - 0.25f, 1.2f, ey + 0.25f, Col.INK)         // ذراع النظارة
            rect(x0, y0, x1, y1, Col.INK)
        }
        val m = if (gl) 0.5f else 0f
        val dn = if (face == Face.SAD) 0.3f else 0f
        when {
            blink || face == Face.CLOSED || face == Face.YAWN -> {
                rect(x0 + m, y0 + m, x1 - m, y1 - m, look.skinD)
                rect(x0 + m, ey, x1 - m, ey + 0.5f, Col.INK)
            }
            face == Face.HAPPY || face == Face.LAUGH -> {
                rect(x0 + m, y0 + m, x1 - m, y1 - m, look.skinD)
                rect(x0 + m, ey + 0.1f, x0 + m + 0.9f, ey + 0.6f, Col.INK)
                rect(x0 + m + 0.9f, ey - 0.4f, x1 - m - 0.9f, ey + 0.1f, Col.INK)
                rect(x1 - m - 0.9f, ey + 0.1f, x1 - m, ey + 0.6f, Col.INK)
            }
            else -> {
                rect(x0 + m, y0 + m, x1 - m, y1 - m, Col.LENS)
                rect(x1 - m - 1.1f, ey - 0.4f + dn, x1 - m - 0.1f, ey + 0.6f + dn, Col.INK)
                if (face == Face.SLEEPY) rect(x0 + m, y0 + m, x1 - m, ey, look.skinD)
            }
        }
        if (look.gender == Gender.FEMALE) rect(x1 - 0.2f, y0 - 0.5f, x1 + 0.8f, y0 + 0.1f, Col.INK)

        val yb = ey - 2.2f
        val b = look.hairD
        when (face) {
            Face.ANGRY -> { rect(1f, yb, 4.2f, yb + 0.5f, b); rect(3f, yb + 0.4f, 4.2f, yb + 1f, b) }
            Face.SAD -> rect(1f, yb + 0.7f, 4.2f, yb + 1.2f, b)
            Face.SURPRISED -> rect(1f, yb - 0.6f, 4.2f, yb - 0.1f, b)
            else -> if (!gl) rect(1.4f, yb + 0.1f, 4.2f, yb + 0.6f, b)
        }
        if (look.gender == Gender.FEMALE && p.blush >= 0f) rect(0.4f, ey + 1.6f, 2.2f, ey + 2.4f, Col.BLUSH, 90)

        if (look.beard) {
            val bc = shade(look.hairCol, 1.35f)
            rect(-1f, -3.9f, 3.8f, -1f, bc)
            rect(2f, -4.6f, 4.4f, -3.7f, bc)
        }
        val mc = mouthCol()
        when (face) {
            Face.HAPPY -> { rect(1.9f, -3.3f, 4f, -2.8f, mc); rect(3.6f, -3.8f, 4f, -3.3f, mc) }
            Face.LAUGH -> rect(1.9f, -3.9f, 4f, -1.8f, mc)
            Face.YAWN -> rect(2.2f, -4f, 4f, -1.5f, mc)
            Face.SURPRISED -> rect(2.6f, -3.6f, 3.9f, -2.2f, mc)
            Face.SAD -> { rect(2.2f, -3f, 4f, -2.5f, mc); rect(3.6f, -2.5f, 4f, -2f, mc) }
            else -> rect(2.4f, -3.3f, 4f, -2.8f, mc)
        }
    }
}
