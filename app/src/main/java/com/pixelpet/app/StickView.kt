package com.pixelpet.app

import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

/** نافذة صغيرة لشخصية عيدان واحدة. تلتقط اللمس: نقرة، نقرتان، ضغط مطوّل/سحب. */
class StickView(
    ctx: Context,
    private val r: StickRenderer,
    val actor: StickActor
) : View(ctx) {

    interface Listener {
        fun onTap(i: Int)
        fun onDoubleTap(i: Int)
        fun onDragStart(i: Int)
        fun onDrag(i: Int, rawX: Float, rawY: Float)
        fun onDragEnd(i: Int)
    }

    var listener: Listener? = null
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop.toFloat()
    private val h = Handler(Looper.getMainLooper())
    private var downX = 0f; private var downY = 0f
    private var dragging = false
    private var lastTapAt = 0L
    private val longPress = Runnable {
        if (!dragging) { dragging = true; listener?.onDragStart(actor.id) }
    }

    override fun onMeasure(w: Int, hh: Int) = setMeasuredDimension(r.winW, r.winH)

    override fun onDraw(c: Canvas) {
        val a = actor
        r.draw(c, a.color.argb, a.pose, a.facingVis, a.prop, a.animT, a.emote, a.emoteT, a.block)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX; downY = e.rawY; dragging = false
                h.postDelayed(longPress, 380)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && (abs(e.rawX - downX) > slop * 1.5f || abs(e.rawY - downY) > slop * 1.5f)) {
                    h.removeCallbacks(longPress)
                    dragging = true
                    listener?.onDragStart(actor.id)
                }
                if (dragging) listener?.onDrag(actor.id, e.rawX, e.rawY)
            }
            MotionEvent.ACTION_UP -> {
                h.removeCallbacks(longPress)
                if (dragging) { dragging = false; listener?.onDragEnd(actor.id) }
                else {
                    val now = SystemClock.uptimeMillis()
                    if (now - lastTapAt < 320) { lastTapAt = 0; listener?.onDoubleTap(actor.id) }
                    else { lastTapAt = now; listener?.onTap(actor.id) }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                h.removeCallbacks(longPress)
                if (dragging) { dragging = false; listener?.onDragEnd(actor.id) }
            }
        }
        return true
    }
}
