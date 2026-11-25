package com.pixelpet.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * العرض المخصص (Custom View) الذي يرسم الشخصية وفقاعة الحوار على Canvas.
 * نافذة الـ Overlay تكبر/تصغر لتحيط بالشخصية والفقاعة فقط، والخدمة تحرّكها عبر updateViewLayout().
 */
class PixelPetView(context: Context) : View(context) {

    interface Listener {
        fun onTap()
        fun onAnswer(yes: Boolean)
        fun onDragStart()
        fun onDrag(dx: Float, dy: Float)
        fun onDragEnd()
    }

    var listener: Listener? = null

    private val d = resources.displayMetrics.density
    val renderer = PetRenderer(d)

    /** جنس الشخصية: 0 ذكر، 1 أنثى. */
    fun setGender(g: Int) { renderer.gender = g; invalidate() }

    /** متغيرات المظهر: نظارة، وشاح، ملابس (0-2)، تسريحة (0-2). */
    fun setLook(glassesOn: Boolean, scarfOn: Boolean, outfitIdx: Int, hairIdx: Int) {
        renderer.glasses = glassesOn
        renderer.scarf = scarfOn
        renderer.outfit = outfitIdx
        renderer.hairstyle = hairIdx
        invalidate()
    }
    private val pose = Pose()
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    // ───── الحالة ─────
    var state: PetState = PetState.WAVE_HELLO
        private set
    var facing = 1
    var faceOverride: Face? = null
    private var stateStart = SystemClock.uptimeMillis()

    // ───── فقاعة الحوار ─────
    private var bubbleLayout: StaticLayout? = null
    private var bubbleButtons = false
    private var bubbleBoxW = 0f      // عرض الفقاعة (px)
    private var bubbleBoxH = 0f      // ارتفاع الفقاعة بدون الذيل (px)
    private val tailH = 10f * d
    private val bw = 3f * d          // سماكة الإطار
    private val pad = 12f * d
    private val btnH = 32f * d
    private val btnGap = 8f * d

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 14f * d
        color = Color.parseColor("#111111")
    }
    private val btnPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 14f * d
        color = Color.parseColor("#111111")
        textAlign = Paint.Align.CENTER
    }
    private val fill = Paint().apply { isAntiAlias = false }
    private val yesRect = RectF()
    private val noRect = RectF()
    private var pressed = 0  // 0 لا شيء، 1 نعم، 2 لا

    // ───── موضع مربع الشخصية داخل النافذة ─────
    var charOffsetX = 0f
        private set
    var charOffsetY = 0f
        private set

    // ───── اللمس ─────
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    var dragging = false
        private set

    val charW: Int get() = ceil(renderer.boxW).toInt()
    val charH: Int get() = ceil(renderer.boxH).toInt()
    val hasBubble: Boolean get() = bubbleLayout != null

    // ───── واجهة للخدمة ─────
    fun setPetState(s: PetState) {
        state = s
        stateStart = SystemClock.uptimeMillis()
        faceOverride = null
    }

    fun stateElapsedMs(): Long = SystemClock.uptimeMillis() - stateStart

    /** إظهار فقاعة بنص (مع أزرار نعم/لا اختيارياً) أو إخفاؤها بتمرير null. */
    fun setBubble(text: String?, buttons: Boolean) {
        if (text == null) {
            bubbleLayout = null
            bubbleButtons = false
            bubbleBoxW = 0f; bubbleBoxH = 0f
            return
        }
        val maxText = 190f * d
        val desired = Layout.getDesiredWidth(text, textPaint)
        var textW = ceil(min(desired, maxText)).toInt()
        textW = max(textW, (60f * d).toInt())
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, textW)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setIncludePad(false)
            .build()
        bubbleLayout = layout
        bubbleButtons = buttons
        var w = textW + 2 * pad + 2 * bw
        if (buttons) w = max(w, 2 * (84f * d) + btnGap + 2 * pad)
        bubbleBoxW = w
        bubbleBoxH = layout.height + 2 * pad + 2 * bw + (if (buttons) btnGap + btnH else 0f)
    }

    /**
     * يحسب مستطيل النافذة (x, y, w, h) بحيث تحيط بالشخصية والفقاعة فقط،
     * مع إبقاء الشخصية في مكانها على الشاشة قدر الإمكان.
     */
    fun computeWindow(charLeft: Int, charTop: Int, screenW: Int, out: IntArray) {
        val hasB = bubbleLayout != null
        val w = if (hasB) max(ceil(bubbleBoxW).toInt(), charW) else charW
        val extra = if (hasB) ceil(bubbleBoxH + tailH).toInt() else 0
        val h = extra + charH
        var x = charLeft + charW / 2 - w / 2
        x = x.coerceIn(0, max(0, screenW - w))
        var y = charTop - extra
        if (y < 0) y = 0
        charOffsetX = (charLeft - x).toFloat()
        charOffsetY = extra.toFloat()
        out[0] = x; out[1] = y; out[2] = w; out[3] = h
    }

    // ───── الرسم ─────
    override fun onDraw(canvas: Canvas) {
        val tMs = stateElapsedMs()
        PetPose.compute(state, tMs, state.durationMs, pose)
        val clock = SystemClock.uptimeMillis() / 1000f

        var face = pose.face ?: faceOverride ?: state.face
        var t = tMs / 1000f
        if (dragging) {
            // الشخصية "معلّقة" أثناء السحب
            pose.reset()
            pose.armL = 140f; pose.armR = 140f
            pose.legL = 1.5f + sin(clock * 12f); pose.legR = 1.5f - sin(clock * 12f)
            pose.dy = -1f
            face = Face.SURPRISED
            t = clock
        }

        val fixedFacing = state.prop == Prop.BED || state.prop == Prop.SNOWMAN ||
            state.prop == Prop.LAPTOP || state.anim == Anim.SHOW
        val f = if (fixedFacing && !dragging) 1 else facing

        renderer.draw(canvas, charOffsetX, charOffsetY, state, pose, face, f, t)
        if (bubbleLayout != null) drawBubble(canvas)
    }

    private fun box(c: Canvas, l: Float, t: Float, r: Float, b: Float, border: Int, bg: Int, w: Float) {
        fill.color = border
        c.drawRect(l + w, t, r - w, b, fill)
        c.drawRect(l, t + w, r, b - w, fill)
        fill.color = bg
        c.drawRect(l + 2 * w, t + w, r - 2 * w, b - w, fill)
        c.drawRect(l + w, t + 2 * w, r - w, b - 2 * w, fill)
    }

    private fun drawBubble(c: Canvas) {
        val layout = bubbleLayout ?: return
        val left = (charOffsetX + renderer.feetX - bubbleBoxW / 2f).coerceIn(0f, max(0f, width - bubbleBoxW))
        val top = 2f * d
        val right = left + bubbleBoxW
        val bottom = top + bubbleBoxH
        val shadow = Color.parseColor("#33000000")

        // ظل بكسلي
        box(c, left + 3f * d, top + 3f * d, right + 3f * d, bottom + 3f * d, shadow, shadow, bw)
        // الفقاعة
        box(c, left, top, right, bottom, Color.BLACK, Color.WHITE, bw)

        // الذيل (مثلث بكسلي) باتجاه الشخصية
        val tx = (charOffsetX + renderer.feetX - 6f * d).coerceIn(left + bw + 4f * d, right - bw - 16f * d)
        val rowH = tailH / 3f
        for (i in 0..2) {
            val w = (14f - i * 5f) * d
            fill.color = Color.BLACK
            c.drawRect(tx + i * 2.5f * d, bottom - bw + i * rowH, tx + i * 2.5f * d + w, bottom - bw + (i + 1) * rowH, fill)
            fill.color = Color.WHITE
            c.drawRect(tx + i * 2.5f * d + bw, bottom - bw + i * rowH, tx + i * 2.5f * d + w - bw, bottom - bw + (i + 1) * rowH, fill)
        }
        // تغطية خط الإطار السفلي عند قاعدة الذيل
        fill.color = Color.WHITE
        c.drawRect(tx + bw, bottom - bw, tx + 14f * d - bw, bottom, fill)

        // النص
        c.save()
        c.translate(left + (bubbleBoxW - layout.width) / 2f, top + bw + pad)
        layout.draw(c)
        c.restore()

        // الأزرار
        if (bubbleButtons) {
            val by = top + bw + pad + layout.height + btnGap
            val bwid = (bubbleBoxW - 2 * pad - 2 * bw - btnGap) / 2f
            val x0 = left + bw + pad
            // «نعم» على اليمين و«لا» على اليسار ليناسب اتجاه العربية
            noRect.set(x0, by, x0 + bwid, by + btnH)
            yesRect.set(x0 + bwid + btnGap, by, x0 + 2 * bwid + btnGap, by + btnH)
            drawButton(c, yesRect, "نعم", Color.parseColor("#F3D84A"), pressed == 1)
            drawButton(c, noRect, "لا", Color.parseColor("#EE8FA0"), pressed == 2)
        }
    }

    private fun drawButton(c: Canvas, r: RectF, label: String, color: Int, down: Boolean) {
        val off = if (down) 2f * d else 0f
        val bg = if (down) darker(color) else color
        box(c, r.left, r.top + off, r.right, r.bottom + off, Color.BLACK, bg, 2f * d)
        val fm = btnPaint.fontMetrics
        c.drawText(label, r.centerX(), r.centerY() + off - (fm.ascent + fm.descent) / 2f, btnPaint)
    }

    private fun darker(c: Int): Int =
        Color.rgb((Color.red(c) * 0.8f).toInt(), (Color.green(c) * 0.8f).toInt(), (Color.blue(c) * 0.8f).toInt())

    // ───── اللمس: سحب + نقر + أزرار الفقاعة ─────
    private fun hitButton(x: Float, y: Float): Int {
        if (!bubbleButtons || bubbleLayout == null) return 0
        if (yesRect.contains(x, y)) return 1
        if (noRect.contains(x, y)) return 2
        return 0
    }

    private fun onChar(x: Float, y: Float): Boolean =
        x >= charOffsetX && x <= charOffsetX + renderer.boxW && y >= charOffsetY && y <= charOffsetY + renderer.boxH

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = e.rawX; downRawY = e.rawY
                lastRawX = e.rawX; lastRawY = e.rawY
                dragging = false
                pressed = hitButton(e.x, e.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && pressed == 0 &&
                    hypot(e.rawX - downRawX, e.rawY - downRawY) > slop
                ) {
                    dragging = true
                    listener?.onDragStart()
                }
                if (dragging) listener?.onDrag(e.rawX - lastRawX, e.rawY - lastRawY)
                lastRawX = e.rawX; lastRawY = e.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (dragging) {
                    dragging = false
                    listener?.onDragEnd()
                } else {
                    val hit = hitButton(e.x, e.y)
                    if (pressed != 0 && hit == pressed) listener?.onAnswer(pressed == 1)
                    else if (pressed == 0 && onChar(e.x, e.y)) listener?.onTap()
                }
                pressed = 0
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (dragging) { dragging = false; listener?.onDragEnd() }
                pressed = 0
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(e)
    }
}
