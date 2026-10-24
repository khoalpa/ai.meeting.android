package com.example.aimeeting;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressLint("SetTextI18n")
public class ModelManagerActivity extends Activity {
    private static final int REQUEST_IMPORT_MODEL_FILE = 71;
    private static final String VOSK_BASE_URL = "https://alphacephei.com/vosk/models/";
    private static final String TTS_MODEL_NAME = TtsModelCatalog.SUPERTONIC_MODEL_NAME;
    private static final String TTS_MODEL_URL = TtsModelCatalog.SUPERTONIC_MODEL_URL;
    private static final String ASSISTANT_MODEL_NAME = "qwen2.5-1.5b-instruct-q4_k_m.gguf";
    private static final String ASSISTANT_MODEL_URL = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/"
            + ASSISTANT_MODEL_NAME;
    private static final String FALLBACK_ASSISTANT_MODEL_NAME = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf";
    private static final String FALLBACK_ASSISTANT_MODEL_URL = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/"
            + FALLBACK_ASSISTANT_MODEL_NAME;
    private static final int DOWNLOAD_CONNECT_TIMEOUT_MS = 60000;
    private static final int DOWNLOAD_READ_TIMEOUT_MS = 300000;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<ModelItem> items = new ArrayList<>();
    private ModelDownloadManager modelDownloadManager;

    private LinearLayout listLayout;
    private ScrollView modelScrollView;
    private LinearLayout rootLayout;
    private LinearLayout headerLayout;
    private TextView statusText;
    private TextView storageText;
    private TextView dashboardTitleText;
    private TextView dashboardSummaryText;
    private TextView dashboardNextStepText;
    private Button sttTab;
    private Button translationTab;
    private Button ttsTab;
    private Button assistantTab;
    private Button importFileButton;
    private Button importUrlButton;
    private Button rescanLocalButton;
    private SharedPreferences preferences;
    private String activeTab = "stt";
    private String uiLanguageCode = "vi";
    private String uiLanguageName = "Vietnamese";
    private String pendingImportTab = "stt";

    private interface ProgressReporter {
        void report(String phase, long doneBytes, long totalBytes);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String requestedLanguage = getIntent().getStringExtra("ui_language_code");
        if (requestedLanguage != null && !requestedLanguage.trim().isEmpty()) {
            uiLanguageCode = requestedLanguage;
        }
        String requestedLanguageName = getIntent().getStringExtra("ui_language_name");
        if (requestedLanguageName != null && !requestedLanguageName.trim().isEmpty()) {
            uiLanguageName = requestedLanguageName;
        }
        preferences = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        modelDownloadManager = new ModelDownloadManager(this, DOWNLOAD_CONNECT_TIMEOUT_MS, DOWNLOAD_READ_TIMEOUT_MS);
        reloadModelCatalog();
        buildUi();
        refreshModels();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        rootLayout = root;
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.app_background);

        LinearLayout header = new LinearLayout(this);
        headerLayout = header;
        header.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.applyShell(this, root, header);
        header.setPadding(dp(20), dp(16), dp(20), dp(12));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.addView(titleRow, new LinearLayout.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText(t("manage_models"));
        title.setTextColor(getColor(R.color.meeting_text));
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button refresh = actionButton("");
        PremiumUi.secondaryIconButton(this, refresh, R.drawable.ic_action_refresh, t("refresh"));
        refresh.setOnClickListener(v -> refreshModels());
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(dp(56), dp(36));
        refreshParams.setMargins(dp(8), 0, 0, 0);
        titleRow.addView(refresh, refreshParams);

        statusText = new TextView(this);
        statusText.setText(t("manage_models"));
        statusText.setTextColor(getColor(R.color.meeting_muted));
        statusText.setTextSize(14);
        statusText.setMaxLines(2);
        header.addView(statusText);

        storageText = new TextView(this);
        storageText.setTextColor(getColor(R.color.meeting_primary_dark));
        storageText.setTextSize(14);
        storageText.setPadding(0, dp(8), 0, dp(8));
        header.addView(storageText);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        header.addView(tabs);

        sttTab = tabButton(buttonLabel("tab_stt"), "stt");
        translationTab = tabButton(buttonLabel("tab_translation"), "translation");
        ttsTab = tabButton(buttonLabel("tab_tts"), "tts");
        tabs.addView(sttTab, weightedParams(1, 0));
        tabs.addView(translationTab, weightedParams(1, dp(8)));
        tabs.addView(ttsTab, weightedParams(1, dp(8)));
        assistantTab = tabButton(buttonLabel("assistant"), "assistant");
        tabs.addView(assistantTab, weightedParams(1, dp(8)));

        LinearLayout customActions = new LinearLayout(this);
        customActions.setOrientation(LinearLayout.HORIZONTAL);
        customActions.setPadding(0, dp(8), 0, 0);
        header.addView(customActions);

        Button downloadBest = actionButton(buttonLabel("download_best"));
        PremiumUi.primaryIconTextButton(this, downloadBest, R.drawable.ic_action_download,
                buttonLabel("download_best"), t("download_best"));
        downloadBest.setOnClickListener(v -> downloadBestModelsForActiveTab());
        customActions.addView(downloadBest, weightedParams(1, 0));

        importFileButton = actionButton(importFileLabel());
        PremiumUi.secondaryIconTextButton(this, importFileButton, R.drawable.ic_action_folder,
                importFileLabel(), importFileDescription());
        importFileButton.setOnClickListener(v -> openImportFilePicker());
        customActions.addView(importFileButton, weightedParams(1, dp(8)));

        importUrlButton = actionButton(importUrlLabel());
        PremiumUi.secondaryIconTextButton(this, importUrlButton, R.drawable.ic_action_download,
                importUrlLabel(), importUrlDescription());
        importUrlButton.setOnClickListener(v -> showImportUrlDialog());
        customActions.addView(importUrlButton, weightedParams(1, dp(8)));

        rescanLocalButton = actionButton(rescanLabel());
        PremiumUi.secondaryIconTextButton(this, rescanLocalButton, R.drawable.ic_action_refresh,
                rescanLabel(), rescanDescription());
        rescanLocalButton.setOnClickListener(v -> {
            reloadModelCatalog();
            refreshModels();
            statusText.setText(isVietnameseUi() ? "Đã quét lại model local." : "Local models rescanned.");
        });
        customActions.addView(rescanLocalButton, weightedParams(1, dp(8)));

        ScrollView scrollView = new ScrollView(this);
        modelScrollView = scrollView;
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        listLayout.setPadding(dp(16), dp(12), dp(16), dp(20));
        scrollView.addView(listLayout);
        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        PremiumUi.addBottomNavigation(this, root, "models", uiLanguageCode,
                uiLanguageName, uiLanguageCode, "English", "en");
        setContentView(root);
        applySystemBarInsets();
        updateTabs();
    }

    private void applySystemBarInsets() {
        int baseHeaderTopPadding = headerLayout.getPaddingTop();
        int baseScrollBottomPadding = modelScrollView.getPaddingBottom();
        rootLayout.setOnApplyWindowInsetsListener((view, insets) -> {
            headerLayout.setPadding(
                    headerLayout.getPaddingLeft(),
                    baseHeaderTopPadding + InsetsCompat.systemBarTop(insets),
                    headerLayout.getPaddingRight(),
                    headerLayout.getPaddingBottom()
            );
            modelScrollView.setPadding(
                    modelScrollView.getPaddingLeft(),
                    modelScrollView.getPaddingTop(),
                    modelScrollView.getPaddingRight(),
                    baseScrollBottomPadding + InsetsCompat.systemBarBottom(insets)
            );
            return insets;
        });
        rootLayout.requestApplyInsets();
    }

    private void seedModels() {
        File sttParent = AppStorage.modelDir(this, "stt");
        File ttsParent = AppStorage.modelDir(this, "tts");

        addStt("STT tốt nhất", "Tiếng Việt", "vosk-model-vn-0.4", sttParent, true);
        addStt("STT tốt nhất", "English", "vosk-model-en-us-0.22", sttParent, true);
        addStt("STT tốt nhất", "日本語", "vosk-model-ja-0.22", sttParent, true);
        addStt("STT tốt nhất", "中文", "vosk-model-cn-0.22", sttParent, true);
        addStt("STT tốt nhất", "Français", "vosk-model-fr-0.22", sttParent, true);
        addStt("STT tốt nhất", "Deutsch", "vosk-model-de-0.21", sttParent, true);
        addStt("STT tốt nhất", "Español", "vosk-model-es-0.42", sttParent, true);
        addStt("STT tốt nhất", "한국어", "vosk-model-small-ko-0.22", sttParent, true);

        addStt("STT dự phòng", "Tiếng Việt small", "vosk-model-small-vn-0.4", sttParent, false);
        addStt("STT dự phòng", "English small", "vosk-model-small-en-us-0.15", sttParent, false);
        addStt("STT dự phòng", "日本語 small", "vosk-model-small-ja-0.22", sttParent, false);
        addStt("STT dự phòng", "中文 small", "vosk-model-small-cn-0.22", sttParent, false);
        addStt("STT dự phòng", "Français small", "vosk-model-small-fr-0.22", sttParent, false);
        addStt("STT dự phòng", "Deutsch small", "vosk-model-small-de-0.15", sttParent, false);
        addStt("STT dự phòng", "Español small", "vosk-model-small-es-0.42", sttParent, false);

        addTranslation("Tiếng Việt", TranslateLanguage.VIETNAMESE);
        addBundledOcr("OCR ML Kit", "Latin OCR (vi/en/fr/de/es/th)");
        addBundledOcr("OCR ML Kit", "Chinese OCR");
        addBundledOcr("OCR ML Kit", "Japanese OCR");
        addBundledOcr("OCR ML Kit", "Korean OCR");

        addTranslation("English", TranslateLanguage.ENGLISH);
        addTranslation("日本語", TranslateLanguage.JAPANESE);
        addTranslation("한국어", TranslateLanguage.KOREAN);
        addTranslation("中文", TranslateLanguage.CHINESE);
        addTranslation("Français", TranslateLanguage.FRENCH);
        addTranslation("Deutsch", TranslateLanguage.GERMAN);
        addTranslation("Español", TranslateLanguage.SPANISH);
        addTranslation("ไทย", TranslateLanguage.THAI);

        addAssistantModels(AppStorage.modelDir(this, "assistant"));
        addTtsVoices(ttsParent);
    }

    private void reloadModelCatalog() {
        items.clear();
        seedModels();
        addCustomLocalModels();
    }

    private void addCustomLocalModels() {
        File sttParent = AppStorage.modelDir(this, "stt");
        File[] sttChildren = sttParent.listFiles(File::isDirectory);
        if (sttChildren != null) {
            for (File child : sttChildren) {
                if (looksLikeVoskModel(child) && !hasLocalModel(child)) {
                    items.add(new ModelItem("stt", customGroup(), child.getName(),
                            child.getName(), child, null, null, false));
                }
            }
        }

        File ttsParent = AppStorage.modelDir(this, "tts");
        File[] ttsChildren = ttsParent.listFiles(File::isDirectory);
        if (ttsChildren != null) {
            for (File child : ttsChildren) {
                if (looksLikeTtsModel(child) && !hasLocalModel(child)) {
                    items.add(new ModelItem("tts", customGroup(), TtsModelCatalog.displayName(child),
                            child.getName(), child, null, null, false));
                }
            }
        }

        File assistantParent = AppStorage.modelDir(this, "assistant");
        File[] assistantFiles = assistantParent.listFiles(file -> file.isFile()
                && file.getName().toLowerCase(Locale.US).endsWith(".gguf"));
        if (assistantFiles != null) {
            for (File file : assistantFiles) {
                if (!hasLocalModel(file)) {
                    items.add(new ModelItem("assistant", customGroup(), file.getName(),
                            file.getName(), file, null, null, false));
                }
            }
        }
    }

    private boolean hasLocalModel(File file) {
        if (file == null) {
            return false;
        }
        try {
            String target = file.getCanonicalPath();
            for (ModelItem item : items) {
                if (item.localDir != null && target.equals(item.localDir.getCanonicalPath())) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private void addAssistantModels(File parent) {
        items.add(new ModelItem("assistant", "LLM nội bộ", "Qwen2.5 1.5B Instruct Q4_K_M",
                ASSISTANT_MODEL_NAME, new File(parent, ASSISTANT_MODEL_NAME), ASSISTANT_MODEL_URL, null, false));
        items.add(new ModelItem("assistant", "LLM nội bộ", "TinyLlama 1.1B Chat Q4_K_M",
                FALLBACK_ASSISTANT_MODEL_NAME, new File(parent, FALLBACK_ASSISTANT_MODEL_NAME), FALLBACK_ASSISTANT_MODEL_URL, null, true));
    }

    private void addStt(String group, String label, String modelName, File parent, boolean best) {
        items.add(new ModelItem("stt", group, label, modelName, new File(parent, modelName), VOSK_BASE_URL + modelName + ".zip", null, best));
    }

    private void addTranslation(String label, String languageCode) {
        items.add(new ModelItem("translation", "Dịch offline ML Kit", label, "translate-" + languageCode, null, null, languageCode, true));
    }

    private void addTtsVoices(File parent) {
        File viDir = new File(parent, TtsModelCatalog.VIETNAMESE_VITS_MODEL_NAME);
        items.add(new ModelItem("tts", "TTS nội bộ", "Tiếng Việt - VITS Mimic3 VAIS1000 offline",
                TtsModelCatalog.VIETNAMESE_VITS_MODEL_NAME, viDir, TtsModelCatalog.VIETNAMESE_VITS_MODEL_URL, null, false));
        addTtsVoice(parent, "Tiếng Việt - Piper VIVOS X low offline",
                TtsModelCatalog.VIETNAMESE_PIPER_VIVOS_MODEL_NAME, TtsModelCatalog.VIETNAMESE_PIPER_VIVOS_MODEL_URL, false);
        addTtsVoice(parent, "Tiếng Việt - Piper VAIS1000 medium offline (khuyến nghị)",
                TtsModelCatalog.VIETNAMESE_PIPER_VAIS1000_MODEL_NAME, TtsModelCatalog.VIETNAMESE_PIPER_VAIS1000_MODEL_URL, true);
        addTtsVoice(parent, "Tiếng Việt - Piper 25 hours single low offline",
                TtsModelCatalog.VIETNAMESE_PIPER_25H_MODEL_NAME, TtsModelCatalog.VIETNAMESE_PIPER_25H_MODEL_URL, false);

        File supertonicDir = new File(parent, TTS_MODEL_NAME);
        items.add(new ModelItem("tts", "TTS nội bộ", "Supertonic 3 multilingual",
                TTS_MODEL_NAME, supertonicDir, TTS_MODEL_URL, null, false));
    }

    private void addTtsVoice(File parent, String label, String modelName, String url, boolean best) {
        items.add(new ModelItem("tts", "TTS nội bộ", label, modelName, new File(parent, modelName), url, null, best));
    }

    private void addBundledOcr(String group, String label) {
        items.add(new ModelItem("stt", group, label, "mlkit-text-recognition-bundled", null, null, null, true));
    }

    private void refreshModels() {
        reloadModelCatalog();
        updateTabs();
        listLayout.removeAllViews();
        listLayout.addView(buildDashboardCard());
        addProviderCredentialCards(activeTab);
        addCustomModelCard(activeTab);
        int total = 0;
        for (ModelItem item : items) {
            if (!item.type.equals(activeTab)) {
                continue;
            }
            total++;
            View row = buildRow(item);
            listLayout.addView(row);
            executor.execute(() -> {
                boolean installed = item.isInstalledSync();
                item.installed = installed;
                mainHandler.post(() -> updateRow(row, item));
            });
        }

        storageText.setText(t("model_storage") + ": " + formatBytes(folderSize(AppStorage.modelsRoot(this))));
        statusText.setText(tabTitle(activeTab) + ": " + total + " " + t("model_count_suffix"));
        refreshDashboardAsync();
    }

    private View buildDashboardCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.elevateCard(this, card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);

        dashboardTitleText = new TextView(this);
        dashboardTitleText.setText(dashboardTitle());
        dashboardTitleText.setTextColor(getColor(R.color.meeting_text));
        dashboardTitleText.setTextSize(17);
        dashboardTitleText.setTypeface(null, Typeface.BOLD);
        card.addView(dashboardTitleText);

        dashboardSummaryText = new TextView(this);
        dashboardSummaryText.setText(t("checking_models"));
        dashboardSummaryText.setTextColor(getColor(R.color.meeting_primary_dark));
        dashboardSummaryText.setTextSize(15);
        dashboardSummaryText.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.setMargins(0, dp(8), 0, 0);
        card.addView(dashboardSummaryText, summaryParams);

        dashboardNextStepText = new TextView(this);
        dashboardNextStepText.setText("");
        dashboardNextStepText.setTextColor(getColor(R.color.meeting_muted));
        dashboardNextStepText.setTextSize(13);
        LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(-1, -2);
        nextParams.setMargins(0, dp(4), 0, 0);
        card.addView(dashboardNextStepText, nextParams);

        return card;
    }

    private void refreshDashboardAsync() {
        String tabAtStart = activeTab;
        executor.execute(() -> {
            int total = 0;
            int installed = 0;
            int best = 0;
            int missingBest = 0;
            for (ModelItem item : items) {
                if (!item.type.equals(tabAtStart)) {
                    continue;
                }
                total++;
                boolean ready = item.isInstalledSync();
                item.installed = ready;
                if (ready) {
                    installed++;
                }
                if (item.best) {
                    best++;
                    if (!ready) {
                        missingBest++;
                    }
                }
            }
            int finalTotal = total;
            int finalInstalled = installed;
            int finalBest = best;
            int finalMissingBest = missingBest;
            mainHandler.post(() -> {
                if (!tabAtStart.equals(activeTab) || dashboardSummaryText == null) {
                    return;
                }
                dashboardTitleText.setText(dashboardTitle());
                dashboardSummaryText.setText(finalInstalled + "/" + finalTotal + " " + dashboardInstalledSuffix());
                dashboardSummaryText.setTextColor(getColor(finalMissingBest == 0 ? R.color.meeting_primary_dark : android.R.color.holo_orange_dark));
                dashboardNextStepText.setText(dashboardNextStep(finalBest, finalMissingBest));
            });
        });
    }

    private void addProviderCredentialCards(String tab) {
        TextView title = new TextView(this);
        title.setText("Online provider API");
        title.setTextColor(getColor(R.color.meeting_text));
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(8), 0, dp(8));
        listLayout.addView(title);

        if ("stt".equals(tab)) {
            addCredentialCard("OpenAI", "openai", true, true, true, true, true);
            addCredentialCard("Groq", "groq", true, true, true, false, true);
            addCredentialCard("Hugging Face", "huggingface", true, true, true, true, true);
            addCredentialCard("Google Cloud / Gemini", "google", true, true, false, false, true);
            return;
        }
        if ("translation".equals(tab)) {
            addCredentialCard("LibreTranslate", "libretranslate", false, true, false, false, false);
            addCredentialCard("Google Cloud / Gemini", "google", true, true, false, false, false);
            return;
        }
        if ("tts".equals(tab)) {
            addCredentialCard("OpenAI", "openai", true, true, false, true, false);
            addCredentialCard("Hugging Face", "huggingface", true, true, false, true, false);
            return;
        }
        if ("assistant".equals(tab)) {
            addCredentialCard("OpenAI", "openai", true, true, false, false, true);
            addCredentialCard("Gemini", "google", true, true, false, false, true);
            addCredentialCard("OpenRouter", "openrouter", true, true, false, false, true);
            addCredentialCard("Groq", "groq", true, true, false, false, true);
            addCredentialCard("Hugging Face", "huggingface", true, true, false, false, true);
        }
    }

    private void addCustomModelCard(String tab) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.elevateCard(this, card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        listLayout.addView(card, params);

        TextView title = new TextView(this);
        title.setText(customModelTitle(tab));
        title.setTextColor(getColor(R.color.meeting_text));
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        TextView body = new TextView(this);
        body.setText(customModelHint(tab));
        body.setTextColor(getColor(R.color.meeting_muted));
        body.setTextSize(13);
        body.setPadding(0, dp(6), 0, dp(8));
        card.addView(body);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button importFile = actionButton(importFileLabel());
        PremiumUi.secondaryIconTextButton(this, importFile, R.drawable.ic_action_folder,
                importFileLabel(), importFileDescription());
        importFile.setOnClickListener(v -> openImportFilePicker());
        actions.addView(importFile, weightedParams(1, 0));

        Button importUrl = actionButton(importUrlLabel());
        PremiumUi.secondaryIconTextButton(this, importUrl, R.drawable.ic_action_download,
                importUrlLabel(), importUrlDescription());
        importUrl.setOnClickListener(v -> showImportUrlDialog());
        actions.addView(importUrl, weightedParams(1, dp(8)));
        card.addView(actions);
    }

    private void addCredentialCard(String titleText, String providerId, boolean apiKey, boolean baseUrl,
                                   boolean sttModel, boolean ttsModel, boolean chatModel) {
        OnlineProviderConfig.ProviderDefaults defaults = OnlineProviderConfig.defaultsFor(providerId);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.elevateCard(this, card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        listLayout.addView(card, params);

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(getColor(R.color.meeting_text));
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        if (apiKey) {
            addCredentialInput(card, providerId, "api_key", "API key", "", true);
        }
        if (baseUrl) {
            addCredentialInput(card, providerId, "base_url", "Endpoint/base URL", defaults.baseUrl, false);
        }
        if (sttModel) {
            addCredentialInput(card, providerId, "stt_model", "STT model", defaults.sttModel, false);
        }
        if (ttsModel) {
            addCredentialInput(card, providerId, "tts_model", "TTS model", defaults.ttsModel, false);
            addCredentialInput(card, providerId, "tts_voice", "TTS voice", defaults.ttsVoice, false);
        }
        if (chatModel) {
            addCredentialInput(card, providerId, "chat_model", "LLM/OCR model", defaults.chatModel, false);
        }
    }

    private void addCredentialInput(LinearLayout card, String providerId, String field, String label,
                                    String fallback, boolean secret) {
        TextView text = new TextView(this);
        text.setText(label);
        text.setTextColor(getColor(R.color.meeting_muted));
        text.setTextSize(13);
        text.setPadding(0, dp(8), 0, 0);
        card.addView(text);

        EditText input = new EditText(this);
        String key = OnlineProviderConfig.key(providerId, field);
        input.setSingleLine(true);
        input.setHint(fallback == null || fallback.isEmpty() ? label : fallback);
        input.setText(secret ? SecretStore.get(this, preferences, key)
                : OnlineProviderConfig.safeString(preferences, key, ""));
        input.setTextColor(getColor(R.color.meeting_text));
        input.setHintTextColor(getColor(R.color.meeting_muted));
        input.setInputType(secret
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (secret) {
                    SecretStore.put(preferences, key, s.toString());
                } else {
                    preferences.edit().putString(key, s.toString().trim()).apply();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        card.addView(input, new LinearLayout.LayoutParams(-1, dp(48)));
    }

    private View buildRow(ModelItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.elevateCard(this, card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setTag("title");
        title.setTextColor(getColor(R.color.meeting_text));
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        TextView status = new TextView(this);
        status.setTag("status");
        status.setTextColor(getColor(R.color.meeting_primary_dark));
        status.setTextSize(12);
        status.setTypeface(null, Typeface.BOLD);
        status.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-2, -2);
        statusParams.setMargins(0, dp(8), 0, 0);
        card.addView(status, statusParams);

        TextView detail = new TextView(this);
        detail.setTag("detail");
        detail.setTextColor(getColor(R.color.meeting_muted));
        detail.setTextSize(13);
        detail.setPadding(0, dp(8), 0, dp(8));
        card.addView(detail);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button download = actionButton(buttonLabel("download"));
        download.setTag("download");
        PremiumUi.primaryIconTextButton(this, download, R.drawable.ic_action_download,
                buttonLabel("download"), t("download"));
        download.setOnClickListener(v -> downloadModel(item));
        actions.addView(download, weightedParams(1, 0));
        Button delete = actionButton(buttonLabel("delete"));
        delete.setTag("delete");
        PremiumUi.dangerIconTextButton(this, delete, R.drawable.ic_action_trash,
                buttonLabel("delete"), t("delete"));
        delete.setOnClickListener(v -> deleteModel(item));
        actions.addView(delete, weightedParams(1, dp(8)));
        card.addView(actions);

        updateRow(card, item);
        return card;
    }

    private void updateRow(View row, ModelItem item) {
        TextView title = row.findViewWithTag("title");
        TextView status = row.findViewWithTag("status");
        TextView detail = row.findViewWithTag("detail");
        Button download = row.findViewWithTag("download");
        Button delete = row.findViewWithTag("delete");
        String badge = item.best ? " · " + t("best_default") : "";
        title.setText(displayGroup(item.group, item.type) + " · " + item.label + badge);
        status.setText(item.installed ? t("installed") : t("not_installed"));
        status.setTextColor(getColor(item.installed ? R.color.meeting_primary_dark : android.R.color.holo_orange_dark));
        status.setBackgroundResource(item.installed ? R.drawable.status_chip_neutral : R.drawable.status_chip_warning);
        detail.setText(modelTypeHint(item) + modelSizeLabel(item) + "\n" + item.modelName);
        download.setEnabled(!item.installed && (item.url != null || item.translateLanguageCode != null));
        delete.setEnabled(item.installed && item.localDir != null);
        download.setAlpha(download.isEnabled() ? 1.0f : 0.45f);
        delete.setAlpha(delete.isEnabled() ? 1.0f : 0.45f);
    }

    private void downloadBestModelsForActiveTab() {
        statusText.setText(String.format(t("downloading_best_in_tab"), tabTitle(activeTab)));
        executor.execute(() -> {
            for (ModelItem item : items) {
                if (item.type.equals(activeTab) && item.best && !item.isInstalledSync()) {
                    try {
                        item.downloadSync((phase, done, totalBytes) -> postDownloadProgress(item, phase, done, totalBytes));
                    } catch (Exception e) {
                        mainHandler.post(() -> statusText.setText(String.format(t("download_failed_named"), item.label) + ": " + safeMessage(e)));
                        return;
                    }
                }
            }
            mainHandler.post(() -> {
                statusText.setText(String.format(t("downloaded_best_in_tab"), tabTitle(activeTab)));
                refreshModels();
            });
        });
    }

    private void downloadModel(ModelItem item) {
        statusText.setText(String.format(t("downloading_named"), item.label));
        executor.execute(() -> {
            try {
                item.downloadSync((phase, done, totalBytes) -> postDownloadProgress(item, phase, done, totalBytes));
                mainHandler.post(() -> {
                    statusText.setText(String.format(t("downloaded_named"), item.label));
                    refreshModels();
                });
            } catch (Exception e) {
                mainHandler.post(() -> statusText.setText(String.format(t("download_failed_named"), item.label) + ": " + safeMessage(e)));
            }
        });
    }

    private void deleteModel(ModelItem item) {
        statusText.setText(String.format(t("deleting_named"), item.label));
        executor.execute(() -> {
            try {
                item.deleteSync();
                mainHandler.post(() -> {
                    statusText.setText(String.format(t("deleted_named"), item.label));
                    refreshModels();
                });
            } catch (Exception e) {
                mainHandler.post(() -> statusText.setText(String.format(t("delete_failed_named"), item.label) + ": " + safeMessage(e)));
            }
        });
    }

    private void openImportFilePicker() {
        if ("translation".equals(activeTab)) {
            statusText.setText(isVietnameseUi()
                    ? "ML Kit Translation không hỗ trợ import model thủ công."
                    : "ML Kit Translation does not support manual model import.");
            return;
        }
        pendingImportTab = activeTab;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_IMPORT_MODEL_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_IMPORT_MODEL_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        String tab = pendingImportTab;
        String displayName = displayNameForUri(uri);
        statusText.setText((isVietnameseUi() ? "Đang import " : "Importing ") + displayName + "...");
        executor.execute(() -> {
            try {
                importModelFile(tab, uri, displayName);
                mainHandler.post(() -> {
                    statusText.setText((isVietnameseUi() ? "Đã import model: " : "Imported model: ") + displayName);
                    refreshModels();
                });
            } catch (Exception e) {
                mainHandler.post(() -> statusText.setText((isVietnameseUi() ? "Import thất bại: " : "Import failed: ")
                        + safeMessage(e)));
            }
        });
    }

    private void showImportUrlDialog() {
        if ("translation".equals(activeTab)) {
            statusText.setText(isVietnameseUi()
                    ? "ML Kit Translation không hỗ trợ import model thủ công."
                    : "ML Kit Translation does not support manual model import.");
            return;
        }
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(18);
        form.setPadding(padding, dp(8), padding, 0);

        EditText nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setHint(defaultCustomModelName(activeTab));
        nameInput.setText(defaultCustomModelName(activeTab));
        form.addView(nameInput, new LinearLayout.LayoutParams(-1, dp(52)));

        EditText urlInput = new EditText(this);
        urlInput.setSingleLine(false);
        urlInput.setMinLines(2);
        urlInput.setMaxLines(4);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setHint(importUrlHint(activeTab));
        form.addView(urlInput, new LinearLayout.LayoutParams(-1, -2));

        String tab = activeTab;
        new android.app.AlertDialog.Builder(this)
                .setTitle(importUrlDescription())
                .setMessage(customModelHint(tab))
                .setView(form)
                .setNegativeButton(isVietnameseUi() ? "Hủy" : "Cancel", null)
                .setPositiveButton(isVietnameseUi() ? "Import" : "Import", (dialog, which) -> {
                    String modelName = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
                    String url = urlInput.getText() == null ? "" : urlInput.getText().toString().trim();
                    importModelFromUrl(tab, modelName, url);
                })
                .show();
    }

    private void importModelFromUrl(String tab, String modelName, String url) {
        if (url.isEmpty()) {
            statusText.setText(isVietnameseUi() ? "Cần nhập URL model." : "Model URL is required.");
            return;
        }
        final String safeName = sanitizeModelName(modelName.isEmpty() ? fileNameFromUrl(url) : modelName);
        statusText.setText((isVietnameseUi() ? "Đang tải model ngoài: " : "Downloading custom model: ") + safeName);
        executor.execute(() -> {
            try {
                if ("stt".equals(tab)) {
                    requireSuffix(url, ".zip", "STT custom cần file .zip của Vosk.");
                    File parent = ensureModelParent("stt");
                    List<String> before = validDirectoryPaths(parent, this::looksLikeVoskModel);
                    modelDownloadManager.downloadZip(url, parent, "custom-stt",
                            t("download_zip"), t("extract_zip"), t("extract_done"),
                            t("resume_from"), t("already_available"),
                            (phase, done, total) -> postCustomImportProgress(safeName, phase, done, total));
                    File imported = findImportedDirectory(parent, safeName, this::looksLikeVoskModel, before);
                    if (imported == null) {
                        throw new IOException("Không tìm thấy thư mục Vosk hợp lệ sau khi giải nén.");
                    }
                } else if ("tts".equals(tab)) {
                    requireSuffix(url, ".tar.bz2", "TTS custom cần file .tar.bz2 của Sherpa-ONNX.");
                    File parent = ensureModelParent("tts");
                    List<String> before = validDirectoryPaths(parent, this::looksLikeTtsModel);
                    modelDownloadManager.downloadTarBz2(url, parent, "custom-tts",
                            t("download_tar"), t("extract_tar"), t("extract_done"),
                            t("resume_from"), t("already_available"),
                            (phase, done, total) -> postCustomImportProgress(safeName, phase, done, total));
                    File imported = findImportedDirectory(parent, safeName, this::looksLikeTtsModel, before);
                    if (imported == null) {
                        throw new IOException("Không tìm thấy thư mục TTS hợp lệ sau khi giải nén.");
                    }
                } else if ("assistant".equals(tab)) {
                    requireSuffix(url, ".gguf", "Assistant custom cần file .gguf.");
                    String assistantName = safeName.toLowerCase(Locale.US).endsWith(".gguf")
                            ? safeName : safeName + ".gguf";
                    File output = new File(ensureModelParent("assistant"), assistantName);
                    modelDownloadManager.downloadFile(url, output, t("download_gguf"), t("save_gguf"),
                            t("resume_from"), t("already_available"),
                            (phase, done, total) -> postCustomImportProgress(output.getName(), phase, done, total));
                    if (!output.isFile() || output.length() == 0L) {
                        throw new IOException("File GGUF không hợp lệ.");
                    }
                }
                mainHandler.post(() -> {
                    statusText.setText(isVietnameseUi() ? "Đã import model ngoài." : "Custom model imported.");
                    refreshModels();
                });
            } catch (Exception e) {
                mainHandler.post(() -> statusText.setText((isVietnameseUi() ? "Import thất bại: " : "Import failed: ")
                        + safeMessage(e)));
            }
        });
    }

    private void importModelFile(String tab, Uri uri, String displayName) throws IOException {
        File cacheFile = copyUriToCache(uri, displayName);
        String lowerName = displayName.toLowerCase(Locale.US);
        try {
            if ("stt".equals(tab)) {
                if (!lowerName.endsWith(".zip")) {
                    throw new IOException("STT custom cần file .zip của Vosk.");
                }
                File parent = ensureModelParent("stt");
                List<String> before = validDirectoryPaths(parent, this::looksLikeVoskModel);
                unzip(cacheFile, parent);
                File imported = findImportedDirectory(parent, displayName, this::looksLikeVoskModel, before);
                if (imported == null) {
                    throw new IOException("Không tìm thấy thư mục Vosk hợp lệ sau khi giải nén.");
                }
                return;
            }
            if ("tts".equals(tab)) {
                if (!lowerName.endsWith(".tar.bz2")) {
                    throw new IOException("TTS custom cần file .tar.bz2 của Sherpa-ONNX.");
                }
                File parent = ensureModelParent("tts");
                List<String> before = validDirectoryPaths(parent, this::looksLikeTtsModel);
                extractTarBz2(cacheFile, parent);
                File imported = findImportedDirectory(parent, displayName, this::looksLikeTtsModel, before);
                if (imported == null) {
                    throw new IOException("Không tìm thấy thư mục TTS hợp lệ sau khi giải nén.");
                }
                return;
            }
            if ("assistant".equals(tab)) {
                if (!lowerName.endsWith(".gguf")) {
                    throw new IOException("Assistant custom cần file .gguf.");
                }
                File output = new File(ensureModelParent("assistant"), sanitizeModelName(displayName));
                copyFile(cacheFile, output);
                if (!output.isFile() || output.length() == 0L) {
                    throw new IOException("File GGUF không hợp lệ.");
                }
            }
        } finally {
            if (!cacheFile.delete()) {
                cacheFile.deleteOnExit();
            }
        }
    }

    private File copyUriToCache(Uri uri, String displayName) throws IOException {
        File output = new File(getCacheDir(), "import-" + System.currentTimeMillis() + "-" + sanitizeModelName(displayName));
        InputStream rawInput = getContentResolver().openInputStream(uri);
        if (rawInput == null) {
            throw new IOException("Không đọc được file đã chọn.");
        }
        try (InputStream input = new BufferedInputStream(rawInput);
             FileOutputStream fileOutput = new FileOutputStream(output)) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                fileOutput.write(buffer, 0, count);
            }
        }
        return output;
    }

    private void copyFile(File source, File output) throws IOException {
        File parent = output.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Không tạo được thư mục model.");
        }
        File temp = new File(parent, output.getName() + ".part");
        try (InputStream input = new BufferedInputStream(new FileInputStream(source));
             FileOutputStream fileOutput = new FileOutputStream(temp)) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                fileOutput.write(buffer, 0, count);
            }
        }
        if (output.exists() && !output.delete()) {
            throw new IOException("Không xóa được model cũ.");
        }
        if (!temp.renameTo(output)) {
            throw new IOException("Không lưu được model.");
        }
    }

    private File ensureModelParent(String type) throws IOException {
        File parent = AppStorage.modelDir(this, type);
        if (parent.exists() && !parent.isDirectory()) {
            throw new IOException("Could not create model folder.");
        }
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Không tạo được thư mục model.");
        }
        return parent;
    }

    private void ensureDirectory(File directory, String message) throws IOException {
        if (directory == null || (directory.exists() && !directory.isDirectory())) {
            throw new IOException(message);
        }
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException(message);
        }
    }

    private List<String> validDirectoryPaths(File parent, Predicate<File> validator) {
        List<String> result = new ArrayList<>();
        File[] children = parent.listFiles(File::isDirectory);
        if (children == null) {
            return result;
        }
        for (File child : children) {
            if (validator.test(child)) {
                try {
                    result.add(child.getCanonicalPath());
                } catch (IOException ignored) {
                }
            }
        }
        return result;
    }

    private File findImportedDirectory(File parent, String preferredName, Predicate<File> validator, List<String> before) {
        File preferred = new File(parent, stripKnownArchiveSuffix(sanitizeModelName(preferredName)));
        if (validator.test(preferred) && !containsCanonicalPath(before, preferred)) {
            return preferred;
        }
        File[] children = parent.listFiles(File::isDirectory);
        if (children == null) {
            return null;
        }
        File best = null;
        long bestTime = Long.MIN_VALUE;
        for (File child : children) {
            if (validator.test(child) && !containsCanonicalPath(before, child) && child.lastModified() >= bestTime) {
                best = child;
                bestTime = child.lastModified();
            }
        }
        return best;
    }

    private boolean containsCanonicalPath(List<String> paths, File file) {
        try {
            return paths != null && paths.contains(file.getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }

    private String displayNameForUri(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    name = cursor.getString(index);
                }
            }
        } catch (Exception ignored) {
        }
        if (name == null || name.trim().isEmpty()) {
            String path = uri.getLastPathSegment();
            name = path == null || path.trim().isEmpty() ? "custom-model" : path;
        }
        return sanitizeModelName(name);
    }

    private String sanitizeModelName(String value) {
        String clean = value == null ? "" : value.trim();
        clean = clean.replace('\\', '_').replace('/', '_').replace(':', '_');
        clean = clean.replaceAll("[\\r\\n\\t]", "_");
        return clean.isEmpty() ? "custom-model" : clean;
    }

    private String stripKnownArchiveSuffix(String name) {
        String lower = name.toLowerCase(Locale.US);
        if (lower.endsWith(".tar.bz2")) {
            return name.substring(0, name.length() - ".tar.bz2".length());
        }
        if (lower.endsWith(".zip")) {
            return name.substring(0, name.length() - ".zip".length());
        }
        if (lower.endsWith(".gguf")) {
            return name.substring(0, name.length() - ".gguf".length());
        }
        return name;
    }

    private String fileNameFromUrl(String url) {
        try {
            String path = new URL(url).getPath();
            int slash = path.lastIndexOf('/');
            return slash >= 0 ? path.substring(slash + 1) : path;
        } catch (Exception e) {
            return "custom-model";
        }
    }

    private void requireSuffix(String value, String suffix, String message) throws IOException {
        String clean = value == null ? "" : value.toLowerCase(Locale.US);
        int query = clean.indexOf('?');
        if (query >= 0) {
            clean = clean.substring(0, query);
        }
        if (!clean.endsWith(suffix)) {
            throw new IOException(message);
        }
    }

    private void postCustomImportProgress(String label, String phase, long doneBytes, long totalBytes) {
        mainHandler.post(() -> statusText.setText(downloadProgressText(label, phase, doneBytes, totalBytes)));
    }

    private void postDownloadProgress(ModelItem item, String phase, long doneBytes, long totalBytes) {
        mainHandler.post(() -> statusText.setText(downloadProgressText(item.label, phase, doneBytes, totalBytes)));
    }

    private String downloadProgressText(String label, String phase, long doneBytes, long totalBytes) {
        StringBuilder builder = new StringBuilder(String.format(t("download_progress"), label, phase));
        if (doneBytes > 0 && totalBytes > 0) {
            int percent = (int) Math.min(100, (doneBytes * 100L) / totalBytes);
            builder.append(" ").append(percent).append("% (")
                    .append(formatBytes(doneBytes)).append("/")
                    .append(formatBytes(totalBytes)).append(")");
        } else if (doneBytes > 0) {
            builder.append(" (").append(formatBytes(doneBytes)).append(")");
        }
        return builder.toString();
    }

    private Button tabButton(String label, String tab) {
        Button button = actionButton(label);
        button.setContentDescription(tabTitle(tab));
        button.setOnClickListener(v -> {
            activeTab = tab;
            refreshModels();
        });
        return button;
    }

    private void updateTabs() {
        styleTab(sttTab, "stt".equals(activeTab));
        styleTab(translationTab, "translation".equals(activeTab));
        styleTab(ttsTab, "tts".equals(activeTab));
        styleTab(assistantTab, "assistant".equals(activeTab));
        boolean importEnabled = !"translation".equals(activeTab);
        if (importFileButton != null) {
            importFileButton.setEnabled(importEnabled);
            importFileButton.setAlpha(importEnabled ? 1.0f : 0.45f);
        }
        if (importUrlButton != null) {
            importUrlButton.setEnabled(importEnabled);
            importUrlButton.setAlpha(importEnabled ? 1.0f : 0.45f);
        }
    }

    private void styleTab(Button button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setEnabled(!selected);
        button.setTextColor(selected ? getColor(R.color.meeting_primary_dark) : 0xffffffff);
        button.setBackgroundResource(selected ? R.drawable.panel_background : R.drawable.primary_button);
    }

    private String tabTitle(String tab) {
        if ("translation".equals(tab)) {
            return t("tab_translation");
        }
        if ("tts".equals(tab)) {
            return t("tab_tts");
        }
        if ("assistant".equals(tab)) {
            return t("assistant");
        }
        return t("tab_stt");
    }

    private void downloadAndUnzip(String urlString, File destinationParent) throws IOException {
        if (!destinationParent.exists() && !destinationParent.mkdirs()) {
            throw new IOException("Không tạo được thư mục model.");
        }
        File zipFile = resumableCacheFile("stt-model", urlString, ".zip.part");
        HttpURLConnection connection = DownloadConnection.open(urlString, DOWNLOAD_CONNECT_TIMEOUT_MS, DOWNLOAD_READ_TIMEOUT_MS);
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new IOException("HTTP " + responseCode);
        }
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             FileOutputStream output = new FileOutputStream(zipFile)) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        } finally {
            connection.disconnect();
        }
        unzip(zipFile, destinationParent);
        zipFile.delete();
    }

    private void downloadToFile(String urlString, File outputFile) throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Không tạo được thư mục model.");
        }
        File tempFile = File.createTempFile("assistant-model", ".download", getCacheDir());
        HttpURLConnection connection = DownloadConnection.open(urlString, DOWNLOAD_CONNECT_TIMEOUT_MS, DOWNLOAD_READ_TIMEOUT_MS);
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new IOException("HTTP " + responseCode);
        }
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             FileOutputStream output = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        } finally {
            connection.disconnect();
        }
        if (outputFile.exists()) {
            outputFile.delete();
        }
        if (!tempFile.renameTo(outputFile)) {
            throw new IOException("Không lưu được model Trợ lý.");
        }
    }

    private void unzip(File zipFile, File destinationParent) throws IOException {
        byte[] buffer = new byte[32 * 1024];
        String destinationRoot = destinationParent.getCanonicalPath() + File.separator;
        try (ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                File outputFile = new File(destinationParent, entry.getName());
                String outputPath = outputFile.getCanonicalPath();
                if (!outputPath.startsWith(destinationRoot)) {
                    throw new IOException("Zip không hợp lệ.");
                }
                if (entry.isDirectory()) {
                    ensureDirectory(outputFile, "Could not create model folder.");
                } else {
                    File parent = outputFile.getParentFile();
                    if (parent != null) {
                        ensureDirectory(parent, "Could not create model folder.");
                    }
                    try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                        int count;
                        while ((count = zipInput.read(buffer)) != -1) {
                            output.write(buffer, 0, count);
                        }
                    }
                }
                zipInput.closeEntry();
            }
        }
    }

    private void downloadAndExtractTarBz2(String urlString, File destinationParent) throws IOException {
        if (!destinationParent.exists() && !destinationParent.mkdirs()) {
            throw new IOException("Không tạo được thư mục TTS.");
        }
        File archiveFile = resumableCacheFile("tts-model", urlString, ".tar.bz2.part");
        HttpURLConnection connection = DownloadConnection.open(urlString, DOWNLOAD_CONNECT_TIMEOUT_MS, DOWNLOAD_READ_TIMEOUT_MS);
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            throw new IOException("HTTP " + responseCode);
        }
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             FileOutputStream output = new FileOutputStream(archiveFile)) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        } finally {
            connection.disconnect();
        }
        extractTarBz2(archiveFile, destinationParent);
        archiveFile.delete();
    }

    private void extractTarBz2(File archiveFile, File destinationParent) throws IOException {
        byte[] buffer = new byte[32 * 1024];
        String destinationRoot = destinationParent.getCanonicalPath() + File.separator;
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(
                new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(archiveFile))))) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextEntry()) != null) {
                if (entry.isSymbolicLink() || entry.isLink() || (!entry.isDirectory() && !entry.isFile())) {
                    throw new IOException("Archive không hợp lệ.");
                }
                File outputFile = new File(destinationParent, entry.getName());
                String outputPath = outputFile.getCanonicalPath();
                if (!outputPath.startsWith(destinationRoot)) {
                    throw new IOException("Archive không hợp lệ.");
                }
                if (entry.isDirectory()) {
                    ensureDirectory(outputFile, "Could not create model folder.");
                } else {
                    File parent = outputFile.getParentFile();
                    if (parent != null) {
                        ensureDirectory(parent, "Could not create model folder.");
                    }
                    try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                        int count;
                        while ((count = tarInput.read(buffer)) != -1) {
                            output.write(buffer, 0, count);
                        }
                    }
                }
            }
        }
    }

    private boolean deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                boolean deletedChildren = true;
                for (File child : children) {
                    deletedChildren &= deleteRecursively(child);
                }
                if (!deletedChildren) {
                    return false;
                }
            }
        }
        return file.delete();
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

    private String modelSizeLabel(ModelItem item) {
        if (item.localDir != null) {
            return " · " + formatBytes(folderSize(item.localDir));
        }
        if ("translation".equals(item.type)) {
            return " · ML Kit managed";
        }
        if (item.url == null) {
            return " · bundled";
        }
        return "";
    }

    private String dashboardTitle() {
        return isVietnameseUi() ? "Tổng quan " + tabTitle(activeTab) : tabTitle(activeTab) + " overview";
    }

    private String dashboardInstalledSuffix() {
        return isVietnameseUi() ? "model đã sẵn sàng" : "models ready";
    }

    private String dashboardNextStep(int bestCount, int missingBest) {
        if (missingBest == 0) {
            return isVietnameseUi()
                    ? "Các model khuyến nghị trong tab này đã sẵn sàng."
                    : "Recommended models in this tab are ready.";
        }
        return isVietnameseUi()
                ? "Còn thiếu " + missingBest + "/" + bestCount + " model khuyến nghị. Bấm \""
                + t("download_best") + "\" để tải nhanh."
                : missingBest + "/" + bestCount + " recommended models are missing. Tap \""
                + t("download_best") + "\" to fetch them.";
    }

    private String customGroup() {
        return isVietnameseUi() ? "Model ngoài" : "Custom model";
    }

    private String importFileLabel() {
        return isVietnameseUi() ? "Import file" : "Import file";
    }

    private String importFileDescription() {
        return isVietnameseUi() ? "Import model từ file local" : "Import model from local file";
    }

    private String importUrlLabel() {
        return isVietnameseUi() ? "Import URL" : "Import URL";
    }

    private String importUrlDescription() {
        return isVietnameseUi() ? "Import model từ URL" : "Import model from URL";
    }

    private String rescanLabel() {
        return isVietnameseUi() ? "Quét lại" : "Rescan";
    }

    private String rescanDescription() {
        return isVietnameseUi() ? "Quét lại model local" : "Rescan local models";
    }

    private String customModelTitle(String tab) {
        if ("translation".equals(tab)) {
            return isVietnameseUi() ? "Model ngoài: không áp dụng" : "Custom models: not applicable";
        }
        return isVietnameseUi() ? "Model ngoài" : "Custom models";
    }

    private String customModelHint(String tab) {
        if ("stt".equals(tab)) {
            return isVietnameseUi()
                    ? "Import file .zip Vosk hoặc nhập URL .zip. Sau khi giải nén, app kiểm tra thư mục có conf/, am/ hoặc graph/."
                    : "Import a Vosk .zip file or .zip URL. After extraction, the app checks for conf/, am/, or graph/.";
        }
        if ("tts".equals(tab)) {
            return isVietnameseUi()
                    ? "Import file/URL .tar.bz2 Sherpa-ONNX. App hỗ trợ Supertonic và VITS/Piper."
                    : "Import a Sherpa-ONNX .tar.bz2 file/URL. Supertonic and VITS/Piper are supported.";
        }
        if ("assistant".equals(tab)) {
            return isVietnameseUi()
                    ? "Import file/URL .gguf cho Trợ lý local."
                    : "Import a .gguf file/URL for the local Assistant.";
        }
        return isVietnameseUi()
                ? "ML Kit Translation quản lý model riêng nên không import thủ công."
                : "ML Kit Translation manages its own models, so manual import is not available.";
    }

    private String importUrlHint(String tab) {
        if ("stt".equals(tab)) {
            return "https://.../vosk-model-custom.zip";
        }
        if ("tts".equals(tab)) {
            return "https://.../sherpa-onnx-custom-tts.tar.bz2";
        }
        if ("assistant".equals(tab)) {
            return "https://.../model.gguf";
        }
        return "https://...";
    }

    private String defaultCustomModelName(String tab) {
        if ("assistant".equals(tab)) {
            return "custom-assistant.gguf";
        }
        if ("tts".equals(tab)) {
            return "custom-tts-model";
        }
        return "custom-stt-model";
    }

    private String modelTypeHint(ModelItem item) {
        if ("translation".equals(item.type)) {
            return isVietnameseUi() ? "ML Kit quản lý trên thiết bị" : "Managed by ML Kit on device";
        }
        if ("stt".equals(item.type) && item.localDir == null) {
            return isVietnameseUi() ? "OCR đi kèm ứng dụng" : "Bundled OCR";
        }
        if ("stt".equals(item.type)) {
            return isVietnameseUi() ? "Nhận dạng giọng nói offline" : "Offline speech recognition";
        }
        if ("tts".equals(item.type)) {
            return isVietnameseUi() ? "Phát âm offline nội bộ" : "Internal offline voice";
        }
        if ("assistant".equals(item.type)) {
            return isVietnameseUi() ? "LLM chạy trên thiết bị" : "On-device LLM";
        }
        return item.group == null ? "" : item.group;
    }

    private boolean isVietnameseUi() {
        return "vi".equals(uiLanguageCode);
    }

    private String displayGroup(String group, String type) {
        if ("translation".equals(type)) {
            return t("tab_translation");
        }
        if ("tts".equals(type)) {
            return t("tab_tts");
        }
        if ("assistant".equals(type)) {
            return t("assistant");
        }
        if (group != null && group.toLowerCase(Locale.US).contains("ocr")) {
            return "OCR";
        }
        return t("tab_stt");
    }

    private boolean looksLikeVoskModel(File dir) {
        return dir != null && dir.isDirectory()
                && (new File(dir, "conf").isDirectory()
                || new File(dir, "am").isDirectory()
                || new File(dir, "graph").isDirectory());
    }

    private boolean looksLikeTtsModel(File dir) {
        return TtsModelCatalog.looksLikeUsableModel(dir);
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        PremiumUi.primaryButton(button);
        PremiumUi.compactTextButton(this, button, text);
        return button;
    }

    private LinearLayout.LayoutParams weightedParams(int weight, int marginStart) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(38), weight);
        params.setMargins(marginStart, 0, 0, 0);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void downloadAndUnzipWithProgress(String urlString, File destinationParent, ProgressReporter progress) throws IOException {
        if (modelDownloadManager != null) {
            modelDownloadManager.downloadZip(
                    urlString,
                    destinationParent,
                    "stt-model",
                    t("download_zip"),
                    t("extract_zip"),
                    t("extract_done"),
                    t("resume_from"),
                    t("already_available"),
                    progress == null ? null : progress::report
            );
            return;
        }
        if (!destinationParent.exists() && !destinationParent.mkdirs()) {
            throw new IOException("Không tạo được thư mục model.");
        }
        File zipFile = resumableCacheFile("stt-model", urlString, ".zip.part");
        downloadArchiveWithProgress(urlString, zipFile, t("download_zip"), progress);
        report(progress, t("extract_zip"), zipFile.length(), zipFile.length());
        unzip(zipFile, destinationParent);
        report(progress, t("extract_done"), zipFile.length(), zipFile.length());
        if (!zipFile.delete()) {
            zipFile.deleteOnExit();
        }
    }

    private void downloadAndExtractTarBz2WithProgress(String urlString, File destinationParent, ProgressReporter progress) throws IOException {
        if (modelDownloadManager != null) {
            modelDownloadManager.downloadTarBz2(
                    urlString,
                    destinationParent,
                    "tts-model",
                    t("download_tar"),
                    t("extract_tar"),
                    t("extract_done"),
                    t("resume_from"),
                    t("already_available"),
                    progress == null ? null : progress::report
            );
            return;
        }
        if (!destinationParent.exists() && !destinationParent.mkdirs()) {
            throw new IOException("Không tạo được thư mục model TTS.");
        }
        File archiveFile = resumableCacheFile("tts-model", urlString, ".tar.bz2.part");
        downloadArchiveWithProgress(urlString, archiveFile, t("download_tar"), progress);
        report(progress, t("extract_tar"), archiveFile.length(), archiveFile.length());
        extractTarBz2(archiveFile, destinationParent);
        report(progress, t("extract_done"), archiveFile.length(), archiveFile.length());
        if (!archiveFile.delete()) {
            archiveFile.deleteOnExit();
        }
    }

    private void downloadToFileWithProgress(String urlString, File outputFile, ProgressReporter progress) throws IOException {
        if (modelDownloadManager != null) {
            modelDownloadManager.downloadFile(
                    urlString,
                    outputFile,
                    t("download_gguf"),
                    t("save_gguf"),
                    t("resume_from"),
                    t("already_available"),
                    progress == null ? null : progress::report
            );
            return;
        }
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Không tạo được thư mục model.");
        }
        File tempFile = new File(parent, outputFile.getName() + ".part");
        downloadArchiveWithProgress(urlString, tempFile, t("download_gguf"), progress);
        report(progress, t("save_gguf"), tempFile.length(), tempFile.length());
        if (outputFile.exists() && !outputFile.delete()) {
            throw new IOException("Không xóa được model cũ.");
        }
        if (!tempFile.renameTo(outputFile)) {
            throw new IOException("Không lưu được model Trợ lý.");
        }
    }

    private void downloadArchiveWithProgress(String urlString, File outputFile, String phase, ProgressReporter progress) throws IOException {
        long resumeBytes = outputFile.isFile() ? outputFile.length() : 0L;
        String rangeHeader = resumeBytes > 0 ? "bytes=" + resumeBytes + "-" : null;
        HttpURLConnection connection = DownloadConnection.open(urlString, DOWNLOAD_CONNECT_TIMEOUT_MS, DOWNLOAD_READ_TIMEOUT_MS, rangeHeader);
        int responseCode;
        try {
            connection.connect();
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            connection.disconnect();
            connection = DownloadConnection.reopenAfterTlsFailure(urlString, DOWNLOAD_CONNECT_TIMEOUT_MS, DOWNLOAD_READ_TIMEOUT_MS, rangeHeader, e);
            connection.connect();
            responseCode = connection.getResponseCode();
        }
        if (responseCode == 416 && resumeBytes > 0) {
            report(progress, phase + " " + t("already_available"), resumeBytes, resumeBytes);
            connection.disconnect();
            return;
        }
        if (responseCode == HttpURLConnection.HTTP_OK && resumeBytes > 0) {
            if (!outputFile.delete()) {
                connection.disconnect();
                throw new IOException("Không xóa được phần tải dở.");
            }
            resumeBytes = 0L;
        } else if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
            connection.disconnect();
            throw new IOException("HTTP " + responseCode);
        }
        long responseLength = connection.getContentLengthLong();
        long totalBytes = responseCode == HttpURLConnection.HTTP_PARTIAL && responseLength > 0
                ? resumeBytes + responseLength : responseLength;
        long doneBytes = resumeBytes;
        long startedAt = System.currentTimeMillis();
        long lastReportAt = 0L;
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             FileOutputStream output = new FileOutputStream(outputFile, resumeBytes > 0)) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                doneBytes += count;
                long now = System.currentTimeMillis();
                if (now - lastReportAt > 450L || doneBytes == totalBytes) {
                    report(progress, resumedPhase(phase, resumeBytes) + speedSuffix(doneBytes - resumeBytes, startedAt), doneBytes, totalBytes);
                    lastReportAt = now;
                }
            }
        } finally {
            connection.disconnect();
        }
        report(progress, resumedPhase(phase, resumeBytes) + speedSuffix(doneBytes - resumeBytes, startedAt), doneBytes, totalBytes);
    }

    private File resumableCacheFile(String prefix, String urlString, String suffix) {
        String key = Integer.toHexString(urlString.hashCode());
        return new File(getCacheDir(), prefix + "-" + key + suffix);
    }

    private String resumedPhase(String phase, long resumeBytes) {
        return resumeBytes > 0 ? phase + " • " + t("resume_from") + " " + formatBytes(resumeBytes) : phase;
    }

    private void report(ProgressReporter progress, String phase, long doneBytes, long totalBytes) {
        if (progress != null) {
            progress.report(phase, doneBytes, totalBytes);
        }
    }

    private String speedSuffix(long doneBytes, long startedAt) {
        long elapsedMs = Math.max(1L, System.currentTimeMillis() - startedAt);
        long bytesPerSecond = doneBytes * 1000L / elapsedMs;
        return " • " + formatBytes(bytesPerSecond) + "/s";
    }

    private String formatBytes(long bytes) {
        if (modelDownloadManager != null) {
            return ModelDownloadManager.formatBytes(bytes);
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
        if (DownloadConnection.isChainValidationFailure(e)) {
            return "TLS chain validation failed. Retried with trusted download-host fallback.";
        }
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private String t(String key) {
        return LocalizedText.get(uiLanguageCode, key);
    }

    private String buttonLabel(String key) {
        return LocalizedText.button(uiLanguageCode, key);
    }

    private class ModelItem {
        final String type;
        final String group;
        final String label;
        final String modelName;
        final File localDir;
        final String url;
        final String translateLanguageCode;
        final boolean best;
        boolean installed;

        ModelItem(String type, String group, String label, String modelName, File localDir, String url,
                  String translateLanguageCode, boolean best) {
            this.type = type;
            this.group = group;
            this.label = label;
            this.modelName = modelName;
            this.localDir = localDir;
            this.url = url;
            this.translateLanguageCode = translateLanguageCode;
            this.best = best;
        }

        boolean isInstalledSync() {
            try {
                if (localDir == null && url == null && translateLanguageCode == null) {
                    return true;
                }
                if ("stt".equals(type)) {
                    return looksLikeVoskModel(localDir);
                }
                if ("tts".equals(type)) {
                    return looksLikeTtsModel(localDir);
                }
                if ("assistant".equals(type)) {
                    return localDir != null && localDir.isFile() && localDir.length() > 1024L * 1024L;
                }
                TranslateRemoteModel model = new TranslateRemoteModel.Builder(translateLanguageCode).build();
                return Tasks.await(RemoteModelManager.getInstance().isModelDownloaded(model));
            } catch (Exception e) {
                return false;
            }
        }

        void downloadSync() throws Exception {
            downloadSync(null);
        }

        void downloadSync(ProgressReporter progress) throws Exception {
            if ("stt".equals(type)) {
                downloadAndUnzipWithProgress(url, localDir.getParentFile(), progress);
                return;
            }
            if ("tts".equals(type)) {
                downloadAndExtractTarBz2WithProgress(url, localDir.getParentFile(), progress);
                return;
            }
            if ("assistant".equals(type)) {
                downloadToFileWithProgress(url, localDir, progress);
                return;
            }
            report(progress, t("mlkit_downloading"), 0L, 0L);
            TranslateRemoteModel model = new TranslateRemoteModel.Builder(translateLanguageCode).build();
            Tasks.await(RemoteModelManager.getInstance().download(model, new DownloadConditions.Builder().build()));
            report(progress, t("mlkit_downloaded"), 1L, 1L);
        }

        void deleteSync() throws Exception {
            if ("stt".equals(type) || "tts".equals(type) || "assistant".equals(type)) {
                if (!deleteRecursively(localDir)) {
                    throw new IOException("Không xóa được toàn bộ model.");
                }
                return;
            }
            TranslateRemoteModel model = new TranslateRemoteModel.Builder(translateLanguageCode).build();
            Tasks.await(RemoteModelManager.getInstance().deleteDownloadedModel(model));
        }
    }
}
