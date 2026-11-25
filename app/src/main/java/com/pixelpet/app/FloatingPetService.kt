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
import android.graphics.drawable.Icon
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Choreographer
import android.view.Gravity
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * خدمة أمامية (Foreground Service) تعرض الشخصية فوق كل التطبيقات.
 *
 * نقطة مهمة: نافذة الـ Overlay صغيرة وتحيط بالشخصية والفقاعة فقط. عند تحرك الشخصية
 * نحدّث x و y في WindowManager.LayoutParams عبر updateViewLayout()، فتبقى بقية الشاشة
 * قابلة للمس والاستخدام بشكل طبيعي تماماً.
 */
class FloatingPetService : Service(), PixelPetView.Listener {

    companion object {
        const val ACTION_START = "com.pixelpet.app.START"
        const val ACTION_STOP = "com.pixelpet.app.STOP"
        private const val CHANNEL_ID = "pixel_pet_channel"
        private const val NOTIF_ID = 1

        @Volatile
        var running = false
            private set

        fun start(c: Context) {
            c.startForegroundService(Intent(c, FloatingPetService::class.java).setAction(ACTION_START))
        }

        fun stop(c: Context) {
            c.stopService(Intent(c, FloatingPetService::class.java))
        }
    }

    private enum class Phase { STATE, GAP }

    private lateinit var wm: WindowManager
    private var view: PixelPetView? = null
    private lateinit var params: WindowManager.LayoutParams
    private var attached = false

    private var screenW = 0
    private var screenH = 0
    private var density = 1f

    // موضع مربع الشخصية (الزاوية العلوية اليسرى) بالبكسل
    private var charX = 0f
    private var charY = 0f
    private val win = IntArray(4)

    // آلة الحالات
    private var phase = Phase.GAP
    private var stateEnd = 0L
    private var lastState = PetState.WAVE_HELLO
    private var reacting = false
    private var edgeTarget = 0f
    private val idleState = PetState.CURIOUS_LOOK

    private var active = false
    private var lastFrame = 0L

    // ───── دورة حياة الخدمة ─────
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        density = resources.displayMetrics.density
        updateScreenSize()

        val v = PixelPetView(this)
        v.listener = this
        v.setGender(Prefs.gender(this))
        v.setLook(Prefs.glasses(this), Prefs.scarf(this), Prefs.outfit(this), Prefs.hairstyle(this))
        v.setQuality(Prefs.quality(this))
        view = v

        params = WindowManager.LayoutParams(
            v.charW, v.charH,
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

        // الموضع الابتدائي: المحفوظ، أو أسفل الشاشة تقريباً في المنتصف
        val sx = Prefs.savedX(this)
        val sy = Prefs.savedY(this)
        charX = if (sx >= 0) sx.toFloat() else (screenW - v.charW) / 2f
        charY = if (sy >= 0) sy.toFloat() else screenH * 0.62f
        clampPosition()

        startState(PetState.WAVE_HELLO)
        applyLayout(force = true)

        try {
            wm.addView(v, params)
            attached = true
        } catch (e: Exception) {
            stopSelf()
            return
        }

        val f = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, f, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(screenReceiver, f)
        receiverRegistered = true

        running = true
        resumeLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (view == null) return
        updateScreenSize()
        clampPosition()
        applyLayout(force = true)
    }

    override fun onDestroy() {
        running = false
        pauseLoop()
        if (receiverRegistered) {
            try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
            try { hitPlayer?.release(); hitPlayer = null } catch (_: Exception) {}
            receiverRegistered = false
        }
        if (attached) {
            Prefs.savePos(this, charX.roundToInt(), charY.roundToInt())
            try { wm.removeView(view) } catch (_: Exception) {}
            attached = false
        }
        view = null
        super.onDestroy()
    }

    // ───── الشاشة ─────
    private var receiverRegistered = false
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> pauseLoop()      // توفير البطارية
                Intent.ACTION_SCREEN_ON -> resumeLoop()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun updateScreenSize() {
        if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.currentWindowMetrics.bounds
            screenW = b.width(); screenH = b.height()
        } else {
            val dm = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(dm)
            screenW = dm.widthPixels; screenH = dm.heightPixels
        }
    }

    private fun maxX(): Float = (screenW - (view?.charW ?: 0)).toFloat().coerceAtLeast(0f)
    private fun maxY(): Float = (screenH - (view?.charH ?: 0)).toFloat().coerceAtLeast(0f)

    private fun clampPosition() {
        charX = charX.coerceIn(0f, maxX())
        charY = charY.coerceIn(0f, maxY())
    }

    /** يحدّث إحداثيات ومقاس النافذة الصغيرة المحيطة بالشخصية والفقاعة فقط. */
    private fun applyLayout(force: Boolean = false) {
        val v = view ?: return
        v.computeWindow(charX.roundToInt(), charY.roundToInt(), screenW, win)
        if (force || params.x != win[0] || params.y != win[1] || params.width != win[2] || params.height != win[3]) {
            params.x = win[0]; params.y = win[1]
            params.width = win[2]; params.height = win[3]
            if (attached) {
                try { wm.updateViewLayout(v, params) } catch (_: Exception) {}
            }
        }
    }

    // ───── حلقة الحركة ─────
    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!active) return
            val now = SystemClock.uptimeMillis()
            if (now - lastFrame >= 30) {   // ~30 إطاراً في الثانية يكفي للبكسل آرت ويوفر البطارية
                val dt = min(now - lastFrame, 100L)
                lastFrame = now
                update(now, dt)
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun resumeLoop() {
        if (active || view == null) return
        active = true
        lastFrame = SystemClock.uptimeMillis()
        Choreographer.getInstance().postFrameCallback(frame)
    }

    private fun pauseLoop() {
        active = false
        Choreographer.getInstance().removeFrameCallback(frame)
    }

    private fun update(now: Long, dt: Long) {
        val v = view ?: return
        if (v.dragging) {
            stateEnd += dt           // نجمّد مؤقت الحالة أثناء السحب
        } else {
            move(v, dt / 1000f)
            if (now >= stateEnd) onStateFinished()
        }
        v.invalidate()
    }

    private fun move(v: PixelPetView, dtS: Float) {
        if (phase != Phase.STATE) return
        val s = v.state
        val speedDp = when (s.motion) {
            Motion.WALK -> 38f
            Motion.RUN -> 110f
            Motion.SNEAK -> 18f
            Motion.ROLL -> 32f
            Motion.EDGE -> 150f
            Motion.NONE -> return
        }
        val step = speedDp * density * dtS
        val old = charX
        if (s.motion == Motion.EDGE) {
            val diff = edgeTarget - charX
            charX += if (abs(diff) <= step) diff else step * (if (diff > 0) 1 else -1)
        } else {
            charX += v.facing * step
            if (charX <= 0f) { charX = 0f; v.facing = 1 }
            else if (charX >= maxX()) { charX = maxX(); v.facing = -1 }
        }
        if (charX != old) applyLayout()
    }

    // ───── آلة الحالات ─────
    private fun startState(s: PetState) {
        val v = view ?: return
        phase = Phase.STATE
        reacting = false
        lastState = s
        v.setPetState(s)

        when (s.motion) {
            Motion.EDGE -> {
                val left = charX + v.charW / 2f < screenW / 2f
                edgeTarget = if (left) 0f else maxX()
                v.facing = if (left) -1 else 1
            }
            Motion.NONE -> Unit
            else -> if (Random.nextBoolean()) v.facing = -v.facing
        }

        v.setBubble(if (s.bubble) s.text else null, s.isQuestion)
        val scale = if (s.isQuestion) 1f else Prefs.durationScale(this)
        stateEnd = SystemClock.uptimeMillis() + (s.durationMs * scale).toLong()
        applyLayout()
    }

    private fun startGap() {
        val v = view ?: return
        phase = Phase.GAP
        reacting = false
        v.setPetState(idleState)
        v.setBubble(null, false)
        val chain = lastState == PetState.YAWN_SLEEPY || lastState.sleeping || lastState == PetState.WAKE_SUDDEN
        val gap = when {
            !Prefs.autoMove(this) -> Long.MAX_VALUE / 4      // ينتظر لمسة المستخدم
            chain -> 0L
            else -> Prefs.idleGapMs(this)
        }
        stateEnd = SystemClock.uptimeMillis() + gap
        applyLayout()
    }

    private fun onStateFinished() {
        if (phase == Phase.STATE) {
            startGap()
        } else {
            startState(PetState.follow(lastState))
        }
    }

    // ───── أحداث اللمس من العرض ─────
    override fun onTap() {
        val v = view ?: return
        if (phase == Phase.STATE && v.state.isQuestion && !reacting) return   // لا نقاطع السؤال
        playHitSound()
        view?.renderer?.hitStart = SystemClock.uptimeMillis()   // أداة الضرب تظهر وتضرب
        if (phase == Phase.STATE && v.state.sleeping) {
            startState(PetState.WAKE_SUDDEN)     // «هاه؟! لم أكن نائماً!»
        } else if (phase == Phase.STATE && lastState == PetState.ANGRY_CUTE && reacting) {
            startState(PetState.random(lastState))
        } else {
            // ردة فعل الضربة: وجه مندهش وحركة انزعاج كيوت لثانيتين
            reacting = true
            v.faceOverride = Face.SURPRISED
            startState(PetState.ANGRY_CUTE)
        }
    }

    private var hitPlayer: MediaPlayer? = null

    private fun playHitSound() {
        if (!Prefs.tapSound(this)) return
        try {
            hitPlayer?.release()
            hitPlayer = MediaPlayer.create(this, R.raw.hit)?.apply { start() }
        } catch (_: Exception) {
        }
    }

    override fun onAnswer(yes: Boolean) {
        val v = view ?: return
        val q = v.state.question ?: return
        if (reacting) return
        reacting = true
        v.faceOverride = if (yes) q.yesFace else q.noFace
        v.setBubble(if (yes) q.yesText else q.noText, false)
        stateEnd = SystemClock.uptimeMillis() + 2800L
        applyLayout()
    }

    override fun onDragStart() {
        val v = view ?: return
        if (phase == Phase.STATE && v.state.sleeping) startState(PetState.WAKE_SUDDEN)
    }

    override fun onDrag(dx: Float, dy: Float) {
        charX += dx
        charY += dy
        clampPosition()
        applyLayout()
    }

    override fun onDragEnd() {
        Prefs.savePos(this, charX.roundToInt(), charY.roundToInt())
    }

    // ───── الإشعار ─────
    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW)
        ch.description = getString(R.string.notif_channel_desc)
        ch.setShowBadge(false)
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flags)
        val stopI = PendingIntent.getService(
            this, 1, Intent(this, FloatingPetService::class.java).setAction(ACTION_STOP), flags
        )
        val icon = Icon.createWithResource(this, R.drawable.ic_notif)
        val b = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(icon, getString(R.string.notif_stop), stopI).build())
        if (Build.VERSION.SDK_INT >= 31) {
            b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return b.build()
    }
}
