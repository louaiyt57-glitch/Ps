package com.pixelpet.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/** ألوان الشخصية والأغراض */
object Col {
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
    val MOUTH = Color.parseColor("#2A1410")
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
        c.scale(facing.toFloat(), p.squash)
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

    private fun leg(x: Float, lift: Float, h: Float) {
        rect(x, -h - lift, x + 3f, -1f - lift, Col.PANTS)
        rect(x - 0.4f, -1.2f - lift, x + 3.4f, -lift, Col.SHOE)
    }

    private fun body(p: Pose, face: Face, t: Float, st: PetState, facing: Int) {
        val legH = if (p.sit) 2.2f else 5f
        val bb = -legH
        val neck = bb - 9f
        val sp = p.legSpread

        // الأرجل
        leg(-3.7f - sp, p.legL, legH)
        leg(0.7f + sp, p.legR, legH)

        // المعطف
        rect(-4.4f, bb - 3f, 4.4f, bb, Col.COAT)
        rect(-4f, neck, 4f, bb - 3f, Col.COAT)
        rect(-4f, neck, -3.2f, bb, Col.COAT_D)
        rect(3.2f, neck, 4f, bb, Col.COAT_D)
        rect(-0.3f, neck + 1.4f, 0.3f, bb, Col.COAT_D)
        rect(0.9f, neck + 3.5f, 1.6f, neck + 4.2f, Col.HAIR)
        rect(0.9f, neck + 5.5f, 1.6f, neck + 6.2f, Col.HAIR)
        rect(-4.4f, bb - 0.8f, 4.4f, bb, Col.COAT_D)

        // الوشاح
        val sw = sin(t * 3f) * 0.3f
        rect(-4.6f, neck - 1.3f, 4.6f, neck + 1.5f, Col.SCARF)
        rect(-4.6f, neck + 0.9f, 4.6f, neck + 1.5f, Col.SCARF_D)
        rect(0.6f + sw, neck + 1.5f, 3.2f + sw, neck + 6.5f, Col.SCARF)
        rect(0.6f + sw, neck + 5.6f, 3.2f + sw, neck + 6.5f, Col.SCARF_D)

        // الرأس
        c.save()
        c.translate(0f, neck)
        c.rotate(p.headTilt)
        c.translate(p.look * 0.8f, 0f)
        head(p, face, t)
        c.restore()

        // الذراعان
        val sy = neck + 1.8f
        arm(-4.8f, sy, p.armL, false)
        arm(4.8f, sy, p.armR, true)

        // مواقع اليدين (محلية → عالم تقريبي) لرسم الأغراض
        val len = 6.2f
        val rad = Math.PI.toFloat() / 180f
        val rLx = -4.8f - len * sin(p.armL * rad); val rLy = sy + len * cos(p.armL * rad)
        val rRx = 4.8f + len * sin(p.armR * rad); val rRy = sy + len * cos(p.armR * rad)
        lhX = p.dx + facing * rLx; lhY = p.dy + rLy
        rhX = p.dx + facing * rRx; rhY = p.dy + rRy

        // أغراض محمولة في اليد
        props.drawHeld(st.prop, rRx, rRy, rLx, rLy, p, t)
    }

    private fun arm(sx: Float, sy: Float, angle: Float, mirror: Boolean) {
        c.save()
        c.translate(sx, sy)
        c.rotate(if (mirror) -angle else angle)
        rect(-1f, -0.8f, 1f, 5.2f, Col.COAT)
        rect(-1f, 4.4f, 1f, 5.2f, Col.COAT_D)
        rect(-1f, 5.2f, 1f, 7.2f, Col.SKIN)
        c.restore()
    }

    private fun head(p: Pose, face: Face, t: Float) {
        // الوجه
        rect(-4f, -9.8f, 4f, -1f, Col.SKIN)
        rect(-4f, -1.8f, 4f, -1f, Col.SKIN_D)
        if (p.blush > 0f && face == Face.ANGRY) rect(-4f, -9.8f, 4f, -1f, Color.RED, (p.blush * 90).toInt())

        // الشعر / القبعة الداكنة
        rect(-4.6f, -12.2f, 4.6f, -8.3f, Col.HAIR)
        rect(-4.6f, -9.2f, -3.5f, -4.8f, Col.HAIR)
        rect(3.5f, -9.2f, 4.6f, -4.8f, Col.HAIR)
        rect(-3.5f, -8.3f, 3.5f, -7.6f, Col.HAIR)

        val eyeY = -5.8f
        val blink = face == Face.NORMAL && (t % 3.4f) < 0.12f
        eye(-2.1f, eyeY, face, blink, p, -1)
        eye(2.1f, eyeY, face, blink, p, 1)
        // جسر النظارة
        rect(-0.5f, eyeY - 0.3f, 0.5f, eyeY + 0.3f, Col.HAIR)

        brows(face, eyeY)
        mouth(face, p)

        // احمرار الخدين
        val b = if (face == Face.SHY) max(p.blush, 0.9f) else p.blush
        if (b > 0f && face != Face.ANGRY) {
            val a = (b * 190).toInt()
            rect(-3.9f, eyeY + 1.9f, -2.1f, eyeY + 2.9f, Col.BLUSH, a)
            rect(2.1f, eyeY + 1.9f, 3.9f, eyeY + 2.9f, Col.BLUSH, a)
        }
    }

    private fun eye(cx: Float, ey: Float, face: Face, blink: Boolean, p: Pose, side: Int) {
        val big = face == Face.SURPRISED
        val e = if (big) 0.4f else 0f
        val x0 = cx - 1.6f - e; val x1 = cx + 1.6f + e
        val y0 = ey - 1.4f - e; val y1 = ey + 1.4f + e
        rect(x0, y0, x1, y1, Col.HAIR)                       // إطار النظارة
        val ix0 = x0 + 0.5f; val ix1 = x1 - 0.5f; val iy0 = y0 + 0.5f; val iy1 = y1 - 0.5f

        when {
            blink || face == Face.CLOSED || face == Face.YAWN -> {
                rect(ix0, iy0, ix1, iy1, Col.SKIN_D)
                rect(ix0, ey + 0.1f, ix1, ey + 0.6f, Col.HAIR)
            }
            face == Face.HAPPY || face == Face.LAUGH -> {
                rect(ix0, iy0, ix1, iy1, Col.SKIN_D)
                rect(ix0, ey + 0.1f, cx - 0.3f, ey + 0.6f, Col.HAIR)
                rect(cx - 0.4f, ey - 0.4f, cx + 0.4f, ey + 0.1f, Col.HAIR)
                rect(cx + 0.3f, ey + 0.1f, ix1, ey + 0.6f, Col.HAIR)
            }
            else -> {
                rect(ix0, iy0, ix1, iy1, Col.LENS)
                val ps = if (big) 0.7f else 1f
                var px = cx + p.look * 0.6f
                var py = ey + p.lookY * 0.4f
                if (face == Face.SAD) py += 0.3f
                if (face == Face.THINK) { px += 0.4f * side.coerceAtLeast(0); py -= 0.3f }
                rect(px - ps / 2, py - ps / 2, px + ps / 2, py + ps / 2, Col.HAIR)
                if (face == Face.SLEEPY) rect(ix0, iy0, ix1, ey, Col.SKIN_D)
            }
        }
    }

    private fun brows(face: Face, ey: Float) {
        val y = ey - 2.4f
        when (face) {
            Face.ANGRY -> {
                rect(-3.8f, y, -2.7f, y + 0.5f, Col.HAIR); rect(-2.7f, y + 0.3f, -1.6f, y + 0.8f, Col.HAIR); rect(-1.6f, y + 0.6f, -0.4f, y + 1.1f, Col.HAIR)
                rect(2.7f, y, 3.8f, y + 0.5f, Col.HAIR); rect(1.6f, y + 0.3f, 2.7f, y + 0.8f, Col.HAIR); rect(0.4f, y + 0.6f, 1.6f, y + 1.1f, Col.HAIR)
            }
            Face.SAD -> {
                rect(-3.8f, y + 0.6f, -2.7f, y + 1.1f, Col.HAIR); rect(-2.7f, y + 0.3f, -1.6f, y + 0.8f, Col.HAIR); rect(-1.6f, y, -0.4f, y + 0.5f, Col.HAIR)
                rect(2.7f, y + 0.6f, 3.8f, y + 1.1f, Col.HAIR); rect(1.6f, y + 0.3f, 2.7f, y + 0.8f, Col.HAIR); rect(0.4f, y, 1.6f, y + 0.5f, Col.HAIR)
            }
            Face.THINK -> {
                rect(-3.6f, y + 0.4f, -0.6f, y + 0.9f, Col.HAIR)
                rect(0.6f, y - 0.4f, 3.6f, y + 0.1f, Col.HAIR)
            }
            Face.SURPRISED -> {
                rect(-3.6f, y - 0.6f, -0.6f, y - 0.1f, Col.HAIR)
                rect(0.6f, y - 0.6f, 3.6f, y - 0.1f, Col.HAIR)
            }
            else -> Unit
        }
    }

    private fun mouth(face: Face, p: Pose) {
        val m = Col.MOUTH
        if (p.chew > 0.5f) { rect(-0.9f, -3.5f, 0.9f, -2.2f, m); return }
        when (face) {
            Face.HAPPY -> {
                rect(-1.4f, -3.0f, 1.4f, -2.5f, m)
                rect(-1.9f, -3.5f, -1.4f, -3.0f, m); rect(1.4f, -3.5f, 1.9f, -3.0f, m)
            }
            Face.LAUGH -> {
                rect(-1.8f, -3.8f, 1.8f, -1.7f, m)
                rect(-0.9f, -2.3f, 0.9f, -1.7f, Col.BLUSH)
            }
            Face.YAWN -> rect(-1.3f, -4.2f, 1.3f, -1.4f, m)
            Face.SURPRISED -> rect(-0.7f, -3.7f, 0.7f, -2.1f, m)
            Face.SAD -> {
                rect(-1.2f, -2.9f, 1.2f, -2.4f, m)
                rect(-1.7f, -2.4f, -1.2f, -1.9f, m); rect(1.2f, -2.4f, 1.7f, -1.9f, m)
            }
            Face.ANGRY -> rect(-1.3f, -2.9f, 1.3f, -2.3f, m)
            Face.THINK -> rect(-0.3f, -3.0f, 1.5f, -2.6f, m)
            Face.SHY -> { rect(-1f, -2.9f, 0f, -2.5f, m); rect(0f, -3.2f, 1f, -2.8f, m) }
            else -> rect(-0.9f, -3.1f, 0.9f, -2.6f, m)
        }
    }
}
