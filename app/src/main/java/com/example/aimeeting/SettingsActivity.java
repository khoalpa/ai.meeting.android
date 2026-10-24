package com.example.aimeeting;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@SuppressLint("SetTextI18n")
public class SettingsActivity extends Activity {
    public static final String PREFS = "model_settings";
    public static final String LOCAL_PREFIX = "local:";

    private static final String TAB_STT = "stt";
    private static final String TAB_OCR = "ocr";
    private static final String TAB_TRANSLATION = "translation";
    private static final String TAB_TTS = "tts";
    private static final String TAB_ASSISTANT = "assistant";

    private LinearLayout rootLayout;
    private LinearLayout headerLayout;
    private ScrollView scrollView;
    private LinearLayout contentLayout;
    private TextView statusText;
    private TextView overviewSummaryText;
    private TextView overviewDetailText;
    private Button sttTab;
    private Button translationTab;
    private Button ttsTab;
    private Button assistantTab;
    private SharedPreferences preferences;
    private String activeTab = TAB_STT;
    private String uiLanguageCode = "vi";
    private String languageOneName = "Ngon ngu nguon";
    private String languageOneCode = "vi";
    private String languageTwoCode = "en";
    private String languageTwoName = "Ngôn ngữ đích";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        String requestedLanguage = getIntent().getStringExtra("ui_language_code");
        if (requestedLanguage != null && !requestedLanguage.trim().isEmpty()) {
            uiLanguageCode = requestedLanguage;
        }
        languageOneName = extraOrDefault("language_1_name", LocalizedText.get(uiLanguageCode, "language_1"));
        languageTwoName = extraOrDefault("language_2_name", LocalizedText.get(uiLanguageCode, "language_2"));
        languageOneCode = extraOrDefault("language_1_code", "vi");
        languageTwoCode = extraOrDefault("language_2_code", "en");
        buildUi();
        renderActiveTab();
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

        TextView title = new TextView(this);
        title.setText(t("settings"));
        title.setTextColor(getColor(R.color.meeting_text));
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        headerLayout.addView(title);

        statusText = new TextView(this);
        statusText.setText(t("settings_hint"));
        statusText.setTextColor(getColor(R.color.meeting_muted));
        statusText.setTextSize(14);
        statusText.setMaxLines(3);
        headerLayout.addView(statusText);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, dp(8), 0, 0);
        headerLayout.addView(tabs);

        sttTab = tabButton(buttonLabel("tab_stt"), TAB_STT);
        translationTab = tabButton(buttonLabel("tab_translation"), TAB_TRANSLATION);
        ttsTab = tabButton(buttonLabel("tab_tts"), TAB_TTS);
        assistantTab = tabButton(buttonLabel("assistant"), TAB_ASSISTANT);
        tabs.addView(sttTab, weightedParams(1, 0));
        tabs.addView(translationTab, weightedParams(1, dp(8)));
        tabs.addView(ttsTab, weightedParams(1, dp(8)));
        tabs.addView(assistantTab, weightedParams(1, dp(8)));

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(dp(16), dp(12), dp(16), dp(20));
        scrollView.addView(contentLayout);
        rootLayout.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        PremiumUi.addBottomNavigation(this, rootLayout, "settings", uiLanguageCode,
                languageOneName, languageOneCode, languageTwoName, languageTwoCode);
        setContentView(rootLayout);
        applySystemBarInsets();
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

    private Button tabButton(String label, String tab) {
        Button button = actionButton(label);
        button.setContentDescription(tabTitle(tab));
        button.setOnClickListener(v -> {
            activeTab = tab;
            renderActiveTab();
        });
        return button;
    }

    private void renderActiveTab() {
        updateTabs();
        contentLayout.removeAllViews();
        addSettingsOverviewCard();
        if (TAB_STT.equals(activeTab)) {
            List<ModelChoice> speechChoicesOne = speechRecognitionChoices(languageOneCode);
            List<ModelChoice> speechChoicesTwo = speechRecognitionChoices(languageTwoCode);
            List<ModelChoice> textChoicesOne = textRecognitionChoices(languageOneCode);
            List<ModelChoice> textChoicesTwo = textRecognitionChoices(languageTwoCode);
            addPreferenceCard(t("speech_recognition") + " - " + LocalizedText.get(uiLanguageCode, "language_1") + ": " + languageOneName,
                    preferenceKey(TAB_STT, 1), speechChoicesOne);
            addPreferenceCard(t("speech_recognition") + " - " + LocalizedText.get(uiLanguageCode, "language_2") + ": " + languageTwoName,
                    preferenceKey(TAB_STT, 2), speechChoicesTwo);
            addPreferenceCard(t("text_recognition") + " - " + LocalizedText.get(uiLanguageCode, "language_1") + ": " + languageOneName,
                    preferenceKey(TAB_OCR, 1), textChoicesOne);
            addPreferenceCard(t("text_recognition") + " - " + LocalizedText.get(uiLanguageCode, "language_2") + ": " + languageTwoName,
                    preferenceKey(TAB_OCR, 2), textChoicesTwo);
            statusText.setText(tabTitle(activeTab) + ": "
                    + (speechChoicesOne.size() + speechChoicesTwo.size() + textChoicesOne.size() + textChoicesTwo.size())
                    + " " + t("settings_model_options"));
            return;
        }

        addPreferenceCard(LocalizedText.get(uiLanguageCode, "language_1") + ": " + languageOneName,
                preferenceKey(activeTab, 1), choicesFor(activeTab, languageOneCode));
        addPreferenceCard(LocalizedText.get(uiLanguageCode, "language_2") + ": " + languageTwoName,
                preferenceKey(activeTab, 2), choicesFor(activeTab, languageTwoCode));
        statusText.setText(tabTitle(activeTab) + ": " + t("settings_model_options"));
    }

    private void addSettingsOverviewCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.elevateCard(this, card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(12));
        contentLayout.addView(card, params);

        TextView title = new TextView(this);
        title.setText("Tổng quan cấu hình");
        title.setTextColor(getColor(R.color.meeting_text));
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        overviewSummaryText = new TextView(this);
        overviewSummaryText.setTextColor(getColor(R.color.meeting_primary_dark));
        overviewSummaryText.setTextSize(15);
        overviewSummaryText.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.setMargins(0, dp(8), 0, 0);
        card.addView(overviewSummaryText, summaryParams);

        overviewDetailText = new TextView(this);
        overviewDetailText.setTextColor(getColor(R.color.meeting_muted));
        overviewDetailText.setTextSize(13);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.setMargins(0, dp(4), 0, 0);
        card.addView(overviewDetailText, detailParams);

        updateSettingsOverview();
    }

    private void updateSettingsOverview() {
        if (overviewSummaryText == null || overviewDetailText == null) {
            return;
        }
        int ready = 0;
        int total = 0;
        StringBuilder detail = new StringBuilder();
        if (TAB_STT.equals(activeTab)) {
            total = 4;
            ready += appendOverviewLine(detail, "Giọng nói " + languageOneName, preferenceKey(TAB_STT, 1));
            ready += appendOverviewLine(detail, "Giọng nói " + languageTwoName, preferenceKey(TAB_STT, 2));
            ready += appendOverviewLine(detail, "OCR " + languageOneName, preferenceKey(TAB_OCR, 1));
            ready += appendOverviewLine(detail, "OCR " + languageTwoName, preferenceKey(TAB_OCR, 2));
        } else {
            total = 2;
            ready += appendOverviewLine(detail, languageOneName, preferenceKey(activeTab, 1));
            ready += appendOverviewLine(detail, languageTwoName, preferenceKey(activeTab, 2));
        }
        overviewSummaryText.setText(ready + "/" + total + " cấu hình có runtime khả dụng");
        overviewSummaryText.setTextColor(getColor(ready == total ? R.color.meeting_primary_dark : android.R.color.holo_orange_dark));
        overviewDetailText.setText(detail.toString().trim());
    }

    private int appendOverviewLine(StringBuilder builder, String label, String key) {
        String value = OnlineProviderConfig.safeString(preferences, key, "");
        String choiceLabel = OnlineProviderConfig.safeString(preferences, key + "_label", "");
        if (choiceLabel.isEmpty()) {
            choiceLabel = value.isEmpty() ? "Chưa chọn" : runtimeLabel(value);
        }
        boolean ready = isChoiceUsable(value);
        builder.append(label)
                .append(": ")
                .append(choiceLabel)
                .append(" · ")
                .append(ready ? "sẵn sàng" : "cần kiểm tra")
                .append('\n');
        return ready ? 1 : 0;
    }

    private void addPreferenceCard(String label, String key, List<ModelChoice> choices) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.elevateCard(this, card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        contentLayout.addView(card, params);

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(getColor(R.color.meeting_text));
        title.setTextSize(16);
        title.setTypeface(null, Typeface.BOLD);
        card.addView(title);

        TextView detail = new TextView(this);
        detail.setText(t("preferred_model"));
        detail.setTextColor(getColor(R.color.meeting_muted));
        detail.setTextSize(13);
        detail.setPadding(0, dp(4), 0, dp(8));
        card.addView(detail);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams chipRowParams = new LinearLayout.LayoutParams(-1, -2);
        chipRowParams.setMargins(0, 0, 0, dp(8));
        card.addView(chips, chipRowParams);

        TextView runtimeChip = settingChip();
        chips.addView(runtimeChip);

        TextView readyChip = settingChip();
        LinearLayout.LayoutParams readyChipParams = new LinearLayout.LayoutParams(-2, -2);
        readyChipParams.setMargins(dp(8), 0, 0, 0);
        chips.addView(readyChip, readyChipParams);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<ModelChoice> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selectionFor(key, choices));
        TextView modelStatus = new TextView(this);
        modelStatus.setTextColor(getColor(R.color.meeting_muted));
        modelStatus.setTextSize(13);
        modelStatus.setPadding(0, dp(8), 0, 0);
        modelStatus.setTextIsSelectable(true);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ModelChoice choice = choices.get(position);
                preferences.edit()
                        .putString(key, choice.value)
                        .putString(key + "_label", choice.label)
                        .apply();
                runtimeChip.setText(runtimeLabel(choice.value));
                readyChip.setText(isChoiceUsable(choice.value) ? t("ready") : t("not_ready"));
                readyChip.setTextColor(getColor(isChoiceUsable(choice.value) ? R.color.meeting_primary_dark : android.R.color.holo_orange_dark));
                readyChip.setBackgroundResource(isChoiceUsable(choice.value) ? R.drawable.status_chip_neutral : R.drawable.status_chip_warning);
                modelStatus.setText(modelStatusText(choice));
                updateSettingsOverview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        card.addView(spinner, new LinearLayout.LayoutParams(-1, dp(46)));
        card.addView(modelStatus);
        ModelChoice initialChoice = choices.get(spinner.getSelectedItemPosition() < 0 ? 0 : spinner.getSelectedItemPosition());
        runtimeChip.setText(runtimeLabel(initialChoice.value));
        readyChip.setText(isChoiceUsable(initialChoice.value) ? t("ready") : t("not_ready"));
        readyChip.setTextColor(getColor(isChoiceUsable(initialChoice.value) ? R.color.meeting_primary_dark : android.R.color.holo_orange_dark));
        readyChip.setBackgroundResource(isChoiceUsable(initialChoice.value) ? R.drawable.status_chip_neutral : R.drawable.status_chip_warning);
        modelStatus.setText(modelStatusText(initialChoice));
        preferences.edit()
                .putString(key, initialChoice.value)
                .putString(key + "_label", initialChoice.label)
                .apply();
        updateSettingsOverview();
    }

    private TextView settingChip() {
        TextView chip = new TextView(this);
        chip.setTextColor(getColor(R.color.meeting_primary_dark));
        chip.setTextSize(12);
        chip.setTypeface(null, Typeface.BOLD);
        chip.setBackgroundResource(R.drawable.status_chip_neutral);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        return chip;
    }

    private int selectionFor(String key, List<ModelChoice> choices) {
        String saved = OnlineProviderConfig.safeString(preferences, key, "");
        if (!saved.isEmpty()) {
            for (int i = 0; i < choices.size(); i++) {
                if (saved.equals(choices.get(i).value)) {
                    return i;
                }
            }
        }
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).value.startsWith(LOCAL_PREFIX)) {
                return i;
            }
        }
        return 0;
    }

    private List<ModelChoice> choicesFor(String tab, String languageCode) {
        List<ModelChoice> choices = new ArrayList<>();
        if (TAB_STT.equals(tab)) {
            choices.addAll(speechRecognitionChoices(languageCode));
        } else if (TAB_TRANSLATION.equals(tab)) {
            choices.add(new ModelChoice("Google Service - ML Kit on-device Translate", "google:mlkit-translate"));
            choices.add(new ModelChoice("Google Service - Google Translate", "google:translate-service"));
            choices.add(new ModelChoice("Provider miễn phí - LibreTranslate public", "provider:libretranslate-free"));
            choices.add(new ModelChoice("Provider miễn phí - MyMemory Translate", "provider:mymemory-free"));
        } else if (TAB_TTS.equals(tab)) {
            addLocalDirectories(choices, "models/tts", "Máy - ");
            choices.add(new ModelChoice("Google Service - Text-to-Speech", "google:text-to-speech"));
            choices.add(new ModelChoice("Provider online - OpenAI Text-to-Speech", "provider:openai-tts"));
            choices.add(new ModelChoice("Provider miễn phí - StreamElements TTS", "provider:streamelements-tts-free"));
            choices.add(new ModelChoice("Provider miễn phí - Hugging Face SpeechT5", "provider:huggingface-speecht5-free"));
        } else {
            addLocalFiles(choices, "models/assistant", ".gguf", "Máy - ");
            choices.add(new ModelChoice("Provider online - OpenAI Chat", "provider:openai-chat"));
            choices.add(new ModelChoice("Google Service - Gemini", "google:gemini-service"));
            choices.add(new ModelChoice("Provider miễn phí - OpenRouter free model", "provider:openrouter-free"));
            choices.add(new ModelChoice("Provider miễn phí - Groq Llama free", "provider:groq-llama-free"));
            choices.add(new ModelChoice("Provider miễn phí - Hugging Face chat", "provider:huggingface-chat-free"));
        }
        choices = readyChoices(choices, languageCode, tab);
        if (choices.isEmpty()) {
            choices.add(new ModelChoice(t("settings_no_model"), "none"));
        }
        return choices;
    }

    private List<ModelChoice> speechRecognitionChoices(String languageCode) {
        List<ModelChoice> choices = new ArrayList<>();
        addLocalDirectories(choices, "models/stt", "Máy - ");
        choices.add(new ModelChoice("Google Service - Speech Recognizer", "google:speech-recognizer"));
        choices.add(new ModelChoice("Provider online - OpenAI Transcribe", "provider:openai-stt"));
        choices.add(new ModelChoice("Provider miễn phí - Groq Whisper Large V3 Turbo", "provider:groq-whisper-free"));
        choices.add(new ModelChoice("Provider miễn phí - Hugging Face Whisper", "provider:huggingface-whisper-free"));
        choices = readyChoices(choices, languageCode, TAB_STT);
        if (choices.isEmpty()) {
            choices.add(new ModelChoice(t("settings_no_model"), "none"));
        }
        return choices;
    }

    private List<ModelChoice> textRecognitionChoices(String languageCode) {
        List<ModelChoice> choices = new ArrayList<>();
        choices.add(new ModelChoice("Google Service - ML Kit OCR bundled", "google:mlkit-ocr"));
        choices.add(new ModelChoice("Google Cloud - Vision OCR", "google:cloud-vision-ocr"));
        choices.add(new ModelChoice("Provider online - OpenAI Vision OCR", "provider:openai-ocr"));
        choices = readyChoices(choices, languageCode, TAB_OCR);
        if (choices.isEmpty()) {
            choices.add(new ModelChoice(t("settings_no_model"), "none"));
        }
        return choices;
    }

    private List<ModelChoice> readyChoices(List<ModelChoice> source, String languageCode, String tab) {
        List<ModelChoice> ready = new ArrayList<>();
        for (ModelChoice choice : source) {
            if (isChoiceReadyFor(choice.value, languageCode, tab)) {
                ready.add(choice);
            }
        }
        return ready;
    }

    private boolean isChoiceReadyFor(String value, String languageCode, String tab) {
        if (value == null || value.isEmpty() || "none".equals(value)) {
            return false;
        }
        if (value.startsWith(LOCAL_PREFIX)) {
            File file = localFileFromValue(value);
            if (file == null) {
                return false;
            }
            if (TAB_STT.equals(tab)) {
                return looksLikeVoskModel(file) && sttModelMatchesLanguage(file.getName(), languageCode);
            }
            if (TAB_TTS.equals(tab)) {
                return looksLikeTtsModel(file) && ttsModelMatchesLanguage(file, languageCode);
            }
            if (TAB_ASSISTANT.equals(tab)) {
                return file.isFile() && file.getName().toLowerCase(Locale.US).endsWith(".gguf");
            }
            return file.exists();
        }
        if (value.startsWith("google:")) {
            if ("google:mlkit-translate".equals(value)) {
                return isTranslateModelReady(languageCode);
            }
            if ("google:mlkit-ocr".equals(value)) {
                return ocrSupportsLanguage(languageCode);
            }
            if ("google:speech-recognizer".equals(value) || "google:text-to-speech".equals(value)) {
                return isKnownAppLanguage(languageCode);
            }
            return "google:cloud-vision-ocr".equals(value)
                    || "google:gemini-service".equals(value)
                    || "google:translate-service".equals(value)
                    ? providerHasApiKey(value)
                    : false;
        }
        if (value.startsWith("provider:")) {
            if ("provider:mymemory-free".equals(value) || "provider:libretranslate-free".equals(value)) {
                return TAB_TRANSLATION.equals(tab) && isKnownAppLanguage(languageCode);
            }
            if ("provider:streamelements-tts-free".equals(value)) {
                return TAB_TTS.equals(tab) && "en".equals(languageCode);
            }
            return providerHasApiKey(value);
        }
        return false;
    }

    private boolean providerHasApiKey(String providerValue) {
        String providerId = OnlineProviderConfig.providerId(providerValue);
        return !SecretStore.get(this, preferences, OnlineProviderConfig.key(providerId, "api_key")).isEmpty();
    }

    private boolean isTranslateModelReady(String languageCode) {
        String translateLanguage = translateLanguageFor(languageCode);
        if (translateLanguage == null) {
            return false;
        }
        try {
            TranslateRemoteModel model = new TranslateRemoteModel.Builder(translateLanguage).build();
            return Tasks.await(RemoteModelManager.getInstance().isModelDownloaded(model), 350, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    private String translateLanguageFor(String languageCode) {
        switch (languageCode) {
            case "vi":
                return TranslateLanguage.VIETNAMESE;
            case "en":
                return TranslateLanguage.ENGLISH;
            case "ja":
                return TranslateLanguage.JAPANESE;
            case "ko":
                return TranslateLanguage.KOREAN;
            case "zh":
                return TranslateLanguage.CHINESE;
            case "fr":
                return TranslateLanguage.FRENCH;
            case "de":
                return TranslateLanguage.GERMAN;
            case "es":
                return TranslateLanguage.SPANISH;
            case "th":
                return TranslateLanguage.THAI;
            default:
                return null;
        }
    }

    private boolean sttModelMatchesLanguage(String modelName, String languageCode) {
        String name = modelName == null ? "" : modelName.toLowerCase(Locale.US);
        switch (languageCode) {
            case "vi":
                return name.contains("-vn") || name.contains("_vi") || name.contains("vietnam");
            case "en":
                return name.contains("-en") || name.contains("_en") || name.contains("english");
            case "ja":
                return name.contains("-ja") || name.contains("_ja") || name.contains("japanese");
            case "ko":
                return name.contains("-ko") || name.contains("_ko") || name.contains("korean");
            case "zh":
                return name.contains("-cn") || name.contains("-zh") || name.contains("_zh") || name.contains("chinese");
            case "fr":
                return name.contains("-fr") || name.contains("_fr") || name.contains("french");
            case "de":
                return name.contains("-de") || name.contains("_de") || name.contains("german");
            case "es":
                return name.contains("-es") || name.contains("_es") || name.contains("spanish");
            case "th":
                return name.contains("-th") || name.contains("_th") || name.contains("thai");
            default:
                return false;
        }
    }

    private boolean ttsModelMatchesLanguage(File dir, String languageCode) {
        if (dir == null) {
            return false;
        }
        if ("vi".equals(languageCode)) {
            return TtsModelCatalog.isVietnameseModelName(dir.getName())
                    || dir.getName().toLowerCase(Locale.US).contains("vi");
        }
        return TtsModelCatalog.SUPERTONIC_MODEL_NAME.equals(dir.getName())
                || dir.getName().toLowerCase(Locale.US).contains(languageCode);
    }

    private boolean ocrSupportsLanguage(String languageCode) {
        return isKnownAppLanguage(languageCode);
    }

    private boolean isKnownAppLanguage(String languageCode) {
        return "vi".equals(languageCode) || "en".equals(languageCode) || "ja".equals(languageCode)
                || "ko".equals(languageCode) || "zh".equals(languageCode) || "fr".equals(languageCode)
                || "de".equals(languageCode) || "es".equals(languageCode) || "th".equals(languageCode);
    }

    private String modelStatusText(ModelChoice choice) {
        StringBuilder builder = new StringBuilder();
        builder.append(t("model_status")).append(": ");
        if (choice.value.startsWith(LOCAL_PREFIX)) {
            File file = localFileFromValue(choice.value);
            appendLocalStatus(builder, file);
            return builder.toString();
        }
        if (choice.value.startsWith("google:")) {
            appendGoogleStatus(builder, choice.value);
            return builder.toString();
        }
        if (choice.value.startsWith("provider:")) {
            appendProviderStatus(builder, choice.value);
            return builder.toString();
        }
        builder.append(t("not_ready")).append("\n").append(t("require_select_model"));
        return builder.toString();
    }

    private String runtimeLabel(String value) {
        if (value == null || value.isEmpty() || "none".equals(value)) {
            return "Chưa chọn";
        }
        if (value.startsWith(LOCAL_PREFIX)) {
            return "Local";
        }
        if (value.startsWith("google:")) {
            return "Google";
        }
        if (value.startsWith("provider:")) {
            return "Provider";
        }
        return "Model";
    }

    private boolean isChoiceUsable(String value) {
        if (value == null || value.isEmpty() || "none".equals(value)) {
            return false;
        }
        if (value.startsWith(LOCAL_PREFIX)) {
            File file = localFileFromValue(value);
            if (file == null) {
                return false;
            }
            if (TAB_STT.equals(activeTab)) {
                return looksLikeVoskModel(file);
            }
            if (TAB_TTS.equals(activeTab)) {
                return looksLikeTtsModel(file);
            }
            if (TAB_ASSISTANT.equals(activeTab)) {
                return file.isFile() && file.getName().toLowerCase(Locale.US).endsWith(".gguf");
            }
            return file.exists();
        }
        if (value.startsWith("google:")) {
            return "google:mlkit-translate".equals(value)
                    || "google:mlkit-ocr".equals(value)
                    || "google:speech-recognizer".equals(value)
                    || "google:text-to-speech".equals(value)
                    || "google:gemini-service".equals(value)
                    || "google:cloud-vision-ocr".equals(value);
        }
        if (value.startsWith("provider:")) {
            return "provider:mymemory-free".equals(value)
                    || "provider:libretranslate-free".equals(value)
                    || "provider:streamelements-tts-free".equals(value)
                    || OnlineProviderConfig.isOnlineSttProvider(value)
                    || OnlineProviderConfig.isOnlineOcrProvider(value)
                    || OnlineProviderConfig.isOnlineTtsProvider(value)
                    || OnlineProviderConfig.isOnlineAssistantProvider(value);
        }
        return false;
    }

    private File localFileFromValue(String value) {
        if (value == null || !value.startsWith(LOCAL_PREFIX)) {
            return null;
        }
        String path = value.substring(LOCAL_PREFIX.length()).trim();
        return path.isEmpty() ? null : new File(path);
    }

    private void appendLocalStatus(StringBuilder builder, File file) {
        builder.append(file.exists() ? t("ready") : t("not_ready"));
        builder.append("\n").append(t("model_path")).append(": ").append(file.getAbsolutePath());
        builder.append("\n").append(t("size")).append(": ").append(formatBytes(folderSize(file)));
        if (!file.exists()) {
            builder.append("\n").append(t("require_model_file_exists"));
            return;
        }
        if (TAB_STT.equals(activeTab)) {
            boolean ok = looksLikeVoskModel(file);
            builder.append("\n").append(t("runtime")).append(": Vosk offline");
            builder.append("\n").append(t("required_files")).append(": conf/ hoặc am/ hoặc graph/");
            builder.append("\n").append(t("usable_now")).append(": ").append(ok ? t("yes") : t("no"));
        } else if (TAB_TTS.equals(activeTab)) {
            boolean ok = looksLikeTtsModel(file);
            builder.append("\n").append(t("runtime")).append(": ").append(TtsModelCatalog.runtimeName(file));
            builder.append("\n").append(t("required_files")).append(": ").append(TtsModelCatalog.requiredFilesText());
            builder.append("\n").append(t("usable_now")).append(": ").append(ok ? t("yes") : t("no"));
        } else if (TAB_ASSISTANT.equals(activeTab)) {
            boolean ok = file.isFile() && file.getName().toLowerCase(Locale.US).endsWith(".gguf");
            builder.append("\n").append(t("runtime")).append(": Llama GGUF local");
            builder.append("\n").append(t("required_files")).append(": *.gguf");
            builder.append("\n").append(t("usable_now")).append(": ").append(ok ? t("yes") : t("no"));
        } else {
            builder.append("\n").append(t("runtime")).append(": ML Kit on-device");
            builder.append("\n").append(t("usable_now")).append(": ").append(t("yes"));
        }
    }

    private void appendGoogleStatus(StringBuilder builder, String value) {
        boolean implemented = "google:mlkit-translate".equals(value)
                || "google:mlkit-ocr".equals(value)
                || "google:speech-recognizer".equals(value)
                || "google:text-to-speech".equals(value)
                || "google:gemini-service".equals(value)
                || "google:cloud-vision-ocr".equals(value);
        builder.append(implemented ? t("ready") : t("not_ready"));
        builder.append("\n").append(t("runtime")).append(": Google Service");
        if ("google:mlkit-translate".equals(value)) {
            builder.append("\n").append(t("details")).append(": ML Kit Translate da duoc tich hop, tu tai model khi can.");
            builder.append("\n").append(t("requirements")).append(": Cần Internet lần đầu để tải model dịch.");
        } else if ("google:mlkit-ocr".equals(value)) {
            builder.append("\n").append(t("details")).append(": ML Kit OCR bundled đã được tích hợp cho tệp ảnh/PDF.");
            builder.append("\n").append(t("requirements")).append(": Tep dau vao la anh/PDF ro net.");
        } else if ("google:speech-recognizer".equals(value)) {
            builder.append("\n").append(t("details")).append(": Android/Google SpeechRecognizer da duoc tich hop cho nghe truc tiep.");
            builder.append("\n").append(t("requirements")).append(": Máy có dịch vụ nhận dạng giọng nói và kết nối mạng nếu engine yêu cầu.");
        } else if ("google:text-to-speech".equals(value)) {
            builder.append("\n").append(t("details")).append(": Android/Google Text-to-Speech đã được tích hợp cho phát âm.");
            builder.append("\n").append(t("requirements")).append(": Máy có voice TTS cho ngôn ngữ đang chọn.");
        } else if ("google:gemini-service".equals(value)) {
            builder.append("\n").append(t("details")).append(": Gemini API adapter đã được tích hợp cho trợ lý online.");
            builder.append("\n").append(t("requirements")).append(": API key Google AI Studio va model Gemini.");
        } else if ("google:cloud-vision-ocr".equals(value)) {
            builder.append("\n").append(t("details")).append(": Google Cloud Vision OCR REST adapter da duoc tich hop.");
            builder.append("\n").append(t("requirements")).append(": API key co quyen Cloud Vision API.");
        } else {
            builder.append("\n").append(t("details")).append(": Lựa chọn đã lưu nhưng app chưa có adapter thực thi trực tiếp.");
            builder.append("\n").append(t("requirements")).append(": Cần bổ sung SDK/API key/adapter cho service này.");
        }
        builder.append("\n").append(t("usable_now")).append(": ").append(implemented ? t("yes") : t("no"));
    }

    private void appendProviderStatus(StringBuilder builder, String value) {
        boolean implemented = "provider:mymemory-free".equals(value)
                || "provider:libretranslate-free".equals(value)
                || "provider:streamelements-tts-free".equals(value)
                || OnlineProviderConfig.isOnlineSttProvider(value)
                || OnlineProviderConfig.isOnlineOcrProvider(value)
                || OnlineProviderConfig.isOnlineTtsProvider(value)
                || OnlineProviderConfig.isOnlineAssistantProvider(value);
        builder.append(implemented ? t("ready") : t("not_ready"));
        builder.append("\n").append(t("runtime")).append(": Online provider");
        if ("provider:mymemory-free".equals(value)) {
            builder.append("\n").append(t("details")).append(": Adapter MyMemory Translate da duoc tich hop.");
            builder.append("\n").append(t("requirements")).append(": Internet và quota public của MyMemory.");
        } else if ("provider:libretranslate-free".equals(value)) {
            builder.append("\n").append(t("details")).append(": Adapter LibreTranslate public da duoc tich hop.");
            builder.append("\n").append(t("requirements")).append(": Internet và endpoint public đang sẵn sàng.");
        } else if ("provider:streamelements-tts-free".equals(value)) {
            builder.append("\n").append(t("details")).append(": Adapter StreamElements TTS da duoc tich hop.");
            builder.append("\n").append(t("requirements")).append(": Internet; voice public có thể không hỗ trợ tất cả ngôn ngữ.");
        } else if (OnlineProviderConfig.isOnlineSttProvider(value)) {
            builder.append("\n").append(t("details")).append(": Adapter STT online da duoc tich hop.");
            builder.append("\n").append(t("requirements")).append(": API key/endpoint/model trong Quản lý models > Online provider API.");
        } else if (OnlineProviderConfig.isOnlineOcrProvider(value)) {
            builder.append("\n").append(t("details")).append(": Adapter OCR online da duoc tich hop.");
            builder.append("\n").append(t("requirements")).append(": API key va model/endpoint OCR phu hop.");
        } else if (OnlineProviderConfig.isOnlineTtsProvider(value)) {
            builder.append("\n").append(t("details")).append(": Adapter TTS online da duoc tich hop.");
            builder.append("\n").append(t("requirements")).append(": API key, endpoint, model va voice neu provider yeu cau.");
        } else if (OnlineProviderConfig.isOnlineAssistantProvider(value)) {
            builder.append("\n").append(t("details")).append(": Adapter LLM online da duoc tich hop.");
            builder.append("\n").append(t("requirements")).append(": API key/endpoint/model trong Quản lý models > Online provider API.");
        } else {
            builder.append("\n").append(t("details")).append(": Lựa chọn đã lưu để ưu tiên provider, nhưng app hiện chưa có API key/adapter đầy đủ.");
            builder.append("\n").append(t("requirements")).append(": Cần endpoint, API key nếu có, quota, mapper request/response và xử lý lỗi mạng.");
        }
        builder.append("\n").append(t("usable_now")).append(": ").append(implemented ? t("yes") : t("no"));
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

    private void addLocalDirectories(List<ModelChoice> choices, String relativePath, String prefix) {
        File parent = new File(AppStorage.externalFilesDir(this), relativePath);
        File[] children = parent.listFiles(File::isDirectory);
        if (children == null) {
            return;
        }
        Arrays.sort(children, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File child : children) {
            choices.add(new ModelChoice(prefix + child.getName(), LOCAL_PREFIX + child.getAbsolutePath()));
        }
    }

    private void addLocalFiles(List<ModelChoice> choices, String relativePath, String suffix, String prefix) {
        File parent = new File(AppStorage.externalFilesDir(this), relativePath);
        File[] children = parent.listFiles(file -> file.isFile() && file.getName().toLowerCase(Locale.US).endsWith(suffix));
        if (children == null) {
            return;
        }
        Arrays.sort(children, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        for (File child : children) {
            choices.add(new ModelChoice(prefix + child.getName(), LOCAL_PREFIX + child.getAbsolutePath()));
        }
    }

    static String preferenceKey(String tab, int languageIndex) {
        return "preferred_" + tab + "_language_" + languageIndex;
    }

    private void updateTabs() {
        styleTab(sttTab, TAB_STT.equals(activeTab));
        styleTab(translationTab, TAB_TRANSLATION.equals(activeTab));
        styleTab(ttsTab, TAB_TTS.equals(activeTab));
        styleTab(assistantTab, TAB_ASSISTANT.equals(activeTab));
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
        if (TAB_TRANSLATION.equals(tab)) {
            return t("tab_translation");
        }
        if (TAB_TTS.equals(tab)) {
            return t("tab_tts");
        }
        if (TAB_ASSISTANT.equals(tab)) {
            return t("assistant");
        }
        return t("tab_stt");
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

    private String t(String key) {
        switch (key) {
            case "settings":
                return "C\u00e0i \u0111\u1eb7t";
            case "settings_hint":
                return "Ch\u1ecdn model \u01b0u ti\u00ean cho t\u1eebng t\u00e1c v\u1ee5 v\u00e0 t\u1eebng ng\u00f4n ng\u1eef.";
            case "preferred_model":
                return "Model \u01b0u ti\u00ean";
            case "speech_recognition":
                return "Nh\u1eadn d\u1ea1ng gi\u1ecdng n\u00f3i";
            case "text_recognition":
                return "Nh\u1eadn d\u1ea1ng ch\u1eef vi\u1ebft";
            case "settings_model_options":
                return "l\u1ef1a ch\u1ecdn model.";
            case "settings_no_model":
                return "Ch\u01b0a c\u00f3 model kh\u1ea3 d\u1ee5ng";
            case "model_status":
                return "T\u00ecnh tr\u1ea1ng";
            case "ready":
                return "S\u1eb5n s\u00e0ng";
            case "not_ready":
                return "Ch\u01b0a s\u1eb5n s\u00e0ng";
            case "model_path":
                return "\u0110\u01b0\u1eddng d\u1eabn model";
            case "size":
                return "Dung l\u01b0\u1ee3ng";
            case "runtime":
                return "Runtime";
            case "required_files":
                return "T\u1ec7p/th\u01b0 m\u1ee5c c\u1ea7n c\u00f3";
            case "requirements":
                return "Y\u00eau c\u1ea7u";
            case "details":
                return "Chi ti\u1ebft";
            case "usable_now":
                return "C\u00f3 th\u1ec3 ho\u1ea1t \u0111\u1ed9ng ngay";
            case "yes":
                return "C\u00f3";
            case "no":
                return "Kh\u00f4ng";
            case "require_select_model":
                return "H\u00e3y ch\u1ecdn m\u1ed9t model kh\u1ea3 d\u1ee5ng.";
            case "require_model_file_exists":
                return "C\u1ea7n t\u1ea3i/copy model v\u00e0o \u0111\u00fang \u0111\u01b0\u1eddng d\u1eabn tr\u01b0\u1edbc khi s\u1eed d\u1ee5ng.";
            default:
                return LocalizedText.get(uiLanguageCode, key);
        }
    }

    private String buttonLabel(String key) {
        return LocalizedText.button(uiLanguageCode, key);
    }

    private static class ModelChoice {
        final String label;
        final String value;

        ModelChoice(String label, String value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
