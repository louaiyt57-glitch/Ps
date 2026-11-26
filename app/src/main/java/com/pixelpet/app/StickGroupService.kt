package com.pixelpet.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Choreographer
import android.view.Gravity
import android.view.WindowManager
import kotlin.math.roundToInt

/**
 * خدمة أمامية تعرض 5 شخصيات عيدان، كل واحدة في نافذة Overlay صغيرة خاصة بها،
 * فتبقى بقية الشاشة قابلة للمس. المخرج [StickDirector] ينسّق تفاعلها مع بعضها ومع المستخدم.
 */
class StickGroupService : Service(), StickView.Listener {

    companion object {
        const val ACTION_STOP = "com.pixelpet.app.STICK_STOP"
        private const val CHANNEL_ID = "stick_group_channel"
        private const val NOTIF_ID = 2

        @Volatile
        var running = false
            private set

        fun start(c: Context) {
            c.startForegroundService(Intent(c, StickGroupService::class.java))
        }

        fun stop(c: Context) {
            c.stopService(Intent(c, StickGroupService::class.java))
        }
    }

    private lateinit var wm: WindowManager
    private lateinit var rend: StickRenderer
    private lateinit var dir: StickDirector
    private val views = ArrayList<StickView>()
    private val params = ArrayList<WindowManager.LayoutParams>()
    private var screenW = 0
    private var screenH = 0
    private var density = 1f
    private var baseY = 0f          // أرضية المجموعة (بكسل، y لموضع الأرض)
    private var fxView: FxView? = null
    private lateinit var fxParams: WindowManager.LayoutParams
    private var lastFrame = 0L
    private var lastOy = 0
    private var looping = false
    private var receiverRegistered = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIF_ID, notif)

        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        density = resources.displayMetrics.density
        readScreen()
        rend = StickRenderer(density)
        dir = StickDirector(density, screenW)
        dir.auto = Prefs.autoMove(this)
        dir.fights = Prefs.stickFights(this)

        val sy = Prefs.stickGroundY(this)
        baseY = if (sy >= 0) sy.toFloat() else screenH * 0.82f
        clampBase()

        for (a in dir.actors) {
            val v = StickView(this, rend, a)
            v.listener = this
            val p = WindowManager.LayoutParams(
                rend.winW, rend.winH,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                if (Build.VERSION.SDK_INT >= 28) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            p.x = (a.x - rend.cx).roundToInt(); p.y = (baseY - rend.groundY).roundToInt()
            try { wm.addView(v, p) } catch (e: Exception) { stopSelf(); return }
            views.add(v); params.add(p)
        }

        // نافذة المؤثرات فوق الجميع (لا تلتقط اللمس)
        val fv = FxView(this, dir.fx, screenW, (260f * density).toInt(), 225f * density)
        fxParams = WindowManager.LayoutParams(
            fv.w, fv.h,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = (baseY - fv.groundY).roundToInt() }
        try { wm.addView(fv, fxParams); fxView = fv } catch (e: Exception) { /* بدون مؤثرات */ }

        val f = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, f, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(screenReceiver, f)
        receiverRegistered = true

        running = true
        resumeLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (views.isEmpty()) return
        readScreen(); dir.screenW = screenW; dir.layoutInitial(); clampBase()
    }

    override fun onDestroy() {
        running = false
        pauseLoop()
        if (receiverRegistered) { try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}; receiverRegistered = false }
        if (views.isNotEmpty()) Prefs.setStickGroundY(this, baseY.roundToInt())
        fxView?.let { try { wm.removeView(it) } catch (_: Exception) {} }; fxView = null
        for (v in views) try { wm.removeView(v) } catch (_: Exception) {}
        views.clear(); params.clear()
        super.onDestroy()
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> pauseLoop()
                Intent.ACTION_SCREEN_ON -> resumeLoop()
            }
        }
    }

    private fun readScreen() {
        val m = resources.displayMetrics
        screenW = m.widthPixels; screenH = m.heightPixels
    }

    private fun clampBase() {
        baseY = baseY.coerceIn(rend.winH * 0.9f, screenH - 6f * density)
    }

    // ───── الحلقة ─────
    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(t: Long) {
            if (!looping) return
            val dt = if (lastFrame == 0L) 0.033f else ((t - lastFrame) / 1e9f).coerceIn(0f, 0.1f)
            lastFrame = t
            dir.update(dt)
            // اهتزاز الشاشة عند الضربات القوية
            val sh = dir.shake
            val ox = if (sh > 0.4f) ((Math.random() * 2 - 1) * sh).roundToInt() else 0
            val oy = if (sh > 0.4f) ((Math.random() * 2 - 1) * sh).roundToInt() else 0
            for (i in views.indices) {
                val a = dir.actors[i]; val p = params[i]
                val nx = (a.x - rend.cx).roundToInt() + ox
                val ny = (if (a.dragging) p.y - lastOy else (baseY - rend.groundY).roundToInt()) + oy
                if (nx != p.x || ny != p.y) {
                    p.x = nx; p.y = ny
                    try { wm.updateViewLayout(views[i], p) } catch (_: Exception) {}
                }
                views[i].invalidate()
            }
            lastOy = oy
            fxView?.let { fv ->
                val fy = (baseY - fv.groundY).roundToInt() + oy
                if (fxParams.y != fy || fxParams.x != ox) {
                    fxParams.x = ox; fxParams.y = fy
                    try { wm.updateViewLayout(fv, fxParams) } catch (_: Exception) {}
                }
                if (dir.fx.active) fv.invalidate()
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun resumeLoop() {
        if (looping) return
        looping = true; lastFrame = 0L
        Choreographer.getInstance().postFrameCallback(frame)
    }

    private fun pauseLoop() {
        looping = false
        Choreographer.getInstance().removeFrameCallback(frame)
    }

    // ───── اللمس ─────
    override fun onTap(i: Int) = dir.onTap(i)
    override fun onDoubleTap(i: Int) = dir.onDoubleTap(i)
    override fun onDragStart(i: Int) = dir.onPickUp(i)

    override fun onDrag(i: Int, rawX: Float, rawY: Float) {
        dir.dragMoved(i, rawX.coerceIn(rend.winW / 2f, screenW - rend.winW / 2f))
        val p = params[i]
        p.x = (dir.actors[i].x - rend.cx).roundToInt()
        p.y = (rawY - rend.winH * 0.45f).roundToInt()
        try { wm.updateViewLayout(views[i], p) } catch (_: Exception) {}
    }

    override fun onDragEnd(i: Int) {
        // الأرضية الجديدة للمجموعة = المكان الذي أُفلتت فيه
        val p = params[i]
        baseY = p.y + rend.groundY
        clampBase()
        dir.onDrop(i)
        Prefs.setStickGroundY(this, baseY.roundToInt())
    }

    // ───── الإشعار ─────
    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.stick_channel_name), NotificationManager.IMPORTANCE_LOW)
        ch.setShowBadge(false)
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flags)
        val stopI = PendingIntent.getService(
            this, 2, Intent(this, StickGroupService::class.java).setAction(ACTION_STOP), flags
        )
        val icon = Icon.createWithResource(this, R.drawable.ic_notif)
        val b = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(getString(R.string.stick_notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(icon, getString(R.string.notif_stop), stopI).build())
        if (Build.VERSION.SDK_INT >= 31) b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        return b.build()
    }
}
