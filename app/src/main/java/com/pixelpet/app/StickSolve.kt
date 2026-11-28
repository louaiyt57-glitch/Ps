package com.pixelpet.app

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * الحل الأمامي لمفاصل العود — نقي بلا اعتماد على أندرويد حتى يعمل على JVM للاختبارات
 * ومعاينات PNG. نفس ثوابت وأبعاد `StickRenderer` تماماً.
 */
object StickSolve {
    const val TORSO = 16f
    const val NECK = 1.2f
    const val HEAD_R = 6.6f
    const val ARM1 = 11f
    const val ARM2 = 11f
    const val LEG1 = 14f
    const val LEG2 = 14.5f
    const val HIP = 0; const val NECKP = 1; const val SH = 2; const val HEAD = 3
    const val ELB_N = 4; const val HND_N = 5; const val ELB_F = 6; const val HND_F = 7
    const val KNE_N = 8; const val FOT_N = 9; const val KNE_F = 10; const val FOT_F = 11
    const val N = 12
    private const val R = 0.0174533f

    /** يملأ xy بإحداثيات المفاصل للأصل عند وسط الأرض (y للأسفل موجب). */
    fun solve(p: SPose, xy: FloatArray) {
        require(xy.size >= N)
        val l = p.lean * R
        val tx = sin(l); val ty = -cos(l)
        set(xy, HIP, 0f, 0f)
        set(xy, NECKP, tx * TORSO, ty * TORSO)
        set(xy, SH, tx * (TORSO - 0.5f), ty * (TORSO - 0.5f))
        val hd = (p.lean + p.head) * R
        set(xy, HEAD, x(xy, NECKP) + sin(hd) * (NECK + HEAD_R), y(xy, NECKP) - cos(hd) * (NECK + HEAD_R))

        val m = if (p.front) -1f else 1f
        arm(xy, ELB_N, HND_N, p.nSa, p.nEb, 1f)
        arm(xy, ELB_F, HND_F, p.fSa, p.fEb, m)
        leg(xy, KNE_N, FOT_N, p.nHa, p.nKb, 1f)
        leg(xy, KNE_F, FOT_F, p.fHa, p.fKb, m)

        val rr = p.rot * R
        val cr = cos(rr); val sr = sin(rr)
        for (i in 0 until N) {
            val x = x(xy, i); val y = y(xy, i)
            set(xy, i, x * cr - y * sr, x * sr + y * cr)
        }
        var maxY = -1000f
        for (i in 0 until N) {
            val e = if (i == HEAD) HEAD_R else 2.2f
            if (i != NECKP && i != SH) maxY = max(maxY, y(xy, i) + e)
        }
        val shift = -maxY - p.lift
        for (i in 0 until N) set(xy, i, x(xy, i) + p.dx, y(xy, i) + shift)
    }

    private fun x(xy: FloatArray, i: Int) = xy[2 * i]
    private fun y(xy: FloatArray, i: Int) = xy[2 * i + 1]
    private fun set(xy: FloatArray, i: Int, x: Float, y: Float) { xy[2 * i] = x; xy[2 * i + 1] = y }

    private fun arm(xy: FloatArray, e: Int, h: Int, sa: Float, eb: Float, sign: Float) {
        val a = sa * sign * R
        val b = (sa + eb) * sign * R
        set(xy, e, x(xy, SH) + sin(a) * ARM1, y(xy, SH) + cos(a) * ARM1)
        set(xy, h, x(xy, e) + sin(b) * ARM2, y(xy, e) + cos(b) * ARM2)
    }

    private fun leg(xy: FloatArray, k: Int, f: Int, ha: Float, kb: Float, sign: Float) {
        val kbc = kb.coerceIn(-4f, 82f)
        val a = ha * sign * R
        val b = (ha - kbc) * sign * R
        set(xy, k, x(xy, HIP) + sin(a) * LEG1, y(xy, HIP) + cos(a) * LEG1)
        set(xy, f, x(xy, k) + sin(b) * LEG2, y(xy, k) + cos(b) * LEG2)
    }
}
