package com.pixelpet.app

import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.exp
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
    val run: Boolean = false
) {
    enum class Kind { GO, PLAY, FACETO, SYNC, WAIT }
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

    fun clear() { queue.clear(); cur = null; prop = SProp.NONE; block = 0; emote = SEmote.NONE }
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
        maxStep = 760f * dt
        val k = 1f - exp(-dt * 16f)
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
            switchTo(SAnim.IDLE); vel *= 0.8f; return
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
                if (timer >= s.dur) finish()
            }
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
                prop = s.prop; block = s.block
                if (s.emote != SEmote.NONE) showEmote(s.emote, minOf(s.dur, 2.6f))
            }
            Step.Kind.WAIT -> { switchTo(SAnim.IDLE); prop = SProp.NONE; block = 0 }
            else -> Unit
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

    fun update(dt: Float) {
        for (a in actors) a.update(dt, this)
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
        do { k = Random.nextInt(12) } while (k == lastScene)
        lastScene = k
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
            else -> sceneConga(sh)
        }
    }

    private fun gatherSpots(n: Int): List<Float> = List(n) { cx + (it - (n - 1) / 2f) * sp }

    private fun gatherAll(sh: List<StickActor>, name: String) {
        val spots = gatherSpots(sh.size)
        sh.forEachIndexed { i, a -> a.go(spots[i]); a.face(cx); a.sync(name, sh.size) }
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
