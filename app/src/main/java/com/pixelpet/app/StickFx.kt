package com.pixelpet.app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * مؤثرات بصرية للقتال (بدون صوت): وهج، شرر، حلقات صدمة، شظايا مكعبات، أقواس ضربات،
 * أشعة طاقة، هالة شحن، تشققات الأرض. الإحداثيات: x بكسل شاشة، y بكسل للأعلى من الأرض.
 */
class StickFx(private val d: Float) {

    private class P(
        val type: Int, var x: Float, var y: Float, var vx: Float, var vy: Float,
        val max: Float, var size: Float, var col: Int,
        var rot: Float = 0f, var vr: Float = 0f, var a0: Float = 0f, var a1: Float = 0f, var dir: Float = 1f
    ) { var life = max }

    private class Em(var x: Float, val col: Int, var left: Float) { var acc = 0f; var pulse = 0f; var bolt = 0f }

    private companion object {
        const val SPARK = 0; const val DEBRIS = 1; const val DUST = 2; const val RING = 3; const val FLASH = 4
        const val SLASH = 5; const val BEAM = 6; const val STREAK = 7; const val BOLT = 8; const val MOTE = 9
        const val CRACK = 10; const val SCREEN = 11
        val BLOCKS = intArrayOf(
            Color.parseColor("#9E9E9E"), Color.parseColor("#757575"), Color.parseColor("#8D6E63"),
            Color.parseColor("#66BB6A"), Color.parseColor("#4DD0E1")
        )
    }

    private val ps = ArrayList<P>()
    private val ems = ArrayList<Em>()
    private val paint = Paint().apply { isAntiAlias = true; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val path = Path()
    private val oval = RectF()
    private val rnd = Random(7)

    val active get() = ps.isNotEmpty() || ems.isNotEmpty()

    private fun r(a: Float, b: Float) = a + rnd.nextFloat() * (b - a)
    private fun al(c: Int, a: Float) = Color.argb((a.coerceIn(0f, 1f) * 255).toInt(), Color.red(c), Color.green(c), Color.blue(c))
    private fun add(p: P) { if (ps.size < 260) ps.add(p) }

    // ───── مولّدات جاهزة ─────
    fun impact(x: Float, y: Float, dir: Float, power: Float, col: Int) {
        add(P(FLASH, x, y, 0f, 0f, 0.2f, 34f * d * power, Color.WHITE))
        add(P(RING, x, y, 0f, 0f, 0.36f, 46f * d * power, col))
        val n = (11 * power).toInt()
        for (i in 0 until n) {
            val ang = (r(-70f, 70f)) * 0.01745f
            val sp = r(180f, 560f) * d * power
            add(P(SPARK, x, y, cos(ang) * sp * dir, sin(ang) * sp + r(-40f, 120f) * d, r(0.3f, 0.6f), r(7f, 16f) * d, if (i % 3 == 0) Color.WHITE else col))
        }
        for (i in 0 until (4 * power).toInt()) {
            add(P(DEBRIS, x, y, r(60f, 300f) * d * dir, r(80f, 320f) * d, r(0.5f, 0.9f), r(2.2f, 4.5f) * d, BLOCKS[rnd.nextInt(BLOCKS.size)], r(0f, 6f), r(-12f, 12f)))
        }
        for (i in 0 until 3) {
            add(P(STREAK, x - dir * r(4f, 30f) * d, y + r(-14f, 14f) * d, 0f, 0f, 0.16f, r(30f, 70f) * d, Color.WHITE, dir = dir))
        }
    }

    /** قوس ضربة سيف: الزوايا بالدرجات (0 = أمام، + للأعلى). */
    fun slash(x: Float, y: Float, dir: Float, radius: Float, a0: Float, a1: Float, col: Int) {
        add(P(SLASH, x, y, 0f, 0f, 0.3f, radius, col, a0 = a0 * 0.01745f, a1 = a1 * 0.01745f, dir = dir))
    }

    fun groundWave(x: Float, maxR: Float, col: Int) {
        add(P(RING, x, 2f * d, 0f, 0f, 0.7f, maxR, col, a1 = 0.22f))
        add(P(RING, x, 2f * d, 0f, 0f, 0.5f, maxR * 0.6f, Color.WHITE, a1 = 0.22f))
        for (i in 0 until 10) {
            val s = if (i % 2 == 0) 1f else -1f
            add(P(DUST, x + s * r(0f, 18f) * d, 4f * d, s * r(60f, 260f) * d, r(10f, 60f) * d, r(0.5f, 0.9f), r(8f, 16f) * d, Color.parseColor("#BDBDBD")))
        }
    }

    fun crack(x: Float, len: Float) {
        add(P(CRACK, x, 0f, 0f, 0f, 0.9f, len, Color.parseColor("#FFEB3B"), rot = rnd.nextInt(1000).toFloat(), dir = 1f))
        add(P(CRACK, x, 0f, 0f, 0f, 0.9f, len, Color.parseColor("#FFEB3B"), rot = rnd.nextInt(1000).toFloat(), dir = -1f))
        for (i in 0 until 16) {
            val s = if (i % 2 == 0) 1f else -1f
            add(P(DEBRIS, x + s * r(4f, 60f) * d, 3f * d, s * r(20f, 160f) * d, r(220f, 620f) * d, r(0.7f, 1.2f), r(2.5f, 5.5f) * d,
                BLOCKS[rnd.nextInt(BLOCKS.size)], r(0f, 6f), r(-10f, 10f)))
        }
    }

    fun beam(x: Float, y: Float, dir: Float, len: Float, col: Int, life: Float) {
        add(P(BEAM, x, y, 0f, 0f, life, len, col, a0 = 11f * d, dir = dir))
        add(P(FLASH, x, y, 0f, 0f, 0.35f, 30f * d, Color.WHITE))
        for (i in 0 until 14) {
            val ang = r(-40f, 40f) * 0.01745f; val sp = r(200f, 520f) * d
            add(P(SPARK, x, y, cos(ang) * sp * dir, sin(ang) * sp, r(0.3f, 0.7f), r(8f, 16f) * d, col))
        }
    }

    fun aura(x: Float, col: Int, dur: Float) { ems.add(Em(x, col, dur)) }

    fun screenFlash(a: Float, life: Float, col: Int = Color.WHITE) {
        add(P(SCREEN, 0f, 0f, 0f, 0f, life, a, col))
    }

    fun explosion(x: Float, y: Float, power: Float, col: Int) {
        add(P(FLASH, x, y, 0f, 0f, 0.45f, 100f * d * power, Color.WHITE))
        add(P(FLASH, x, y, 0f, 0f, 0.55f, 70f * d * power, col))
        add(P(RING, x, y, 0f, 0f, 0.6f, 110f * d * power, Color.WHITE))
        add(P(RING, x, y, 0f, 0f, 0.8f, 160f * d * power, col))
        for (i in 0 until (26 * power).toInt()) {
            val ang = r(0f, 6.283f); val sp = r(150f, 700f) * d
            add(P(SPARK, x, y, cos(ang) * sp, sin(ang) * sp, r(0.4f, 0.9f), r(8f, 22f) * d, if (i % 2 == 0) Color.WHITE else col))
        }
        for (i in 0 until (14 * power).toInt()) {
            val ang = r(0f, 6.283f); val sp = r(80f, 420f) * d
            add(P(DEBRIS, x, y, cos(ang) * sp, sin(ang) * sp + 120f * d, r(0.8f, 1.4f), r(2.5f, 6f) * d,
                BLOCKS[rnd.nextInt(BLOCKS.size)], r(0f, 6f), r(-12f, 12f)))
        }
        for (i in 0 until 8) {
            val ang = r(0f, 6.283f)
            add(P(DUST, x, y, cos(ang) * r(30f, 130f) * d, sin(ang) * r(30f, 130f) * d, r(0.6f, 1.1f), r(14f, 26f) * d, Color.parseColor("#9E9E9E")))
        }
        screenFlash(0.55f * min(1f, power), 0.35f)
    }

    fun speedLines(x: Float, y: Float, dir: Float) {
        for (i in 0 until 5) add(P(STREAK, x - dir * r(10f, 40f) * d, y + r(-26f, 26f) * d, 0f, 0f, 0.22f, r(30f, 80f) * d, Color.WHITE, dir = dir))
    }

    // ───── التحديث ─────
    fun update(dt: Float) {
        var i = ems.size - 1
        while (i >= 0) {
            val e = ems[i]
            e.left -= dt; e.acc += dt; e.pulse += dt; e.bolt += dt
            while (e.acc > 0.025f) {
                e.acc -= 0.025f
                add(P(MOTE, e.x + r(-16f, 16f) * d, r(2f, 10f) * d, r(-14f, 14f) * d, r(70f, 190f) * d, r(0.5f, 0.9f), r(2.2f, 4.5f) * d,
                    if (rnd.nextInt(3) == 0) Color.WHITE else e.col))
            }
            if (e.pulse > 0.32f) { e.pulse = 0f; add(P(RING, e.x, 2f * d, 0f, 0f, 0.5f, 48f * d, e.col, a1 = 0.25f)) }
            if (e.bolt > 0.07f) {
                e.bolt = 0f
                add(P(BOLT, e.x + r(-8f, 8f) * d, r(6f, 40f) * d, 0f, 0f, 0.09f, r(14f, 30f) * d, Color.WHITE, rot = rnd.nextInt(10000).toFloat(), a0 = r(0f, 6.28f)))
            }
            if (e.left <= 0f) ems.removeAt(i)
            i--
        }
        i = ps.size - 1
        while (i >= 0) {
            val p = ps[i]
            p.life -= dt
            if (p.life <= 0f) { ps.removeAt(i); i--; continue }
            p.x += p.vx * dt; p.y += p.vy * dt
            when (p.type) {
                SPARK, DEBRIS -> { p.vy -= 900f * d * dt; p.rot += p.vr * dt; if (p.y < 0f) { p.y = 0f; p.vy *= -0.3f; p.vx *= 0.6f } }
                DUST -> { p.vx *= (1f - 2f * dt); p.vy *= (1f - 2f * dt) }
            }
            i--
        }
    }

    // ───── الرسم ─────
    fun draw(c: Canvas, groundY: Float, w: Float, h: Float) {
        for (p in ps) {
            val prog = 1f - p.life / p.max
            val fade = 1f - prog
            val sy = groundY - p.y
            when (p.type) {
                SPARK -> {
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = max(1.5f, 3f * d * fade)
                    paint.color = al(p.col, min(1f, fade * 1.6f))
                    val l = hypot(p.vx, p.vy).coerceAtLeast(1f)
                    c.drawLine(p.x, sy, p.x - p.vx / l * p.size * fade, sy + p.vy / l * p.size * fade, paint)
                }
                DEBRIS -> {
                    c.save(); c.translate(p.x, sy); c.rotate(p.rot * 57.3f)
                    paint.style = Paint.Style.FILL; paint.color = al(p.col, min(1f, fade * 2.2f))
                    c.drawRect(-p.size, -p.size, p.size, p.size, paint)
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 1f * d; paint.color = al(Color.BLACK, min(0.6f, fade))
                    c.drawRect(-p.size, -p.size, p.size, p.size, paint)
                    c.restore()
                }
                DUST -> {
                    paint.style = Paint.Style.FILL; paint.color = al(p.col, 0.4f * fade)
                    c.drawCircle(p.x, sy, p.size * (0.6f + prog), paint)
                }
                MOTE -> {
                    paint.style = Paint.Style.FILL; paint.color = al(p.col, fade * 1.2f)
                    c.drawCircle(p.x, sy, p.size * (0.4f + fade), paint)
                }
                RING -> {
                    val e = 1f - (1f - prog) * (1f - prog)
                    val rr = p.size * e
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = max(1.5f, 7f * d * fade)
                    paint.color = al(p.col, fade)
                    if (p.a1 > 0f) { oval.set(p.x - rr, sy - rr * p.a1, p.x + rr, sy + rr * p.a1); c.drawOval(oval, paint) }
                    else c.drawCircle(p.x, sy, rr, paint)
                }
                FLASH -> {
                    val f2 = fade * fade
                    paint.style = Paint.Style.FILL
                    val rr = p.size * (0.5f + 0.7f * prog)
                    paint.color = al(p.col, 0.25f * f2); c.drawCircle(p.x, sy, rr * 1.5f, paint)
                    paint.color = al(p.col, 0.45f * f2); c.drawCircle(p.x, sy, rr, paint)
                    paint.color = al(Color.WHITE, 0.95f * f2); c.drawCircle(p.x, sy, rr * 0.55f, paint)
                    // نجمة لامعة
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = max(1.5f, 3f * d * fade); paint.color = al(Color.WHITE, f2)
                    c.drawLine(p.x - rr * 1.4f, sy, p.x + rr * 1.4f, sy, paint)
                    c.drawLine(p.x, sy - rr * 1.4f, p.x, sy + rr * 1.4f, paint)
                }
                STREAK -> {
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = max(1f, 2.2f * d * fade); paint.color = al(p.col, fade)
                    c.drawLine(p.x, sy, p.x - p.dir * p.size * (0.5f + prog), sy, paint)
                }
                SLASH -> drawSlash(c, p, sy, prog)
                BEAM -> drawBeam(c, p, sy, prog)
                BOLT -> {
                    val rr = Random(p.rot.toInt())
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = max(1.5f, 2.2f * d); paint.color = al(p.col, 0.9f)
                    var x = p.x; var y = sy; var ang = p.a0
                    for (k in 0 until 4) {
                        ang += (rr.nextFloat() - 0.5f) * 2.4f
                        val nx = x + cos(ang) * p.size * 0.5f; val ny = y + sin(ang) * p.size * 0.5f
                        c.drawLine(x, y, nx, ny, paint); x = nx; y = ny
                    }
                }
                CRACK -> {
                    val rr = Random(p.rot.toInt())
                    val grow = min(1f, prog * 5f)
                    paint.style = Paint.Style.STROKE
                    var x = p.x; var y = groundY - 1f * d
                    val segs = 9
                    for (k in 0 until segs) {
                        if (k.toFloat() / segs > grow) break
                        val nx = x + p.dir * p.size / segs
                        val ny = groundY + (rr.nextFloat() - 0.4f) * 6f * d
                        paint.strokeWidth = max(1.5f, 5f * d * fade); paint.color = al(Color.parseColor("#FF6F00"), fade)
                        c.drawLine(x, y, nx, ny, paint)
                        paint.strokeWidth = max(1f, 2f * d * fade); paint.color = al(p.col, fade)
                        c.drawLine(x, y, nx, ny, paint)
                        x = nx; y = ny
                    }
                }
                SCREEN -> {
                    paint.style = Paint.Style.FILL; paint.color = al(p.col, p.size * fade * fade)
                    c.drawRect(0f, 0f, w, h, paint)
                }
            }
        }
        // وهج حول الهالة
        for (e in ems) {
            paint.style = Paint.Style.FILL
            paint.color = al(e.col, 0.16f); oval.set(e.x - 30f * d, groundY - 8f * d, e.x + 30f * d, groundY + 8f * d); c.drawOval(oval, paint)
        }
    }

    private fun hypot(a: Float, b: Float) = kotlin.math.sqrt(a * a + b * b)

    private fun drawSlash(c: Canvas, p: P, sy: Float, prog: Float) {
        val sweep = min(1f, prog * 2.6f)
        val e = 1f - (1f - sweep) * (1f - sweep)
        val head = p.a0 + (p.a1 - p.a0) * e
        val tailE = max(0f, sweep - 0.55f)
        val tail = p.a0 + (p.a1 - p.a0) * (1f - (1f - tailE) * (1f - tailE))
        val fade = min(1f, (1f - prog) * 1.8f)
        val n = 14
        for (layer in 0..1) {
            val wd = if (layer == 0) 20f * d else 8f * d
            path.reset()
            for (i in 0..n) {
                val a = tail + (head - tail) * i / n
                val tw = wd * 0.5f * sin(PI.toFloat() * i / n)
                val rr = p.size + tw
                val px = p.x + cos(a) * rr * p.dir; val py = sy - sin(a) * rr
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            for (i in n downTo 0) {
                val a = tail + (head - tail) * i / n
                val tw = wd * 0.5f * sin(PI.toFloat() * i / n)
                val rr = p.size - tw
                path.lineTo(p.x + cos(a) * rr * p.dir, sy - sin(a) * rr)
            }
            path.close()
            paint.style = Paint.Style.FILL
            paint.color = if (layer == 0) al(p.col, 0.75f * fade) else al(Color.WHITE, 0.95f * fade)
            c.drawPath(path, paint)
        }
    }

    private fun drawBeam(c: Canvas, p: P, sy: Float, prog: Float) {
        val env = (sin(PI.toFloat() * prog).coerceAtLeast(0f)).pow(0.4f)
        val grow = min(1f, prog * 9f)
        val len = p.size * grow
        val h = p.a0 * env * (1f + 0.12f * sin(prog * 90f))
        val x0 = p.x; val x1 = p.x + p.dir * len
        paint.style = Paint.Style.FILL
        fun band(hh: Float, col: Int) { c.drawRect(min(x0, x1), sy - hh, max(x0, x1), sy + hh, paint.apply { color = col }) }
        band(h * 1.9f, al(p.col, 0.22f))
        band(h * 1.25f, al(p.col, 0.55f))
        band(h * 0.7f, al(Color.WHITE, 0.9f))
        // رأس الشعاع
        paint.color = al(Color.WHITE, 0.9f * env); c.drawCircle(x0, sy, h * 1.6f, paint)
        paint.color = al(p.col, 0.5f * env); c.drawCircle(x0, sy, h * 2.4f, paint)
        // خطوط طاقة متحركة
        paint.style = Paint.Style.STROKE; paint.strokeWidth = max(1.5f, 2f * d); paint.color = al(Color.WHITE, 0.7f * env)
        for (k in 0 until 6) {
            val ox = ((prog * 1600f * d + k * 140f * d) % max(1f, len))
            val xx = if (p.dir > 0) x0 + ox else x0 - ox
            c.drawLine(xx, sy - h * 1.6f * (1f - k % 3 * 0.4f), xx - p.dir * 26f * d, sy - h * 1.6f * (1f - k % 3 * 0.4f), paint)
        }
    }
}
