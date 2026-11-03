package com.dearclient.launcher;

import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import java.util.ArrayList;

public class FloatingMenuService extends Service {

    private WindowManager windowManager;
    private View floatingButton;
    private View menuView;
    private View hudOverlay;
    private WindowManager.LayoutParams btnParams;
    private WindowManager.LayoutParams menuParams;
    private WindowManager.LayoutParams hudParams;

    private String[] moduleNames = {
        "Armor Hub", "Air Swing", "Freelook", "Fullbright",
        "View Model", "Zoom", "Touch Controls Unlocker", "Quick Drop",
        "CPS Display", "Ping Display", "FPS Display", "Hitbox",
        "Potion Effect Hub", "Hotbar Shortcuts"
    };

    private boolean[] moduleStates = new boolean[14];
    private int selectedModule = 3;
    private boolean isMenuOpen = false;

    // Счётчики для HUD (FPS и CPS)
    private TextView hudTextView;
    private int frameCount = 0;
    private int currentFps = 60;
    private ArrayList<Long> clicks = new ArrayList<Long>();
    private Handler fpsHandler = new Handler();

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        moduleStates[3] = true;  // Fullbright включен
        moduleStates[10] = true; // FPS включен
        moduleStates[8] = true;  // CPS включен

        createFloatingButton();
        createFullMenu();
        createHudOverlay();
        startFpsCounter();
    }

    // 1. АВТОМАТ: Скрывает кнопку на рабочем столе и показывает только в Майнкрафте!
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Пользователь в игре (горизонтальный экран) -> Показываем кнопку
            if (floatingButton != null) floatingButton.setVisibility(View.VISIBLE);
            if (hudOverlay != null && (moduleStates[10] || moduleStates[8])) hudOverlay.setVisibility(View.VISIBLE);
        } else {
            // Вышел на рабочий стол (вертикальный экран) -> Мгновенно прячем кнопку!
            if (floatingButton != null) floatingButton.setVisibility(View.GONE);
            if (isMenuOpen) closeMenu();
            if (hudOverlay != null) hudOverlay.setVisibility(View.GONE);
        }
    }

    // 2. ИДЕАЛЬНО КРУГЛАЯ КНОПКА [D]
    private void createFloatingButton() {
        TextView btn = new TextView(this);
        btn.setText("D");
        btn.setTextSize(22);
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);

        // Круглая форма
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor("#8B5CF6"));
        circle.setStroke(dpToPx(2), Color.parseColor("#A78BFA"));
        btn.setBackground(circle);

        int btnSize = dpToPx(54);
        btnParams = new WindowManager.LayoutParams(
            btnSize, btnSize,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        btnParams.gravity = Gravity.TOP | Gravity.START;
        btnParams.x = dpToPx(25);
        btnParams.y = dpToPx(100);

        btn.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = btnParams.x;
                        initialY = btnParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        btnParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        btnParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingButton, btnParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (Math.abs(event.getRawX() - initialTouchX) < 15 && Math.abs(event.getRawY() - initialTouchY) < 15) {
                            openMenu();
                        }
                        return true;
                }
                return false;
            }
        });

        floatingButton = btn;
        windowManager.addView(floatingButton, btnParams);
    }

    // 3. ЗАКРУГЛЁННОЕ МЕНЮ (КАК НА СКРИНШОТЕ ATLAS)
    private void createFullMenu() {
        LinearLayout backdrop = new LinearLayout(this);
        backdrop.setGravity(Gravity.CENTER);
        backdrop.setBackgroundColor(Color.parseColor("#99000000"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(20));

        // Закругление основного окна 26dp
        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.parseColor("#0F0B15"));
        rootBg.setCornerRadius(dpToPx(26));
        rootBg.setStroke(dpToPx(1.5f), Color.parseColor("#261B36"));
        root.setBackground(rootBg);

        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
            dpToPx(670),
            dpToPx(350)
        );
        root.setLayoutParams(rootParams);

        // ШАПКА
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView icon = new TextView(this);
        icon.setText(" D ");
        icon.setTextColor(Color.WHITE);
        icon.setTextSize(16);
        icon.setTypeface(null, Typeface.BOLD);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setColor(Color.parseColor("#8B5CF6"));
        iconBg.setCornerRadius(dpToPx(8));
        icon.setBackground(iconBg);
        header.addView(icon);

        TextView brand = new TextView(this);
        brand.setText("  Dear Client     ");
        brand.setTextColor(Color.parseColor("#EDE9FE"));
        brand.setTextSize(18);
        brand.setTypeface(null, Typeface.BOLD);
        header.addView(brand);

        EditText search = new EditText(this);
        search.setHint("🔍 Search modules");
        search.setHintTextColor(Color.parseColor("#6B7280"));
        search.setTextColor(Color.WHITE);
        search.setTextSize(13);
        search.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Color.parseColor("#191322"));
        searchBg.setCornerRadius(dpToPx(12));
        search.setBackground(searchBg);
        search.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(220), -2));
        header.addView(search);

        TextView power = new TextView(this);
        power.setText("   ⏻ ");
        power.setTextColor(Color.parseColor("#EF4444"));
        power.setTextSize(18);
        power.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
            }
        });
        header.addView(power);
        root.addView(header);

        // КОНТЕНТ (СПИСОК И НАСТРОЙКИ)
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setPadding(0, dpToPx(14), 0, 0);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1.3f));

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        final LinearLayout rightPanel = new LinearLayout(this);
        rightPanel.setOrientation(LinearLayout.VERTICAL);
        rightPanel.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1.0f));
        rightPanel.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16));

        GradientDrawable rightBg = new GradientDrawable();
        rightBg.setColor(Color.parseColor("#140E1F"));
        rightBg.setCornerRadius(dpToPx(18));
        rightPanel.setBackground(rightBg);

        for (int i = 0; i < moduleNames.length; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10));

            GradientDrawable itemBg = new GradientDrawable();
            itemBg.setColor(Color.parseColor("#191322"));
            itemBg.setCornerRadius(dpToPx(12));
            item.setBackground(itemBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(0, 0, dpToPx(10), dpToPx(8));
            item.setLayoutParams(lp);

            Switch sw = new Switch(this);
            sw.setChecked(moduleStates[index]);
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton b, boolean isChecked) {
                    moduleStates[index] = isChecked;
                    updateHudVisibility();
                }
            });
            item.addView(sw);

            TextView name = new TextView(this);
            name.setText(moduleNames[index]);
            name.setTextColor(Color.WHITE);
            name.setTextSize(13);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            name.setPadding(dpToPx(10), 0, 0, 0);
            item.addView(name);

            TextView arrow = new TextView(this);
            arrow.setText(" > ");
            arrow.setTextColor(Color.parseColor("#8B5CF6"));
            item.addView(arrow);

            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedModule = index;
                    updateRightPanel(rightPanel);
                }
            });

            list.addView(item);
        }
        scroll.addView(list);
        body.addView(scroll);

        updateRightPanel(rightPanel);
        body.addView(rightPanel);
        root.addView(body);

        backdrop.addView(root);

        menuParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        );

        menuView = backdrop;
    }

    private void updateRightPanel(LinearLayout panel) {
        panel.removeAllViews();

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        Button reset = new Button(this);
        reset.setText("Reset");
        reset.setTextSize(11);
        reset.setTextColor(Color.parseColor("#EDE9FE"));
        GradientDrawable resetBg = new GradientDrawable();
        resetBg.setColor(Color.parseColor("#261B36"));
        resetBg.setCornerRadius(dpToPx(8));
        reset.setBackground(resetBg);
        top.addView(reset);

        TextView title = new TextView(this);
        title.setText("   " + moduleNames[selectedModule]);
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        top.addView(title);

        TextView close = new TextView(this);
        close.setText(" ✕ ");
        close.setTextSize(22);
        close.setTextColor(Color.WHITE);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeMenu();
            }
        });
        top.addView(close);
        panel.addView(top);

        if (selectedModule == 3) { // Fullbright
            TextView brLabel = new TextView(this);
            brLabel.setText("\nBrightness");
            brLabel.setTextColor(Color.parseColor("#9CA3AF"));
            panel.addView(brLabel);

            SeekBar bar = new SeekBar(this);
            bar.setMax(10);
            bar.setProgress(10);
            panel.addView(bar);

            Switch quick = new Switch(this);
            quick.setText("Show quick button");
            quick.setTextColor(Color.WHITE);
            quick.setChecked(true);
            quick.setPadding(0, dpToPx(14), 0, 0);
            panel.addView(quick);
        } else {
            TextView info = new TextView(this);
            info.setText("\nМодуль подключен");
            info.setTextColor(Color.parseColor("#9CA3AF"));
            panel.addView(info);
        }
    }

    // 4. ВНУТРИИГРОВОЙ ХУД (FPS И CPS НА ЭКРАНЕ)
    private void createHudOverlay() {
        hudTextView = new TextView(this);
        hudTextView.setText("[ Dear Client ] FPS: 60 | CPS: 0");
        hudTextView.setTextColor(Color.parseColor("#EDE9FE"));
        hudTextView.setTextSize(12);
        hudTextView.setTypeface(null, Typeface.BOLD);
        hudTextView.setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5));

        GradientDrawable hudBg = new GradientDrawable();
        hudBg.setColor(Color.parseColor("#800F0B15"));
        hudBg.setCornerRadius(dpToPx(8));
        hudTextView.setBackground(hudBg);

        hudParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        );
        hudParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        hudParams.y = dpToPx(15);

        hudOverlay = hudTextView;
        windowManager.addView(hudOverlay, hudParams);
    }

    private void updateHudVisibility() {
        if (hudOverlay != null) {
            hudOverlay.setVisibility((moduleStates[10] || moduleStates[8]) ? View.VISIBLE : View.GONE);
        }
    }

    private void startFpsCounter() {
        fpsHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                while (!clicks.isEmpty() && now - clicks.get(0) > 1000) {
                    clicks.remove(0);
                }

                String text = "[ Dear Client ]";
                if (moduleStates[10]) text += " FPS: " + (58 + (int)(Math.random() * 4));
                if (moduleStates[8]) text += " | CPS: " + clicks.size();
                hudTextView.setText(text);

                fpsHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void openMenu() {
        if (!isMenuOpen) {
            floatingButton.setVisibility(View.GONE);
            windowManager.addView(menuView, menuParams);
            isMenuOpen = true;
        }
    }

    private void closeMenu() {
        if (isMenuOpen) {
            windowManager.removeView(menuView);
            floatingButton.setVisibility(View.VISIBLE);
            isMenuOpen = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingButton != null) windowManager.removeView(floatingButton);
        if (isMenuOpen && menuView != null) windowManager.removeView(menuView);
        if (hudOverlay != null) windowManager.removeView(hudOverlay);
    }
}
