package com.pixelpet.app

import android.graphics.Color
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val WOOD = Color.parseColor("#8A5A34")
private val WOOD_D = Color.parseColor("#6B4326")
private val MATTRESS = Color.parseColor("#EDEDED")
private val BLUE = Color.parseColor("#4E79D6")
private val BLUE_D = Color.parseColor("#3C62B5")
private val GRAY = Color.parseColor("#B9BDC4")
private val GRAY_L = Color.parseColor("#D6D9DE")
private val GRAY_D = Color.parseColor("#7C828B")
private val COAL = Color.parseColor("#202020")
private val CARROT = Color.parseColor("#F08A24")
private val STICK = Color.parseColor("#5A3B22")
private val RED = Color.parseColor("#D8352A")
private val GREEN = Color.parseColor("#4CA64C")
private val CHEESE = Color.parseColor("#F2B93B")
private val CRUST = Color.parseColor("#C9822E")
private val COFFEE_C = Color.parseColor("#4B2E1E")
private val TEA_C = Color.parseColor("#C98A3C")
private val YELLOW = Color.parseColor("#FFD84A")
private val PAPER = Color.parseColor("#F4E37A")
private val SKY = Color.parseColor("#7FD3FF")

/** الأغراض: خلف الشخصية، أمامها، أو محمولة باليد. */
class PropPainter(private val r: PetRenderer) {

    fun drawBehind(prop: Prop, p: Pose, t: Float) {
        when (prop) {
            Prop.BED -> {
                r.rect(-17f, -3f, 15f, 0f, WOOD)
                r.rect(-17f, -6.2f, -15.6f, 0f, WOOD_D)
                r.rect(13.6f, -4.6f, 15f, 0f, WOOD_D)
                r.rect(-15.6f, -4.2f, 13.6f, -3f, MATTRESS)
                r.rect(-15.2f, -6.6f, -8f, -4.2f, Col.WHITE)
                r.rect(-15.2f, -5.4f, -8f, -4.2f, Col.SNOW_D)
            }
            Prop.SNOWMAN -> snowman()
            else -> Unit
        }
    }

    private fun snowman() {
        val x = -12.5f
        r.disc(x, -4.3f, 4.3f, Col.SNOW)
        r.disc(x, -10.6f, 3.3f, Col.SNOW)
        r.disc(x, -15.2f, 2.5f, Col.SNOW)
        r.rect(x - 4.3f, -3f, x + 4.3f, 0f, Col.SNOW_D)
        // وجه
        r.rect(x - 1.4f, -16.1f, x - 0.8f, -15.5f, COAL)
        r.rect(x + 0.8f, -16.1f, x + 1.4f, -15.5f, COAL)
        r.rect(x, -15.0f, x + 2.0f, -14.5f, CARROT)
        // أزرار
        r.rect(x - 0.3f, -11.8f, x + 0.3f, -11.2f, COAL)
        r.rect(x - 0.3f, -10.0f, x + 0.3f, -9.4f, COAL)
        r.rect(x - 0.3f, -4.8f, x + 0.3f, -4.2f, COAL)
        // ذراعان من العصي
        r.rect(-18.5f, -11.8f, -15.8f, -11.2f, STICK)
        r.rect(-19.8f, -13.0f, -18.5f, -12.4f, STICK)
        r.rect(-9.2f, -11.8f, -6.5f, -11.2f, STICK)
        r.rect(-6.5f, -12.8f, -5.2f, -12.2f, STICK)
    }

    /** يُستدعى داخل تحويل اتجاه النظر (facing). */
    fun drawFront(prop: Prop, p: Pose, t: Float) {
        when (prop) {
            Prop.LAPTOP -> {
                r.rect(-5.8f, -9.8f, 5.8f, -3.4f, GRAY)
                r.rect(-5.8f, -9.8f, 5.8f, -9.3f, GRAY_L)
                r.rect(-0.7f, -7.2f, 0.7f, -5.8f, Col.WHITE)
                r.rect(-6.8f, -3.4f, 6.8f, -2.4f, GRAY_D)
            }
            Prop.BED -> if (p.lieAmt > 0.6f) {
                r.rect(-0.5f, -12.6f, 14.8f, -4.2f, BLUE)
                r.rect(-0.5f, -12.6f, 14.8f, -11.8f, BLUE_D)
                r.rect(-0.5f, -12.6f, 0.4f, -4.2f, Col.WHITE)
            }
            Prop.SNOWBALL_BIG -> {
                val cx = 9.5f
                r.disc(cx, -5f, 5f, Col.SNOW)
                r.rect(cx - 5f, -2.2f, cx + 5f, 0f, Col.SNOW_D, 120)
                for (k in 0..2) {
                    val a = p.propT * 3f + k * 2.1f
                    r.rect(cx + cos(a) * 3f, -5f + sin(a) * 3f, cx + cos(a) * 3f + 0.9f, -5f + sin(a) * 3f + 0.9f, Col.SNOW_D)
                }
            }
            Prop.SNOWBALL_SMALL -> if (p.propT > 0f && p.propT < 0.9f) {
                val x = 5f + 50f * p.propT
                val y = -15f - sin(PI.toFloat() * p.propT) * 8f + p.propT * 10f
                r.disc(x, y, 1.4f, Col.SNOW)
            }
            else -> Unit
        }
    }

    /** أغراض محمولة: تُرسم داخل تحويل الجسم عند اليدين. */
    fun drawHeld(prop: Prop, rx: Float, ry: Float, lx: Float, ly: Float, p: Pose, t: Float) {
        val mx = (rx + lx) / 2f
        val my = (ry + ly) / 2f
        when (prop) {
            Prop.COFFEE -> {
                r.rect(rx - 1.5f, ry - 2.6f, rx + 1.5f, ry + 0.4f, Col.WHITE)
                r.rect(rx - 1.5f, ry - 2.6f, rx + 1.5f, ry - 2.0f, COFFEE_C)
                r.rect(rx + 1.5f, ry - 1.8f, rx + 2.4f, ry - 0.2f, Col.WHITE)
            }
            Prop.TEA -> {
                r.rect(rx - 1.5f, ry - 2.6f, rx + 1.5f, ry + 0.4f, Col.SNOW_D)
                r.rect(rx - 1.3f, ry - 2.2f, rx + 1.3f, ry + 0.2f, TEA_C)
                r.rect(rx + 1.5f, ry - 1.8f, rx + 2.3f, ry - 0.4f, Col.SNOW_D)
            }
            Prop.PIZZA -> {
                val s = 1f - 0.55f * p.propT
                r.rect(rx - 3f * s, ry - 4.2f * s, rx + 3f * s, ry - 3.4f * s, CRUST)
                for (i in 0..5) {
                    val w = (3f - i * 0.5f) * s
                    val y0 = ry - 3.4f * s + i * 0.6f * s
                    r.rect(rx - w, y0, rx + w, y0 + 0.6f * s, CHEESE)
                }
                r.rect(rx - 1.2f * s, ry - 3.0f * s, rx - 0.4f * s, ry - 2.4f * s, RED)
                r.rect(rx + 0.4f * s, ry - 2.0f * s, rx + 1.2f * s, ry - 1.4f * s, RED)
            }
            Prop.APPLE -> {
                val s = 1f - 0.5f * p.propT
                r.disc(rx, ry - 1.6f, 1.9f * s, RED)
                r.rect(rx - 0.2f, ry - 4.0f * s, rx + 0.3f, ry - 3.0f * s, STICK)
                r.rect(rx + 0.3f, ry - 4.0f * s, rx + 1.4f, ry - 3.5f * s, GREEN)
            }
            Prop.PHONE -> {
                r.rect(mx - 1.4f, my - 3.4f, mx + 1.4f, my + 0.4f, COAL)
                r.rect(mx - 1.0f, my - 3.0f, mx + 1.0f, my - 0.1f, if ((t * 2f).toInt() % 2 == 0) SKY else Col.WHITE)
            }
            Prop.BOOK -> {
                r.rect(mx - 4.6f, my - 4.2f, mx + 4.6f, my + 1.6f, Color.parseColor("#8B2E2E"))
                r.rect(mx - 4f, my - 3.6f, mx - 0.2f, my + 1f, Col.WHITE)
                r.rect(mx + 0.2f, my - 3.6f, mx + 4f, my + 1f, Col.WHITE)
                r.rect(mx - 0.2f, my - 3.8f, mx + 0.2f, my + 1.2f, STICK)
                for (k in 0..2) {
                    val yy = my - 2.8f + k * 1.2f
                    r.rect(mx - 3.4f, yy, mx - 0.8f, yy + 0.35f, Col.SNOW_D)
                    r.rect(mx + 0.8f, yy, mx + 3.4f, yy + 0.35f, Col.SNOW_D)
                }
                val w = abs(cos(p.propT * 2f * PI.toFloat())) * 3.6f
                r.rect(mx + 0.2f, my - 3.6f, mx + 0.2f + w, my + 1f, Col.SNOW_D)
            }
            Prop.NOTEBOOK -> {
                r.rect(mx - 2.8f, my - 4.4f, mx + 2.8f, my + 1.2f, PAPER)
                for (k in 0..3) r.rect(mx - 2.2f, my - 3.4f + k * 1.2f, mx + 2.2f, my - 3.1f + k * 1.2f, Color.parseColor("#B9A93E"))
                for (k in 0..3) r.rect(mx - 3.1f, my - 3.8f + k * 1.4f, mx - 2.5f, my - 3.3f + k * 1.4f, GRAY_D)
                r.rect(rx - 0.25f, ry - 3.4f, rx + 0.25f, ry, Color.parseColor("#2255AA"))
            }
            Prop.SCREWDRIVER -> {
                val tap = p.propT * 0.4f
                r.rect(rx - 0.7f, ry - 2.8f, rx + 0.7f, ry - 0.2f, RED)
                r.rect(rx - 0.25f, ry - 0.2f, rx + 0.25f, ry + 3.2f + tap, GRAY)
            }
            Prop.SNOWBALL_SMALL -> if (p.propT == 0f) r.disc(rx, ry - 1f, 1.4f, Col.SNOW)
            Prop.UMBRELLA -> umbrella(rx, ry, t)
            else -> Unit
        }
    }

    private fun umbrella(hx: Float, hy: Float, t: Float) {
        val top = -31f
        val cx = hx - 3f
        // العصا
        r.paint.style = Paint.Style.STROKE
        r.paint.strokeWidth = 0.5f
        r.paint.color = STICK
        r.c.drawLine(hx, hy, cx, top, r.paint)
        r.paint.style = Paint.Style.FILL
        // المظلة
        val rows = arrayOf(floatArrayOf(-31f, 3f), floatArrayOf(-30f, 6f), floatArrayOf(-29f, 8f), floatArrayOf(-28f, 9f))
        for (row in rows) {
            val y = row[0]; val hw = row[1]
            var x = -hw
            var i = 0
            while (x < hw) {
                val x1 = minOf(x + 3f, hw)
                r.rect(cx + x, y, cx + x1, y + 1f, if (i % 2 == 0) BLUE else Col.WHITE)
                x = x1; i++
            }
        }
        r.rect(cx - 9f, -27f, cx - 8f, -26.4f, BLUE)
        r.rect(cx - 3f, -27f, cx - 2f, -26.4f, Col.WHITE)
        r.rect(cx + 3f, -27f, cx + 4f, -26.4f, BLUE)
        r.rect(cx + 8f, -27f, cx + 9f, -26.4f, Col.WHITE)
    }
}

/** مؤثرات عائمة: z z z، قلوب، نجوم، بخار، ثلج... */
class FxPainter(private val r: PetRenderer) {
    private val heart = arrayOf(".X.X.", "XXXXX", "XXXXX", ".XXX.", "..X..")
    private val star = arrayOf("..X..", "..X..", "XXXXX", "..X..", "..X..")
    private val bulb = arrayOf(".XXX.", "XXXXX", "XXXXX", "XXXXX", ".XXX.", "..o..", "..o..")
    private val anger = arrayOf("X...X", ".X.X.", "..X..", ".X.X.", "X...X")
    private val drop = arrayOf(".X.", "XXX", "XXX", ".X.")

    private fun frac(x: Float): Float = x - kotlin.math.floor(x)

    fun draw(fx: Fx, p: Pose, t: Float, facing: Int) {
        val lying = p.lieAmt > 0.5f
        val headTop = r.neckW - 13f
        val cx = r.curDx
        when (fx) {
            Fx.ZZZ -> for (i in 0..2) {
                val ph = frac(t * 0.45f + i / 3f)
                r.outlineText("Z", -6f + ph * 7f + i, -19f - ph * 12f, 2.6f + i * 0.9f, Col.WHITE, ((1f - ph) * 255).toInt())
            }
            Fx.HEARTS -> for (i in 0..3) {
                val ph = frac(t * 0.45f + i * 0.25f)
                r.sprite(heart, cx + sin(ph * 6f + i * 1.7f) * 7f - 1.4f, headTop - 2f - ph * 12f, 0.6f,
                    Color.parseColor("#FF5A7A"), Color.parseColor("#FF5A7A"), ((1f - ph) * 255).toInt())
            }
            Fx.STARS -> for (i in 0..4) {
                val a = t * 1.5f + i * 1.26f
                val s = if (((t * 4f).toInt() + i) % 2 == 0) 0.6f else 0.45f
                r.sprite(star, cx + cos(a) * 10f - 1.4f, r.neckW - 6f + sin(a) * 8f, s, YELLOW)
            }
            Fx.STEAM -> for (i in 0..2) {
                val ph = frac(t * 0.8f + i * 0.33f)
                val x = r.rhX + sin(ph * 8f + i) * 0.9f
                r.rect(x - 0.3f, r.rhY - 3.4f - ph * 6f, x + 0.3f, r.rhY - 2.6f - ph * 6f, Col.WHITE, ((1f - ph) * 200).toInt())
            }
            Fx.TEAR -> {
                val ph = frac(t * 0.8f)
                r.sprite(drop, cx - 2.9f, r.neckW - 4.2f + ph * 4.5f, 0.55f, Color.parseColor("#6EC1FF"), Color.parseColor("#6EC1FF"), ((1f - ph) * 255).toInt())
            }
            Fx.QMARK -> r.outlineText("?", cx + 5f + sin(t * 2f) * 0.6f, headTop - 0.5f, 5f, YELLOW)
            Fx.EXCL -> {
                val pop = 4.6f + abs(sin(t * 8f)) * 0.8f
                r.outlineText("!", cx + (if (lying) -6f else 4.5f), (if (lying) -22f else headTop) - 0.5f, pop, RED)
            }
            Fx.BULB -> if (t > 1.0f && ((t * 5f).toInt() % 2 == 0 || t > 2f)) {
                r.sprite(bulb, cx - 1.5f, headTop - 4f, 0.6f, YELLOW, GRAY_D)
                for (k in 0..2) {
                    val a = -2.4f + k * 0.8f
                    r.rect(cx + cos(a) * 3.4f - 0.2f, headTop - 1.2f + sin(a) * 3.4f, cx + cos(a) * 3.4f + 0.2f, headTop - 0.8f + sin(a) * 3.4f, YELLOW)
                }
            }
            Fx.NOTES -> for (i in 0..2) {
                val ph = frac(t * 0.6f + i * 0.33f)
                r.outlineText(if (i == 1) "♫" else "♪", -8f + i * 8f + sin(t * 2f + i) * 2f, -22f - ph * 10f, 4.4f, Color.parseColor("#7FF0E8"), ((1f - ph * 0.6f) * 255).toInt())
            }
            Fx.SNOW -> for (i in 0..13) {
                val sp = 0.35f + (i % 3) * 0.12f
                val ph = frac(t * sp + i * 0.173f)
                val x = ((i * 37) % 36) - 18f + sin(t + i) * 0.6f
                r.rect(x, -38f + ph * 38f, x + 0.7f, -37.3f + ph * 38f, Col.SNOW)
            }
            Fx.SWEAT -> {
                val ph = frac(t * 1.1f)
                val x = if (lying) -4f else 5.8f
                val y = if (lying) -19f else r.neckW - 8f
                r.sprite(drop, x, y + ph * 5f, 0.55f, Color.parseColor("#6EC1FF"), Color.parseColor("#6EC1FF"), ((1f - ph) * 255).toInt())
            }
            Fx.ANGER -> {
                val pulse = if ((t * 6f).toInt() % 2 == 0) 0.6f else 0.5f
                r.sprite(anger, cx + 3.5f, headTop - 1f, pulse, RED)
                for (i in 0..1) {
                    val ph = frac(t * 0.9f + i * 0.5f)
                    val sx = if (i == 0) -6.5f else 6.5f
                    r.rect(cx + sx, r.neckW - 8f - ph * 5f, cx + sx + 1.2f, r.neckW - 7.2f - ph * 5f, Col.SNOW_D, ((1f - ph) * 200).toInt())
                }
            }
            Fx.NONE -> Unit
        }
    }
}
