package com.utbeseo.app   // change to your appId

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "Overlay")
class OverlayPlugin : Plugin() {
    private val h = Handler(Looper.getMainLooper())
    private var wm: WindowManager? = null
    private var box: LinearLayout? = null
    private var run: Runnable? = null
    private val ok get() = Settings.canDrawOverlays(context)

    @PluginMethod fun hasPermission(c: PluginCall) { c.resolve(JSObject().put("granted", ok)) }

    @PluginMethod fun requestPermission(c: PluginCall) {
        val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(i); c.resolve()
    }

    @PluginMethod fun openUrl(c: PluginCall) {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(c.getString("url")))
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(i); c.resolve()
    }

    @PluginMethod fun show(c: PluginCall) {
        if (!ok) { c.reject("no permission"); return }
        val secs = c.getInt("seconds") ?: 45
        val coins = c.getInt("coins") ?: 0
        activity.runOnUiThread {
            hideNow()
            wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val tv = TextView(context).apply { textSize = 22f; setTextColor(Color.DKGRAY); gravity = Gravity.CENTER }
            val back = Button(context).apply {
                text = "RETURN TO APP"; visibility = View.GONE
                setOnClickListener {
                    val l = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    l?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(l); hideNow()
                }
            }
            box = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(24, 16, 24, 16)
                addView(tv); addView(back)
            }
            val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                       else WindowManager.LayoutParams.TYPE_PHONE
            val p = WindowManager.LayoutParams(-1, -2, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT)
            p.gravity = Gravity.BOTTOM
            wm!!.addView(box, p)
            var left = secs
            run = object : Runnable {
                override fun run() {
                    if (left <= 0) { tv.text = "Done  +$coins coins"; back.visibility = View.VISIBLE; return }
                    val playing = am.isMusicActive          // video paused => countdown pauses
                    if (playing) left--
                    tv.text = (if (playing) "" else "Paused  ") + "${left}s  |  +$coins coins"
                    h.postDelayed(this, 1000)
                }
            }
            h.post(run!!)
        }
        c.resolve()
    }

    @PluginMethod fun hide(c: PluginCall) { activity.runOnUiThread { hideNow() }; c.resolve() }

    private fun hideNow() {
        run?.let { h.removeCallbacks(it) }
        box?.let { try { wm?.removeView(it) } catch (e: Exception) {} }
        box = null
    }
}
