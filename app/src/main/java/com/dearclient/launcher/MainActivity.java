package com.dearclient.launcher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Главный темный фон #0F0B15
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0F0B15"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 50, 40, 50);

        // 1. Карточка Dear Client v1.0
        LinearLayout card1 = createCard();
        TextView logo = new TextView(this);
        logo.setText(" D ");
        logo.setTextSize(22);
        logo.setTextColor(Color.WHITE);
        logo.setBackgroundColor(Color.parseColor("#8B5CF6"));
        logo.setTypeface(null, Typeface.BOLD);
        card1.addView(logo);

        TextView title1 = new TextView(this);
        title1.setText("  Dear Client");
        title1.setTextSize(20);
        title1.setTextColor(Color.parseColor("#EDE9FE"));
        title1.setTypeface(null, Typeface.BOLD);
        title1.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        card1.addView(title1);

        TextView v1 = new TextView(this);
        v1.setText("v1.0");
        v1.setTextColor(Color.parseColor("#9CA3AF"));
        card1.addView(v1);
        root.addView(card1);

        // 2. Карточка Minecraft 1.26.50
        LinearLayout card2 = createCard();
        TextView title2 = new TextView(this);
        title2.setText("Minecraft");
        title2.setTextSize(20);
        title2.setTextColor(Color.parseColor("#EDE9FE"));
        title2.setTypeface(null, Typeface.BOLD);
        title2.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        card2.addView(title2);

        TextView v2 = new TextView(this);
        v2.setText("1.26.50");
        v2.setTextColor(Color.parseColor("#9CA3AF"));
        card2.addView(v2);
        root.addView(card2);

        // 3. Заголовок Changelogs
        TextView chTitle = new TextView(this);
        chTitle.setText("\nChangelogs\n");
        chTitle.setTextSize(22);
        chTitle.setTextColor(Color.WHITE);
        chTitle.setTypeface(null, Typeface.BOLD);
        root.addView(chTitle);

        // 4. Список ваших 14 функций
        LinearLayout chCard = createCard();
        chCard.setOrientation(LinearLayout.VERTICAL);
        TextView list = new TextView(this);
        list.setText("1.0\n\n" +
                "- Armor Hub: armor and durability HUD\n" +
                "- Air Swing: arm swing settings\n" +
                "- Freelook: look around without turning\n" +
                "- Fullbright: maximum brightness\n" +
                "- View Model: hand and item position\n" +
                "- Zoom: zoom in on demand\n" +
                "- Touch Controls Unlocker: remove layout limits\n" +
                "- Quick Drop: drop items fast\n" +
                "- CPS Display: clicks per second\n" +
                "- Ping Display: server latency\n" +
                "- FPS Display: frames per second\n" +
                "- Hitbox: show entity hitboxes\n" +
                "- Potion Effect Hub: active effects HUD\n" +
                "- Hotbar Shortcuts: slot buttons on screen");
        list.setTextColor(Color.parseColor("#9CA3AF"));
        list.setTextSize(14);
        list.setLineSpacing(10, 1);
        chCard.addView(list);
        root.addView(chCard);

        // 5. Кнопка Launch Minecraft
        Button launchBtn = new Button(this);
        launchBtn.setText("▶  Launch Minecraft 1.26.50 with Dear Client");
        launchBtn.setBackgroundColor(Color.parseColor("#8B5CF6"));
        launchBtn.setTextColor(Color.BLACK);
        launchBtn.setTextSize(15);
        launchBtn.setTypeface(null, Typeface.BOLD);
        launchBtn.setPadding(0, 40, 0, 40);

        launchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = getPackageManager().getLaunchIntentForPackage("com.mojang.minecraftpe");
                if (intent != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Minecraft 1.26.50 не найден!", Toast.LENGTH_LONG).show();
                }
            }
        });
        root.addView(launchBtn);

        scroll.addView(root);
        setContentView(scroll);
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setBackgroundColor(Color.parseColor("#191322"));
        card.setPadding(35, 35, 35, 35);
        card.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, 25);
        card.setLayoutParams(lp);
        return card;
    }
}
