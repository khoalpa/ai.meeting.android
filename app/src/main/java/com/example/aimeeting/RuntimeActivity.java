package com.example.aimeeting;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressLint("SetTextI18n")
public class RuntimeActivity extends Activity {
    private static final String[] MODEL_TABS = {"stt", "ocr", "translation", "tts", "assistant"};
    private static final String TAB_SYSTEM = "system";
    private static final String TAB_MODELS = "models";
    private static final String TAB_GUIDE = "guide";
    private static final String TAB_LOGS = "logs";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private LinearLayout rootLayout;
    private LinearLayout headerLayout;
    private ScrollView scrollView;
    private LinearLayout contentLayout;
    private TextView statusText;
    private Button systemTabButton;
    private Button modelsTabButton;
    private Button guideTabButton;
    private Button logsTabButton;
    private String activeRuntimeTab = TAB_SYSTEM;
    private String systemInfo = "";
    private String modelInfo = "";
    private String usageGuide = "";
    private String logsInfo = "";
    private int refreshToken;
    private String uiLanguageCode = "vi";
    private String languageOneName = "Ngon ngu nguon";
    private String languageTwoName = "Ngôn ngữ đích";
    private String languageOneCode = "vi";
    private String languageTwoCode = "en";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiLanguageCode = extraOrDefault("ui_language_code", "vi");
        languageOneName = extraOrDefault("language_1_name", LocalizedText.get(uiLanguageCode, "language_1"));
        languageTwoName = extraOrDefault("language_2_name", LocalizedText.get(uiLanguageCode, "language_2"));
        languageOneCode = extraOrDefault("language_1_code", "vi");
        languageTwoCode = extraOrDefault("language_2_code", "en");
        buildUi();
        refreshRuntime();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private String extraOrDefault(String key, String fallback) {
        String value = getIntent().getStringExtra(key);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void buildUi() {
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundResource(R.drawable.app_background);

        headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.applyShell(this, rootLayout, headerLayout);
        headerLayout.setPadding(dp(20), dp(16), dp(20), dp(12));
        rootLayout.addView(headerLayout, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerLayout.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText(t("runtime"));
        title.setTextColor(getColor(R.color.meeting_text));
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button refresh = actionButton("");
        PremiumUi.secondaryIconButton(this, refresh, R.drawable.ic_action_refresh, t("refresh"));
        refresh.setOnClickListener(v -> refreshRuntime());
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(dp(56), dp(36));
        refreshParams.setMargins(dp(8), 0, 0, 0);
        titleRow.addView(refresh, refreshParams);

        statusText = new TextView(this);
        statusText.setText(t("runtime_reading"));
        statusText.setTextColor(getColor(R.color.meeting_muted));
        statusText.setTextSize(14);
        statusText.setMaxLines(3);
        headerLayout.addView(statusText);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(-1, -2);
        tabsParams.setMargins(0, dp(8), 0, 0);
        headerLayout.addView(tabs, tabsParams);

        systemTabButton = runtimeTabButton(t("runtime_tab_system"), TAB_SYSTEM);
        modelsTabButton = runtimeTabButton(t("runtime_tab_models"), TAB_MODELS);
        guideTabButton = runtimeTabButton(t("runtime_tab_guide"), TAB_GUIDE);
        logsTabButton = runtimeTabButton(t("logs"), TAB_LOGS);
        tabs.addView(systemTabButton, weightedParams(1, 0));
        tabs.addView(modelsTabButton, weightedParams(1, dp(6)));
        tabs.addView(guideTabButton, weightedParams(1, dp(6)));
        tabs.addView(logsTabButton, weightedParams(1, dp(6)));

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(dp(16), dp(12), dp(16), dp(20));
        scrollView.addView(contentLayout);
        rootLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        PremiumUi.addBottomNavigation(this, rootLayout, "runtime", uiLanguageCode,
                languageOneName, languageOneCode, languageTwoName, languageTwoCode);
        setContentView(rootLayout);
        applySystemBarInsets();
        updateRuntimeTabs();
    }

    private void applySystemBarInsets() {
        int baseHeaderTop = headerLayout.getPaddingTop();
        int baseBottom = scrollView.getPaddingBottom();
        rootLayout.setOnApplyWindowInsetsListener((view, insets) -> {
            headerLayout.setPadding(headerLayout.getPaddingLeft(), baseHeaderTop + InsetsCompat.systemBarTop(insets),
                    headerLayout.getPaddingRight(), headerLayout.getPaddingBottom());
            scrollView.setPadding(scrollView.getPaddingLeft(), scrollView.getPaddingTop(),
                    scrollView.getPaddingRight(), baseBottom + InsetsCompat.systemBarBottom(insets));
            return insets;
        });
        rootLayout.requestApplyInsets();
    }

    private void refreshRuntime() {
        statusText.setText(t("runtime_updating"));
        int token = ++refreshToken;
        systemInfo = buildSystemInfo();
        modelInfo = buildModelInfo();
        usageGuide = buildUsageGuide();
        logsInfo = t("logs_reading");
        renderActiveRuntimeTab();
        executor.execute(() -> {
            String logs = readLogs();
            mainHandler.post(() -> {
                if (token != refreshToken) {
                    return;
                }
                logsInfo = logs;
                if (TAB_LOGS.equals(activeRuntimeTab)) {
                    renderActiveRuntimeTab();
                }
                statusText.setText(t("updated_at") + " " + new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()));
            });
        });
    }

    private void renderActiveRuntimeTab() {
        contentLayout.removeAllViews();
        if (TAB_MODELS.equals(activeRuntimeTab)) {
            addCard(t("models_in_use"), modelInfo, false);
        } else if (TAB_GUIDE.equals(activeRuntimeTab)) {
            addCard(t("usage_guide"), usageGuide, false);
        } else if (TAB_LOGS.equals(activeRuntimeTab)) {
            addCard(t("logs"), logsInfo, true);
        } else {
            addCard(t("system_info"), systemInfo, false);
        }
    }

    private void selectRuntimeTab(String tab) {
        activeRuntimeTab = tab;
        updateRuntimeTabs();
        renderActiveRuntimeTab();
    }

    private void updateRuntimeTabs() {
        styleRuntimeTab(systemTabButton, TAB_SYSTEM);
        styleRuntimeTab(modelsTabButton, TAB_MODELS);
        styleRuntimeTab(guideTabButton, TAB_GUIDE);
        styleRuntimeTab(logsTabButton, TAB_LOGS);
    }

    private String buildSystemInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append(t("app")).append(": ").append(packageLine()).append('\n');
        builder.append(t("device")).append(": ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
        builder.append(t("product")).append(": ").append(Build.PRODUCT).append(" / ").append(Build.DEVICE).append('\n');
        builder.append(t("android")).append(": ").append(Build.VERSION.RELEASE)
                .append(" API ").append(Build.VERSION.SDK_INT).append('\n');
        builder.append(t("build")).append(": ").append(Build.DISPLAY).append('\n');
        builder.append(t("abis")).append(": ").append(Arrays.toString(Build.SUPPORTED_ABIS)).append('\n');
        builder.append(t("cpu_cores")).append(": ").append(Runtime.getRuntime().availableProcessors()).append('\n');
        builder.append("Accelerator").append(": ").append('\n')
                .append(AcceleratorConfig.runtimeSummary().replace("\n", "\n  ")).append('\n');
        builder.append(t("java_heap")).append(": ").append(formatBytes(Runtime.getRuntime().totalMemory()))
                .append(" ").append(t("used")).append(" / ").append(formatBytes(Runtime.getRuntime().maxMemory()))
                .append(" ").append(t("max")).append('\n');

        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            activityManager.getMemoryInfo(memoryInfo);
            builder.append("RAM: ").append(formatBytes(memoryInfo.availMem)).append(" ").append(t("free")).append(" / ")
                    .append(formatBytes(memoryInfo.totalMem)).append(" ").append(t("total"));
            if (memoryInfo.lowMemory) {
                builder.append(" (").append(t("low_memory")).append(")");
            }
            builder.append('\n');
        }

        appendStorage(builder, t("internal_data"), getFilesDir());
        appendStorage(builder, t("external_files"), AppStorage.externalFilesDir(this));
        appendStorage(builder, t("shared_storage"), Environment.getExternalStorageDirectory());
        builder.append(t("models_folder_size")).append(": ").append(formatBytes(folderSize(AppStorage.modelsRoot(this))));
        return builder.toString();
    }

    private String packageLine() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            long versionCode = info.getLongVersionCode();
            return getPackageName() + " " + info.versionName + " (" + versionCode + ")";
        } catch (Exception e) {
            return getPackageName();
        }
    }

    private void appendStorage(StringBuilder builder, String label, File path) {
        if (path == null) {
            builder.append(label).append(": ").append(t("unavailable")).append('\n');
            return;
        }
        try {
            StatFs stat = new StatFs(path.getAbsolutePath());
            long available = stat.getAvailableBytes();
            long total = stat.getTotalBytes();
            builder.append(label).append(": ").append(formatBytes(available)).append(" ").append(t("free"))
                    .append(" / ").append(formatBytes(total)).append(" ").append(t("total")).append('\n');
        } catch (Exception e) {
            builder.append(label).append(": ").append(path.getAbsolutePath()).append(" (").append(e.getClass().getSimpleName()).append(")\n");
        }
    }

    private String buildModelInfo() {
        StringBuilder builder = new StringBuilder();
        SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        builder.append(LocalizedText.get(uiLanguageCode, "language_1")).append(": ")
                .append(languageOneName).append(" (").append(languageOneCode).append(")\n");
        builder.append(LocalizedText.get(uiLanguageCode, "language_2")).append(": ")
                .append(languageTwoName).append(" (").append(languageTwoCode).append(")\n\n");

        for (int i = 0; i < MODEL_TABS.length; i++) {
            String tab = MODEL_TABS[i];
            builder.append(tabTitle(tab)).append('\n');
            appendSelectedModel(builder, settings, tab, 1);
            appendSelectedModel(builder, settings, tab, 2);
            builder.append('\n');
        }

        builder.append(t("installed_local_models")).append('\n');
        appendModelTree(builder, LocalizedText.get(uiLanguageCode, "tab_stt"), AppStorage.modelDir(this, "stt"), 0);
        appendModelTree(builder, LocalizedText.get(uiLanguageCode, "tab_tts"), AppStorage.modelDir(this, "tts"), 0);
        appendModelTree(builder, LocalizedText.get(uiLanguageCode, "assistant"), AppStorage.modelDir(this, "assistant"), 0);
        builder.append('\n').append(t("raw_settings_preferences")).append('\n');
        for (Map.Entry<String, ?> entry : settings.getAll().entrySet()) {
            builder.append(entry.getKey()).append(" = ").append(displayPreferenceValue(entry)).append('\n');
        }
        return builder.toString().trim();
    }

    private String buildUsageGuide() {
        if ("vi".equals(uiLanguageCode)) {
            return "1. M\u1edf Models \u0111\u1ec3 t\u1ea3i model c\u1ea7n d\u00f9ng: STT, D\u1ecbch, TTS v\u00e0 Tr\u1ee3 l\u00fd.\n"
                    + "2. M\u1edf Settings \u0111\u1ec3 ch\u1ecdn runtime local ho\u1eb7c provider online cho t\u1eebng t\u00e1c v\u1ee5.\n"
                    + "3. D\u00f9ng Meeting \u0111\u1ec3 nghe/nh\u1eadp transcript, d\u1ecbch v\u00e0 ph\u00e1t \u00e2m.\n"
                    + "4. D\u00f9ng Tr\u1ee3 l\u00fd \u0111\u1ec3 h\u1ecfi \u0111\u00e1p theo ng\u1eef c\u1ea3nh \u0111\u00e3 l\u01b0u trong m\u00e1y.\n"
                    + "5. N\u1ebfu Tr\u1ee3 l\u00fd kh\u00f4ng tr\u1ea3 l\u1eddi, h\u00e3y th\u1eed Qwen/TinyLlama, h\u1ecfi ng\u1eafn h\u01a1n, X\u00f3a nh\u1edb, r\u1ed3i xem Logs b\u00ean d\u01b0\u1edbi.";
        }
        return "1. Open Models to download required STT, translation, TTS, and Assistant models.\n"
                + "2. Open Settings to choose a local runtime or online provider for each task.\n"
                + "3. Use Meeting to listen or enter transcripts, translate, and play speech.\n"
                + "4. Use Assistant to ask questions with the conversation context stored on this device.\n"
                + "5. If Assistant does not answer, try Qwen/TinyLlama, ask a shorter question, clear memory, then review Logs below.";
    }

    private String displayPreferenceValue(Map.Entry<String, ?> entry) {
        String key = entry.getKey() == null ? "" : entry.getKey().toLowerCase(Locale.US);
        if (key.contains("api_key") || key.endsWith("_encrypted")) {
            return "••••";
        }
        return String.valueOf(entry.getValue());
    }

    private void appendSelectedModel(StringBuilder builder, SharedPreferences settings, String tab, int languageIndex) {
        String key = SettingsActivity.preferenceKey(tab, languageIndex);
        String label = OnlineProviderConfig.safeString(settings, key + "_label", "");
        String value = OnlineProviderConfig.safeString(settings, key, "");
        if (label.isEmpty() && value.isEmpty()) {
            builder.append("  ").append(LocalizedText.get(uiLanguageCode, languageIndex == 1 ? "language_1" : "language_2"))
                    .append(": ").append(t("not_configured")).append('\n');
            return;
        }
        builder.append("  ").append(LocalizedText.get(uiLanguageCode, languageIndex == 1 ? "language_1" : "language_2"))
                .append(": ");
        builder.append(label.isEmpty() ? value : label);
        if (!value.isEmpty()) {
            builder.append("\n    id: ").append(value);
        }
        if (value.startsWith(SettingsActivity.LOCAL_PREFIX)) {
            String path = value.substring(SettingsActivity.LOCAL_PREFIX.length()).trim();
            if (path.isEmpty()) {
                builder.append("\n    ").append(t("exists")).append(": false");
            } else {
                File file = new File(path);
                builder.append("\n    ").append(t("exists")).append(": ").append(file.exists());
                builder.append("\n    ").append(t("size")).append(": ").append(formatBytes(folderSize(file)));
            }
        }
        builder.append('\n');
    }

    private void appendModelTree(StringBuilder builder, String label, File parent, int depth) {
        builder.append(label).append(": ");
        if (parent == null || !parent.exists()) {
            builder.append(t("none")).append('\n');
            return;
        }
        builder.append(parent.getAbsolutePath()).append(" (").append(formatBytes(folderSize(parent))).append(")\n");
        File[] children = parent.listFiles();
        if (children == null || children.length == 0) {
            builder.append("  ").append(t("empty")).append('\n');
            return;
        }
        Arrays.sort(children, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        int count = 0;
        for (File child : children) {
            if (count >= 40) {
                builder.append("  ... ").append(children.length - count).append(" ").append(t("more")).append('\n');
                break;
            }
            builder.append("  ").append(child.getName());
            builder.append(child.isDirectory() ? " [" + t("dir") + "] " : " [" + t("file") + "] ");
            builder.append(formatBytes(folderSize(child))).append('\n');
            count++;
        }
    }

    private String readLogs() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-t", "240", "AIMeeting:D", "AndroidRuntime:E", "*:S"});
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
            }
            process.waitFor(3, TimeUnit.SECONDS);
            String output = builder.toString().trim();
            if (output.isEmpty()) {
                return t("no_logs");
            }
            return output;
        } catch (Exception e) {
            return t("logcat_failed") + ": " + e.getClass().getSimpleName() + " - " + safeMessage(e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private void addCard(String title, String body, boolean mono) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.elevateCard(this, card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        contentLayout.addView(card, params);

        TextView titleText = new TextView(this);
        titleText.setText(title);
        titleText.setTextColor(getColor(R.color.meeting_text));
        titleText.setTextSize(16);
        titleText.setTypeface(null, Typeface.BOLD);
        card.addView(titleText);

        TextView bodyText = new TextView(this);
        bodyText.setText(body);
        bodyText.setTextColor(getColor(R.color.meeting_muted));
        bodyText.setTextSize(mono ? 12 : 13);
        bodyText.setPadding(0, dp(8), 0, 0);
        bodyText.setTextIsSelectable(true);
        if (mono) {
            bodyText.setTypeface(Typeface.MONOSPACE);
        }
        card.addView(bodyText);
    }

    private String tabTitle(String tab) {
        if ("ocr".equals(tab)) {
            return "vi".equals(uiLanguageCode) ? "Nh\u1eadn d\u1ea1ng ch\u1eef vi\u1ebft" : "Text recognition";
        }
        if ("translation".equals(tab)) {
            return LocalizedText.get(uiLanguageCode, "tab_translation");
        }
        if ("tts".equals(tab)) {
            return LocalizedText.get(uiLanguageCode, "tab_tts");
        }
        if ("assistant".equals(tab)) {
            return LocalizedText.get(uiLanguageCode, "assistant");
        }
        return LocalizedText.get(uiLanguageCode, "tab_stt");
    }

    private String t(String key) {
        if ("refresh".equals(key)) {
            return LocalizedText.get(uiLanguageCode, "refresh");
        }
        boolean vi = "vi".equals(uiLanguageCode);
        switch (key) {
            case "runtime":
                return vi ? "Runtime" : "Runtime";
            case "runtime_reading":
                return vi ? "Đang đọc thông tin runtime..." : "Reading runtime information...";
            case "runtime_updating":
                return vi ? "Đang cập nhật runtime snapshot..." : "Updating runtime snapshot...";
            case "runtime_tab_system":
                return vi ? "H\u1ec7 th\u1ed1ng" : "System";
            case "runtime_tab_models":
                return vi ? "Models" : "Models";
            case "runtime_tab_guide":
                return vi ? "H\u01b0\u1edbng d\u1eabn" : "Guide";
            case "system_info":
                return vi ? "Thông tin hệ thống" : "System information";
            case "models_in_use":
                return vi ? "Models đang được sử dụng" : "Models in use";
            case "usage_guide":
                return vi ? "H\u01b0\u1edbng d\u1eabn s\u1eed d\u1ee5ng" : "Usage guide";
            case "logs":
                return vi ? "Logs" : "Logs";
            case "logs_reading":
                return vi ? "Đang đọc logs..." : "Reading logs...";
            case "updated_at":
                return vi ? "Cập nhật lúc" : "Updated at";
            case "app":
                return vi ? "Ứng dụng" : "App";
            case "device":
                return vi ? "Thiết bị" : "Device";
            case "product":
                return vi ? "Sản phẩm" : "Product";
            case "android":
                return "Android";
            case "build":
                return "Build";
            case "abis":
                return "ABIs";
            case "cpu_cores":
                return vi ? "Nhân CPU" : "CPU cores";
            case "java_heap":
                return vi ? "Java heap" : "Java heap";
            case "used":
                return vi ? "đã dùng" : "used";
            case "max":
                return vi ? "tối đa" : "max";
            case "free":
                return vi ? "trống" : "free";
            case "total":
                return vi ? "tổng" : "total";
            case "low_memory":
                return vi ? "thiếu bộ nhớ" : "low memory";
            case "internal_data":
                return vi ? "Dữ liệu nội bộ" : "Internal data";
            case "external_files":
                return vi ? "Tệp ngoài của app" : "External app files";
            case "shared_storage":
                return vi ? "Bộ nhớ dùng chung" : "Shared storage";
            case "models_folder_size":
                return vi ? "Dung lượng thư mục models" : "Models folder size";
            case "unavailable":
                return vi ? "không khả dụng" : "unavailable";
            case "installed_local_models":
                return vi ? "Models local đã cài" : "Installed local models";
            case "raw_settings_preferences":
                return vi ? "Tuỳ chọn Settings thô" : "Raw Settings preferences";
            case "not_configured":
                return vi ? "chưa cấu hình" : "not configured";
            case "exists":
                return vi ? "tồn tại" : "exists";
            case "size":
                return vi ? "dung lượng" : "size";
            case "none":
                return vi ? "không có" : "none";
            case "empty":
                return vi ? "trống" : "empty";
            case "more":
                return vi ? "mục nữa" : "more";
            case "dir":
                return vi ? "thư mục" : "dir";
            case "file":
                return vi ? "tệp" : "file";
            case "no_logs":
                return vi ? "Chưa có log phù hợp với tag AIMeeting/AndroidRuntime." : "No logs found for AIMeeting/AndroidRuntime.";
            case "logcat_failed":
                return vi ? "Không đọc được logcat" : "Could not read logcat";
            default:
                return key;
        }
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        PremiumUi.primaryButton(button);
        PremiumUi.compactTextButton(this, button, text);
        return button;
    }

    private Button runtimeTabButton(String text, String tab) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(12);
        button.setOnClickListener(v -> selectRuntimeTab(tab));
        PremiumUi.compactTextButton(this, button, text);
        return button;
    }

    private void styleRuntimeTab(Button button, String tab) {
        if (button == null) {
            return;
        }
        boolean active = tab.equals(activeRuntimeTab);
        if (active) {
            PremiumUi.primaryButton(button);
        } else {
            PremiumUi.secondaryButton(this, button);
        }
        button.setEnabled(!active);
        button.setAlpha(active ? 1.0f : 0.92f);
    }

    private LinearLayout.LayoutParams weightedParams(int weight, int marginStart) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(36), weight);
        params.setMargins(marginStart, 0, 0, 0);
        return params;
    }

    private String buttonLabel(String key) {
        return LocalizedText.button(uiLanguageCode, key);
    }

    private long folderSize(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        if (file.isFile()) {
            return file.length();
        }
        long total = 0L;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                total += folderSize(child);
            }
        }
        return total;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024f);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(Locale.US, "%.1f MB", bytes / 1024f / 1024f);
        }
        return String.format(Locale.US, "%.2f GB", bytes / 1024f / 1024f / 1024f);
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.trim().isEmpty() ? e.getClass().getSimpleName() : message;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
