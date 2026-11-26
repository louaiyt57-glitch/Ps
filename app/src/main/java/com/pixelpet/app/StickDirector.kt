package com.pixelpet.app

import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.exp
import android.graphics.Color
import kotlin.math.min
import kotlin.random.Random

/** خطوة في سيناريو شخصية واحدة. */
class Step(
    val kind: Kind,
    val x: Float = 0f,
    val anim: SAnim = SAnim.IDLE,
    val dur: Float = 0f,
    val emote: SEmote = SEmote.NONE,
    val prop: SProp = SProp.NONE,
    val block: Int = 0,
    val name: String = "",
    val n: Int = 0,
    val run: Boolean = false,
    val mx: Float = Float.NaN,
    val call: (() -> Unit)? = null
) {
    enum class Kind { GO, PLAY, FACETO, SYNC, WAIT, CALL, UNTIL }
}

class StickActor(val id: Int, val color: SColor) {
    var x = 0f                  // مركز الأرض أفقياً (بكسل)
    var facing = 1
    var anim = SAnim.IDLE
    var animT = 0f
    var prop = SProp.NONE
    var block = 0
    var emote = SEmote.NONE
    var emoteT = 0f
    private var emoteDur = 0f
    var dragging = false
    var weapon = SProp.NONE
    private var dashFrom = 0f
    private var dashTo = Float.NaN
    val pose = SPose()               // الوضعية المعروضة (ممزوجة بسلاسة)
    private val target = SPose()
    var phase = Random.nextFloat() * 5f
    var facingVis = 1f               // اتجاه مرئي يلتف تدريجياً
    private var vel = 0f

    private val queue = ArrayDeque<Step>()
    private var cur: Step? = null
    private var timer = 0f
    private var arrived = false

    val busy get() = cur != null || queue.isNotEmpty()

    fun clear() { queue.clear(); cur = null; weapon = SProp.NONE; dashTo = Float.NaN; prop = SProp.NONE; block = 0; emote = SEmote.NONE }
    fun add(s: Step) { queue.add(s) }

    fun switchTo(a: SAnim) { if (a != anim) { anim = a; animT = 0f } }
    fun showEmote(e: SEmote, dur: Float) { emote = e; emoteT = 0f; emoteDur = dur }

    private var maxStep = 20f
    private fun blend(a: Float, b: Float, k: Float): Float {
        val st = (b - a) * k
        return a + st.coerceIn(-maxStep, maxStep)   // سرعة زاوية قصوى: لا قفزات حتى في التبدلات الكبيرة
    }

    private fun smoothPose(dt: Float) {
        StickAnims.apply(anim, animT + phase, target)
        val sn = anim.snappy
        maxStep = (if (sn) 2600f else 760f) * dt
        val k = 1f - exp(-dt * (if (sn) 44f else 16f))
        val p = pose
        p.lean = blend(p.lean, target.lean, k); p.head = blend(p.head, target.head, k)
        p.nSa = blend(p.nSa, target.nSa, k); p.nEb = blend(p.nEb, target.nEb, k)
        p.fSa = blend(p.fSa, target.fSa, k); p.fEb = blend(p.fEb, target.fEb, k)
        p.nHa = blend(p.nHa, target.nHa, k); p.nKb = blend(p.nKb, target.nKb, k)
        p.fHa = blend(p.fHa, target.fHa, k); p.fKb = blend(p.fKb, target.fKb, k)
        p.rot = blend(p.rot, target.rot, 1f - exp(-dt * 11f))
        p.lift = p.lift + (target.lift - p.lift) * (1f - exp(-dt * 22f))
        p.dx = p.dx + (target.dx - p.dx) * k
        p.front = target.front; p.thumb = target.thumb
        facingVis = facingVis + (facing - facingVis) * (1f - exp(-dt * 14f))
    }

    fun update(dt: Float, d: StickDirector) {
        step(dt, d)
        smoothPose(dt)
    }

    private fun step(dt: Float, d: StickDirector) {
        animT += dt
        if (emote != SEmote.NONE) { emoteT += dt; if (emoteT > emoteDur) emote = SEmote.NONE }
        if (dragging) { switchTo(SAnim.CARRIED); vel = 0f; return }
        if (cur == null) {
            cur = queue.poll()
            cur?.let { begin(it, d) }
        }
        val s = cur ?: run {
            // لا شيء مجدول: وقفة هادئة
            switchTo(SAnim.IDLE); prop = weapon; vel *= 0.8f; return
        }
        when (s.kind) {
            Step.Kind.GO -> {
                val dx = s.x - x
                val sp = (if (s.run) 150f else 64f) * d.density
                val sgn = if (dx > 0) 1 else -1
                if (abs(dx) > 3f) facing = sgn
                // تسارع عند البدء وتباطؤ قبل الوصول
                val want = sgn * min(sp, maxOf(abs(dx) * 5f, 14f * d.density))
                vel += (want - vel) * (1f - exp(-dt * 7f))
                switchTo(if (s.run) SAnim.RUN else SAnim.WALK)
                animT -= dt * (1f - (abs(vel) / sp).coerceIn(0.25f, 1.2f))   // سرعة الخطوات تتبع السرعة
                if (abs(dx) <= 1.5f + abs(vel) * dt * 0.5f && abs(dx) < 6f) { x = s.x; vel = 0f; finish() }
                else x += vel * dt
            }
            Step.Kind.PLAY, Step.Kind.WAIT -> {
                vel *= exp(-dt * 10f)
                timer += dt
                if (!dashTo.isNaN()) {
                    val u = (timer / s.dur).coerceIn(0f, 1f)
                    val e = 1f - (1f - u) * (1f - u) * (1f - u)
                    x = dashFrom + (dashTo - dashFrom) * e
                }
                if (timer >= s.dur) { if (!dashTo.isNaN()) x = dashTo; dashTo = Float.NaN; finish() }
            }
            Step.Kind.CALL -> finish()
            Step.Kind.UNTIL -> { vel *= exp(-dt * 10f); if (d.arrivedCount(s.name) >= s.n) finish() }
            Step.Kind.FACETO -> { if (abs(s.x - x) > 2f) facing = if (s.x > x) 1 else -1; finish() }
            Step.Kind.SYNC -> {
                if (!arrived) { arrived = true; d.arrive(s.name) }
                switchTo(SAnim.IDLE)
                if (d.arrivedCount(s.name) >= s.n) finish()
            }
        }
    }

    private fun begin(s: Step, d: StickDirector) {
        timer = 0f; arrived = false
        when (s.kind) {
            Step.Kind.PLAY -> {
                switchTo(s.anim); animT = 0f
                prop = if (s.prop != SProp.NONE) s.prop else weapon; block = s.block
                if (!s.mx.isNaN()) { dashFrom = x; dashTo = s.mx } else dashTo = Float.NaN
                if (s.emote != SEmote.NONE) showEmote(s.emote, minOf(s.dur, 2.6f))
            }
            Step.Kind.WAIT -> { switchTo(SAnim.IDLE); prop = weapon; block = 0 }
            Step.Kind.UNTIL -> {
                switchTo(s.anim); prop = weapon
                if (s.emote != SEmote.NONE) showEmote(s.emote, 2.6f)
            }
            Step.Kind.CALL -> s.call?.invoke()
            else -> prop = weapon
        }
    }

    private fun finish() { cur = null }
}

/**
 * المخرج: يوزّع سيناريوهات اجتماعية على الشخصيات الخمس (بدون كلام) ويتعامل مع لمس المستخدم.
 */
class StickDirector(val density: Float, var screenW: Int) {
    val actors = List(5) { StickActor(it, SColor.values()[it]) }
    private val sync = HashMap<String, Int>()
    private var gap = 1.5f
    private var lastScene = -1
    var auto = true

    private val sp get() = 46f * density           // التباعد المريح بين الشخصيات
    private val cx get() = screenW / 2f
    private fun lo() = screenW * 0.1f
    private fun hi() = screenW * 0.9f
    private fun clampX(v: Float) = v.coerceIn(lo(), hi())

    init { layoutInitial() }

    fun layoutInitial() {
        val span = hi() - lo()
        actors.forEachIndexed { i, a -> a.x = lo() + span * (i + 0.5f) / 5f; a.facing = if (i % 2 == 0) 1 else -1 }
    }

    fun arrive(name: String) { sync[name] = (sync[name] ?: 0) + 1 }
    fun arrivedCount(name: String) = sync[name] ?: 0

    // ───── مؤثرات القتال ─────
    val fx = StickFx(density)
    var fights = true
    var shake = 0f
    private var hitStopT = 0f
    private var slowT = 0f
    private var slowK = 1f
    fun hit(shakePx: Float, stop: Float) { shake = maxOf(shake, shakePx * density); hitStopT = maxOf(hitStopT, stop) }
    fun slowmo(sec: Float, k: Float) { slowT = sec; slowK = k }

    fun update(dt: Float) {
        fx.update(dt)
        shake *= exp(-dt * 9f)
        if (hitStopT > 0f) { hitStopT -= dt; return }
        var sdt = dt
        if (slowT > 0f) { slowT -= dt; sdt = dt * slowK }
        for (a in actors) a.update(sdt, this)
        if (actors.none { it.busy || it.dragging }) {
            gap -= dt
            if (gap <= 0f && auto) startScene()
        }
    }

    // ───── أدوات بناء الخطوات ─────
    private fun StickActor.go(x: Float, run: Boolean = false) = add(Step(Step.Kind.GO, x = clampX(x), run = run))
    private fun StickActor.play(a: SAnim, dur: Float, e: SEmote = SEmote.NONE, p: SProp = SProp.NONE, b: Int = 0) =
        add(Step(Step.Kind.PLAY, anim = a, dur = dur, emote = e, prop = p, block = b))
    private fun StickActor.wait(d: Float) = add(Step(Step.Kind.WAIT, dur = d))
    private fun StickActor.face(x: Float) = add(Step(Step.Kind.FACETO, x = x))
    private fun StickActor.sync(name: String, n: Int) = add(Step(Step.Kind.SYNC, name = name, n = n))

    private fun reset() { for (a in actors) a.clear(); sync.clear() }

    /** يختار مشهداً جديداً مختلفاً عن السابق. */
    fun startScene() {
        reset()
        gap = Random.nextFloat() * 2f + 1.5f
        var k: Int
        do { k = Random.nextInt(if (fights) 15 else 12) } while (k == lastScene)
        lastScene = k
        playScene(k)
    }

    fun playScene(k: Int) {
        val sh = actors.shuffled()
        when (k) {
            0 -> sceneGreet(sh)
            1 -> sceneHighFives(sh)
            2 -> sceneHug(sh)
            3 -> sceneDance(sh)
            4 -> sceneChase(sh)
            5 -> sceneMine(sh)
            6 -> sceneStrength(sh)
            7 -> sceneSleep(sh)
            8 -> sceneScare(sh)
            9 -> sceneBow(sh)
            10 -> sceneIdea(sh)
            12, 13 -> sceneDuel(sh)
            14 -> sceneBossRush(sh)
            else -> sceneConga(sh)
        }
    }

    private fun gatherSpots(n: Int): List<Float> = List(n) { cx + (it - (n - 1) / 2f) * sp }

    private fun gatherAll(sh: List<StickActor>, name: String) {
        val spots = gatherSpots(sh.size)
        sh.forEachIndexed { i, a -> a.go(spots[i]); a.face(cx); a.sync(name, sh.size) }
    }

    // ───── مشاهد القتال ─────
    private fun StickActor.call(f: () -> Unit) = add(Step(Step.Kind.CALL, call = f))
    private fun StickActor.until(name: String, n: Int, a: SAnim, e: SEmote = SEmote.NONE) =
        add(Step(Step.Kind.UNTIL, anim = a, emote = e, name = name, n = n))
    private fun StickActor.dash(a: SAnim, dur: Float, mx: Float, e: SEmote = SEmote.NONE) =
        add(Step(Step.Kind.PLAY, anim = a, dur = dur, mx = clampX(mx), emote = e))

    private val cyan = Color.parseColor("#4DD0E1")
    private val gold = Color.parseColor("#FFC107")
    private val orange = Color.parseColor("#FF6D00")
    private fun chest() = 32f * density

    /** تبادل ضربة واحدة بين مهاجم ومدافع (الاتجاه dir: +1 المهاجم يواجه اليمين). */
    private fun exchange(att: StickActor, de: StickActor, ax: Float, dx: Float, dir: Float, kind: Int, n: Int) {
        val co = 27f * density
        val hx = dx - dir * 8f * density
        val tag = "r$n"
        att.sync(tag, 2); de.sync(tag, 2)
        att.face(dx); de.face(ax)
        when (kind) {
            0 -> { // ضربة سيف
                att.play(SAnim.WINDUP, 0.32f)
                att.dash(SAnim.LUNGE, 0.16f, dx - dir * co)
                att.call { fx.speedLines(att.x, chest(), dir); fx.slash(att.x + dir * 8f * density, chest(), dir, 36f * density, 75f, -45f, cyan); fx.impact(hx, chest(), dir, 1f, cyan); hit(3f, 0.06f) }
                att.play(SAnim.SLASH, 0.42f)
                att.dash(SAnim.SHUFFLE, 0.5f, ax)
                de.play(SAnim.FSTANCE, 0.48f)
                de.dash(SAnim.HIT, 0.55f, dx + dir * 34f * density, SEmote.SWEAT)
                de.dash(SAnim.SHUFFLE, 0.5f, dx)
            }
            1 -> { // صدّ بالسيف
                att.play(SAnim.WINDUP, 0.32f)
                att.dash(SAnim.LUNGE, 0.16f, dx - dir * co)
                att.call { fx.slash(att.x + dir * 8f * density, chest(), dir, 36f * density, 75f, -45f, Color.WHITE); fx.impact(hx, chest(), dir, 1.4f, gold); fx.groundWave(hx, 40f * density, gold); hit(2f, 0.09f) }
                att.dash(SAnim.HIT, 0.4f, dx - dir * (co + 20f * density), SEmote.EXCL)
                att.dash(SAnim.SHUFFLE, 0.5f, ax)
                de.play(SAnim.FSTANCE, 0.48f)
                de.dash(SAnim.BLOCK, 0.5f, dx + dir * 12f * density, SEmote.NONE)
                de.dash(SAnim.SHUFFLE, 0.5f, dx)
            }
            2 -> { // ركلة
                att.dash(SAnim.LUNGE, 0.2f, dx - dir * (co + 10f * density))
                att.call { fx.speedLines(att.x, chest(), dir); fx.impact(hx - dir * 4f * density, 22f * density, dir, 1.15f, orange); hit(4f, 0.07f) }
                att.play(SAnim.KICK, 0.45f)
                att.dash(SAnim.SHUFFLE, 0.5f, ax)
                de.play(SAnim.FSTANCE, 0.2f)
                de.dash(SAnim.HIT, 0.6f, dx + dir * 40f * density, SEmote.ANGER)
                de.dash(SAnim.SHUFFLE, 0.5f, dx)
            }
            2 + 1 -> { // كومبو ثلاثي
                att.play(SAnim.WINDUP, 0.25f)
                att.dash(SAnim.LUNGE, 0.14f, dx - dir * co)
                att.call { fx.slash(att.x + dir * 8f * density, chest(), dir, 36f * density, 75f, -45f, cyan); fx.impact(hx, chest(), dir, 1f, cyan); hit(2f, 0.05f) }
                att.play(SAnim.SLASH, 0.26f)
                att.call { fx.slash(att.x + dir * 8f * density, chest(), dir, 38f * density, -40f, 80f, Color.WHITE); fx.impact(hx, chest(), dir, 1.1f, cyan); hit(3f, 0.05f) }
                att.play(SAnim.WINDUP, 0.2f)
                att.play(SAnim.SLASH, 0.3f)
                att.call { fx.slash(att.x + dir * 8f * density, chest(), dir, 42f * density, 95f, -60f, gold); fx.impact(hx, chest(), dir, 1.7f, gold); hit(6f, 0.1f); slowmo(0.5f, 0.35f) }
                att.play(SAnim.SLASH, 0.3f)
                att.dash(SAnim.SHUFFLE, 0.55f, ax)
                de.play(SAnim.FSTANCE, 0.39f)
                de.dash(SAnim.HIT, 0.3f, dx + dir * 8f * density)
                de.dash(SAnim.HIT, 0.4f, dx + dir * 16f * density, SEmote.EXCL)
                de.dash(SAnim.HIT, 0.7f, dx + dir * 44f * density, SEmote.SWEAT)
                de.dash(SAnim.SHUFFLE, 0.5f, dx)
            }
            else -> { // قفزة وضربة أرضية
                att.dash(SAnim.SLAM, 0.62f, dx - dir * co)
                att.call { fx.groundWave(att.x + dir * 14f * density, 70f * density, orange); fx.impact(hx, chest(), dir, 1.5f, orange); fx.crack(att.x, 55f * density); hit(7f, 0.1f) }
                att.play(SAnim.SLASH, 0.4f)
                att.dash(SAnim.SHUFFLE, 0.55f, ax)
                de.play(SAnim.FSTANCE, 0.62f)
                de.dash(SAnim.HIT, 0.65f, dx + dir * 36f * density, SEmote.EXCL)
                de.dash(SAnim.SHUFFLE, 0.5f, dx)
            }
        }
    }

    private fun sceneDuel(sh: List<StickActor>) {
        val half = minOf(62f * density, (hi() - lo()) * 0.2f)
        val a = sh[0]; val b = sh[1]
        val spec = sh.drop(2)
        val lx = cx - half; val rx = cx + half
        a.weapon = SProp.SWORD; b.weapon = SProp.SWORD
        // وضعية البداية والتحدّي
        a.go(lx); b.go(rx); a.face(rx); b.face(lx)
        a.sync("st", 5); b.sync("st", 5)
        a.play(SAnim.FSTANCE, 1.0f, SEmote.ANGER); b.play(SAnim.FSTANCE, 1.0f, SEmote.ANGER)
        val sx = listOf(lo() + 16f * density, hi() - 16f * density, lo() + 58f * density)
        spec.forEachIndexed { i, o ->
            o.go(sx[i]); o.face(cx); o.sync("st", 5)
            o.play(SAnim.IDLE, 1.0f, SEmote.EXCL)
        }
        // جولات متبادلة
        val rounds = Random.nextInt(3, 5)
        for (n in 0 until rounds) {
            val aAtt = n % 2 == 0
            val att = if (aAtt) a else b; val de = if (aAtt) b else a
            val ax = if (aAtt) lx else rx; val dx = if (aAtt) rx else lx
            exchange(att, de, ax, dx, if (aAtt) 1f else -1f, Random.nextInt(5), n)
        }
        // اللحظة الحاسمة
        val winnerIsA = Random.nextBoolean()
        val w = if (winnerIsA) a else b; val l = if (winnerIsA) b else a
        val wx = if (winnerIsA) lx else rx; val lx0 = if (winnerIsA) rx else lx
        val dir = if (winnerIsA) 1f else -1f
        val fin = Random.nextInt(3)
        spec.forEach { o -> o.until("fin", 1, SAnim.CHEER, SEmote.STAR) }
        w.sync("pre", 2); l.sync("pre", 2)
        w.face(lx0); l.face(wx)
        w.call { arrive("fin") }
        val landX = clampX(lx0 + dir * 120f * density)
        when (fin) {
            0 -> { // شعاع طاقة
                w.call { fx.aura(wx, cyan, 1.7f); fx.screenFlash(0.2f, 0.4f, cyan) }
                w.play(SAnim.CHARGE, 1.7f, SEmote.ANGER)
                w.call {
                    fx.beam(w.x + dir * 12f * density, chest(), dir, 700f * density, cyan, 1.3f)
                    fx.explosion(l.x, chest(), 1.5f, cyan); hit(14f, 0.14f); slowmo(0.9f, 0.3f); arrive("bm")
                }
                w.play(SAnim.BEAM, 1.3f)
                l.play(SAnim.SCARED, 1.75f, SEmote.EXCL)
                l.dash(SAnim.FLYBACK, 0.95f, landX)
            }
            1 -> { // ضربة أرضية ضخمة
                w.call { fx.aura(wx, gold, 1.0f); fx.screenFlash(0.15f, 0.3f, gold) }
                w.play(SAnim.CHARGE, 1.0f, SEmote.ANGER)
                w.dash(SAnim.SLAM, 0.62f, lx0 - dir * 30f * density)
                w.call {
                    fx.explosion(l.x, chest(), 1.3f, orange); fx.groundWave(w.x, 160f * density, orange)
                    fx.crack(w.x, 120f * density); fx.slash(w.x + dir * 8f * density, chest(), dir, 50f * density, 100f, -60f, Color.WHITE)
                    hit(16f, 0.15f); slowmo(0.9f, 0.3f); arrive("bm")
                }
                w.play(SAnim.SLASH, 0.5f)
                l.play(SAnim.SCARED, 1.62f, SEmote.EXCL)
                l.dash(SAnim.FLYBACK, 0.95f, landX)
            }
            else -> { // مئة ضربة بالسرعة
                w.call { fx.aura(wx, Color.WHITE, 0.6f) }
                w.play(SAnim.CHARGE, 0.6f, SEmote.ANGER)
                for (k in 0 until 5) {
                    val side = if (k % 2 == 0) -1f else 1f
                    w.add(Step(Step.Kind.FACETO, x = lx0))
                    w.dash(SAnim.LUNGE, 0.1f, lx0 + side * 30f * density)
                    w.add(Step(Step.Kind.FACETO, x = lx0))
                    w.call {
                        val face = if (lx0 > w.x) 1f else -1f
                        fx.slash(w.x + face * 6f * density, chest(), face, 34f * density, 70f - 30f * (k % 3), -40f + 25f * (k % 2), if (k % 2 == 0) cyan else Color.WHITE)
                        fx.impact(lx0, chest(), face, 0.9f, cyan); hit(2f, 0.04f)
                    }
                    w.play(SAnim.SLASH, 0.2f)
                }
                w.call {
                    fx.slash(l.x, chest(), 1f, 50f * density, 120f, -120f, Color.WHITE)
                    fx.slash(l.x, chest(), -1f, 50f * density, 120f, -120f, gold)
                    fx.explosion(l.x, chest(), 1.4f, gold); hit(14f, 0.14f); slowmo(0.9f, 0.3f); arrive("bm")
                }
                w.play(SAnim.SLASH, 0.5f)
                l.play(SAnim.BLOCK, 2.3f, SEmote.EXCL)
                l.dash(SAnim.FLYBACK, 0.95f, landX)
            }
        }
        w.play(SAnim.VICTORY, 2.0f, SEmote.STAR)
        l.play(SAnim.KOED, 2.0f, SEmote.SPARK)
        l.play(SAnim.GETUP, 0.8f)
        l.face(w.x)
        w.go(landX - dir * 28f * density)
        w.sync("up", 2); l.sync("up", 2)
        w.face(landX); l.face(wx)
        w.play(SAnim.HIGHFIVE, 1.3f, SEmote.HEART); l.play(SAnim.HIGHFIVE, 1.3f, SEmote.HEART)
        spec.forEach { o ->
            o.play(SAnim.SCARED, 2.0f, SEmote.EXCL)
            o.until("up", 2, SAnim.CLAP, SEmote.NONE)
            o.play(SAnim.CHEER, 1.6f, SEmote.HEART)
        }
    }

    private fun sceneBossRush(sh: List<StickActor>) {
        val boss = sh[0]
        val atk = sh.drop(1)
        val side = listOf(-1f, -1f, 1f, 1f)
        val dist = listOf(52f, 98f, 52f, 98f)
        boss.weapon = SProp.SWORD
        boss.go(cx); boss.sync("go", 5)
        boss.play(SAnim.FSTANCE, 0.6f, SEmote.ANGER)
        atk.forEachIndexed { i, o ->
            o.weapon = SProp.SWORD
            val spot = clampX(cx + side[i] * dist[i] * density * 1.2f)
            o.go(spot); o.face(cx); o.sync("go", 5)
            o.play(SAnim.FSTANCE, 0.6f, SEmote.EXCL)
        }
        // موجة الهجمات
        atk.forEachIndexed { i, o ->
            val sx = cx + side[i] * dist[i] * density * 1.2f
            val dir = -side[i]
            val bx = cx
            o.wait(0.1f + i * 0.45f)
            o.play(SAnim.WINDUP, 0.3f)
            o.dash(SAnim.LUNGE, 0.16f, bx + side[i] * (28f + (if (i > 1) 0f else 0f)) * density)
            o.call {
                fx.slash(o.x - side[i] * 8f * density, chest(), dir, 34f * density, 75f, -45f, if (i % 2 == 0) cyan else gold)
                fx.impact(bx + side[i] * 6f * density, chest(), dir, 1.2f, gold); hit(3f, 0.07f)
            }
            o.dash(SAnim.HIT, 0.45f, sx, SEmote.EXCL)
            o.dash(SAnim.SHUFFLE, 0.3f, sx)
        }
        for (i in 0 until 4) {
            boss.wait(if (i == 0) 0.7f + 0.2f else 0.33f)
            boss.add(Step(Step.Kind.FACETO, x = cx + side[i] * 50f * density))
            boss.play(SAnim.BLOCK, 0.12f)
        }
        boss.play(SAnim.FSTANCE, 0.1f)
        // الانفجار
        boss.sync("n", 5); atk.forEach { it.sync("n", 5) }
        boss.call { fx.aura(cx, Color.parseColor("#E040FB"), 1.5f); fx.screenFlash(0.2f, 0.5f, Color.parseColor("#E040FB")) }
        boss.play(SAnim.CHARGE, 1.5f, SEmote.ANGER)
        boss.call {
            fx.explosion(boss.x, chest(), 1.6f, Color.parseColor("#E040FB"))
            fx.groundWave(boss.x, 230f * density, Color.parseColor("#E040FB")); fx.groundWave(boss.x, 150f * density, Color.WHITE)
            hit(16f, 0.16f); slowmo(0.9f, 0.3f)
        }
        boss.play(SAnim.SPINATK, 0.8f, SEmote.SPARK)
        boss.play(SAnim.VICTORY, 2.6f, SEmote.STAR)
        atk.forEachIndexed { i, o ->
            o.play(SAnim.SCARED, 1.5f, SEmote.EXCL)
            o.dash(SAnim.FLYBACK, 0.95f, o.x + side[i] * 100f * density)
            o.play(SAnim.KOED, 1.5f, SEmote.SPARK)
            o.play(SAnim.GETUP, 0.8f)
        }
        boss.sync("u", 5); atk.forEach { it.sync("u", 5) }
        (atk + boss).forEach { it.play(SAnim.CHEER, 2.0f, SEmote.HEART) }
    }

    private fun sceneGreet(sh: List<StickActor>) {
        gatherAll(sh, "g")
        sh.forEach { it.play(SAnim.WAVE, 2.2f, SEmote.NONE); it.wait(0.2f) }
        sh.forEach { it.play(SAnim.CHEER, 1.4f, SEmote.STAR) }
    }

    private fun sceneHighFives(sh: List<StickActor>) {
        val pairs = listOf(sh[0] to sh[1], sh[2] to sh[3])
        pairs.forEachIndexed { i, (a, b) ->
            val m = clampX(cx + (if (i == 0) -1 else 1) * 90f * density)
            a.go(m - 18f * density); b.go(m + 18f * density)
            a.face(b.x); b.face(a.x)
            a.sync("p$i", 2); b.sync("p$i", 2)
            a.play(SAnim.HIGHFIVE, 1.0f, SEmote.SPARK); b.play(SAnim.HIGHFIVE, 1.0f, SEmote.SPARK)
            a.play(SAnim.JUMP, 0.8f); b.play(SAnim.JUMP, 0.8f)
        }
        val e = sh[4]
        e.go(cx); e.play(SAnim.CLAP, 2.5f, SEmote.STAR)
    }

    private fun sceneHug(sh: List<StickActor>) {
        val a = sh[0]; val b = sh[1]
        val m = clampX(cx)
        a.go(m - 12f * density); b.go(m + 12f * density)
        a.add(Step(Step.Kind.FACETO, x = m + 12f * density)); b.add(Step(Step.Kind.FACETO, x = m - 12f * density))
        a.sync("h", 2); b.sync("h", 2)
        a.play(SAnim.HUG, 2.6f, SEmote.HEART); b.play(SAnim.HUG, 2.6f, SEmote.HEART)
        for (o in sh.drop(2)) { o.go(m + (if (o.x < m) -1 else 1) * 70f * density); o.face(m); o.play(SAnim.CLAP, 2.8f, SEmote.HEART) }
    }

    private fun sceneDance(sh: List<StickActor>) {
        gatherAll(sh, "d")
        sh.forEachIndexed { i, a -> a.play(SAnim.DANCE, 5f, if (i % 2 == 0) SEmote.NOTE else SEmote.NONE) }
        sh.forEach { it.play(SAnim.CHEER, 1.2f, SEmote.STAR) }
    }

    private fun sceneChase(sh: List<StickActor>) {
        val tagger = sh[0]; val runner = sh[1]
        val far = if (runner.x < cx) hi() else lo()
        tagger.face(runner.x); tagger.play(SAnim.POINT, 0.9f, SEmote.EXCL)
        runner.play(SAnim.SCARED, 0.9f, SEmote.SWEAT)
        runner.go(far, run = true); tagger.go(far, run = true)
        tagger.play(SAnim.JUMP, 0.8f, SEmote.ANGER)
        runner.play(SAnim.LAUGH, 1.8f)
        for (o in sh.drop(2)) { o.face(runner.x); o.play(SAnim.LAUGH, 3.5f, SEmote.NONE) }
    }

    private fun sceneMine(sh: List<StickActor>) {
        val miner = sh[0]
        val x = clampX(cx - 60f * density)
        miner.go(x); miner.face(x + 100f)
        miner.play(SAnim.MINE, 4f, SEmote.NONE, SProp.PICKAXE, 3)
        miner.play(SAnim.JUMP, 0.8f, SEmote.STAR, SProp.NONE)
        miner.play(SAnim.CHEER, 2.2f, SEmote.STAR, SProp.DIAMOND)
        for (o in sh.drop(1)) {
            o.go(x + (o.id + 1) * 38f * density); o.face(x)
            o.play(SAnim.IDLE, 3.5f, SEmote.QUEST)
            o.play(SAnim.CHEER, 2.8f, SEmote.STAR)
        }
    }

    private fun sceneStrength(sh: List<StickActor>) {
        val a = sh[0]; val b = sh[1]
        a.go(cx - 40f * density); b.go(cx + 40f * density)
        a.face(b.x); b.face(a.x); a.sync("s", 2); b.sync("s", 2)
        a.play(SAnim.FLEX, 2.4f, SEmote.NONE); b.play(SAnim.FLEX, 2.4f, SEmote.NONE)
        a.play(SAnim.FLEX, 1.4f, SEmote.ANGER); b.play(SAnim.SHRUG, 1.4f, SEmote.QUEST)
        a.play(SAnim.CHEER, 1.6f, SEmote.STAR); b.play(SAnim.TIRED, 1.6f, SEmote.SWEAT)
        for (o in sh.drop(2)) { o.go(cx + (o.id - 2) * 30f * density); o.face(cx); o.play(SAnim.CLAP, 6f, SEmote.NONE) }
    }

    private fun sceneSleep(sh: List<StickActor>) {
        val spots = gatherSpots(5)
        sh.forEachIndexed { i, a -> a.go(spots[i]); a.face(cx) }
        sh.forEach { it.play(SAnim.TIRED, 1.2f, SEmote.NONE) }
        sh.forEachIndexed { i, a ->
            if (i == 0) { a.play(SAnim.LOOKAROUND, 5.5f, SEmote.QUEST) }
            else { a.play(SAnim.SIT, 1.2f); a.play(SAnim.SLEEP, 4.3f, SEmote.ZZZ) }
        }
        sh[0].play(SAnim.SHRUG, 1.2f, SEmote.QUEST)
    }

    private fun sceneScare(sh: List<StickActor>) {
        val seer = sh[0]
        seer.play(SAnim.POINT, 1.0f, SEmote.EXCL)
        for (o in sh.drop(1)) { o.play(SAnim.SCARED, 1.4f, SEmote.EXCL) }
        for (o in sh) { val dir = if (o.x < cx) lo() else hi(); o.go(dir + (o.id - 2) * 6f, run = true) }
        sh.forEach { it.play(SAnim.SCARED, 0.8f, SEmote.SWEAT) }
        sh.forEach { it.play(SAnim.LAUGH, 2.2f) }
    }

    private fun sceneBow(sh: List<StickActor>) {
        val star = sh[0]
        star.go(cx)
        for (o in sh.drop(1)) { o.go(cx + (if (o.id % 2 == 0) -1 else 1) * (60f + 20f * o.id) * density); o.face(cx) }
        star.play(SAnim.BOW, 1.6f, SEmote.NONE)
        star.play(SAnim.WAVE, 1.6f, SEmote.HEART)
        for (o in sh.drop(1)) { o.wait(0.4f); o.play(SAnim.CLAP, 3.2f, SEmote.STAR); o.play(SAnim.CHEER, 1.4f, SEmote.HEART) }
    }

    private fun sceneIdea(sh: List<StickActor>) {
        val th = sh[0]
        th.play(SAnim.THINK, 2.6f, SEmote.QUEST)
        th.play(SAnim.JUMP, 0.8f, SEmote.BULB)
        val tx = clampX(cx)
        th.go(tx)
        th.sync("i", 5)
        th.play(SAnim.POINT, 1.6f, SEmote.EXCL)
        th.play(SAnim.THUMBS, 1.6f, SEmote.CHECK)
        for (o in sh.drop(1)) {
            o.go(tx + (if (o.x < tx) -1 else 1) * (40f + 26f * o.id) * density); o.face(tx)
            o.sync("i", 5)
            o.play(SAnim.NOD, 1.6f, SEmote.QUEST)
            o.play(SAnim.THUMBS, 1.6f, SEmote.CHECK)
        }
    }

    private fun sceneConga(sh: List<StickActor>) {
        val left = sh[0].x > cx
        val xs = if (left) lo() else hi()
        val xe = if (left) hi() else lo()
        sh.forEachIndexed { i, a ->
            a.go(xs - (if (left) -1 else 1) * i * 0f); a.face(xe)
        }
        sh.forEachIndexed { i, a ->
            a.wait(i * 0.45f)
            a.go(xe, run = false)
            a.play(SAnim.DANCE, 1.6f, if (i == 0) SEmote.NOTE else SEmote.NONE)
        }
    }

    // ───── تفاعل المستخدم ─────
    private val gestures = listOf(
        SAnim.WAVE to SEmote.NONE, SAnim.THUMBS to SEmote.CHECK, SAnim.HEART to SEmote.HEART,
        SAnim.PEACE to SEmote.SPARK, SAnim.KISS to SEmote.HEART, SAnim.SALUTE to SEmote.NONE,
        SAnim.FLEX to SEmote.STAR, SAnim.CLAP to SEmote.STAR, SAnim.CHEER to SEmote.STAR,
        SAnim.DANCE to SEmote.NOTE, SAnim.BECKON to SEmote.EXCL, SAnim.SHRUG to SEmote.QUEST,
        SAnim.BOW to SEmote.NONE, SAnim.LAUGH to SEmote.NONE
    )

    /** نقرة على شخصية: تحيّيك بإشارة موجّهة إليك، وجار يلتفت إليها. */
    fun onTap(i: Int) {
        reset(); gap = 2.5f
        val a = actors[i]
        val (g, e) = gestures.random()
        a.play(g, 2.4f, e)
        val nb = actors.filter { it !== a }.minByOrNull { abs(it.x - a.x) }!!
        nb.face(a.x); nb.wait(0.3f); nb.play(if (Random.nextBoolean()) SAnim.CLAP else SAnim.POINT, 1.8f, SEmote.EXCL)
    }

    /** نقرتان: الكل يتجمع ويحتفل. */
    fun onDoubleTap(i: Int) {
        reset(); gap = 3f
        val spots = gatherSpots(5)
        val order = actors.sortedBy { it.x }
        order.forEachIndexed { k, a -> a.go(spots[k], run = true); a.face(cx); a.sync("dt", 5) }
        actors.forEach { it.play(SAnim.CHEER, 2.8f, SEmote.STAR) }
        actors.forEachIndexed { k, a -> a.play(if (k % 2 == 0) SAnim.JUMP else SAnim.DANCE, 1.6f, SEmote.HEART) }
    }

    fun onPickUp(i: Int) {
        reset(); gap = 4f
        val a = actors[i]
        a.dragging = true
        for (o in actors) if (o !== a) {
            o.face(a.x); o.play(SAnim.SCARED, 1.2f, SEmote.EXCL)
            o.play(if (o.id % 2 == 0) SAnim.POINT else SAnim.LAUGH, 6f, if (o.id % 2 == 0) SEmote.QUEST else SEmote.NONE)
        }
    }

    /** يُرسل كل فريم أثناء السحب لتتابعه العيون. */
    fun dragMoved(i: Int, x: Float) {
        val a = actors[i]; a.x = x
        for (o in actors) if (o !== a) o.facing = if (a.x > o.x) 1 else -1
    }

    fun onDrop(i: Int) {
        val a = actors[i]
        a.dragging = false
        reset(); gap = 4f
        a.play(SAnim.FALL, 1.2f, SEmote.SPARK)
        a.wait(0.5f)
        a.play(SAnim.GETUP, 0.8f)
        a.play(SAnim.SHRUG, 1.0f, SEmote.QUEST)
        val helper = actors.filter { it !== a }.minByOrNull { abs(it.x - a.x) }!!
        helper.go(a.x + (if (helper.x < a.x) -1 else 1) * 26f * density, run = true)
        helper.face(a.x)
        helper.play(SAnim.SCARED, 1.0f, SEmote.EXCL)
        helper.play(SAnim.HIGHFIVE, 1.4f, SEmote.HEART)
        for (o in actors) if (o !== a && o !== helper) {
            o.face(a.x); o.play(SAnim.LAUGH, 2.4f)
            o.play(SAnim.CLAP, 1.8f, SEmote.STAR)
        }
    }
}
