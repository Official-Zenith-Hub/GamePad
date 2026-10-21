package com.example.yandkb

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        root.addView(TextView(this).apply {
            text = "Yandere Keyboard"
            textSize = 24f
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "1) อนุญาตแสดงทับแอพอื่น\n2) เปิด Accessibility ของแอพนี้\n3) กลับมาเปิดแอพนี้ ปุ่มลอย ⌨ จะขึ้น\n4) เปิดเกม กดปุ่มลอยเพื่อเปิด/ปิดคีย์บอร์ด"
            textSize = 14f
            setPadding(0, 24, 0, 24)
        })

        fun btn(label: String, action: () -> Unit) {
            root.addView(Button(this).apply {
                text = label
                setOnClickListener { action() }
            })
        }

        btn("1) อนุญาต Overlay") {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
        btn("2) เปิด Accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btn("เริ่มปุ่มลอย") {
            if (Settings.canDrawOverlays(this)) {
                startService(Intent(this, OverlayService::class.java))
            }
        }
        btn("หยุดปุ่มลอย") {
            stopService(Intent(this, OverlayService::class.java))
        }

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        if (Settings.canDrawOverlays(this)) {
            startService(Intent(this, OverlayService::class.java))
        }
    }
}
