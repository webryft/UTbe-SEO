package com.utbeseo.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "Overlay")
public class OverlayPlugin extends Plugin {
    private final Handler h = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private LinearLayout box;
    private Runnable run;
    private int left;

    private boolean ok() { return Settings.canDrawOverlays(getContext()); }

    @PluginMethod
    public void hasPermission(PluginCall c) {
        JSObject r = new JSObject(); r.put("granted", ok()); c.resolve(r);
    }

    @PluginMethod
    public void requestPermission(PluginCall c) {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getContext().getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        c.resolve();
    }

    @PluginMethod
    public void openUrl(PluginCall c) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(c.getString("url")));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        c.resolve();
    }

    @PluginMethod
    public void show(final PluginCall c) {
        if (!ok()) { c.reject("no permission"); return; }
        final int secs = c.getInt("seconds", 45);
        final int coins = c.getInt("coins", 0);
        getActivity().runOnUiThread(() -> {
            hideNow();
            Context ctx = getContext();
            wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            final AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            final TextView tv = new TextView(ctx);
            tv.setTextSize(22f); tv.setTextColor(Color.DKGRAY); tv.setGravity(Gravity.CENTER);
            final Button back = new Button(ctx);
            back.setText("RETURN TO APP"); back.setVisibility(View.GONE);
            back.setOnClickListener(v -> {
                Intent l = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
                if (l != null) {
                    l.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    ctx.startActivity(l);
                }
                hideNow();
            });
            box = new LinearLayout(ctx);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setBackgroundColor(Color.WHITE);
            box.setPadding(24, 16, 24, 16);
            box.addView(tv); box.addView(back);
            int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams p = new WindowManager.LayoutParams(-1, -2, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
            p.gravity = Gravity.BOTTOM;
            wm.addView(box, p);
            left = secs;
            run = new Runnable() {
                @Override public void run() {
                    if (left <= 0) { tv.setText("Done  +" + coins + " coins"); back.setVisibility(View.VISIBLE); return; }
                    boolean playing = am.isMusicActive();   // video paused => countdown pauses
                    if (playing) left--;
                    tv.setText((playing ? "" : "Paused  ") + left + "s  |  +" + coins + " coins");
                    h.postDelayed(this, 1000);
                }
            };
            h.post(run);
        });
        c.resolve();
    }

    @PluginMethod
    public void hide(PluginCall c) { getActivity().runOnUiThread(this::hideNow); c.resolve(); }

    private void hideNow() {
        if (run != null) h.removeCallbacks(run);
        if (box != null && wm != null) { try { wm.removeView(box); } catch (Exception e) { } }
        box = null;
    }
}
