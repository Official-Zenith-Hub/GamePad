package com.example.yandkb

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, r: Int = 16, oval: Boolean = false) =
        GradientDrawable().apply {
            if (oval) shape = GradientDrawable.OVAL else cornerRadius = dp(r).toFloat()
            setColor(color)
        }

    private fun tv(s: String, size: Float, color: String, bold: Boolean = false) =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(Color.parseColor(color))
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(Color.parseColor("#1E1E2A"), 20)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
    }

    private fun button(label: String, action: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 15f
        setTextColor(Color.WHITE)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        background = rounded(Color.parseColor("#FF4D6D"), 14)
        setPadding(dp(12), dp(12), dp(12), dp(12))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
        setOnClickListener { action() }
    }

    private fun stepRow(num: String, title: String, desc: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val badge = TextView(this).apply {
            text = num
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = rounded(Color.parseColor("#FF4D6D"), oval = true)
        }
        row.addView(badge, LinearLayout.LayoutParams(dp(36), dp(36)).apply {
            rightMargin = dp(12)
        })
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(tv(title, 16f, "#FFFFFF", true))
        col.addView(tv(desc, 13f, "#A0A0B0"))
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        return row
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#14141C")
        window.navigationBarColor = Color.parseColor("#14141C")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(32), dp(20), dp(32))
        }

        // ปก
        val cover = ImageView(this).apply {
            setImageResource(R.drawable.app_icon)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.parseColor("#FF4D6D"), 28)
            clipToOutline = true
        }
        root.addView(cover, LinearLayout.LayoutParams(dp(120), dp(120)))

        root.addView(tv("GamePad", 32f, "#FF4D6D", true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        })
        root.addView(tv("คีย์บอร์ดเกมมิ่งสำหรับมือถือ", 14f, "#A0A0B0").apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(8))
        })

        // ขั้นที่ 1
        val c1 = card()
        c1.addView(stepRow("1", "เปิดใช้งานคีย์บอร์ด", "เปิดสวิตช์ GamePad ในรายการ"))
        c1.addView(button("เปิดการตั้งค่าคีย์บอร์ด") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        })
        root.addView(c1)

        // ขั้นที่ 2
        val c2 = card()
        c2.addView(stepRow("2", "เลือกใช้ GamePad", "เลือกเป็นคีย์บอร์ดที่ใช้อยู่"))
        c2.addView(button("เลือกคีย์บอร์ด") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showInputMethodPicker()
        })
        root.addView(c2)

        // ขั้นที่ 3
        val c3 = card()
        c3.addView(stepRow("3", "อนุญาตแจ้งเตือน", "ใช้แตะเปิด/ซ่อนคีย์บอร์ดในเกม"))
        c3.addView(button("อนุญาตแจ้งเตือน") {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            } else {
                Toast.makeText(this, "เครื่องนี้ไม่ต้องอนุญาต", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(c3)

        // วิธีใช้ในเกม
        val c4 = card()
        c4.addView(tv("วิธีใช้ในเกม", 16f, "#FFFFFF", true))
        c4.addView(
            tv(
                "1) เปิดเกม\n2) ดึงแถบแจ้งเตือนลง แล้วแตะ GamePad\n3) กดปุ่ม ◐ ที่แถบหัวเพื่อปรับความโปร่งใส\n4) แตะแจ้งเตือนอีกครั้ง หรือกด ▼ เพื่อซ่อน",
                13f, "#A0A0B0"
            ).apply { setPadding(0, dp(6), 0, 0) }
        )
        root.addView(c4)

        // ช่องทดสอบ
        val c5 = card()
        c5.addView(tv("ช่องทดสอบ", 16f, "#FFFFFF", true))
        val test = EditText(this).apply {
            hint = "แตะที่นี่แล้วลองกดปุ่ม"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#707080"))
            background = rounded(Color.parseColor("#2A2A38"), 12)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        c5.addView(test, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        root.addView(c5)

        root.addView(
            tv(
                "อยากพิมพ์ไทย: กดไอคอนคีย์บอร์ดมุมล่างขวาของจอ แล้วเลือกคีย์บอร์ดเดิม",
                12f, "#808090"
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(16), 0, 0)
            }
        )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#14141C"))
            isFillViewport = true
            addView(root)
        }
        setContentView(scroll)
    }
}
