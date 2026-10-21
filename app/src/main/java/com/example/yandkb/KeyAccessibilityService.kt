package com.example.yandkb

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class KeyAccessibilityService : AccessibilityService() {

    companion object {
        var instance: KeyAccessibilityService? = null
        private const val TICK = 40L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val want = HashMap<Int, FloatArray>()
    private val strokes = HashMap<Int, GestureDescription.StrokeDescription>()
    private val last = HashMap<Int, FloatArray>()
    private var running = false

    fun press(id: Int, x: Float, y: Float) {
        want[id] = floatArrayOf(x, y)
        kick()
    }

    fun release(id: Int) {
        if (want.remove(id) != null) kick()
    }

    fun releaseAll() {
        want.clear()
        kick()
    }

    private fun kick() {
        if (!running) {
            running = true
            loop()
        }
    }

    private fun loop() {
        val b = GestureDescription.Builder()
        var n = 0
        val ids = HashSet<Int>()
        ids.addAll(strokes.keys)
        ids.addAll(want.keys)

        for (id in ids) {
            val w = want[id]
            val s = strokes[id]
            if (s == null && w != null) {
                val p = Path().apply { moveTo(w[0], w[1]) }
                val ns = GestureDescription.StrokeDescription(p, 0, TICK, true)
                strokes[id] = ns
                last[id] = w.copyOf()
                b.addStroke(ns)
                n++
            } else if (s != null) {
                val l = last[id] ?: continue
                val p = Path().apply {
                    moveTo(l[0], l[1])
                    if (w != null) lineTo(w[0], w[1])
                }
                val keep = w != null
                val ns = s.continueStroke(p, 0, TICK, keep)
                if (keep) {
                    strokes[id] = ns
                    last[id] = w!!.copyOf()
                } else {
                    strokes.remove(id)
                    last.remove(id)
                }
                b.addStroke(ns)
                n++
            }
        }

        if (n == 0) {
            running = false
            return
        }

        val ok = dispatchGesture(b.build(), object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) {
                handler.post { loop() }
            }

            override fun onCancelled(g: GestureDescription?) {
                strokes.clear()
                last.clear()
                handler.post { loop() }
            }
        }, null)

        if (!ok) {
            running = false
            strokes.clear()
            last.clear()
        }
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        want.clear()
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
