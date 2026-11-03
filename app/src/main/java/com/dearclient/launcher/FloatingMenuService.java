package com.dearclient.launcher;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.*;
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
    private View zoomButton;
    private View zoomScopeOverlay;
    private View fullbrightQuickBtn;

    private WindowManager.LayoutParams btnParams;
    private WindowManager.LayoutParams menuParams;
    private WindowManager.LayoutParams hudParams;
    private WindowManager.LayoutParams zoomBtnParams;
    private WindowManager.LayoutParams zoomScopeParams;
    private WindowManager.LayoutParams fbQuickParams;

    private String[] moduleNames = {
        "Armor Hub", "Air Swing", "Freelook", "Fullbright",
        "View Model", "Zoom", "Touch Controls Unlocker", "Quick Drop",
        "CPS Display", "Ping Display", "FPS Display", "Hitbox",
        "Potion Effect Hub", "Hotbar Shortcuts"
    };

    private boolean[] moduleStates = new boolean[14];
    private int selectedModule = 3;
    private boolean isMenuOpen = false;

    private float fullbrightLevel = 10.0f;
    private boolean showFbQuickBtn = true;

    // HUD элементы
    private TextView hudTextView;
    private ArrayList<Long> clicks = new ArrayList<Long>();
    private Handler fpsHandler = new Handler();

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        moduleStates[3] = true;  // Fullbright включен
        moduleStates[5] = true;  // Zoom включен
        moduleStates[10] = true; // FPS включен
        moduleStates[8] = true;  // CPS включен

        createFloatingButton();
        createFullMenu();
        createHudOverlay();
        createZoomWidgets();
        createFullbrightQuickButton();
        startFpsCounter();
        applyBrightness();
    }

    // 1. АВТО-СКРЫТИЕ: Прячет ВСЕ кнопки клиента на рабочем столе
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean isLandscape = (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
        int visibility = isLandscape ? View.VISIBLE : View.GONE;

        if (floatingButton != null) floatingButton.setVisibility(visibility);
        if (hudOverlay != null && (moduleStates[10] || moduleStates[8])) hudOverlay.setVisibility(visibility);
        if (zoomButton != null && moduleStates[5]) zoomButton.setVisibility(visibility);
        if (fullbrightQuickBtn != null && showFbQuickBtn && moduleStates[3]) fullbrightQuickBtn.setVisibility(visibility);

        if (!isLandscape && isMenuOpen) closeMenu();
        if (!isLandscape && zoomScopeOverlay != null) zoomScopeOverlay.setVisibility(View.GONE);
    }

    // 2. ГЛАВНАЯ КРУГЛАЯ КНОПКА [ D ]
    private void createFloatingButton() {
        TextView btn = new TextView(this);
        btn.setText("D");
        btn.setTextSize(22);
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);

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

    // 3. ОЖИВЛЕНИЕ ЗУМА: Плавающая кнопка 🔍 и прицел подзорной трубы
    private void createZoomWidgets() {
        // Кнопка 🔍 ZOOM
        final TextView zBtn = new TextView(this);
        zBtn.setText("🔍");
        zBtn.setTextSize(18);
        zBtn.setGravity(Gravity.CENTER);

        GradientDrawable zBg = new GradientDrawable();
        zBg.setShape(GradientDrawable.OVAL);
        zBg.setColor(Color.parseColor("#99191322"));
        zBg.setStroke(dpToPx(1.5f), Color.parseColor("#8B5CF6"));
        zBtn.setBackground(zBg);

        int zSize = dpToPx(50);
        zoomBtnParams = new WindowManager.LayoutParams(
            zSize, zSize,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        zoomBtnParams.gravity = Gravity.BOTTOM | Gravity.START;
        zoomBtnParams.x = dpToPx(40);
        zoomBtnParams.y = dpToPx(60);

        // Полноэкранный прицел подзорной трубы (Spyglass Zoom)
        final View scopeView = new View(this) {
            private Paint darkPaint = new Paint();
            private Paint linePaint = new Paint();

            {
                darkPaint.setColor(Color.parseColor("#E60A0710"));
                linePaint.setColor(Color.parseColor("#8B5CF6"));
                linePaint.setStrokeWidth(dpToPx(2));
                linePaint.setStyle(Paint.Style.STROKE);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int radius = dpToPx(160);

                // Затемнение с круглым окном по центру
                Path path = new Path();
                path.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
                path.addCircle(cx, cy, radius, Path.Direction.CCW);
                canvas.drawPath(path, darkPaint);

                // Оптический прицел внутри
                canvas.drawCircle(cx, cy, radius, linePaint);
                canvas.drawLine(cx - dpToPx(25), cy, cx + dpToPx(25), cy, linePaint);
                canvas.drawLine(cx, cy - dpToPx(25), cx, cy + dpToPx(25), linePaint);
            }
        };

        zoomScopeParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        );

        scopeView.setVisibility(View.GONE);
        zoomScopeOverlay = scopeView;
        windowManager.addView(zoomScopeOverlay, zoomScopeParams);

        // Нажатие на ЗУМ: зажал — приближение, отпустил — выключился
        zBtn.setOnTouchListener(new View.OnTouchListener() {
            private int initX, initY;
            private float touchX, touchY;
            private boolean isDrag = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = zoomBtnParams.x;
                        initY = zoomBtnParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        isDrag = false;
                        zoomScopeOverlay.setVisibility(View.VISIBLE); // ВКЛЮЧАЕМ ЗУМ!
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(event.getRawX() - touchX) > 20 || Math.abs(event.getRawY() - touchY) > 20) {
                            isDrag = true;
                            zoomBtnParams.x = initX + (int) (event.getRawX() - touchX);
                            zoomBtnParams.y = initY - (int) (event.getRawY() - touchY);
                            windowManager.updateViewLayout(zoomButton, zoomBtnParams);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        zoomScopeOverlay.setVisibility(View.GONE); // ВЫКЛЮЧАЕМ ЗУМ!
                        return true;
                }
                return false;
            }
        });

        zoomButton = zBtn;
        windowManager.addView(zoomButton, zoomBtnParams);
    }

    // 4. ОЖИВЛЕНИЕ FULLBRIGHT (ЯРКОСТЬ) И БЫСТРАЯ КНОПКА ☀️
    private void createFullbrightQuickButton() {
        final TextView fbBtn = new TextView(this);
        fbBtn.setText("☀️");
        fbBtn.setTextSize(16);
        fbBtn.setGravity(Gravity.CENTER);

        updateFbButtonVisual(fbBtn);

        int fbSize = dpToPx(44);
        fbQuickParams = new WindowManager.LayoutParams(
            fbSize, fbSize,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        fbQuickParams.gravity = Gravity.TOP | Gravity.START;
        fbQuickParams.x = dpToPx(85);
        fbQuickParams.y = dpToPx(105);

        fbBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moduleStates[3] = !moduleStates[3];
                applyBrightness();
                updateFbButtonVisual(fbBtn);
                Toast.makeText(FloatingMenuService.this, "Fullbright: " + (moduleStates[3] ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            }
        });

        fullbrightQuickBtn = fbBtn;
        windowManager.addView(fullbrightQuickBtn, fbQuickParams);
    }

    private void updateFbButtonVisual(TextView fbBtn) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(moduleStates[3] ? Color.parseColor("#8B5CF6") : Color.parseColor("#99191322"));
        bg.setStroke(dpToPx(1), Color.WHITE);
        fbBtn.setBackground(bg);
    }

    // Применение яркости экрана (ночное зрение)
    private void applyBrightness() {
        if (btnParams != null && floatingButton != null) {
            if (moduleStates[3]) {
                // Выкручиваем яркость на заданный уровень
                btnParams.screenBrightness = Math.max(0.2f, fullbrightLevel / 10.0f);
            } else {
                btnParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            }
            windowManager.updateViewLayout(floatingButton, btnParams);
        }
    }

    // 5. ОСНОВНОЕ ОКНО МЕНЮ
    private void createFullMenu() {
        LinearLayout backdrop = new LinearLayout(this);
        backdrop.setGravity(Gravity.CENTER);
        backdrop.setBackgroundColor(Color.parseColor("#99000000"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(20));

        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(Color.parseColor("#0F0B15"));
        rootBg.setCornerRadius(dpToPx(26));
        rootBg.setStroke(dpToPx(1.5f), Color.parseColor("#261B36"));
        root.setBackground(rootBg);

        LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(dpToPx(670), dpToPx(350));
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

        // КОЛОНКИ
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
                    if (index == 3) applyBrightness();
                    if (index == 5 && zoomButton != null) zoomButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);
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

    private void updateRightPanel(final LinearLayout panel) {
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
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            publ
