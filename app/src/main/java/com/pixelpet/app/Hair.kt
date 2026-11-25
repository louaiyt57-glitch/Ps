package com.pixelpet.app

import android.graphics.Color
import kotlin.math.abs

/** BACK: يُرسم قبل الوجه (خلف الرأس)، TOP: يُرسم فوق الوجه (غرّة، قمة الرأس...). */
enum class Layer { BACK, TOP }

/**
 * سياق رسم الشعر داخل إحداثيات الرأس (الأصل عند الرقبة، والوجه من y=-9.8 إلى y=-1).
 * sway: إزاحة أفقية للخصلات المتدلية (موجبة = نحو +x)، تتغير مع الحركة.
 */
class HC(
    val r: PetRenderer, val look: Look, val side: Boolean, val sway: Float, val t: Float
) {
    val col = look.hairCol
    val dark = look.hairD
    val skinD = look.skinD
    fun rect(x0: Float, y0: Float, x1: Float, y1: Float, c: Int, a: Int = 255) = r.rect(x0, y0, x1, y1, c, a)
    fun disc(cx: Float, cy: Float, rad: Float, c: Int) = r.disc(cx, cy, rad, c)
}

private val TIE = Color.parseColor("#E84393")

/** دوال رسم التسريحات: دالة لكل تسريحة. */
object Hair {

    fun draw(x: HC, style: HairStyle, layer: Layer) {
        when (style) {
            HairStyle.SHORT -> short(x, layer)
            HairStyle.LONG -> long(x, layer)
            HairStyle.PONYTAIL -> ponytail(x, layer)
            HairStyle.BRAID -> braid(x, layer)
            HairStyle.BUN -> bun(x, layer)
            HairStyle.CURLY -> curly(x, layer)
            HairStyle.MOHAWK -> mohawk(x, layer)
            HairStyle.SPIKY -> spiky(x, layer)
        }
    }

    // ───── أجزاء مشتركة ─────
    private fun capFront(x: HC) {
        x.rect(-4.6f, -12.2f, 4.6f, -8.2f, x.col)
        x.rect(-4.6f, -8.4f, -3.6f, -4.6f, x.col)
        x.rect(3.6f, -8.4f, 4.6f, -4.6f, x.col)
    }

    private fun capSide(x: HC, backTo: Float) {
        x.rect(-4.6f, -12.2f, 4.2f, -8.2f, x.col)
        x.rect(2.6f, -8.4f, 4.2f, -7.7f, x.col)                  // الغرّة
        x.rect(-4.6f, -8.4f, -1.6f, backTo, x.col)               // الشعر خلف الأذن
    }

    // ───── 1) قصير ─────
    fun short(x: HC, layer: Layer) {
        if (layer != Layer.TOP) return
        if (x.side) capSide(x, -3.2f) else capFront(x)
    }

    // ───── 2) طويل ─────
    fun long(x: HC, layer: Layer) {
        if (layer == Layer.BACK) {
            for (i in 0..3) {
                val y0 = -10.5f + i * 4.5f
                val off = x.sway * (i / 3f)
                val c = if (i == 3) x.dark else x.col
                if (x.side) x.rect(-5.8f + off, y0, -1.4f + off, y0 + 4.6f, c)
                else {
                    x.rect(-5.6f + off, y0, -3.6f + off, y0 + 4.6f, c)
                    x.rect(3.6f + off, y0, 5.6f + off, y0 + 4.6f, c)
                }
            }
        } else {
            if (x.side) {
                capSide(x, -3.2f)
            } else {
                x.rect(-4.6f, -12.2f, 4.6f, -8.2f, x.col)
                x.rect(-4.6f, -8.4f, -3.7f, -1.5f, x.col)        // خصلتان تحيطان بالوجه
                x.rect(3.7f, -8.4f, 4.6f, -1.5f, x.col)
                x.rect(-4.6f, -8.4f, 1.8f, -7.6f, x.col)        // غرّة جانبية
            }
        }
    }

    // ───── 3) ذيل حصان ─────
    fun ponytail(x: HC, layer: Layer) {
        if (layer == Layer.BACK) {
            if (x.side) {
                for (i in 0..5) {
                    val off = x.sway * (i / 5f)
                    val wdt = 2.3f - i * 0.25f
                    val x0 = -4.8f - wdt - i * 0.35f + off
                    x.rect(x0, -11.2f + i * 2.4f, x0 + wdt + 0.8f, -9f + i * 2.4f, if (i == 5) x.dark else x.col)
                }
                x.rect(-5.4f, -11.8f, -3.8f, -10.4f, TIE)
            } else {
                for (i in 0..3) {
                    val off = x.sway * (i / 3f)
                    val wdt = 2.2f - i * 0.3f
                    x.rect(4.2f + off, -10.5f + i * 3f, 4.2f + wdt + off, -7.9f + i * 3f, if (i == 3) x.dark else x.col)
                }
                x.rect(4.2f, -11f, 6.4f, -10.1f, TIE)
            }
        } else {
            if (x.side) capSide(x, -5.4f) else capFront(x)
        }
    }

    // ───── 4) ضفيرة ─────
    fun braid(x: HC, layer: Layer) {
        if (layer == Layer.BACK) {
            if (x.side) {
                for (i in 0..5) {
                    val off = x.sway * (i / 5f)
                    val x0 = -5.3f - i * 0.45f + off
                    x.rect(x0, -9f + i * 2.6f, x0 + 2f, -6.2f + i * 2.6f, if (i % 2 == 0) x.col else x.dark)
                }
                x.rect(-8.2f + x.sway, 6.2f, -5.8f + x.sway, 7.4f, TIE)
            }
        } else {
            if (x.side) {
                capSide(x, -5f)
            } else {
                capFront(x)
                for (i in 0..5) {
                    val off = x.sway * (i / 5f)
                    val bx = 3.7f + (i % 2) * 0.5f + off
                    x.rect(bx, -5f + i * 2.5f, bx + 2f, -2.3f + i * 2.5f, if (i % 2 == 0) x.col else x.dark)
                }
                x.rect(3.7f + x.sway, 9.4f, 5.9f + x.sway, 10.6f, TIE)
            }
        }
    }

    // ───── 5) كعكة ─────
    fun bun(x: HC, layer: Layer) {
        if (layer != Layer.TOP) return
        val bob = abs(x.sway) * 0.25f
        if (x.side) {
            capSide(x, -5f)
            x.disc(-2.6f + x.sway * 0.3f, -14.2f - bob, 2.7f, x.col)
        } else {
            capFront(x)
            x.disc(x.sway * 0.3f, -14.5f - bob, 2.7f, x.col)
        }
    }

    // ───── 6) مجعّد (أفرو) ─────
    fun curly(x: HC, layer: Layer) {
        val s = x.sway * 0.4f
        if (layer == Layer.BACK) {
            if (x.side) x.disc(-1f + s, -8.5f, 7.4f, x.col) else x.disc(s, -8.5f, 7.6f, x.col)
        } else {
            if (x.side) {
                x.disc(1.8f + s, -10.6f, 2.4f, x.col)
                x.disc(-1.2f + s, -11.6f, 2.5f, x.col)
                x.disc(3.3f + s, -9.2f, 1.8f, x.col)
                x.disc(-4.2f + s, -6.8f, 1.8f, x.col)
            } else {
                x.disc(-3.2f + s, -10.3f, 2.4f, x.col)
                x.disc(s, -11.4f, 2.6f, x.col)
                x.disc(3.2f + s, -10.3f, 2.4f, x.col)
                x.disc(-4.4f + s, -6.8f, 1.7f, x.col)
                x.disc(4.4f + s, -6.8f, 1.7f, x.col)
            }
        }
    }

    // ───── 7) موهوك ─────
    fun mohawk(x: HC, layer: Layer) {
        if (layer != Layer.TOP) return
        val s = x.sway
        if (x.side) {
            x.rect(-4.4f, -11f, 4f, -8.2f, x.skinD)                       // جانبا الرأس محلوقان
            x.rect(-3.8f, -12.6f, 3.2f, -8.2f, x.col)
            val xs = floatArrayOf(-3.6f, -1.7f, 0.2f, 2.1f)
            val hs = floatArrayOf(2.6f, 3.8f, 3.4f, 2.4f)
            for (i in 0..3) x.rect(xs[i] + s * 0.7f, -12.6f - hs[i], xs[i] + 1.4f + s * 0.7f, -12.6f, x.col)
        } else {
            x.rect(-4.4f, -11f, 4.4f, -8.2f, x.skinD)
            x.rect(-1.4f + s * 0.3f, -12.6f, 1.4f + s * 0.3f, -8.2f, x.col)
            x.rect(-1.4f + s * 0.7f, -15.4f, -0.4f + s * 0.7f, -12.6f, x.col)
            x.rect(-0.5f + s, -17.6f, 0.5f + s, -12.6f, x.col)
            x.rect(0.4f + s * 0.7f, -15.4f, 1.4f + s * 0.7f, -12.6f, x.col)
        }
    }

    // ───── 8) أشواك ─────
    fun spiky(x: HC, layer: Layer) {
        if (layer != Layer.TOP) return
        val s = x.sway
        if (x.side) {
            capSide(x, -5f)
            val xs = floatArrayOf(-4.2f, -2.2f, -0.2f, 1.8f)
            val hs = floatArrayOf(3f, 4.4f, 4.8f, 3.6f)
            for (i in 0..3) spike(x, xs[i], 2f, hs[i], s)
        } else {
            capFront(x)
            val xs = floatArrayOf(-4.6f, -2.3f, 0f, 2.3f)
            val hs = floatArrayOf(3f, 4.6f, 5.4f, 4.6f)
            for (i in 0..3) spike(x, xs[i], 2.3f, hs[i], s)
        }
    }

    private fun spike(x: HC, x0: Float, w: Float, h: Float, s: Float) {
        x.rect(x0, -12.2f - h * 0.55f, x0 + w, -12.2f, x.col)
        x.rect(x0 + w * 0.25f + s * 0.3f, -12.2f - h, x0 + w * 0.75f + s * 0.3f, -12.2f - h * 0.55f, x.col)
    }

    // ───── قلنسوة الهودي عند رفعها فوق الرأس ─────
    fun hood(x: HC, main: Int, dark: Int) {
        if (x.side) {
            x.rect(-5.4f, -13.4f, 4.6f, -8f, main)
            x.rect(-5.4f, -8f, -2.6f, -1f, main)
            x.rect(2f, -9.8f, 4.6f, -8.4f, main)
            x.rect(2f, -8.4f, 4.8f, -7.9f, dark)
        } else {
            x.rect(-5.2f, -13.4f, 5.2f, -8f, main)
            x.rect(-5.2f, -8f, -4f, -1.5f, main)
            x.rect(4f, -8f, 5.2f, -1.5f, main)
            x.rect(-4f, -9.8f, 4f, -8.4f, main)
            x.rect(-4f, -8.4f, 4f, -7.9f, dark)
        }
    }
}
