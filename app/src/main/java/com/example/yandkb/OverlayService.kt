package com.example.yandkb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class OverlayService : Service() {

    private class K(
        val id: String,
        val text: String = id,
        val w: Float = 1f,
        val go: Int = -1
    )

    private val wide = setOf("Esc", "Tab", "Caps", "Shift", "Ctrl", "Alt")

    private fun row(vararg s: String) = s.map { K(it, it, if (it in wide) 1.5f else 1f) }

    private val pages: List<List<List<K>>> = listOf(
        // หน้า 0: ตัวอักษร
        listOf(
            row("Esc", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            row("Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            row("Caps", "A", "S", "D", "F", "G", "H", "J", "K", "L"),
            row("Shift", "Z", "X", "C", "V", "B", "N", "M") + K("⌫", "⌫", 1.5f),
            listOf(
                K("Ctrl", "Ctrl", 1.3f),
                K("Alt", "Alt", 1.3f),
                K("?123", "?123", 1.3f, go = 1),
                K("Space", "Space", 4f),
                K("Enter", "↵", 1.5f)
            )
        ),
        // หน้า 1: ตัวเลขและสัญลักษณ์
        listOf(
            row("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            row("@", "#", "฿", "_", "&", "-", "+", "(", ")", "/"),
            listOf(K("=\\<", "=\\<", 1.5f, go = 2)) +
                    row("*", "\"", "'", ":", ";", "!", "?") +
                    K("⌫", "⌫", 1.5f),
            listOf(
                K("กขค", "กขค", 1.5f, go = 0),
                K(","),
                K("12/34"),
                K("Space", "Space", 4f),
                K("."),
                K("Enter", "↵", 1.5f)
            )
        ),
        // หน้า 2: สัญลักษณ์เพิ่มเติม
        listOf(
            row("~", "`", "|", "•", "√", "π", "÷", "×", "§", "Δ"),
            row("£", "€", "\$", "¢", "^", "°", "=", "{", "}", "\\"),
            listOf(K("?123", "?123", 1.5f, go = 1)) +
                    row("%", "©", "®", "™", "✓", "[", "]") +
                    K("⌫", "⌫", 1.5f),
            listOf(
                K("กขค", "กขค", 1.5f, go = 0),
                K("<"),
                K("12/34"),
                K("Space", "Space", 4f),
                K(">"),
                K("Enter", "↵", 1.5f)
            )
        )
    )

    private val allIds = pages.flatten().flatten().filter { it.go < 0 }.map { it.id }.distinct()
    private val navIds = pages.flatten().flatten().filter { it.go >= 0 }.map { it.id }.toSet()

    private lateinit var wm: WindowManager
    private val prefs by lazy { getSharedPreferences("kb2", MODE_PRIVATE) }

    private val keyViews = HashMap<String, TextView>()
    private val binds = HashMap<String, FloatArray>()
    private val dots = HashMap<String, TextView>()

    private lateinit var panel: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var title: TextView
    private lateinit var editBtn: TextView
    private lateinit var fab: TextView
    private lateinit var fabP: WindowManager.LayoutParams

    private var page = 0
    private var panelShown = false
    private var editing = false
    private var selected: String? = null
    private var capture: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun flags(touchable: Boolean): Int {
        var f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (!touchable) f = f or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return f
    }

    private fun lp(w: Int, h: Int, touchable: Boolean) = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        flags(touchable),
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun bg(color: Int, oval: Boolean = false, radius: Float = 14f) =
        GradientDrawable().apply {
            if (oval) shape = GradientDrawable.OVAL else cornerRadius = radius
            setColor(color)
        }

    private fun label(text: String, size: Float = 14f) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startFg()

        allIds.forEach { k ->
            prefs.getString("k_$k", null)?.split(",")?.let {
                binds[k] = floatArrayOf(it[0].toFloat(), it[1].toFloat())
            }
        }

        buildPanel()
        buildFab()
    }

    override fun onStartCommand(intent: Intent?, fl: Int, startId: Int): Int {
        startFg()
        return START_STICKY
    }

    private fun startFg() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("kb", "Yandere Keyboard", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, "kb")
            .setContentTitle("Yandere Keyboard กำลังทำงาน")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    // ---------- คีย์บอร์ด ----------
    private fun buildPanel() {
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = bg(Color.parseColor("#E61A1A22"), radius = 24f)
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        title = label("⌨ Yandere Keyboard", 13f).apply { gravity = Gravity.START }
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))

        editBtn = label("✎ ตั้งปุ่ม", 13f).apply {
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = bg(Color.parseColor("#FF3A3A4A"))
            setOnClickListener { toggleEdit() }
        }
        header.addView(editBtn)

        val close = label("▼", 13f).apply {
            setPadding(dp(14), dp(6), dp(14), dp(6))
            background = bg(Color.parseColor("#FF3A3A4A"))
            setOnClickListener { hidePanel() }
        }
        header.addView(close, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(6) })
        panel.addView(header)

        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        panel.addView(body)
        renderPage()

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags(true),
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM }
        panel.visibility = View.GONE
        wm.addView(panel, p)
    }

    private fun renderPage() {
        KeyAccessibilityService.instance?.releaseAll()
        body.removeAllViews()
        keyViews.clear()
        pages[page].forEach { r ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            r.forEach { key ->
                val tv = label(key.text, if (key.text.length > 1) 11f else 15f)
                keyViews[key.id] = tv
                refreshKey(key.id)
                attachKey(tv, key)
                line.addView(tv, LinearLayout.LayoutParams(0, dp(40), key.w).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                })
            }
            body.addView(line)
        }
    }

    private fun refreshKey(id: String) {
        val tv = keyViews[id] ?: return
        val color = when {
            id == selected -> "#FFFACC15"
            id in navIds -> "#FF55557A"
            binds.containsKey(id) -> "#FFFF4D6D"
            else -> "#FF3A3A4A"
        }
        tv.background = bg(Color.parseColor(color), radius = 12f)
        tv.setTextColor(if (id == selected) Color.BLACK else Color.WHITE)
    }

    private fun refreshAll() {
        keyViews.keys.toList().forEach { refreshKey(it) }
    }

    private fun attachKey(tv: TextView, key: K) {
        val id = allIds.indexOf(key.id) + 1
        tv.setOnTouchListener { _, e ->
            if (key.go >= 0) {
                if (e.actionMasked == MotionEvent.ACTION_UP) {
                    selected = null
                    removeCapture()
                    page = key.go
                    renderPage()
                }
                return@setOnTouchListener true
            }
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (editing) {
                        selectKey(key.id)
                    } else {
                        val b = binds[key.id]
                        if (b != null) {
                            tv.background = bg(Color.WHITE, radius = 12f)
                            tv.setTextColor(Color.BLACK)
                            KeyAccessibilityService.instance?.press(id, b[0], b[1])
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!editing) {
                        KeyAccessibilityService.instance?.release(id)
                        refreshKey(key.id)
                    }
                }
            }
            true
        }
    }

    // ---------- ปุ่มลอย ----------
    private fun buildFab() {
        val size = dp(54)
        fab = label("⌨", 22f).apply {
            background = bg(Color.parseColor("#E6FF4D6D"), oval = true)
        }
        fabP = lp(size, size, true).apply {
            x = prefs.getInt("fx", dp(12))
            y = prefs.getInt("fy", dp(120))
        }

        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        fab.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    sx = e.rawX; sy = e.rawY; ox = fabP.x; oy = fabP.y; moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - sx
                    val dy = e.rawY - sy
                    if (abs(dx) > 12 || abs(dy) > 12) moved = true
                    if (moved) {
                        fabP.x = ox + dx.toInt()
                        fabP.y = oy + dy.toInt()
                        wm.updateViewLayout(fab, fabP)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        prefs.edit().putInt("fx", fabP.x).putInt("fy", fabP.y).apply()
                    } else {
                        if (panelShown) hidePanel() else showPanel()
                    }
                }
            }
            true
        }
        wm.addView(fab, fabP)
    }

    private fun showPanel() {
        panelShown = true
        panel.visibility = View.VISIBLE
    }

    private fun hidePanel() {
        panelShown = false
        KeyAccessibilityService.instance?.releaseAll()
        if (editing) toggleEdit()
        removeCapture()
        panel.visibility = View.GONE
    }

    // ---------- โหมดตั้งปุ่ม ----------
    private fun toggleEdit() {
        editing = !editing
        selected = null
        removeCapture()
        editBtn.text = if (editing) "✔ เสร็จ" else "✎ ตั้งปุ่ม"
        title.text = if (editing) "แตะปุ่ม แล้วแตะจุดบนจอเกม" else "⌨ Yandere Keyboard"
        refreshAll()
        if (editing) allIds.forEach { updateDot(it) } else removeDots()
    }

    private fun selectKey(k: String) {
        selected = k
        refreshAll()
        showCapture(k)
    }

    private fun showCapture(k: String) {
        removeCapture()
        val layer = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#66000000"))
        }
        val hint = label("แตะจุดบนจอที่อยากให้ปุ่ม $k กด", 16f).apply {
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = bg(Color.parseColor("#E6000000"))
        }
        layer.addView(hint, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(16)
        })
        val cancel = label("ยกเลิก", 14f).apply {
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = bg(Color.parseColor("#E6B91C1C"))
            setOnClickListener {
                selected = null
                removeCapture()
                refreshAll()
            }
        }
        layer.addView(cancel, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply {
            topMargin = dp(16); rightMargin = dp(16)
        })

        layer.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP) {
                binds[k] = floatArrayOf(e.rawX, e.rawY)
                prefs.edit().putString("k_$k", "${e.rawX},${e.rawY}").apply()
                selected = null
                removeCapture()
                updateDot(k)
                refreshAll()
            }
            true
        }

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags(true),
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        wm.addView(layer, p)
        capture = layer
    }

    private fun removeCapture() {
        capture?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        capture = null
    }

    private fun updateDot(k: String) {
        val b = binds[k] ?: return
        val size = dp(34)
        dots.remove(k)?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        val d = label(k.take(2), 10f).apply {
            background = bg(Color.parseColor("#CC22C55E"), oval = true)
        }
        val p = lp(size, size, false).apply {
            x = (b[0] - size / 2).toInt()
            y = (b[1] - size / 2).toInt()
        }
        wm.addView(d, p)
        dots[k] = d
    }

    private fun removeDots() {
        dots.values.forEach { try { wm.removeView(it) } catch (_: Exception) {} }
        dots.clear()
    }

    override fun onDestroy() {
        KeyAccessibilityService.instance?.releaseAll()
        removeCapture()
        removeDots()
        try { wm.removeView(panel) } catch (_: Exception) {}
        try { wm.removeView(fab) } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
