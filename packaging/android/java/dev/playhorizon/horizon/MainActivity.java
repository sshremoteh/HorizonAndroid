package dev.playhorizon.horizon;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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

    private LinearLayout mContentLayout;
    private String mLastCrashText = null;
    private EditText mNameInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "MainActivity created");

        // Ensure app external files directory exists
        File extFiles = getExternalFilesDir(null);
        if (extFiles != null) {
            new File(extFiles, "resources").mkdirs();
            new File(extFiles, "logs").mkdirs();
        }

        // Try creating /sdcard/horizon if accessible
        try {
            File sdcardHorizon = new File("/sdcard/horizon/resources");
            sdcardHorizon.mkdirs();
            new File("/sdcard/horizon/logs").mkdirs();
        } catch (Throwable ignored) {}

        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor("#121214"));
        scrollView.setFillViewport(true);

        mContentLayout = new LinearLayout(this);
        mContentLayout.setOrientation(LinearLayout.VERTICAL);
        mContentLayout.setPadding(48, 36, 48, 48);

        scrollView.addView(mContentLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(scrollView);
        refreshStatus();
    }

    private void refreshStatus() {
        mContentLayout.removeAllViews();

        // 1. Title Header
        TextView title = new TextView(this);
        title.setText("🎮 Horizon — Minecraft Java 26.3 (Rust)");
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#E4E4E7"));
        title.setPadding(0, 0, 0, 16);
        mContentLayout.addView(title);

        // Subtitle
        TextView subtitle = new TextView(this);
        subtitle.setText("Нативный порт игрового движка Horizon для Android (ARM64 / Vulkan 1.1+)");
        subtitle.setTextSize(13);
        subtitle.setTextColor(Color.parseColor("#A1A1AA"));
        subtitle.setPadding(0, 0, 0, 24);
        mContentLayout.addView(subtitle);

        // 2. Storage Permission Status
        boolean hasStorageAccess = checkStoragePermission();
        LinearLayout permCard = createCard(hasStorageAccess ? "#1C2E20" : "#2E2419");
        TextView permTitle = new TextView(this);
        permTitle.setText(hasStorageAccess ? "✅ Доступ к памяти: Разрешен" : "⚠️ Доступ к памяти: Требуется разрешение");
        permTitle.setTypeface(Typeface.DEFAULT_BOLD);
        permTitle.setTextColor(hasStorageAccess ? Color.parseColor("#4ADE80") : Color.parseColor("#FBBF24"));
        permCard.addView(permTitle);

        TextView permDesc = new TextView(this);
        permDesc.setText(hasStorageAccess
                ? "Приложение может читать и записывать файлы в /sdcard/horizon/."
                : "Для загрузки ресурсов из /sdcard/horizon/ требуется предоставить доступ ко всем файлам (All files access).");
        permDesc.setTextSize(12);
        permDesc.setTextColor(Color.parseColor("#D4D4D8"));
        permDesc.setPadding(0, 8, 0, 0);
        permCard.addView(permDesc);

        if (!hasStorageAccess) {
            Button grantBtn = createButton("📁 Предоставить доступ к файлам", "#EAB308", Color.BLACK);
            grantBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestStoragePermission();
                }
            });
            permCard.addView(grantBtn);
        }
        mContentLayout.addView(permCard);

        // 3. Resources Status
        File resPath = findResourcesDirectory();
        boolean hasResources = (resPath != null);
        LinearLayout resCard = createCard(hasResources ? "#1C2E20" : "#2E1C1C");
        TextView resTitle = new TextView(this);
        resTitle.setText(hasResources ? "✅ Ресурсы игры: Найдены!" : "❌ Ресурсы игры: Не найдены");
        resTitle.setTypeface(Typeface.DEFAULT_BOLD);
        resTitle.setTextColor(hasResources ? Color.parseColor("#4ADE80") : Color.parseColor("#F87171"));
        resCard.addView(resTitle);

        TextView resDesc = new TextView(this);
        if (hasResources) {
            resDesc.setText("Файлы ресурсов обнаружены в:\n" + resPath.getAbsolutePath());
        } else {
            resDesc.setText("Horizon не содержит встроенных ассетов Mojang.\n\n"
                    + "Поместите подготовленные ресурсы (horizon-resources.json, assets/, и т.д.) в одну из папок:\n"
                    + "  1) /sdcard/horizon/resources/\n"
                    + "  2) " + (getExternalFilesDir(null) != null ? getExternalFilesDir(null).getAbsolutePath() + "/resources/" : "Android/data/dev.playhorizon.horizon/files/resources/"));
        }
        resDesc.setTextSize(12);
        resDesc.setTextColor(Color.parseColor("#D4D4D8"));
        resDesc.setPadding(0, 8, 0, 0);
        resCard.addView(resDesc);
        mContentLayout.addView(resCard);

        // 4. Crash report check
        mLastCrashText = readCrashFile();
        if (mLastCrashText != null && !mLastCrashText.trim().isEmpty()) {
            LinearLayout crashCard = createCard("#2D1515");
            TextView crashTitle = new TextView(this);
            crashTitle.setText("⚠️ Лог последней ошибки / завершения:");
            crashTitle.setTypeface(Typeface.DEFAULT_BOLD);
            crashTitle.setTextColor(Color.parseColor("#FCA5A5"));
            crashCard.addView(crashTitle);

            TextView crashContent = new TextView(this);
            crashContent.setText(mLastCrashText);
            crashContent.setTextSize(11);
            crashContent.setTypeface(Typeface.MONOSPACE);
            crashContent.setTextColor(Color.parseColor("#FECACA"));
            crashContent.setPadding(12, 12, 12, 12);
            crashContent.setBackgroundColor(Color.parseColor("#18181B"));
            crashCard.addView(crashContent);

            Button copyBtn = createButton("📋 Скопировать отчет об ошибке", "#DC2626", Color.WHITE);
            copyBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cb != null) {
                        cb.setPrimaryClip(ClipData.newPlainText("Horizon Crash", mLastCrashText));
                        Toast.makeText(MainActivity.this, "Скопировано в буфер обмена!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            crashCard.addView(copyBtn);
            mContentLayout.addView(crashCard);
        }

        // 5. Settings Card (Настройки Horizon Launcher)
        final SharedPreferences prefs = getSharedPreferences("horizon_launcher_prefs", MODE_PRIVATE);
        LinearLayout settingsCard = createCard("#1E222D");

        TextView settingsTitle = new TextView(this);
        settingsTitle.setText("⚙️ Настройки Horizon Launcher");
        settingsTitle.setTextSize(16);
        settingsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        settingsTitle.setTextColor(Color.parseColor("#E0E6ED"));
        settingsCard.addView(settingsTitle);

        // Player Name
        TextView nameLabel = new TextView(this);
        nameLabel.setText("Имя игрока (ник):");
        nameLabel.setTextSize(12);
        nameLabel.setTextColor(Color.parseColor("#A1A1AA"));
        nameLabel.setPadding(0, 16, 0, 6);
        settingsCard.addView(nameLabel);

        mNameInput = new EditText(this);
        mNameInput.setText(prefs.getString("player_name", "Player"));
        mNameInput.setTextColor(Color.WHITE);
        mNameInput.setTextSize(14);
        mNameInput.setBackgroundColor(Color.parseColor("#121214"));
        mNameInput.setPadding(24, 16, 24, 16);
        settingsCard.addView(mNameInput);

        // Render Distance
        TextView distLabel = new TextView(this);
        distLabel.setText("Дистанция прорисовки (чанки):");
        distLabel.setTextSize(12);
        distLabel.setTextColor(Color.parseColor("#A1A1AA"));
        distLabel.setPadding(0, 16, 0, 6);
        settingsCard.addView(distLabel);

        final int currentDist = prefs.getInt("render_distance", 6);
        final int[] distances = new int[] { 4, 6, 8, 10, 12 };
        final Button[] distBtns = new Button[distances.length];
        LinearLayout distBar = new LinearLayout(this);
        distBar.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < distances.length; i++) {
            final int d = distances[i];
            final int idx = i;
            Button b = new Button(this);
            b.setText(d + " ч.");
            b.setTextSize(12);
            b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            boolean sel = (d == currentDist);
            b.setBackgroundColor(Color.parseColor(sel ? "#3B82F6" : "#2A2E3D"));
            b.setTextColor(Color.WHITE);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prefs.edit().putInt("render_distance", d).apply();
                    for (int j = 0; j < distances.length; j++) {
                        distBtns[j].setBackgroundColor(Color.parseColor(j == idx ? "#3B82F6" : "#2A2E3D"));
                    }
                }
            });
            distBtns[i] = b;
            distBar.addView(b);
        }
        settingsCard.addView(distBar);

        // In-Frame toggle (Single thread)
        final CheckBox inFrameCheck = new CheckBox(this);
        inFrameCheck.setText("Режим одного потока (--in-frame)\n(Экономит RAM на устройствах с 3-4 ГБ)");
        inFrameCheck.setTextColor(Color.parseColor("#E4E4E7"));
        inFrameCheck.setTextSize(12);
        inFrameCheck.setChecked(prefs.getBoolean("in_frame", false));
        inFrameCheck.setPadding(0, 16, 0, 8);
        inFrameCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("in_frame", isChecked).apply();
            }
        });
        settingsCard.addView(inFrameCheck);

        // Touch buttons opacity
        TextView opLabel = new TextView(this);
        opLabel.setText("Прозрачность сенсорных кнопок:");
        opLabel.setTextSize(12);
        opLabel.setTextColor(Color.parseColor("#A1A1AA"));
        opLabel.setPadding(0, 12, 0, 6);
        settingsCard.addView(opLabel);

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
            b.setTextSize(12);
            b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            boolean sel = (op == currentOp);
            b.setBackgroundColor(Color.parseColor(sel ? "#3B82F6" : "#2A2E3D"));
            b.setTextColor(Color.WHITE);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prefs.edit().putInt("touch_opacity", op).apply();
                    for (int j = 0; j < opacities.length; j++) {
                        opBtns[j].setBackgroundColor(Color.parseColor(j == idx ? "#3B82F6" : "#2A2E3D"));
                    }
                }
            });
            opBtns[i] = b;
            opBar.addView(b);
        }
        settingsCard.addView(opBar);

        mContentLayout.addView(settingsCard);

        // 6. Actions / Launch Buttons
        LinearLayout btnBar = new LinearLayout(this);
        btnBar.setOrientation(LinearLayout.HORIZONTAL);
        btnBar.setPadding(0, 24, 0, 0);

        Button launchBtn = createButton(hasResources ? "▶️ Запустить Horizon" : "⚠️ Запустить (без ресурсов)", "#22C55E", Color.BLACK);
        launchBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        launchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGame();
            }
        });
        btnBar.addView(launchBtn);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(24, 1));
        btnBar.addView(spacer);

        Button refreshBtn = createButton("🔄 Обновить", "#3F3F46", Color.WHITE);
        refreshBtn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshStatus();
                Toast.makeText(MainActivity.this, "Статус обновлен", Toast.LENGTH_SHORT).show();
            }
        });
        btnBar.addView(refreshBtn);

        mContentLayout.addView(btnBar);
    }

    private LinearLayout createCard(String hexColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 24, 32, 24);
        card.setBackgroundColor(Color.parseColor(hexColor));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 20);
        card.setLayoutParams(lp);
        return card;
    }

    private Button createButton(String text, String bgColor, int textColor) {
        Button b = new Button(this);
        b.setText(text);
        b.setBackgroundColor(Color.parseColor(bgColor));
        b.setTextColor(textColor);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 16, 0, 0);
        b.setLayoutParams(lp);
        return b;
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
        Log.i(TAG, "Launching HorizonGameActivity with configured settings...");
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
