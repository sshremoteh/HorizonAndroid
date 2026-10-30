package dev.playhorizon.horizon;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class MainActivity extends Activity {
    private static final String TAG = "HorizonLauncher";

    private LinearLayout mRootLayout;
    private LinearLayout mLeftColumn;
    private LinearLayout mRightColumn;
    private EditText mNameInput;
    private String mLastCrashText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "MainActivity created - Zalith Modern UI");

        // Ensure directories exist
        File extFiles = getExternalFilesDir(null);
        if (extFiles != null) {
            new File(extFiles, "resources").mkdirs();
            new File(extFiles, "logs").mkdirs();
        }
        try {
            File sdcardHorizon = new File("/sdcard/horizon/resources");
            sdcardHorizon.mkdirs();
            new File("/sdcard/horizon/logs").mkdirs();
        } catch (Throwable ignored) {}

        buildZalithUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildZalithUi() {
        mRootLayout = new LinearLayout(this);
        mRootLayout.setOrientation(LinearLayout.VERTICAL);
        mRootLayout.setBackgroundColor(Color.parseColor("#090A0F")); // Deep sleek OLED background
        mRootLayout.setPadding(32, 24, 32, 24);

        setContentView(mRootLayout);
        refreshStatus();
    }

    private void refreshStatus() {
        mRootLayout.removeAllViews();

        final SharedPreferences prefs = getSharedPreferences("horizon_launcher_prefs", MODE_PRIVATE);

        // --- 1. TOP HEADER BAR (Zalith Style) ---
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(8, 0, 8, 16);

        // App title & badge
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView brandText = new TextView(this);
        brandText.setText("HORIZON");
        brandText.setTextSize(20);
        brandText.setTypeface(Typeface.DEFAULT_BOLD);
        brandText.setTextColor(Color.parseColor("#9A86FF")); // Horizon Accent Violet
        titleRow.addView(brandText);

        TextView badge = new TextView(this);
        badge.setText(" RUST 26.3 ");
        badge.setTextSize(10);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setTextColor(Color.parseColor("#10B981"));
        badge.setBackground(createRoundedBackground("#112A20", "#059669", 2, 8));
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.setMargins(14, 0, 0, 0);
        badge.setLayoutParams(badgeLp);
        badge.setPadding(8, 4, 8, 4);
        titleRow.addView(badge);

        titleBox.addView(titleRow);

        TextView subHeader = new TextView(this);
        subHeader.setText("Zalith Launcher Edition • Vulkan 1.1+ • ARM64 Native");
        subHeader.setTextSize(11);
        subHeader.setTextColor(Color.parseColor("#71717A"));
        titleBox.addView(subHeader);

        header.addView(titleBox);

        // Quick Player Chip in Header
        String currentName = prefs.getString("player_name", "Player");
        TextView userChip = new TextView(this);
        userChip.setText("👤 " + currentName);
        userChip.setTextSize(12);
        userChip.setTypeface(Typeface.DEFAULT_BOLD);
        userChip.setTextColor(Color.parseColor("#E4E4E7"));
        userChip.setBackground(createRoundedBackground("#181920", "#27272A", 1, 16));
        userChip.setPadding(20, 10, 20, 10);
        header.addView(userChip);

        mRootLayout.addView(header);

        // --- 2. MAIN TWO-COLUMN CONTENT ---
        LinearLayout columns = new LinearLayout(this);
        columns.setOrientation(LinearLayout.HORIZONTAL);
        columns.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        // LEFT COLUMN: Settings & Game Config (ScrollView)
        ScrollView leftScroll = new ScrollView(this);
        leftScroll.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.15f));
        leftScroll.setFillViewport(true);

        mLeftColumn = new LinearLayout(this);
        mLeftColumn.setOrientation(LinearLayout.VERTICAL);
        mLeftColumn.setPadding(0, 0, 16, 0);
        leftScroll.addView(mLeftColumn);
        columns.addView(leftScroll);

        // RIGHT COLUMN: Launch & Status (Fixed Action Dashboard)
        mRightColumn = new LinearLayout(this);
        mRightColumn.setOrientation(LinearLayout.VERTICAL);
        mRightColumn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.85f));
        mRightColumn.setPadding(16, 0, 0, 0);
        columns.addView(mRightColumn);

        mRootLayout.addView(columns);

        // Populate Left Column (Controls & Settings)
        populateSettingsCard(prefs);

        // Populate Right Column (Status & Launch CTA)
        populateDashboardCard(prefs);
    }

    private void populateSettingsCard(final SharedPreferences prefs) {
        // --- Nickname Input Box ---
        LinearLayout nameCard = createModernCard("#13141C", "#262838");
        TextView nameLabel = new TextView(this);
        nameLabel.setText("👤 НИКНЕЙМ ИГРОКА");
        nameLabel.setTextSize(11);
        nameLabel.setTypeface(Typeface.DEFAULT_BOLD);
        nameLabel.setTextColor(Color.parseColor("#9A86FF"));
        nameCard.addView(nameLabel);

        mNameInput = new EditText(this);
        mNameInput.setText(prefs.getString("player_name", "Player"));
        mNameInput.setTextColor(Color.WHITE);
        mNameInput.setTextSize(13);
        mNameInput.setBackground(createRoundedBackground("#0D0E13", "#22232E", 1, 8));
        mNameInput.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.setMargins(0, 8, 0, 0);
        mNameInput.setLayoutParams(nameLp);
        nameCard.addView(mNameInput);
        mLeftColumn.addView(nameCard);

        // --- Render Distance Chips ---
        LinearLayout distCard = createModernCard("#13141C", "#262838");
        TextView distLabel = new TextView(this);
        distLabel.setText("⚡ ДИСТАНЦИЯ ПРОРИСОВКИ");
        distLabel.setTextSize(11);
        distLabel.setTypeface(Typeface.DEFAULT_BOLD);
        distLabel.setTextColor(Color.parseColor("#9A86FF"));
        distCard.addView(distLabel);

        final int currentDist = prefs.getInt("render_distance", 6);
        final int[] distances = new int[] { 4, 6, 8, 10, 12 };
        final Button[] distBtns = new Button[distances.length];
        LinearLayout distBar = new LinearLayout(this);
        distBar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        barLp.setMargins(0, 10, 0, 0);
        distBar.setLayoutParams(barLp);

        for (int i = 0; i < distances.length; i++) {
            final int d = distances[i];
            final int idx = i;
            Button b = new Button(this);
            b.setText(d + " ч.");
            b.setTextSize(11);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, 90, 1.0f);
            if (i > 0) bLp.setMargins(8, 0, 0, 0);
            b.setLayoutParams(bLp);

            boolean sel = (d == currentDist);
            b.setBackground(createRoundedBackground(sel ? "#7C3AED" : "#1B1C26", sel ? "#A78BFA" : "#2E3040", 1, 8));
            b.setTextColor(sel ? Color.WHITE : Color.parseColor("#A1A1AA"));

            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prefs.edit().putInt("render_distance", d).apply();
                    for (int j = 0; j < distances.length; j++) {
                        boolean active = (j == idx);
                        distBtns[j].setBackground(createRoundedBackground(
                                active ? "#7C3AED" : "#1B1C26",
                                active ? "#A78BFA" : "#2E3040", 1, 8));
                        distBtns[j].setTextColor(active ? Color.WHITE : Color.parseColor("#A1A1AA"));
                    }
                }
            });
            distBtns[i] = b;
            distBar.addView(b);
        }
        distCard.addView(distBar);
        mLeftColumn.addView(distCard);

        // --- Touch Controls Settings (Zalith Layout & Opacity) ---
        LinearLayout touchCard = createModernCard("#13141C", "#262838");
        TextView touchLabel = new TextView(this);
        touchLabel.setText("🎮 СЕНСОРНОЕ УПРАВЛЕНИЕ (ZALITH)");
        touchLabel.setTextSize(11);
        touchLabel.setTypeface(Typeface.DEFAULT_BOLD);
        touchLabel.setTextColor(Color.parseColor("#9A86FF"));
        touchCard.addView(touchLabel);

        // Opacity Row
        TextView opLabel = new TextView(this);
        opLabel.setText("Прозрачность кнопок:");
        opLabel.setTextSize(11);
        opLabel.setTextColor(Color.parseColor("#A1A1AA"));
        opLabel.setPadding(0, 10, 0, 4);
        touchCard.addView(opLabel);

        final int currentOp = prefs.getInt("touch_opacity", 75);
        final int[] opacities = new int[] { 40, 75, 100 };
        final String[] opLabels = new String[] { "40%", "75%", "100%" };
        final Button[] opBtns = new Button[opacities.length];
        LinearLayout opBar = new LinearLayout(this);
        opBar.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < opacities.length; i++) {
            final int op = opacities[i];
            final int idx = i;
            Button b = new Button(this);
            b.setText(opLabels[i]);
            b.setTextSize(11);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, 80, 1.0f);
            if (i > 0) bLp.setMargins(8, 0, 0, 0);
            b.setLayoutParams(bLp);

            boolean sel = (op == currentOp);
            b.setBackground(createRoundedBackground(sel ? "#2563EB" : "#1B1C26", sel ? "#60A5FA" : "#2E3040", 1, 8));
            b.setTextColor(sel ? Color.WHITE : Color.parseColor("#A1A1AA"));

            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prefs.edit().putInt("touch_opacity", op).apply();
                    for (int j = 0; j < opacities.length; j++) {
                        boolean active = (j == idx);
                        opBtns[j].setBackground(createRoundedBackground(
                                active ? "#2563EB" : "#1B1C26",
                                active ? "#60A5FA" : "#2E3040", 1, 8));
                        opBtns[j].setTextColor(active ? Color.WHITE : Color.parseColor("#A1A1AA"));
                    }
                }
            });
            opBtns[i] = b;
            opBar.addView(b);
        }
        touchCard.addView(opBar);

        // Reset to Zalith Layout Button
        Button resetBtn = new Button(this);
        resetBtn.setText("🔄 Сбросить раскладку на Zalith Launcher");
        resetBtn.setTextSize(11);
        resetBtn.setTypeface(Typeface.DEFAULT_BOLD);
        resetBtn.setBackground(createRoundedBackground("#1E293B", "#334155", 1, 8));
        resetBtn.setTextColor(Color.parseColor("#38BDF8"));
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 84);
        resetLp.setMargins(0, 12, 0, 0);
        resetBtn.setLayoutParams(resetLp);
        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File cfg = new File("/sdcard/horizon/touch_layout.json");
                if (cfg.exists()) {
                    cfg.delete();
                }
                Toast.makeText(MainActivity.this, "Раскладка сброшена! При запуске активируется оригинальный профиль Zalith.", Toast.LENGTH_LONG).show();
            }
        });
        touchCard.addView(resetBtn);

        mLeftColumn.addView(touchCard);

        // In-Frame toggle (Single thread)
        final CheckBox inFrameCheck = new CheckBox(this);
        inFrameCheck.setText("Режим одного потока (--in-frame)\n(Экономит RAM на слабых устройствах)");
        inFrameCheck.setTextColor(Color.parseColor("#D4D4D8"));
        inFrameCheck.setTextSize(11);
        inFrameCheck.setChecked(prefs.getBoolean("in_frame", false));
        inFrameCheck.setPadding(0, 8, 0, 8);
        inFrameCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("in_frame", isChecked).apply();
            }
        });
        mLeftColumn.addView(inFrameCheck);
    }

    private void populateDashboardCard(final SharedPreferences prefs) {
        boolean hasStorageAccess = checkStoragePermission();
        File resPath = findResourcesDirectory();
        boolean hasResources = (resPath != null);

        // --- Status Card ---
        LinearLayout statusCard = createModernCard("#13141C", "#262838");

        TextView statTitle = new TextView(this);
        statTitle.setText("📊 СОСТОЯНИЕ");
        statTitle.setTextSize(11);
        statTitle.setTypeface(Typeface.DEFAULT_BOLD);
        statTitle.setTextColor(Color.parseColor("#A1A1AA"));
        statusCard.addView(statTitle);

        // Storage line
        TextView storeLine = new TextView(this);
        storeLine.setText(hasStorageAccess ? "• Память: Доступ разрешен" : "• Память: Требуется доступ к файлам");
        storeLine.setTextSize(11);
        storeLine.setTextColor(hasStorageAccess ? Color.parseColor("#34D399") : Color.parseColor("#FBBF24"));
        storeLine.setPadding(0, 6, 0, 0);
        statusCard.addView(storeLine);

        if (!hasStorageAccess) {
            Button grantBtn = new Button(this);
            grantBtn.setText("Разрешить доступ к памяти");
            grantBtn.setTextSize(10);
            grantBtn.setBackground(createRoundedBackground("#854D0E", "#FACC15", 1, 6));
            grantBtn.setTextColor(Color.WHITE);
            grantBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestStoragePermission();
                }
            });
            statusCard.addView(grantBtn);
        }

        // Resources line
        TextView resLine = new TextView(this);
        resLine.setText(hasResources ? "• Ресурсы: Загружены (" + resPath.getName() + ")" : "• Ресурсы: Не найдены (/sdcard/horizon/resources)");
        resLine.setTextSize(11);
        resLine.setTextColor(hasResources ? Color.parseColor("#34D399") : Color.parseColor("#F87171"));
        resLine.setPadding(0, 4, 0, 0);
        statusCard.addView(resLine);

        mRightColumn.addView(statusCard);

        // --- Last Crash Card (if present) ---
        mLastCrashText = readCrashFile();
        if (mLastCrashText != null && !mLastCrashText.trim().isEmpty()) {
            LinearLayout crashCard = createModernCard("#241014", "#7F1D1D");
            TextView crashTitle = new TextView(this);
            crashTitle.setText("⚠️ Лог последней ошибки:");
            crashTitle.setTextSize(10);
            crashTitle.setTypeface(Typeface.DEFAULT_BOLD);
            crashTitle.setTextColor(Color.parseColor("#FCA5A5"));
            crashCard.addView(crashTitle);

            Button copyBtn = new Button(this);
            copyBtn.setText("Скопировать отчет");
            copyBtn.setTextSize(10);
            copyBtn.setBackground(createRoundedBackground("#991B1B", "#EF4444", 1, 6));
            copyBtn.setTextColor(Color.WHITE);
            copyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cb != null) {
                        cb.setPrimaryClip(ClipData.newPlainText("Horizon Crash", mLastCrashText));
                        Toast.makeText(MainActivity.this, "Скопировано в буфер!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            crashCard.addView(copyBtn);
            mRightColumn.addView(crashCard);
        }

        // Spacer to push Play button to bottom
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        mRightColumn.addView(spacer);

        // --- BIG ZALITH PLAY BUTTON ---
        Button playBtn = new Button(this);
        playBtn.setText(hasResources ? "▶  ИГРАТЬ (PLAY)" : "▶  ИГРАТЬ (БЕЗ РЕСУРСОВ)");
        playBtn.setTextSize(16);
        playBtn.setTypeface(Typeface.DEFAULT_BOLD);
        playBtn.setTextColor(Color.parseColor("#062013"));
        playBtn.setBackground(createRoundedBackground("#10B981", "#34D399", 2, 14));
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 120);
        playLp.setMargins(0, 8, 0, 0);
        playBtn.setLayoutParams(playLp);
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGame();
            }
        });
        mRightColumn.addView(playBtn);
    }

    private LinearLayout createModernCard(String bgColor, String strokeColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 16, 20, 16);
        card.setBackground(createRoundedBackground(bgColor, strokeColor, 1, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 14);
        card.setLayoutParams(lp);
        return card;
    }

    private GradientDrawable createRoundedBackground(String bgColor, String strokeColor, int strokeWidth, int cornerRadius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(bgColor));
        gd.setCornerRadius(cornerRadius);
        if (strokeColor != null && strokeWidth > 0) {
            gd.setStroke(strokeWidth, Color.parseColor(strokeColor));
        }
        return gd;
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }
    }

    private File findResourcesDirectory() {
        File[] candidates = new File[] {
                new File("/sdcard/horizon/resources/horizon-resources.json"),
                new File(getExternalFilesDir(null), "resources/horizon-resources.json"),
                new File(getFilesDir(), "resources/horizon-resources.json")
        };
        for (File f : candidates) {
            if (f != null && f.exists() && f.isFile()) {
                return f.getParentFile();
            }
        }
        return null;
    }

    private String readCrashFile() {
        File[] candidates = new File[] {
                new File(getExternalFilesDir(null), "crash.txt"),
                new File("/sdcard/horizon/crash.txt"),
                new File("/sdcard/horizon-crash.txt"),
                new File(getFilesDir(), "crash.txt")
        };
        for (File f : candidates) {
            if (f != null && f.exists() && f.isFile()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private void launchGame() {
        Log.i(TAG, "Launching HorizonGameActivity with Zalith profile settings...");
        SharedPreferences prefs = getSharedPreferences("horizon_launcher_prefs", MODE_PRIVATE);
        String name = prefs.getString("player_name", "Player");
        if (mNameInput != null && mNameInput.getText() != null) {
            String entered = mNameInput.getText().toString().trim();
            if (!entered.isEmpty()) {
                name = entered;
                prefs.edit().putString("player_name", name).apply();
            }
        }
        int dist = prefs.getInt("render_distance", 6);
        boolean inFrame = prefs.getBoolean("in_frame", false);
        int op = prefs.getInt("touch_opacity", 75);
        int alpha255 = (int) (op * 2.55f);

        Intent intent = new Intent(this, HorizonGameActivity.class);
        intent.putExtra("HORIZON_PLAYER_NAME", name);
        intent.putExtra("HORIZON_RENDER_DISTANCE", String.valueOf(dist));
        intent.putExtra("HORIZON_SERVER_THREAD", inFrame ? "0" : "1");
        intent.putExtra("HORIZON_TOUCH_OPACITY", String.valueOf(alpha255));
        startActivity(intent);
    }
}
