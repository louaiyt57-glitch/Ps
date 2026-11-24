package com.pixelpet.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** الشاشة الرئيسية البسيطة: الإذن، تشغيل/إيقاف، الحركة التلقائية، السرعة. */
class MainActivity : Activity() {

    private lateinit var tvPerm: TextView
    private lateinit var tvService: TextView
    private lateinit var btnPerm: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var swAuto: Switch
    private lateinit var rgSpeed: RadioGroup

    private val handler = Handler(Looper.getMainLooper())
    private val refresher = object : Runnable {
        override fun run() {
            refreshUi()
            handler.postDelayed(this, 600)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvPerm = findViewById(R.id.tvPermStatus)
        tvService = findViewById(R.id.tvServiceStatus)
        btnPerm = findViewById(R.id.btnPermission)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        swAuto = findViewById(R.id.swAuto)
        rgSpeed = findViewById(R.id.rgSpeed)

        applyInsets(findViewById(R.id.root))

        btnPerm.setOnClickListener { openOverlaySettings() }

        btnStart.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, R.string.need_permission_toast, Toast.LENGTH_LONG).show()
                openOverlaySettings()
                return@setOnClickListener
            }
            requestNotificationPermissionIfNeeded()
            FloatingPetService.start(this)
            handler.postDelayed({ refreshUi() }, 400)
        }

        btnStop.setOnClickListener {
            FloatingPetService.stop(this)
            handler.postDelayed({ refreshUi() }, 400)
        }

        swAuto.isChecked = Prefs.autoMove(this)
        swAuto.setOnCheckedChangeListener { _, checked -> Prefs.setAutoMove(this, checked) }

        rgSpeed.check(
            when (Prefs.speed(this)) {
                Prefs.SPEED_SLOW -> R.id.rbSlow
                Prefs.SPEED_FAST -> R.id.rbFast
                else -> R.id.rbNormal
            }
        )
        rgSpeed.setOnCheckedChangeListener { _, id ->
            Prefs.setSpeed(
                this,
                when (id) {
                    R.id.rbSlow -> Prefs.SPEED_SLOW
                    R.id.rbFast -> Prefs.SPEED_FAST
                    else -> Prefs.SPEED_NORMAL
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refresher)
    }

    override fun onPause() {
        handler.removeCallbacks(refresher)
        super.onPause()
    }

    private fun refreshUi() {
        val granted = Settings.canDrawOverlays(this)
        tvPerm.setText(if (granted) R.string.perm_granted else R.string.perm_missing)
        tvPerm.setTextColor(getColor(if (granted) R.color.ok_green else R.color.bad_red))
        btnPerm.visibility = if (granted) View.GONE else View.VISIBLE

        val running = FloatingPetService.running
        tvService.setText(if (running) R.string.status_running else R.string.status_stopped)
        btnStart.isEnabled = !running
        btnStop.isEnabled = running
        btnStart.alpha = if (running) 0.45f else 1f
        btnStop.alpha = if (running) 1f else 0.45f
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    /** أندرويد 15 يفرض العرض من الحافة إلى الحافة: نضيف حشوة بمقدار أشرطة النظام. */
    @Suppress("DEPRECATION")
    private fun applyInsets(root: View) {
        val base = (20 * resources.displayMetrics.density).toInt()
        root.setOnApplyWindowInsetsListener { v, insets ->
            val top: Int; val bottom: Int; val left: Int; val right: Int
            if (Build.VERSION.SDK_INT >= 30) {
                val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                top = b.top; bottom = b.bottom; left = b.left; right = b.right
            } else {
                top = insets.systemWindowInsetTop; bottom = insets.systemWindowInsetBottom
                left = insets.systemWindowInsetLeft; right = insets.systemWindowInsetRight
            }
            v.setPadding(left, top, right, bottom)
            insets
        }
        root.requestApplyInsets()
    }
}
