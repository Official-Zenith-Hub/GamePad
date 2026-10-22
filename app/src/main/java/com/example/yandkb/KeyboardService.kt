package com.example.yandkb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class KeyboardService : InputMethodService() {

    companion object {
        const val ACTION = "com.example.yandkb.TOGGLE"
    }

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

    // ปุ่มที่ส่งเป็น KeyEvent จริง
    private val codes = HashMap<String, Int>().apply {
        for (c in 'A'..'Z') put(c.toString(), KeyEvent.KEYCODE_A + (c - 'A'))
        for (d in '0'..'9') put(d.toString(), KeyEvent.KEYCODE_0 + (d - '0'))
        put("Space", KeyEvent.KEYCODE_SPACE)
        put("Enter", KeyEvent.KEYCODE_ENTER)
        put("⌫", KeyEvent.KEYCODE_DEL)
        put("Esc", KeyEvent.KEYCODE_ESCAPE)
        put("Tab", KeyEvent.KEYCODE_TAB)
        put("Caps", KeyEvent.KEYCODE_CAPS_LOCK)
        put("Shift", KeyEvent.KEYCODE_SHIFT_LEFT)
        put("Ctrl", KeyEvent.KEYCODE_CTRL_LEFT)
        put("Alt", KeyEvent.KEYCODE_ALT_LEFT)
        put(",", KeyEvent.KEYCODE_COMMA)
        put(".", KeyEvent.KEYCODE_PERIOD)
        put("-", KeyEvent.KEYCODE_MINUS)
        put("=", KeyEvent.KEYCODE_EQUALS)
        put("[", KeyEvent.KEYCODE_LEFT_BRACKET)
        put("]", KeyEvent.KEYCODE_RIGHT_BRACKET)
        put("\\", KeyEvent.KEYCODE_BACKSLASH)
        put(";", KeyEvent.KEYCODE_SEMICOLON)
        put("'", KeyEvent.KEYCODE_APOSTROPHE)
        put("/", KeyEvent.KEYCODE_SLASH)
        put("`", KeyEvent.KEYCODE_GRAVE)
        put("@", KeyEvent.KEYCODE_AT)
        put("#", KeyEvent.KEYCODE_POUND)
        put("*", KeyEvent.KEYCODE_STAR)
        put("+", KeyEvent.KEYCODE_PLUS)
    }

    private lateinit var body: LinearLayout
    private val held = HashSet<String>()
    private var page = 0
    private var windowVisible = false

    private val toggleReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            if (windowVisible) {
                requestHideSelf(0)
            } else {
                Toast.makeText(
                    this@KeyboardService,
                    "กำลังเปิดคีย์บอร์ด...",
                    Toast.LENGTH_SHORT
                ).show()
                showKeyboard()
            }
        }
    }

    private fun showKeyboard() {
        // วิธีที่ 1: สั่งผ่านระบบ (ถ้ามีในเครื่องนี้)
        try {
            InputMethodService::class.java
                .getMethod("requestShowSelf", Int::class.javaPrimitiveType)
                .invoke(this, 0)
        } catch (_: Throwable) {
        }
        // วิธีที่ 2: สั่งแสดงหน้าต่างเอง
        try {
            showWindow(true)
        } catch (_: Throwable) {
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        windowVisible = true
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        windowVisible = false
    }

    // ให้แสดงแป้นได้เสมอ แม้ไม่มีช่องพิมพ์
    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean = true

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun bg(color: Int, radius: Float = 12f) = GradientDrawable().apply {
        cornerRadius = radius
        setColor(color)
    }

    private fun label(text: String, size: Float) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
    }

    override fun onCreate() {
        super.onCreate()
        val f = IntentFilter(ACTION)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(toggleReceiver, f, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(toggleReceiver, f)
        }
        postNotification()
    }

    override fun onDestroy() {
        try { unregisterReceiver(toggleReceiver) } catch (_: Exception) {}
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(1)
        super.onDestroy()
    }

    private fun postNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("kbtoggle", "Gaming keyboard", NotificationManager.IMPORTANCE_LOW)
        )
        val pi = PendingIntent.getBroadcast(
            this, 0,
            Intent(ACTION).setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(this, "kbtoggle")
            .setContentTitle("Gaming keyboard")
            .setContentText("แตะเพื่อแสดง/ซ่อนคีย์บอร์ด")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
        nm.notify(1, n)
    }

    override fun onEvaluateInputViewShown(): Boolean = true

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A22"))
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            label("⌨ Gaming keyboard", 13f).apply { gravity = Gravity.START },
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        header.addView(label("▼ ซ่อน", 13f).apply {
            setPadding(dp(14), dp(6), dp(14), dp(6))
            background = bg(Color.parseColor("#FF3A3A4A"))
            setOnClickListener { requestHideSelf(0) }
        })
        root.addView(header)

        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(body)
        render()
        return root
    }

    private fun render() {
        held.toList().forEach { up(it) }
        held.clear()
        body.removeAllViews()
        pages[page].forEach { r ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            r.forEach { key ->
                val tv = label(key.text, if (key.text.length > 1) 11f else 15f)
                val base = if (key.go >= 0) "#FF55557A" else "#FF3A3A4A"
                tv.background = bg(Color.parseColor(base))
                tv.setOnTouchListener { _, e ->
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            tv.background = bg(Color.parseColor("#FFFF4D6D"))
                            if (key.go < 0) {
                                held.add(key.id)
                                down(key.id)
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            tv.background = bg(Color.parseColor(base))
                            if (key.go >= 0) {
                                if (e.actionMasked == MotionEvent.ACTION_UP) {
                                    tv.post { page = key.go; render() }
                                }
                            } else {
                                held.remove(key.id)
                                up(key.id)
                            }
                        }
                    }
                    true
                }
                line.addView(tv, LinearLayout.LayoutParams(0, dp(44), key.w).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                })
            }
            body.addView(line)
        }
    }

    private fun down(id: String) {
        val ic = currentInputConnection ?: return
        val code = codes[id]
        if (code != null) {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        } else if (id.length == 1) {
            ic.commitText(id, 1)
        }
    }

    private fun up(id: String) {
        val ic = currentInputConnection ?: return
        val code = codes[id] ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}
