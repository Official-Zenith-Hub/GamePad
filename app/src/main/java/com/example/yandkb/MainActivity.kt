package com.example.yandkb

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.parseColor("#14141C"))
        }

        root.addView(TextView(this).apply {
            text = "Gaming keyboard"
            textSize = 30f
            setTextColor(Color.parseColor("#FF4D6D"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "1) เปิดใช้งานคีย์บอร์ด (เปิดสวิตช์ Gaming keyboard)\n2) เลือกใช้ Gaming keyboard\n3) อนุญาตแจ้งเตือน\n4) เปิดเกม แตะแจ้งเตือน เพื่อแสดง/ซ่อนคีย์บอร์ด\n\nอยากกลับไปพิมพ์ไทย: กดไอคอนคีย์บอร์ดที่มุมล่างขวาของจอ แล้วเลือกคีย์บอร์ดเดิม"
            textSize = 14f
            setTextColor(Color.parseColor("#BBBBBB"))
            setPadding(0, 24, 0, 24)
        })

        fun btn(label: String, action: () -> Unit) {
            root.addView(Button(this).apply {
                text = label
                setOnClickListener { action() }
            })
        }

        btn("1) เปิดใช้งานคีย์บอร์ด") {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        btn("2) เลือกใช้ Gaming keyboard") {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
        btn("3) อนุญาตแจ้งเตือน") {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        root.addView(EditText(this).apply {
            hint = "ช่องทดสอบ: แตะตรงนี้แล้วลองกดปุ่ม"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        })

        setContentView(root)
    }
}
