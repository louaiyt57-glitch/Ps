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
    val MOUTH = Color.parseColor("#C2575A")
    val BEARD = Color.parseColor("#2B2522")
    val PANTS_D = Color.parseColor("#2A1E17")
    val BLUSH = Color.parseColor("#E8858A")
    val WHITE = Color.parseColor("#F7F7F7")
    val SNOW = Color.parseColor("#FFFFFF")
    val SNOW_D = Color.parseColor("#C9D6E6")
    // ألوان الشخصية الأنثوية
    val FHAIR = Color.parseColor("#4A2E1B")
    val DRESS = Color.parseColor("#D6588C")
    val DRESS_D = Color.parseColor("#A83B6B")
    val FSCARF = Color.parseColor("#E8A0B4")
    val FSCARF_D = Color.parseColor("#C97A94")
    val BOW = Color.parseColor("#E85A8A")
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
    /** 0 = ذكر (الافتراضي)، 1 = أنثى */
    var gender = 0
    private var hairC = Col.HAIR
    private val fem get() = gender == 1

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
        hairC = if (fem) Col.FHAIR else Col.HAIR
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

    private fun legFront(x: Float, lift: Float, h: Float, color: Int) {
        rect(x, -h - lift, x + 3f, -1f - lift, color)
        rect(x - 0.4f, -1.2f - lift, x + 3.4f, -lift, Col.SHOE)
    }

    /** رجل جانبية: القدم تتجه للأمام (+x). */
    private fun legSide(x: Float, lift: Float, h: Float, color: Int) {
        rect(x - 1.5f, -h - lift, x + 1.5f, -1f - lift, color)
        rect(x - 1.5f, -1.2f - lift, x + 2.9f, -lift, Col.SHOE)
    }

    private fun body(p: Pose, face: Face, t: Float, st: PetState, facing: Int) {
        val legH = if (p.sit) 2.2f else 5f
        val bb = -legH
        val neck = bb - 9f
        val sy = neck + 1.8f
        val sw = sin(t * 3f) * 0.3f

        if (!p.side) {
            // ───── منظر أمامي ─────
            val legC = if (fem) Col.SKIN else Col.PANTS
            legFront(-3.7f - p.legSpread, p.legL, legH, legC)
            legFront(0.7f + p.legSpread, p.legR, legH, legC)

            val bodyC = if (fem) Col.DRESS else Col.COAT
            val bodyD = if (fem) Col.DRESS_D else Col.COAT_D
            val btn = if (fem) Col.DRESS_D else Col.HAIR
            val scC = if (fem) Col.FSCARF else Col.SCARF
            val scD = if (fem) Col.FSCARF_D else Col.SCARF_D

            rect(-4.4f, bb - 3f, 4.4f, bb, bodyC)
            rect(-4f, neck, 4f, bb - 3f, bodyC)
            rect(-4f, neck, -3.2f, bb, bodyD)
            rect(3.2f, neck, 4f, bb, bodyD)
            rect(-0.3f, neck + 1.4f, 0.3f, bb, bodyD)
            rect(0.9f, neck + 3.5f, 1.6f, neck + 4.2f, btn)
            rect(0.9f, neck + 5.5f, 1.6f, neck + 6.2f, btn)
            if (fem) {
                // تنورة أعرض أسفل الفستان
                rect(-5.6f, bb - 1.6f, 5.6f, bb, bodyC)
                rect(-5.6f, bb - 0.8f, 5.6f, bb, bodyD)
            } else {
                rect(-4.4f, bb - 0.8f, 4.4f, bb, bodyD)
            }

            rect(-4.6f, neck - 1.3f, 4.6f, neck + 1.5f, scC)
            rect(-4.6f, neck + 0.9f, 4.6f, neck + 1.5f, scD)
            if (fem) {
                // فيونكة على الوشاح
                rect(-1.6f, neck + 0.2f, -0.4f, neck + 1.4f, Col.BOW)
            } else {
                rect(0.6f + sw, neck + 1.5f, 3.2f + sw, neck + 6.5f, scC)
                rect(0.6f + sw, neck + 5.6f, 3.2f + sw, neck + 6.5f, scD)
            }

            c.save()
            c.translate(0f, neck)
            c.rotate(p.headTilt)
            c.translate(p.look * 0.8f, 0f)
            headFront(p, face, t)
            c.restore()

            armFront(-4.8f, sy, p.armL, false)
            armFront(4.8f, sy, p.armR, true)
        } else {
            // ───── منظر جانبي (للمشي والركض والتسلل) ─────
            armSide(sy, p.armL, false)                       // الذراع البعيدة خلف الجسم
            legSide(-p.swing, p.legR, legH, if (fem) Col.SKIN_D else Col.PANTS_D)  // الرجل الخلفية أغمق
            legSide(p.swing, p.legL, legH, if (fem) Col.SKIN else Col.PANTS)

            val bodyC = if (fem) Col.DRESS else Col.COAT
            val bodyD = if (fem) Col.DRESS_D else Col.COAT_D
            val btn = if (fem) Col.DRESS_D else Col.HAIR
            val scC = if (fem) Col.FSCARF else Col.SCARF
            val scD = if (fem) Col.FSCARF_D else Col.SCARF_D

            rect(-3.4f, bb - 3f, 3.4f, bb, bodyC)
            rect(-3f, neck, 3f, bb - 3f, bodyC)
            rect(-3f, neck, -2.2f, bb, bodyD)
            if (fem) rect(-4.6f, bb - 1.6f, 4.6f, bb, bodyC) // ذيل الفستان من الجانب
            rect(-3.4f, bb - 0.8f, 3.4f, bb, bodyD)
            rect(1.6f, neck + 1.4f, 2.2f, bb, btn)
            rect(2.2f, neck + 3.5f, 2.9f, neck + 4.2f, btn)
            rect(2.2f, neck + 5.5f, 2.9f, neck + 6.2f, btn)

            // الوشاح: طوق + ذيل يرفرف للخلف (أطول عند الركض)
            val speed = (kotlin.math.abs(p.swing) / 3.8f).coerceIn(0f, 1f)
            val tail = 3f + speed * 3f
            val fl = sin(t * 9f) * 0.5f * (0.4f + speed)
            rect(-3.8f, neck - 1.3f, 3.8f, neck + 1.5f, scC)
            rect(-3.8f, neck + 0.9f, 3.8f, neck + 1.5f, scD)
            if (!fem) {
                rect(-3.8f - tail, neck + 0.6f + fl, -3.4f, neck + 2.4f + fl, scC)
                rect(-3.8f - tail, neck + 1.8f + fl, -3.4f, neck + 2.4f + fl, scD)
            }

            c.save()
            c.translate(0.4f, neck)
            c.rotate(p.headTilt)
            headSide(p, face, t)
            c.restore()

            armSide(sy, p.armR, true)                        // الذراع القريبة أمام الجسم
        }

        // مواقع اليدين لرسم الأغراض والمؤثرات
        val len = 6.2f
        val rad = Math.PI.toFloat() / 180f
        val rLx: Float; val rRx: Float
        if (p.side) {
            rLx = len * sin(-p.armL * rad); rRx = len * sin(-p.armR * rad)
        } else {
            rLx = -4.8f - len * sin(p.armL * rad); rRx = 4.8f + len * sin(p.armR * rad)
        }
        val rLy = sy + len * cos(p.armL * rad)
        val rRy = sy + len * cos(p.armR * rad)
        lhX = p.dx + facing * rLx; lhY = p.dy + rLy
        rhX = p.dx + facing * rRx; rhY = p.dy + rRy

        props.drawHeld(st.prop, rRx, rRy, rLx, rLy, p, t)
    }

    private fun armFront(sx: Float, sy: Float, angle: Float, mirror: Boolean) {
        c.save()
        c.translate(sx, sy)
        c.rotate(if (mirror) -angle else angle)
        rect(-1f, -0.8f, 1f, 5.2f, if (fem) Col.DRESS else Col.COAT)
        rect(-1f, 4.4f, 1f, 5.2f, if (fem) Col.DRESS_D else Col.COAT_D)
        rect(-1f, 5.2f, 1f, 7.2f, Col.SKIN)
        c.restore()
    }

    /** ذراع جانبية: الزاوية الموجبة = للأمام (+x). */
    private fun armSide(sy: Float, angle: Float, near: Boolean) {
        c.save()
        c.translate(0f, sy)
        c.rotate(-angle)
        rect(-1f, -0.8f, 1f, 5.2f, if (near) (if (fem) Col.DRESS else Col.COAT) else (if (fem) Col.DRESS_D else Col.COAT_D))
        rect(-1f, 4.4f, 1f, 5.2f, if (near) (if (fem) Col.DRESS_D else Col.COAT_D) else hairC)
        rect(-1f, 5.2f, 1f, 7.2f, if (near) Col.SKIN else Col.SKIN_D)
        c.restore()
    }

    // ───── الرأس الأمامي: شعر داكن، نظارات صغيرة، لحية (كما في الصور المرجعية) ─────
    private fun headFront(p: Pose, face: Face, t: Float) {
        rect(-4f, -9.8f, 4f, -1f, Col.SKIN)
        rect(-4f, -1.8f, 4f, -1f, Col.SKIN_D)
        rect(-4.6f, -12.2f, 4.6f, -8.2f, hairC)
        rect(-4.6f, -8.4f, -3.6f, -4.6f, hairC)
        rect(3.6f, -8.4f, 4.6f, -4.6f, hairC)
        if (fem) {
            // شعر طويل على الجانبين + غرة
            rect(-5.2f, -11.6f, -4.0f, -1.2f, hairC)
            rect(4.0f, -11.6f, 5.2f, -1.2f, hairC)
            rect(-4.6f, -9.8f, 4.6f, -8.6f, hairC)
            // فيونكة على الرأس
            rect(2.4f, -12.6f, 4.4f, -11.6f, Col.BOW)
            rect(3.2f, -12.9f, 3.7f, -11.3f, Col.BOW)
        }

        val ey = -5.7f
        val blink = face == Face.NORMAL && (t % 3.4f) < 0.12f
        eyeFront(-2f, ey, face, blink, p)
        eyeFront(2f, ey, face, blink, p)
        rect(-0.5f, ey - 0.3f, 0.5f, ey + 0.2f, hairC)
        browsFront(face, ey)

        val b = if (face == Face.SHY) max(p.blush, 0.9f) else p.blush
        if (b > 0f && face != Face.ANGRY) {
            val a = (b * 190).toInt()
            rect(-3.9f, ey + 1.6f, -2.3f, ey + 2.4f, Col.BLUSH, a)
            rect(2.3f, ey + 1.6f, 3.9f, ey + 2.4f, Col.BLUSH, a)
        }
        if (fem) mouthFront(face, p) else beardMouthFront(face, p)
    }

    /** الفم فقط (للأنثى) */
    private fun mouthFront(face: Face, p: Pose) {
        val m = Col.MOUTH
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

    private fun eyeFront(cx: Float, ey: Float, face: Face, blink: Boolean, p: Pose) {
        val big = face == Face.SURPRISED
        val e = if (big) 0.3f else 0f
        val x0 = cx - 1.5f - e; val x1 = cx + 1.5f + e
        val y0 = ey - 1.2f - e; val y1 = ey + 1.2f + e
        rect(x0, y0, x1, y1, Col.HAIR)
        val ix0 = x0 + 0.5f; val ix1 = x1 - 0.5f; val iy0 = y0 + 0.5f; val iy1 = y1 - 0.5f
        when {
            blink || face == Face.CLOSED || face == Face.YAWN -> {
                rect(ix0, iy0, ix1, iy1, Col.SKIN_D)
                rect(ix0, ey, ix1, ey + 0.5f, Col.HAIR)
            }
            face == Face.HAPPY || face == Face.LAUGH -> {
                rect(ix0, iy0, ix1, iy1, Col.SKIN_D)
                rect(ix0, ey + 0.1f, cx - 0.2f, ey + 0.6f, Col.HAIR)
                rect(cx - 0.3f, ey - 0.4f, cx + 0.3f, ey + 0.1f, Col.HAIR)
                rect(cx + 0.2f, ey + 0.1f, ix1, ey + 0.6f, Col.HAIR)
            }
            else -> {
                rect(ix0, iy0, ix1, iy1, Col.LENS)
                val ps = if (big) 0.7f else 0.9f
                val px = cx + p.look * 0.5f
                val py = ey + p.lookY * 0.3f + (if (face == Face.SAD) 0.3f else 0f)
                rect(px - ps / 2, py - ps / 2, px + ps / 2, py + ps / 2, Col.HAIR)
                if (face == Face.SLEEPY) rect(ix0, iy0, ix1, ey, Col.SKIN_D)
            }
        }
    }

    private fun browsFront(face: Face, ey: Float) {
        val y = ey - 2.2f
        when (face) {
            Face.ANGRY -> {
                rect(-3.5f, y, -2.5f, y + 0.5f, Col.HAIR); rect(-2.5f, y + 0.3f, -1.5f, y + 0.8f, Col.HAIR); rect(-1.5f, y + 0.6f, -0.4f, y + 1.1f, Col.HAIR)
                rect(2.5f, y, 3.5f, y + 0.5f, Col.HAIR); rect(1.5f, y + 0.3f, 2.5f, y + 0.8f, Col.HAIR); rect(0.4f, y + 0.6f, 1.5f, y + 1.1f, Col.HAIR)
            }
            Face.SAD -> {
                rect(-3.5f, y + 0.6f, -2.5f, y + 1.1f, Col.HAIR); rect(-2.5f, y + 0.3f, -1.5f, y + 0.8f, Col.HAIR); rect(-1.5f, y, -0.4f, y + 0.5f, Col.HAIR)
                rect(2.5f, y + 0.6f, 3.5f, y + 1.1f, Col.HAIR); rect(1.5f, y + 0.3f, 2.5f, y + 0.8f, Col.HAIR); rect(0.4f, y, 1.5f, y + 0.5f, Col.HAIR)
            }
            Face.THINK -> {
                rect(-3.4f, y + 0.4f, -0.6f, y + 0.9f, Col.HAIR)
                rect(0.6f, y - 0.4f, 3.4f, y + 0.1f, Col.HAIR)
            }
            Face.SURPRISED -> {
                rect(-3.4f, y - 0.6f, -0.6f, y - 0.1f, Col.HAIR)
                rect(0.6f, y - 0.6f, 3.4f, y - 0.1f, Col.HAIR)
            }
            else -> Unit
        }
    }

    /** لحية داكنة أسفل الوجه والفم بلون وردي فوقها. */
    private fun beardMouthFront(face: Face, p: Pose) {
        rect(-4f, -3.9f, 4f, -1f, Col.BEARD)
        rect(-4f, -5f, -3.2f, -3.9f, Col.BEARD)
        rect(3.2f, -5f, 4f, -3.9f, Col.BEARD)
        rect(-2.6f, -4.5f, 2.6f, -3.7f, Col.BEARD)
        mouthFront(face, p)
    }

    // ───── الرأس الجانبي (يتجه نحو +x): عين واحدة، أنف، لحية وشارب للذكر ─────
    private fun headSide(p: Pose, face: Face, t: Float) {
        rect(-3.8f, -9.8f, 3.8f, -1f, Col.SKIN)
        rect(-3.8f, -1.8f, 3.8f, -1f, Col.SKIN_D)
        rect(3.8f, -5.4f, 5f, -3.9f, Col.SKIN)                 // الأنف
        rect(-4.6f, -12.2f, 4.2f, -8.2f, hairC)
        rect(-4.6f, -8.4f, -1.6f, -3.2f, hairC)                // شعر الخلف
        rect(2.6f, -8.4f, 4.2f, -7.7f, hairC)                  // الغرة
        if (fem) {
            // شعر طويل خلفي + ذيل حصان
            rect(-6.4f, -11.8f, -4.4f, -2f, hairC)
            val fl = sin(t * 3f) * 0.5f
            rect(-7.6f, -11.4f + fl, -6.2f, -6f + fl, hairC)
            rect(2.4f, -12.6f, 4.4f, -11.6f, Col.BOW)
            rect(3.2f, -12.9f, 3.7f, -11.3f, Col.BOW)
        }

        val ey = -5.7f
        val blink = face == Face.NORMAL && (t % 3.4f) < 0.12f
        if (!fem) rect(-3.5f, ey - 0.25f, 1.2f, ey + 0.25f, hairC)   // ذراع النظارة
        val x0 = 1.1f
        var x1 = 4.4f
        var y0 = ey - 1.3f
        var y1 = ey + 1.3f
        if (face == Face.SURPRISED) { x1 += 0.3f; y0 -= 0.3f; y1 += 0.3f }
        rect(x0, y0, x1, y1, hairC)
        val dn = if (face == Face.SAD) 0.3f else 0f
        when {
            blink || face == Face.CLOSED || face == Face.YAWN -> {
                rect(x0 + 0.5f, y0 + 0.5f, x1 - 0.5f, y1 - 0.5f, Col.SKIN_D)
                rect(x0 + 0.5f, ey, x1 - 0.5f, ey + 0.5f, Col.HAIR)
            }
            face == Face.HAPPY || face == Face.LAUGH -> {
                rect(x0 + 0.5f, y0 + 0.5f, x1 - 0.5f, y1 - 0.5f, Col.SKIN_D)
                rect(x0 + 0.5f, ey + 0.1f, x0 + 1.4f, ey + 0.6f, Col.HAIR)
                rect(x0 + 1.4f, ey - 0.4f, x1 - 1.4f, ey + 0.1f, Col.HAIR)
                rect(x1 - 1.4f, ey + 0.1f, x1 - 0.5f, ey + 0.6f, Col.HAIR)
            }
            else -> {
                rect(x0 + 0.5f, y0 + 0.5f, x1 - 0.5f, y1 - 0.5f, Col.LENS)
                rect(x1 - 1.6f, ey - 0.4f + dn, x1 - 0.6f, ey + 0.6f + dn, Col.HAIR)
                if (face == Face.SLEEPY) rect(x0 + 0.5f, y0 + 0.5f, x1 - 0.5f, ey, Col.SKIN_D)
            }
        }
        val yb = ey - 2.2f
        when (face) {
            Face.ANGRY -> { rect(1f, yb, 4.2f, yb + 0.5f, Col.HAIR); rect(3f, yb + 0.4f, 4.2f, yb + 1f, Col.HAIR) }
            Face.SAD -> rect(1f, yb + 0.7f, 4.2f, yb + 1.2f, Col.HAIR)
            Face.SURPRISED -> rect(1f, yb - 0.6f, 4.2f, yb - 0.1f, Col.HAIR)
            else -> Unit
        }

        // لحية وشارب (للذكر فقط)
        if (fem) {
            // فم الأنثى من الجانب
            val m = Col.MOUTH
            when (face) {
                Face.HAPPY -> { rect(1.9f, -3.3f, 4f, -2.8f, m); rect(3.6f, -3.8f, 4f, -3.3f, m) }
                Face.LAUGH -> rect(1.9f, -3.9f, 4f, -1.8f, m)
                Face.YAWN -> rect(2.2f, -4f, 4f, -1.5f, m)
                Face.SURPRISED -> rect(2.6f, -3.6f, 3.9f, -2.2f, m)
                Face.SAD -> { rect(2.2f, -3f, 4f, -2.5f, m); rect(3.6f, -2.5f, 4f, -2f, m) }
                else -> rect(2.4f, -3.3f, 4f, -2.8f, m)
            }
            return
        }
        rect(-1f, -3.9f, 3.8f, -1f, Col.BEARD)
        rect(2f, -4.6f, 4.4f, -3.7f, Col.BEARD)
        val m = Col.MOUTH
        when (face) {
            Face.HAPPY -> { rect(1.9f, -3.3f, 4f, -2.8f, m); rect(3.6f, -3.8f, 4f, -3.3f, m) }
            Face.LAUGH -> rect(1.9f, -3.9f, 4f, -1.8f, m)
            Face.YAWN -> rect(2.2f, -4f, 4f, -1.5f, m)
            Face.SURPRISED -> rect(2.6f, -3.6f, 3.9f, -2.2f, m)
            Face.SAD -> { rect(2.2f, -3f, 4f, -2.5f, m); rect(3.6f, -2.5f, 4f, -2f, m) }
            else -> rect(2.4f, -3.3f, 4f, -2.8f, m)
        }
    }
}
