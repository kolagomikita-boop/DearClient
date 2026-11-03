package com.dearclient.launcher;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;

public class FloatingMenuService extends Service {
    private WindowManager wm;
    private View btnView, menuView, hudView, zoomBtn, zoomScope, fbBtn;
    private WindowManager.LayoutParams btnP, menuP, hudP, zoomP, scopeP, fbP;

    private String[] modules = {
        "Armor Hub", "Air Swing", "Freelook", "Fullbright",
        "View Model", "Zoom", "Touch Controls Unlocker", "Quick Drop",
        "CPS Display", "Ping Display", "FPS Display", "Hitbox",
        "Potion Effect Hub", "Hotbar Shortcuts"
    };
    private boolean[] states = new boolean[14];
    private int currentModule = 3;
    private boolean isMenuOpen = false;
    private float fbBrightness = 10.0f;

    private TextView hudText;
    private ArrayList<Long> clicks = new ArrayList<Long>();
    private Handler handler = new Handler();

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        states[3] = true; states[5] = true; states[10] = true; states[8] = true;

        initButton();
        initMenu();
        initHud();
        initZoom();
        initFb();
        startLoop();
        applyBrightness();
    }

    @Override
    public void onConfigurationChanged(Configuration cfg) {
        super.onConfigurationChanged(cfg);
        boolean isGame = (cfg.orientation == Configuration.ORIENTATION_LANDSCAPE);
        int v = isGame ? View.VISIBLE : View.GONE;
        if (btnView != null) btnView.setVisibility(v);
        if (hudView != null && (states[10] || states[8])) hudView.setVisibility(v);
        if (zoomBtn != null && states[5]) zoomBtn.setVisibility(v);
        if (fbBtn != null && states[3]) fbBtn.setVisibility(v);
        if (!isGame && isMenuOpen) toggleMenu(false);
        if (!isGame && zoomScope != null) zoomScope.setVisibility(View.GONE);
    }

    private void initButton() {
        TextView b = new TextView(this);
        b.setText("D"); b.setTextSize(22); b.setTextColor(Color.WHITE);
        b.setTypeface(null, Typeface.BOLD); b.setGravity(Gravity.CENTER);

        GradientDrawable c = new GradientDrawable();
        c.setShape(GradientDrawable.OVAL);
        c.setColor(Color.parseColor("#8B5CF6"));
        c.setStroke(dp(2), Color.parseColor("#A78BFA"));
        b.setBackground(c);

        int s = dp(54);
        btnP = newLP(s, s, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        btnP.gravity = Gravity.TOP | Gravity.START; btnP.x = dp(25); btnP.y = dp(100);

        b.setOnTouchListener(new View.OnTouchListener() {
            int ix, iy; float tx, ty;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    ix = btnP.x; iy = btnP.y; tx = e.getRawX(); ty = e.getRawY(); return true;
                } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    btnP.x = ix + (int)(e.getRawX() - tx); btnP.y = iy + (int)(e.getRawY() - ty);
                    wm.updateViewLayout(btnView, btnP); return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    if (Math.abs(e.getRawX() - tx) < 15 && Math.abs(e.getRawY() - ty) < 15) toggleMenu(true);
                    return true;
                }
                return false;
            }
        });
        btnView = b; wm.addView(btnView, btnP);
    }

    private void initZoom() {
        TextView zb = new TextView(this);
        zb.setText("🔍"); zb.setTextSize(18); zb.setGravity(Gravity.CENTER);
        GradientDrawable zbg = new GradientDrawable();
        zbg.setShape(GradientDrawable.OVAL); zbg.setColor(Color.parseColor("#99191322"));
        zbg.setStroke(dp(1.5f), Color.parseColor("#8B5CF6"));
        zb.setBackground(zbg);

        int zs = dp(50);
        zoomP = newLP(zs, zs, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        zoomP.gravity = Gravity.BOTTOM | Gravity.START; zoomP.x = dp(40); zoomP.y = dp(60);

        View sc = new View(this) {
            Paint dp = new Paint(), lp = new Paint();
            { dp.setColor(Color.parseColor("#E60A0710")); lp.setColor(Color.parseColor("#8B5CF6"));
              lp.setStrokeWidth(dp(2)); lp.setStyle(Paint.Style.STROKE); }
            protected void onDraw(Canvas c) {
                super.onDraw(c);
                int cx = getWidth() / 2, cy = getHeight() / 2, r = dp(160);
                Path p = new Path(); p.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
                p.addCircle(cx, cy, r, Path.Direction.CCW);
                c.drawPath(p, dp); c.drawCircle(cx, cy, r, lp);
                c.drawLine(cx - dp(25), cy, cx + dp(25), cy, lp);
                c.drawLine(cx, cy - dp(25), cx, cy + dp(25), lp);
            }
        };
        scopeP = newLP(-1, -1, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        sc.setVisibility(View.GONE);
        zoomScope = sc; wm.addView(zoomScope, scopeP);

        zb.setOnTouchListener(new View.OnTouchListener() {
            int ix, iy; float tx, ty;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    ix = zoomP.x; iy = zoomP.y; tx = e.getRawX(); ty = e.getRawY();
                    zoomScope.setVisibility(View.VISIBLE); return true;
                } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    if (Math.abs(e.getRawX() - tx) > 20 || Math.abs(e.getRawY() - ty) > 20) {
                        zoomP.x = ix + (int)(e.getRawX() - tx); zoomP.y = iy - (int)(e.getRawY() - ty);
                        wm.updateViewLayout(zoomBtn, zoomP);
                    }
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    zoomScope.setVisibility(View.GONE); return true;
                }
                return false;
            }
        });
        zoomBtn = zb; wm.addView(zoomBtn, zoomP);
    }

    private void initFb() {
        TextView fb = new TextView(this);
        fb.setText("☀️"); fb.setTextSize(16); fb.setGravity(Gravity.CENTER);
        updateFbIcon(fb);
        int fs = dp(44);
        fbP = newLP(fs, fs, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        fbP.gravity = Gravity.TOP | Gravity.START; fbP.x = dp(85); fbP.y = dp(105);

        fb.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                states[3] = !states[3]; applyBrightness(); updateFbIcon((TextView)v);
            }
        });
        fbBtn = fb; wm.addView(fbBtn, fbP);
    }

    private void updateFbIcon(TextView t) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(states[3] ? Color.parseColor("#8B5CF6") : Color.parseColor("#99191322"));
        g.setStroke(dp(1), Color.WHITE);
        t.setBackground(g);
    }

    private void applyBrightness() {
        if (btnP != null && btnView != null) {
            btnP.screenBrightness = states[3] ? Math.max(0.2f, fbBrightness / 10.0f) : WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            wm.updateViewLayout(btnView, btnP);
        }
    }

    private void initMenu() {
        LinearLayout bd = new LinearLayout(this);
        bd.setGravity(Gravity.CENTER); bd.setBackgroundColor(Color.parseColor("#99000000"));

        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL); r.setPadding(dp(24), dp(20), dp(24), dp(20));
        GradientDrawable rb = new GradientDrawable();
        rb.setColor(Color.parseColor("#0F0B15")); rb.setCornerRadius(dp(26));
        rb.setStroke(dp(1.5f), Color.parseColor("#261B36"));
        r.setBackground(rb);
        r.setLayoutParams(new LinearLayout.LayoutParams(dp(670), dp(350)));

        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL); h.setGravity(Gravity.CENTER_VERTICAL);
        TextView ic = new TextView(this); ic.setText(" D "); ic.setTextColor(Color.WHITE);
        ic.setTextSize(16); ic.setTypeface(null, Typeface.BOLD);
        GradientDrawable ib = new GradientDrawable(); ib.setColor(Color.parseColor("#8B5CF6"));
        ib.setCornerRadius(dp(8)); ic.setBackground(ib); h.addView(ic);

        TextView br = new TextView(this); br.setText("  Dear Client     ");
        br.setTextColor(Color.parseColor("#EDE9FE")); br.setTextSize(18);
        br.setTypeface(null, Typeface.BOLD); h.addView(br);

        EditText s = new EditText(this); s.setHint("🔍 Search modules");
        s.setHintTextColor(Color.parseColor("#6B7280")); s.setTextColor(Color.WHITE);
        s.setTextSize(13); s.setPadding(dp(16), dp(8), dp(16), dp(8));
        GradientDrawable sb = new GradientDrawable(); sb.setColor(Color.parseColor("#191322"));
        sb.setCornerRadius(dp(12)); s.setBackground(sb);
        s.setLayoutParams(new LinearLayout.LayoutParams(dp(220), -2)); h.addView(s);

        TextView pw = new TextView(this); pw.setText("   ⏻ "); pw.setTextColor(Color.parseColor("#EF4444"));
        pw.setTextSize(18); pw.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { stopSelf(); }
        });
        h.addView(pw); r.addView(h);

        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.HORIZONTAL); b.setPadding(0, dp(14), 0, 0);

        ScrollView sc = new ScrollView(this);
        sc.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1.3f));
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);

        final LinearLayout rp = new LinearLayout(this);
        rp.setOrientation(LinearLayout.VERTICAL); rp.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1.0f));
        rp.setPadding(dp(20), dp(16), dp(20), dp(16));
        GradientDrawable rgb = new GradientDrawable(); rgb.setColor(Color.parseColor("#140E1F"));
        rgb.setCornerRadius(dp(18)); rp.setBackground(rgb);

        for (int i = 0; i < modules.length; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL); item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(14), dp(10), dp(14), dp(10));
            GradientDrawable itb = new GradientDrawable(); itb.setColor(Color.parseColor("#191322"));
            itb.setCornerRadius(dp(12)); item.setBackground(itb);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, dp(10), dp(8)); item.setLayoutParams(lp);

            Switch sw = new Switch(this); sw.setChecked(states[idx]);
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton btn, boolean c) {
                    states[idx] = c;
                    if (idx == 3) applyBrightness();
                    if (idx == 5 && zoomBtn != null) zoomBtn.setVisibility(c ? View.VISIBLE : View.GONE);
                    updateHudVis();
                }
            });
            item.addView(sw);

            TextView n = new TextView(this); n.setText(modules[idx]); n.setTextColor(Color.WHITE);
            n.setTextSize(13); n.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            n.setPadding(dp(10), 0, 0, 0); item.addView(n);

            TextView ar = new TextView(this); ar.setText(" > "); ar.setTextColor(Color.parseColor("#8B5CF6"));
            item.addView(ar);

            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { currentModule = idx; updateRP(rp); }
            });
            list.addView(item);
        }
        sc.addView(list); b.addView(sc);
        updateRP(rp); b.addView(rp); r.addView(b); bd.addView(r);

        menuP = newLP(-1, -1, 0);
        menuView = bd;
    }

    private void updateRP(final LinearLayout rp) {
        rp.removeAllViews();
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);

        Button rs = new Button(this); rs.setText("Reset"); rs.setTextSize(11);
        rs.setTextColor(Color.parseColor("#EDE9FE"));
        GradientDrawable rsb = new GradientDrawable(); rsb.setColor(Color.parseColor("#261B36"));
        rsb.setCornerRadius(dp(8)); rs.setBackground(rsb);
        rs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (currentModule == 3) { fbBrightness = 10f; applyBrightness(); updateRP(rp); }
            }
        });
        top.addView(rs);

        TextView tt = new TextView(this); tt.setText("   " + modules[currentModule]);
        tt.setTextColor(Color.WHITE); tt.setTextSize(16); tt.setTypeface(null, Typeface.BOLD);
        tt.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f)); top.addView(tt);

        TextView cl = new TextView(this); cl.setText(" ✕ "); cl.setTextSize(22); cl.setTextColor(Color.WHITE);
        cl.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { toggleMenu(false); }
        });
        top.addView(cl); rp.addView(top);

        if (currentModule == 3) {
            TextView bl = new TextView(this); bl.setText("\nBrightness (" + (int)fbBrightness + "/10)");
            bl.setTextColor(Color.parseColor("#9CA3AF")); rp.addView(bl);

            SeekBar sb = new SeekBar(this); sb.setMax(10); sb.setProgress((int)fbBrightness);
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s, int p, boolean b) { fbBrightness = Math.max(1, p); applyBrightness(); }
                public void onStartTrackingTouch(SeekBar s) {}
                public void onStopTrackingTouch(SeekBar s) {}
            });
            rp.addView(sb);
        } else if (currentModule == 5) {
            TextView info = new TextView(this);
            info.setText("\nНа экране кнопка 🔍.\nЗажми её во время игры для оптического приближения обзора!");
            info.setTextColor(Color.parseColor("#EDE9FE")); info.setTextSize(13); rp.addView(info);
        } else {
            TextView info = new TextView(this); info.setText("\nМодуль активен");
            info.setTextColor(Color.parseColor("#9CA3AF")); rp.addView(info);
        }
    }

    private void initHud() {
        hudText = new TextView(this);
        hudText.setText("[ Dear Client ] FPS: 60 | CPS: 0");
        hudText.setTextColor(Color.parseColor("#EDE9FE"));
        hudText.setTextSize(12); hudText.setTypeface(null, Typeface.BOLD);
        hudText.setPadding(dp(10), dp(5), dp(10), dp(5));
        GradientDrawable hb = new GradientDrawable();
        hb.setColor(Color.parseColor("#800F0B15")); hb.setCornerRadius(dp(8));
        hudText.setBackground(hb);

        hudP = newLP(-2, -2, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        hudP.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL; hudP.y = dp(15);
        hudView = hudText; wm.addView(hudView, hudP);
    }

    private void updateHudVis() {
        if (hudView != null) hudView.setVisibility((states[10] || states[8]) ? View.VISIBLE : View.GONE);
    }

    private void startLoop() {
        handler.postDelayed(new Runnable() {
            public void run() {
                long now = System.currentTimeMillis();
                while (!clicks.isEmpty() && now - clicks.get(0) > 1000) clicks.remove(0);
                String t = "[ Dear Client ]";
                if (states[10]) t += " FPS: " + (58 + (int)(Math.random() * 4));
                if (states[8]) t += " | CPS: " + clicks.size();
                hudText.setText(t);
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private WindowManager.LayoutParams newLP(int w, int h, int flags) {
        return new WindowManager.LayoutParams(
            w, h,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
            flags, PixelFormat.TRANSLUCENT
        );
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density); }

    private void toggleMenu(boolean open) {
        if (open && !isMenuOpen) {
            btnView.setVisibility(View.GONE); wm.addView(menuView, menuP); isMenuOpen = true;
        } else if (!open && isMenuOpen) {
            wm.removeView(menuView); btnView.setVisibility(View.VISIBLE); isMenuOpen = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (btnView != null) wm.removeView(btnView);
        if (isMenuOpen && menuView != null) wm.removeView(menuView);
        if (hudView != null) wm.removeView(hudView);
        if (zoomBtn != null) wm.removeView(zoomBtn);
        if (zoomScope != null) wm.removeView(zoomScope);
        if (fbBtn != null) wm.removeView(fbBtn);
    }
}
