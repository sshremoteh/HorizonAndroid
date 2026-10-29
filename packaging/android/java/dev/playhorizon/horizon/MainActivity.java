package dev.playhorizon.horizon;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
        subtitle.setPadding(0, 0, 0, 28);
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

        // 5. Actions / Launch Buttons
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

        Button refreshBtn = createButton("🔄 Проверить снова", "#3F3F46", Color.WHITE);
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
        Log.i(TAG, "Launching HorizonGameActivity...");
        Intent intent = new Intent(this, HorizonGameActivity.class);
        startActivity(intent);
    }
}
