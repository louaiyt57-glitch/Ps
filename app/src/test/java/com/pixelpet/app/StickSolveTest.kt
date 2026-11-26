package com.pixelpet.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.Deflater
import kotlin.math.abs

/**
 * اختبارات JVM نقية لوضع العيدان:
 * 1) كل حركة في SAnim تنتج مفاصل منتهية (بلا NaN أو قيم شاذة) — لا مشهد يُعلَّق.
 * 2) كل حركة جديدة تحصل على معاينة PNG قبل الدمج (القسم 13 من القواعد).
 * الترميز PNG يدوي لأن مسار ترجمة اختبارات أندرويد لا يضم java.awt.
 */
class StickSolveTest {

    private val pose = SPose()
    private val xy = FloatArray(StickSolve.N * 2)

    private fun apply(a: SAnim, t: Float): FloatArray {
        pose.reset()
        StickAnims.apply(a, t, pose)
        StickSolve.solve(pose, xy)
        return xy
    }

    /** القاعدة: لا حركة تُنتج قيماً غير منتهية أو قفزات مفاجئة بين إطارين. */
    private fun checkFinite(a: SAnim, times: List<Float>) {
        var prev = FloatArray(StickSolve.N * 2)
        times.forEachIndexed { k, t ->
            val cur = apply(a, t).copyOf()
            cur.forEachIndexed { i, v ->
                assertTrue("$a t=$t coord#$i=$v ليس رقماً منتهياً", v.isFinite() && abs(v) < 1000f)
                if (k > 0) assertTrue("$a قفزة $v بين $t والإطار السابق", abs(v - prev[i]) < 120f)
            }
            prev = cur
        }
    }

    @Test
    fun everyAnimProducesFiniteJoints() {
        val times = (0..20).map { it * 0.05f }
        SAnim.entries.forEach { checkFinite(it, times) }
    }

    @Test
    fun newAnimsLandOnGroundOrAir() {
        // الوقوف: أدنى نقطة تلامس الأرض (0) وطول الجسم في النطاق المعقول
        apply(SAnim.IDLE, 0f)
        val maxY = (0 until StickSolve.N).maxOf { xy[2 * it + 1] }
        val height = -((0 until StickSolve.N).minOf { xy[2 * it + 1] })
        assertTrue("IDLE لا يلامس الأرض: maxY=$maxY", abs(maxY) < 3f)
        assertTrue("IDLE بطول غير معقول: $height", height in 30f..90f)
        // الشقلبة: الجسم كله فوق الأرض (أدنى نقطة مرتفعة)
        apply(SAnim.BACKFLIP, 0.3f)
        val airMaxY = (0 until StickSolve.N).maxOf { xy[2 * it + 1] }
        assertTrue("BACKFLIP لا يرتفع عن الأرض: maxY=$airMaxY", airMaxY < -5f)
    }

    /** محاكاة الجدولة: كل زاوية لكل حركة تعمل بلا استثناء ولا تعليق. */
    @Test
    fun schedulerSimulationCompletes() {
        val start = System.nanoTime()
        SAnim.entries.forEach { a -> (0..60).map { it / 60f * 2f }.forEach { apply(a, it) } }
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("المحاكاة بطيئة أكثر من اللازم: ${ms}ms", ms < 5000)
    }

    /** معاينات PNG لكل الحركات الجديدة — القسم 13 من القواعد. */
    @Test
    fun renderPreviewsForNewAnims() {
        val newAnims = listOf(SAnim.PROWL, SAnim.TENSE, SAnim.BACKFLIP, SAnim.DODGE, SAnim.COMBO,
            SAnim.SLIDE, SAnim.HANDSTAND, SAnim.MEDITATE, SAnim.PAT, SAnim.RISE)
        val dir = File("build/stick-previews").apply { mkdirs() }
        newAnims.forEach { a ->
            val frames = 8
            val fw = 64; val fh = 150
            val img = PngCanvas(frames * fw, fh)
            (0 until frames).forEach { f ->
                apply(a, f / frames.toFloat() * 1.2f)
                drawJoints(img, f * fw + fw / 2, fh - 12)
            }
            val out = File(dir, "preview-${a.name}.png")
            img.writePng(out)
            assertTrue("لم تُكتب معاينة $a", out.length() > 0)
            println("معاينة: ${out.path} (${out.length()} بايت)")
        }
    }

    /** رسم الهيكل بالخطوط بين المفاصل على لوح بكسلات، مع الأرض. */
    private fun drawJoints(img: PngCanvas, ox: Int, oy: Int) {
        fun p(i: Int) = Pair(ox + xy[2 * i].toInt(), oy + xy[2 * i + 1].toInt())
        fun line(a: Int, b: Int) { val (x1, y1) = p(a); val (x2, y2) = p(b); img.line(x1, y1, x2, y2) }
        val head = p(StickSolve.HEAD)
        img.circle(head.first, head.second, StickSolve.HEAD_R.toInt())
        line(StickSolve.HIP, StickSolve.NECKP)
        line(StickSolve.SH, StickSolve.ELB_N); line(StickSolve.ELB_N, StickSolve.HND_N)
        line(StickSolve.SH, StickSolve.ELB_F); line(StickSolve.ELB_F, StickSolve.HND_F)
        line(StickSolve.HIP, StickSolve.KNE_N); line(StickSolve.KNE_N, StickSolve.FOT_N)
        line(StickSolve.HIP, StickSolve.KNE_F); line(StickSolve.KNE_F, StickSolve.FOT_F)
        img.line(ox - 30, oy, ox + 30, oy)
    }
}

/** لوح بكسلات RGBA مع خطوط بريزنهام وترميز PNG يدوي (بلا java.awt). */
class PngCanvas(val w: Int, val h: Int) {
    private val px = IntArray(w * h)

    fun set(x: Int, y: Int) {
        if (x in 0 until w && y in 0 until h) px[y * w + x] = 0xFF18_1C_2F.toInt()
    }

    fun line(x0: Int, y0: Int, x1: Int, y1: Int) {
        val dx = abs(x1 - x0); val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1
        var err = dx - dy; var x = x0; var y = y0
        while (true) {
            set(x, y)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 > -dy) { err -= dy; x += sx }
            if (e2 < dx) { err += dx; y += sy }
        }
    }

    fun circle(cx: Int, cy: Int, r: Int) {
        for (a in 0 until 360 step 4) {
            val x = cx + (r * Math.cos(Math.toRadians(a * 1.0))).toInt()
            val y = cy + (r * Math.sin(Math.toRadians(a * 1.0))).toInt()
            set(x, y)
        }
    }

    fun writePng(out: File) {
        val raw = ByteArray(h * (1 + w * 4))
        var k = 0
        for (y in 0 until h) {
            raw[k++] = 0 // فلتر لا شيء
            for (x in 0 until w) {
                val v = px[y * w + x]
                raw[k++] = (v ushr 16).toByte(); raw[k++] = (v ushr 8).toByte()
                raw[k++] = v.toByte(); raw[k++] = 0xFF.toByte()
            }
        }
        val def = Deflater().apply { setInput(raw); finish() }
        val idat = java.io.ByteArrayOutputStream()
        val buf = ByteArray(65536)
        while (true) { val n = def.deflate(buf); if (n <= 0) break; idat.write(buf, 0, n) }
        def.end()
        val chunk: (String, ByteArray) -> Unit = { type, data ->
            out.appendBytes(byteArrayOf(((data.size ushr 24)).toByte(), ((data.size ushr 16)).toByte(), ((data.size ushr 8)).toByte(), (data.size).toByte()))
            out.appendBytes(type.toByteArray())
            out.appendBytes(data)
            val crc = java.util.zip.CRC32().apply { update(type.toByteArray()); update(data) }.value.toInt()
            out.appendBytes(byteArrayOf((crc ushr 24).toByte(), (crc ushr 16).toByte(), (crc ushr 8).toByte(), crc.toByte()))
        }
        out.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
        chunk("IHDR", byteArrayOf(
            (w ushr 24).toByte(), (w ushr 16).toByte(), (w ushr 8).toByte(), w.toByte(),
            (h ushr 24).toByte(), (h ushr 16).toByte(), (h ushr 8).toByte(), h.toByte(),
            8, 6, 0, 0, 0))
        chunk("IDAT", idat.toByteArray())
        chunk("IEND", ByteArray(0))
    }
}
