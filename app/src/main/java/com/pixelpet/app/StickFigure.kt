package com.pixelpet.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * قواعد ثابتة لوضع العيدان فقط (انظر STICKMEN_RULES.md). لا تؤثر على وضع البيكسل.
 * الرموز فوق الرؤوس ممنوعة: كل حالة تُفهم من حركة الجسم، ولا كلام ولا وجوه.
 */
object StickRules {
    const val OVERHEAD_SYMBOLS = false
    const val FACES = false
}

/** ألوان الشخصيات الخمس. */
enum class SColor(val argb: Int) {
    RED(Color.parseColor("#E53935")),
    BLUE(Color.parseColor("#4FC3F7")),
    GREEN(Color.parseColor("#43A047")),
    YELLOW(Color.parseColor("#FBC02D")),
    ORANGE(Color.parseColor("#FB8C00"))
}

/** يُغمّق لوناً بمعامل f (0..1). */
fun shade(c: Int, f: Float): Int =
    Color.rgb((Color.red(c) * f).toInt(), (Color.green(c) * f).toInt(), (Color.blue(c) * f).toInt())

/** أغراض تُمسك باليد. */
enum class SProp { NONE, PICKAXE, BLOCK, TORCH, FLAG, DIAMOND, SWORD }

/** رموز تظهر فوق الرأس بدل الكلام. */
enum class SEmote { NONE, HEART, EXCL, QUEST, STAR, NOTE, SWEAT, ANGER, BULB, ZZZ, SPARK, CHECK }

/**
 * وضعية العود: زوايا المفاصل بالدرجات.
 * الذراع: sa = زاوية الكتف (0 = متدلية للأسفل، 90 = أفقياً للأمام، 180 = للأعلى)،
 *          eb = ثني المرفق (موجب = الساعد يدور نحو الأمام/الأعلى).
 * الرجل: ha = زاوية الورك (موجب = للأمام)، kb = ثني الركبة (موجب = الساق للخلف).
 * n = القريبة من المشاهد، f = البعيدة (تُرسم أغمق). front = منظر أمامي متماثل (للإشارات الموجهة للمستخدم).
 */
class SPose {
    var lean = 0f; var head = 0f
    var nSa = 0f; var nEb = 0f; var fSa = 0f; var fEb = 0f
    var nHa = 0f; var nKb = 0f; var fHa = 0f; var fKb = 0f
    var rot = 0f          // دوران الجسم كله حول الورك (للاستلقاء والسقوط)
    var lift = 0f         // ارتفاع عن الأرض (للقفز)
    var dx = 0f           // إزاحة أفقية
    var front = false     // منظر أمامي
    var thumb = false     // إبهام مرفوع في اليد القريبة

    fun reset() {
        lean = 0f; head = 0f
        nSa = 0f; nEb = 0f; fSa = 0f; fEb = 0f
        nHa = 0f; nKb = 0f; fHa = 0f; fKb = 0f
        rot = 0f; lift = 0f; dx = 0f; front = false; thumb = false
    }
}

/**
 * يرسم شخصية عيدان بمفاصل (رأس دائري، جذع، ذراعان وساقان من قطعتين) بحواف مستديرة.
 * الأصل = وسط الأرض تحت القدمين. الأرض تُحسب تلقائياً: أخفض نقطة في الجسم تلامس الأرض.
 */
class StickRenderer(density: Float) {
    val unit = 1.8f * density
    val winW = (124f * density).toInt()
    val winH = (190f * density).toInt()
    val groundY = winH - 18f * density
    val cx = winW / 2f

    private companion object {
        const val TORSO = StickSolve.TORSO
        const val NECK = StickSolve.NECK
        const val HEAD_R = StickSolve.HEAD_R
        const val ARM1 = StickSolve.ARM1
        const val ARM2 = StickSolve.ARM2
        const val LEG1 = StickSolve.LEG1
        const val LEG2 = StickSolve.LEG2
        const val LIMB_W = 4.3f
        const val TORSO_W = 4.8f

        const val HIP = StickSolve.HIP; const val NECKP = StickSolve.NECKP; const val SH = StickSolve.SH; const val HEAD = StickSolve.HEAD
        const val ELB_N = StickSolve.ELB_N; const val HND_N = StickSolve.HND_N; const val ELB_F = StickSolve.ELB_F; const val HND_F = StickSolve.HND_F
        const val KNE_N = StickSolve.KNE_N; const val FOT_N = StickSolve.FOT_N; const val KNE_F = StickSolve.KNE_F; const val FOT_F = StickSolve.FOT_F
        const val N = StickSolve.N
    }

    private val px = FloatArray(N)
    private val py = FloatArray(N)
    private val paint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val text = Paint().apply {
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val path = Path()
    private val oval = RectF()

    /** موضع الرأس على الشاشة (بعد آخر رسم) لاستخدام المؤثرات. */
    var headScreenX = 0f; private set
    var headScreenY = 0f; private set

    // ───── الحل الأمامي للمفاصل (مفوَّض إلى StickSolve النقي) ─────
    private val solved = FloatArray(StickSolve.N * 2)
    private fun solve(p: SPose) {
        StickSolve.solve(p, solved)
        for (i in 0 until N) { px[i] = solved[2 * i]; py[i] = solved[2 * i + 1] }
    }

    private fun seg(c: Canvas, a: Int, b: Int, col: Int, w: Float) {
        paint.color = col; paint.strokeWidth = w
        c.drawLine(px[a], py[a], px[b], py[b], paint)
    }

    /** ذراع منحنية (لا مكسورة): منحنى يمر بالمرفق. */
    private fun armCurve(c: Canvas, s: Int, e: Int, h: Int, col: Int) {
        val cxp = 2f * px[e] - 0.5f * (px[s] + px[h])
        val cyp = 2f * py[e] - 0.5f * (py[s] + py[h])
        paint.color = col; paint.strokeWidth = LIMB_W
        paint.style = Paint.Style.STROKE
        path.reset(); path.moveTo(px[s], py[s]); path.quadTo(cxp, cyp, px[h], py[h])
        c.drawPath(path, paint)
    }

    private fun limb(c: Canvas, a: Int, b: Int, d: Int, col: Int) {
        seg(c, a, b, col, LIMB_W); seg(c, b, d, col, LIMB_W)
    }

    /**
     * @param block كتلة أمام الشخصية: 0 لا شيء، 1 حجر، 2 حجر متشقق، 3 خام ماس
     */
    fun draw(
        c: Canvas, color: Int, p: SPose, facing: Float, prop: SProp, t: Float,
        emote: SEmote, emoteT: Float, block: Int = 0
    ) {
        solve(p)
        val far = shade(color, 0.68f)

        c.save()
        c.translate(cx, groundY)
        c.scale(unit * facing, unit)

        // ظل على الأرض يصغر مع الارتفاع
        val sh = (1f - (p.lift / 40f)).coerceIn(0.35f, 1f)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb((70 * sh).toInt(), 0, 0, 0)
        oval.set(px[HIP] - 9f * sh, -1.6f, px[HIP] + 9f * sh, 1.8f)
        c.drawOval(oval, paint)
        paint.style = Paint.Style.STROKE

        if (block != 0) drawBlock(c, block)

        if (p.front) {
            limb(c, HIP, KNE_F, FOT_F, color); limb(c, HIP, KNE_N, FOT_N, color)
            seg(c, HIP, NECKP, color, TORSO_W)
            armCurve(c, SH, ELB_F, HND_F, color)
            armCurve(c, SH, ELB_N, HND_N, color)
            drawHead(c, color)          // الرأس فوق الجميع: الذراع تختفي خلفه
        } else {
            armCurve(c, SH, ELB_F, HND_F, far)
            limb(c, HIP, KNE_F, FOT_F, far)
            seg(c, HIP, NECKP, color, TORSO_W)
            limb(c, HIP, KNE_N, FOT_N, color)
            armCurve(c, SH, ELB_N, HND_N, color)
            drawHead(c, color)          // الرأس فوق الجميع: الذراع تختفي خلفه
        }

        if (p.thumb) {
            paint.color = color; paint.strokeWidth = 2.4f
            c.drawLine(px[HND_N], py[HND_N], px[HND_N], py[HND_N] - 4.2f, paint)
        }
        if (prop != SProp.NONE) drawProp(c, prop, p, t)

        // موضع الرأس على الشاشة
        headScreenX = cx + px[HEAD] * unit * facing
        headScreenY = groundY + py[HEAD] * unit
        c.restore()

        if (StickRules.OVERHEAD_SYMBOLS && emote != SEmote.NONE) {
            val size = 7.5f * unit
            val ey = max(size * 1.2f, headScreenY - (HEAD_R + 8.5f) * unit)
            drawEmote(c, emote, headScreenX, ey, size, emoteT)
        }
    }

    private fun drawHead(c: Canvas, color: Int) {
        paint.style = Paint.Style.FILL
        paint.color = color
        c.drawCircle(px[HEAD], py[HEAD], HEAD_R, paint)
        paint.style = Paint.Style.STROKE
    }

    // ───── الأغراض (بإحداثيات محلية: +y على امتداد الساعد، +x نحو الأمام) ─────
    private fun drawProp(c: Canvas, prop: SProp, p: SPose, t: Float) {
        val a = (p.nSa + p.nEb) * (if (p.front) 1f else 1f)
        c.save()
        c.translate(px[HND_N], py[HND_N])
        c.rotate(p.rot - a)
        paint.strokeCap = Paint.Cap.BUTT
        when (prop) {
            SProp.PICKAXE -> {
                paint.color = Color.parseColor("#8D6E63"); paint.strokeWidth = 2.2f
                c.drawLine(0f, -4f, 0f, 15f, paint)
                paint.color = Color.parseColor("#4DD0E1"); paint.strokeWidth = 3.2f
                c.drawLine(-7.5f, 13.2f, 7.5f, 13.2f, paint)
                paint.color = Color.parseColor("#00ACC1"); paint.strokeWidth = 3.2f
                c.drawLine(-7.5f, 13.2f, -5.8f, 13.2f, paint)
                c.drawLine(5.8f, 13.2f, 7.5f, 13.2f, paint)
            }
            SProp.BLOCK -> block(c, -5f, 1f, 10f)
            SProp.TORCH -> {
                paint.color = Color.parseColor("#795548"); paint.strokeWidth = 2.4f
                c.drawLine(0f, -2f, 0f, 9f, paint)
                val fl = 1f + sin(t * 14f) * 0.12f
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#FF9800"); c.drawCircle(0f, 11f, 3.3f * fl, paint)
                paint.color = Color.parseColor("#FFEB3B"); c.drawCircle(0f, 11.3f, 1.9f * fl, paint)
                paint.style = Paint.Style.STROKE
            }
            SProp.FLAG -> {
                paint.color = Color.parseColor("#795548"); paint.strokeWidth = 1.8f
                c.drawLine(0f, -5f, 0f, 20f, paint)
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#E53935")
                path.reset()
                val w = sin(t * 9f) * 1.2f
                path.moveTo(0f, 20f); path.lineTo(11f, 17f + w); path.lineTo(0f, 13f); path.close()
                c.drawPath(path, paint)
                paint.style = Paint.Style.STROKE
            }
            SProp.DIAMOND -> {
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#4DD0E1")
                path.reset()
                path.moveTo(0f, 0f); path.lineTo(4.2f, 4f); path.lineTo(0f, 9f); path.lineTo(-4.2f, 4f); path.close()
                c.drawPath(path, paint)
                paint.color = Color.parseColor("#B2EBF2"); c.drawCircle(-1.2f, 3f, 1f, paint)
                paint.style = Paint.Style.STROKE
            }
            SProp.SWORD -> {
                paint.color = Color.parseColor("#5D4037"); paint.strokeWidth = 2.6f
                c.drawLine(0f, -3.5f, 0f, 3f, paint)
                paint.color = Color.parseColor("#37474F"); paint.strokeWidth = 2.2f
                c.drawLine(-3.6f, 3.4f, 3.6f, 3.4f, paint)
                paint.color = Color.parseColor("#00ACC1"); paint.strokeWidth = 3.8f
                c.drawLine(0f, 4.2f, 0f, 21f, paint)
                paint.color = Color.parseColor("#E0F7FA"); paint.strokeWidth = 1.5f
                c.drawLine(0f, 4.6f, 0f, 20f, paint)
            }
            SProp.NONE -> Unit
        }
        paint.strokeCap = Paint.Cap.ROUND
        c.restore()
    }

    /** مكعب ببكسل كبير على طريقة ماين كرافت. */
    private fun block(c: Canvas, x: Float, y: Float, s: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#8D6E63"); c.drawRect(x, y, x + s, y + s, paint)
        paint.color = Color.parseColor("#66BB6A"); c.drawRect(x, y, x + s, y + s * 0.3f, paint)
        paint.color = Color.parseColor("#5D4037"); c.drawRect(x + s * 0.2f, y + s * 0.55f, x + s * 0.4f, y + s * 0.75f, paint)
        c.drawRect(x + s * 0.62f, y + s * 0.7f, x + s * 0.82f, y + s * 0.9f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.9f
        paint.color = Color.parseColor("#3E2723"); paint.strokeCap = Paint.Cap.SQUARE
        c.drawLine(x, y, x + s, y, paint); c.drawLine(x + s, y, x + s, y + s, paint)
        c.drawLine(x + s, y + s, x, y + s, paint); c.drawLine(x, y + s, x, y, paint)
        paint.strokeCap = Paint.Cap.ROUND
    }

    /** كتلة على الأرض أمام الشخصية للتعدين. */
    private fun drawBlock(c: Canvas, kind: Int) {
        val x = 15f; val s = 11f; val y = -s
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#9E9E9E"); c.drawRect(x, y, x + s, y + s, paint)
        paint.color = Color.parseColor("#757575")
        c.drawRect(x + 1.5f, y + 1.5f, x + 4f, y + 4f, paint); c.drawRect(x + 6.5f, y + 5f, x + 9.5f, y + 8f, paint)
        c.drawRect(x + 2f, y + 7.5f, x + 4.2f, y + 9.5f, paint)
        if (kind == 3) {
            paint.color = Color.parseColor("#4DD0E1")
            c.drawRect(x + 2f, y + 2f, x + 4.4f, y + 4.4f, paint); c.drawRect(x + 6.2f, y + 3.4f, x + 8.6f, y + 5.8f, paint)
            c.drawRect(x + 3.6f, y + 6.6f, x + 6f, y + 9f, paint)
        }
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.9f; paint.strokeCap = Paint.Cap.SQUARE
        paint.color = Color.parseColor("#424242")
        c.drawLine(x, y, x + s, y, paint); c.drawLine(x + s, y, x + s, y + s, paint)
        c.drawLine(x + s, y + s, x, y + s, paint); c.drawLine(x, y + s, x, y, paint)
        if (kind == 2) {
            paint.strokeWidth = 0.8f; paint.color = Color.parseColor("#212121")
            c.drawLine(x + 5f, y, x + 4f, y + 4f, paint); c.drawLine(x + 4f, y + 4f, x + 7f, y + 7f, paint)
            c.drawLine(x + 7f, y + 7f, x + 6f, y + s, paint)
        }
        paint.strokeCap = Paint.Cap.ROUND
    }

    // ───── الرموز فوق الرأس ─────
    private fun outlineText(c: Canvas, s: String, x: Float, y: Float, size: Float, fill: Int, outline: Int) {
        text.textSize = size
        text.style = Paint.Style.STROKE; text.strokeWidth = size * 0.16f; text.color = outline
        c.drawText(s, x, y, text)
        text.style = Paint.Style.FILL; text.color = fill
        c.drawText(s, x, y, text)
    }

    private fun drawEmote(c: Canvas, e: SEmote, x: Float, y: Float, s: Float, t: Float) {
        val pop = min(1f, t / 0.18f)
        val sc = (0.4f + 0.6f * pop) * (1f + 0.06f * sin(t * 8f))
        val bob = sin(t * 5f) * s * 0.1f
        c.save()
        c.translate(x, y + bob)
        c.scale(sc, sc)
        val dark = Color.parseColor("#3A1F1F")
        when (e) {
            SEmote.HEART -> {
                path.reset()
                path.moveTo(0f, 0.95f * s)
                path.cubicTo(-1.6f * s, -0.1f * s, -0.9f * s, -1.2f * s, 0f, -0.55f * s)
                path.cubicTo(0.9f * s, -1.2f * s, 1.6f * s, -0.1f * s, 0f, 0.95f * s)
                path.close()
                paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#F44336"); c.drawPath(path, paint)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = s * 0.12f; paint.color = Color.parseColor("#B71C1C"); c.drawPath(path, paint)
            }
            SEmote.EXCL -> outlineText(c, "!", 0f, s * 0.8f, s * 2.6f, Color.parseColor("#FF5252"), dark)
            SEmote.QUEST -> outlineText(c, "?", 0f, s * 0.8f, s * 2.6f, Color.parseColor("#FFD740"), dark)
            SEmote.NOTE -> outlineText(c, "♪", 0f, s * 0.8f, s * 2.6f, Color.parseColor("#4DD0E1"), dark)
            SEmote.ZZZ -> {
                outlineText(c, "Z", -s * 0.5f, s * 0.7f, s * 1.5f, Color.WHITE, dark)
                outlineText(c, "z", s * 0.4f, -s * 0.2f, s * 1.1f, Color.WHITE, dark)
            }
            SEmote.STAR, SEmote.SPARK -> {
                path.reset()
                val outer = s * 1.05f; val inner = s * if (e == SEmote.STAR) 0.45f else 0.2f
                val pts = if (e == SEmote.STAR) 5 else 4
                for (i in 0 until pts * 2) {
                    val ang = -1.5708f + i * 3.14159f / pts
                    val rr = if (i % 2 == 0) outer else inner
                    val px2 = cos(ang) * rr; val py2 = sin(ang) * rr
                    if (i == 0) path.moveTo(px2, py2) else path.lineTo(px2, py2)
                }
                path.close()
                paint.style = Paint.Style.FILL
                paint.color = if (e == SEmote.STAR) Color.parseColor("#FFD54F") else Color.parseColor("#FFFFFF")
                c.drawPath(path, paint)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = s * 0.1f; paint.color = Color.parseColor("#F57F17"); c.drawPath(path, paint)
            }
            SEmote.SWEAT -> {
                path.reset()
                path.moveTo(0f, -s); path.cubicTo(s * 0.9f, 0f, s * 0.7f, s * 0.9f, 0f, s * 0.9f)
                path.cubicTo(-s * 0.7f, s * 0.9f, -s * 0.9f, 0f, 0f, -s); path.close()
                paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#4FC3F7"); c.drawPath(path, paint)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = s * 0.1f; paint.color = Color.parseColor("#0277BD"); c.drawPath(path, paint)
            }
            SEmote.ANGER -> {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = s * 0.3f; paint.color = Color.parseColor("#E53935")
                for (sx in intArrayOf(-1, 1)) for (sy in intArrayOf(-1, 1)) {
                    c.drawLine(sx * 0.25f * s, sy * 0.95f * s, sx * 0.25f * s, sy * 0.35f * s, paint)
                    c.drawLine(sx * 0.95f * s, sy * 0.25f * s, sx * 0.35f * s, sy * 0.25f * s, paint)
                }
            }
            SEmote.BULB -> {
                paint.style = Paint.Style.FILL; paint.color = Color.parseColor("#FFEE58"); c.drawCircle(0f, -s * 0.15f, s * 0.7f, paint)
                paint.color = Color.parseColor("#9E9E9E"); c.drawRect(-s * 0.3f, s * 0.5f, s * 0.3f, s * 0.9f, paint)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = s * 0.14f; paint.color = Color.parseColor("#FFD600")
                for (k in 0..4) {
                    val ang = -2.6f + k * 0.55f
                    c.drawLine(cos(ang) * s * 1.0f, sin(ang) * s * 1.0f - s * 0.15f, cos(ang) * s * 1.4f, sin(ang) * s * 1.4f - s * 0.15f, paint)
                }
            }
            SEmote.CHECK -> {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = s * 0.4f; paint.color = Color.parseColor("#2E7D32")
                c.drawLine(-s * 0.8f, 0f, -s * 0.2f, s * 0.6f, paint); c.drawLine(-s * 0.2f, s * 0.6f, s * 0.9f, -s * 0.7f, paint)
                paint.strokeWidth = s * 0.25f; paint.color = Color.parseColor("#66BB6A")
                c.drawLine(-s * 0.8f, 0f, -s * 0.2f, s * 0.6f, paint); c.drawLine(-s * 0.2f, s * 0.6f, s * 0.9f, -s * 0.7f, paint)
            }
            SEmote.NONE -> Unit
        }
        paint.style = Paint.Style.STROKE
        c.restore()
    }
}
