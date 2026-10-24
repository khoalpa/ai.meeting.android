package com.example.aimeeting;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.k2fsa.sherpa.onnx.GenerationConfig;
import com.k2fsa.sherpa.onnx.OfflineTts;
import com.k2fsa.sherpa.onnx.OfflineTtsConfig;
import com.k2fsa.sherpa.onnx.TtsKt;
import com.llamatik.library.platform.LlamaBridge;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressLint("SetTextI18n")
public class AssistantActivity extends Activity {
    private static final String TAG = "AIMeeting";
    private static final int REQUEST_AUDIO = 62;
    private static final float SAMPLE_RATE = 16000.0f;
    private static final String ASSISTANT_MODEL_NAME = "qwen2.5-1.5b-instruct-q4_k_m.gguf";
    private static final String FALLBACK_ASSISTANT_MODEL_NAME = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf";
    private static final String TTS_MODEL_NAME = TtsModelCatalog.SUPERTONIC_MODEL_NAME;
    private static final int LLM_TIMEOUT_SECONDS = 120;
    private static final int LOCAL_MAX_OUTPUT_TOKENS = 768;
    private static final int LOCAL_CONTEXT_TOKENS = 2048;
    private static final int MAX_CONTINUATION_ATTEMPTS = 2;
    private static final int MEMORY_MAX_CHARS = 4000;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService llmExecutor = Executors.newSingleThreadExecutor();
    private LinearLayout rootLayout;
    private LinearLayout headerLayout;
    private ScrollView chatScroll;
    private LinearLayout chatLayout;
    private LinearLayout composerLayout;
    private TextView statusText;
    private TextView assistantModelStatusText;
    private TextView ttsModelStatusText;
    private EditText inputText;
    private Button listenButton;
    private Button sendButton;
    private Button clearButton;
    private Button stopSpeechButton;
    private Spinner assistantModelSpinner;
    private Spinner ttsModelSpinner;
    private final List<AssistantModelChoice> assistantModelChoices = new ArrayList<>();
    private final List<TtsModelChoice> ttsModelChoices = new ArrayList<>();
    private AssistantModelChoice selectedAssistantModelChoice;
    private TtsModelChoice selectedTtsModelChoice;
    private String languageName;
    private String languageCode;
    private SharedPreferences memory;
    private MediaPlayer mediaPlayer;
    private String loadedAssistantModelPath;
    private volatile boolean voiceModeEnabled = false;
    private volatile boolean assistantBusy;
    private volatile boolean speechBusy;
    private volatile int assistantGenerationToken;
    private int speechPlaybackToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        languageName = getIntent().getStringExtra("language_name");
        languageCode = getIntent().getStringExtra("language_code");
        if (languageName == null) {
            languageName = "Tiếng Việt";
        }
        if (languageCode == null) {
            languageCode = "vi";
        }
        memory = getSharedPreferences("assistant_memory_" + languageCode, MODE_PRIVATE);
        buildUi();
        loadConversation();
    }

    @SuppressWarnings("deprecation")
    private void configureSoftInputMode() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    protected void onStop() {
        if (voiceModeEnabled) {
            stopVoiceMode();
        }
        if (assistantBusy) {
            cancelAssistantGeneration(false);
        }
        if (speechBusy) {
            stopSpeechPlayback(false);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        llmExecutor.shutdownNow();
        try {
            LlamaBridge.INSTANCE.nativeCancelGenerate();
            LlamaBridge.INSTANCE.shutdown();
        } catch (Throwable ignored) {
        }
        stopSpeechPlayback(false);
        super.onDestroy();
    }

    private void buildUi() {
        configureSoftInputMode();
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundResource(R.drawable.app_background);

        headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.applyShell(this, rootLayout, headerLayout);
        headerLayout.setPadding(dp(20), dp(16), dp(20), dp(12));
        rootLayout.addView(headerLayout, new LinearLayout.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText(t("assistant"));
        title.setTextColor(getColor(R.color.meeting_text));
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        headerLayout.addView(title);

        statusText = new TextView(this);
        statusText.setText(t("language_1") + ": " + languageName + ". " + t("assistant_stores_context"));
        statusText.setTextColor(getColor(R.color.meeting_muted));
        statusText.setTextSize(14);
        statusText.setMaxLines(2);
        headerLayout.addView(statusText);
        setupAssistantModelPicker();
        setupTtsModelPicker();
        addModelPickerPanel();

        chatScroll = new ScrollView(this);
        chatScroll.setFillViewport(true);
        chatScroll.setClipToPadding(false);
        chatLayout = new LinearLayout(this);
        chatLayout.setOrientation(LinearLayout.VERTICAL);
        chatLayout.setPadding(dp(16), dp(12), dp(16), dp(12));
        chatScroll.addView(chatLayout);
        rootLayout.addView(chatScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout composer = new LinearLayout(this);
        composerLayout = composer;
        composer.setOrientation(LinearLayout.VERTICAL);
        composer.setPadding(dp(16), dp(10), dp(16), dp(10));
        composer.setBackgroundResource(R.drawable.premium_header);
        rootLayout.addView(composer, new LinearLayout.LayoutParams(-1, -2));

        inputText = new EditText(this);
        inputText.setMinLines(2);
        inputText.setMaxLines(4);
        inputText.setHint(t("manual_label") + " / " + t("listening"));
        inputText.setTextColor(getColor(R.color.meeting_text));
        inputText.setHintTextColor(getColor(R.color.meeting_muted));
        inputText.setBackgroundResource(R.drawable.panel_background);
        inputText.setPadding(dp(12), dp(10), dp(12), dp(10));
        composer.addView(inputText, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);
        composer.addView(actions);

        listenButton = actionButton(buttonLabel("start_listening"));
        listenButton.setOnClickListener(v -> toggleVoiceMode());
        actions.addView(listenButton, weightedParams(1, 0));
        updateVoiceButton();

        sendButton = actionButton(buttonLabel("send"));
        PremiumUi.primaryIconTextButton(this, sendButton, R.drawable.ic_action_send,
                buttonLabel("send"), t("send"));
        sendButton.setOnClickListener(v -> submitPromptFromInput(false));
        actions.addView(sendButton, weightedParams(1, dp(8)));

        LinearLayout utilityActions = new LinearLayout(this);
        utilityActions.setOrientation(LinearLayout.HORIZONTAL);
        utilityActions.setPadding(0, dp(8), 0, 0);
        composer.addView(utilityActions);

        clearButton = actionButton(buttonLabel("clear_memory"));
        PremiumUi.dangerIconTextButton(this, clearButton, R.drawable.ic_action_trash,
                buttonLabel("clear_memory"), t("clear_memory"));
        clearButton.setOnClickListener(v -> confirmClearMemory());
        utilityActions.addView(clearButton, weightedParams(1, 0));

        stopSpeechButton = actionButton(buttonLabel("stop_speech"));
        PremiumUi.secondaryIconTextButton(this, stopSpeechButton, R.drawable.ic_action_stop,
                buttonLabel("stop_speech"), t("stop_speech"));
        stopSpeechButton.setOnClickListener(v -> stopSpeechPlayback(true));
        utilityActions.addView(stopSpeechButton, weightedParams(1, dp(8)));
        updateStopSpeechButton();

        PremiumUi.addBottomNavigation(this, rootLayout, "assistant", languageCode,
                languageName, languageCode, "English", "en");
        setContentView(rootLayout);
        applySystemBarInsets();
    }

    private void applySystemBarInsets() {
        int baseHeaderTop = headerLayout.getPaddingTop();
        int baseBottom = chatScroll.getPaddingBottom();
        int baseComposerBottom = composerLayout.getPaddingBottom();
        rootLayout.setOnApplyWindowInsetsListener((view, insets) -> {
            int systemBottom = InsetsCompat.systemBarBottom(insets);
            int keyboardBottom = 0;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                keyboardBottom = insets.getInsets(WindowInsets.Type.ime()).bottom;
            }
            int bottomInset = Math.max(systemBottom, keyboardBottom);
            headerLayout.setPadding(headerLayout.getPaddingLeft(), baseHeaderTop + InsetsCompat.systemBarTop(insets),
                    headerLayout.getPaddingRight(), headerLayout.getPaddingBottom());
            chatScroll.setPadding(chatScroll.getPaddingLeft(), chatScroll.getPaddingTop(),
                    chatScroll.getPaddingRight(), baseBottom + bottomInset);
            composerLayout.setPadding(composerLayout.getPaddingLeft(), composerLayout.getPaddingTop(),
                    composerLayout.getPaddingRight(), baseComposerBottom + bottomInset);
            return insets;
        });
        rootLayout.requestApplyInsets();
        inputText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                chatScroll.postDelayed(() -> chatScroll.fullScroll(View.FOCUS_DOWN), 250);
            }
        });
    }

    private void addModelPickerPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.elevateCard(this, panel);
        panel.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(-1, -2);
        panelParams.setMargins(0, dp(10), 0, 0);
        headerLayout.addView(panel, panelParams);

        assistantModelStatusText = modelStatusText();
        panel.addView(assistantModelStatusText, new LinearLayout.LayoutParams(-1, -2));

        assistantModelSpinner = new Spinner(this);
        ArrayAdapter<AssistantModelChoice> assistantAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, assistantModelChoices);
        assistantAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        assistantModelSpinner.setAdapter(assistantAdapter);
        assistantModelSpinner.setSelection(Math.max(0, assistantModelChoices.indexOf(selectedAssistantModelChoice)));
        assistantModelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAssistantModelChoice = assistantModelChoices.get(position);
                updateModelStatus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        panel.addView(assistantModelSpinner, new LinearLayout.LayoutParams(-1, dp(44)));

        ttsModelStatusText = modelStatusText();
        LinearLayout.LayoutParams ttsStatusParams = new LinearLayout.LayoutParams(-1, -2);
        ttsStatusParams.setMargins(0, dp(8), 0, 0);
        panel.addView(ttsModelStatusText, ttsStatusParams);

        ttsModelSpinner = new Spinner(this);
        ArrayAdapter<TtsModelChoice> ttsAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ttsModelChoices);
        ttsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ttsModelSpinner.setAdapter(ttsAdapter);
        ttsModelSpinner.setSelection(Math.max(0, ttsModelChoices.indexOf(selectedTtsModelChoice)));
        ttsModelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTtsModelChoice = ttsModelChoices.get(position);
                updateModelStatus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        panel.addView(ttsModelSpinner, new LinearLayout.LayoutParams(-1, dp(44)));

        updateModelStatus();
    }

    private TextView modelStatusText() {
        TextView text = new TextView(this);
        text.setTextColor(getColor(R.color.meeting_primary_dark));
        text.setTextSize(13);
        text.setTypeface(null, Typeface.BOLD);
        return text;
    }

    private void updateModelStatus() {
        if (assistantModelStatusText != null) {
            AssistantModelChoice assistant = getSelectedAssistantModel();
            boolean ready = assistant != null && assistant.file != null && assistant.file.isFile();
            assistantModelStatusText.setText((isVietnameseUi() ? "Model trợ lý: " : "Assistant model: ")
                    + (ready ? (isVietnameseUi() ? "sẵn sàng" : "ready") : (isVietnameseUi() ? "chưa tải" : "not downloaded")));
            assistantModelStatusText.setTextColor(getColor(ready ? R.color.meeting_primary_dark : android.R.color.holo_orange_dark));
        }
        if (ttsModelStatusText != null) {
            TtsModelChoice tts = getSelectedTtsModel();
            boolean ready = tts != null && looksLikeTtsModel(tts.file);
            ttsModelStatusText.setText((isVietnameseUi() ? "Giọng đọc: " : "Voice: ")
                    + (ready ? (isVietnameseUi() ? "sẵn sàng" : "ready") : (isVietnameseUi() ? "chưa tải" : "not downloaded")));
            ttsModelStatusText.setTextColor(getColor(ready ? R.color.meeting_primary_dark : android.R.color.holo_orange_dark));
        }
    }

    private void setupAssistantModelPicker() {
        assistantModelChoices.clear();
        File parent = AppStorage.modelDir(this, "assistant");
        File tiny = new File(parent, FALLBACK_ASSISTANT_MODEL_NAME);
        File qwen = new File(parent, ASSISTANT_MODEL_NAME);
        if (tiny.isFile()) {
            assistantModelChoices.add(new AssistantModelChoice("TinyLlama 1.1B Chat Q4_K_M", tiny));
        }
        if (qwen.isFile()) {
            assistantModelChoices.add(new AssistantModelChoice("Qwen2.5 1.5B Instruct Q4_K_M", qwen));
        }
        File[] others = parent.listFiles(file -> file.isFile() && file.getName().endsWith(".gguf")
                && !file.getName().equals(ASSISTANT_MODEL_NAME)
                && !file.getName().equals(FALLBACK_ASSISTANT_MODEL_NAME));
        if (others != null) {
            Arrays.sort(others, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File file : others) {
                assistantModelChoices.add(new AssistantModelChoice(file.getName(), file));
            }
        }
        if (assistantModelChoices.isEmpty()) {
            assistantModelChoices.add(new AssistantModelChoice(t("assistant_model_missing"), null));
        }

        String preferredPath = preferredLocalPath("assistant");
        for (int i = 0; i < assistantModelChoices.size(); i++) {
            File file = assistantModelChoices.get(i).file;
            if (file != null && file.getAbsolutePath().equals(preferredPath)) {
                selectedAssistantModelChoice = assistantModelChoices.get(i);
                return;
            }
        }
        selectedAssistantModelChoice = assistantModelChoices.get(0);
    }

    private void setupTtsModelPicker() {
        ttsModelChoices.clear();
        File parent = AppStorage.modelDir(this, "tts");
        File vietnameseModel = new File(parent, TtsModelCatalog.VIETNAMESE_VITS_MODEL_NAME);
        if (TtsModelCatalog.looksLikeUsableModel(vietnameseModel)) {
            ttsModelChoices.add(new TtsModelChoice(TtsModelCatalog.displayName(vietnameseModel), vietnameseModel));
        }
        File defaultModel = new File(parent, TTS_MODEL_NAME);
        if (looksLikeTtsModel(defaultModel)) {
            ttsModelChoices.add(new TtsModelChoice(TtsModelCatalog.displayName(defaultModel), defaultModel));
        }
        File[] children = parent.listFiles(File::isDirectory);
        if (children != null) {
            Arrays.sort(children, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            for (File child : children) {
                if (looksLikeTtsModel(child)
                        && !child.getName().equals(TTS_MODEL_NAME)
                        && !child.getName().equals(TtsModelCatalog.VIETNAMESE_VITS_MODEL_NAME)) {
                    ttsModelChoices.add(new TtsModelChoice(TtsModelCatalog.displayName(child), child));
                }
            }
        }
        if (ttsModelChoices.isEmpty()) {
            ttsModelChoices.add(new TtsModelChoice(t("assistant_no_tts"), null));
        }

        String preferredPath = preferredLocalPath("tts");
        for (int i = 0; i < ttsModelChoices.size(); i++) {
            File file = ttsModelChoices.get(i).file;
            if (file != null && file.getAbsolutePath().equals(preferredPath)) {
                selectedTtsModelChoice = ttsModelChoices.get(i);
                return;
            }
        }
        selectedTtsModelChoice = ttsModelChoices.get(0);
    }

    private String preferredLocalPath(String tab) {
        SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        String value = OnlineProviderConfig.safeString(settings, SettingsActivity.preferenceKey(tab, 1), "");
        if (value.startsWith(SettingsActivity.LOCAL_PREFIX)) {
            String path = value.substring(SettingsActivity.LOCAL_PREFIX.length()).trim();
            return path.isEmpty() ? "" : path;
        }
        return "";
    }

    private String preferredModelValue(String tab) {
        SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE);
        return OnlineProviderConfig.safeString(settings, SettingsActivity.preferenceKey(tab, 1), "");
    }

    private void loadConversation() {
        chatLayout.removeAllViews();
        String saved = memory.getString("conversation", "");
        if (saved.trim().isEmpty()) {
            addBubble(t("assistant"), String.format(t("assistant_hello"), languageName));
        } else {
            for (String block : saved.split("\\n\\n")) {
                int separator = block.indexOf(": ");
                if (separator > 0) {
                    addBubble(block.substring(0, separator), block.substring(separator + 2));
                }
            }
        }
    }

    private void toggleVoiceMode() {
        if (voiceModeEnabled) {
            stopVoiceMode();
        } else {
            startVoiceMode();
        }
    }

    private void startVoiceMode() {
        voiceModeEnabled = true;
        updateVoiceButton();
        listenOnce();
    }

    private void stopVoiceMode() {
        voiceModeEnabled = false;
        updateVoiceButton();
        statusText.setText(t("stop_listening"));
        if (assistantBusy) {
            cancelAssistantGeneration(false);
        }
    }

    private void cancelAssistantGeneration(boolean reportStatus) {
        assistantGenerationToken++;
        try {
            LlamaBridge.INSTANCE.nativeCancelGenerate();
        } catch (Throwable ignored) {
        }
        setAssistantBusy(false);
        if (reportStatus) {
            statusText.setText(t("speech_stopped"));
        }
    }

    private void updateVoiceButton() {
        if (listenButton == null) {
            return;
        }
        PremiumUi.primaryIconTextButton(this, listenButton,
                voiceModeEnabled ? R.drawable.ic_action_stop : R.drawable.ic_action_mic,
                voiceModeEnabled ? buttonLabel("stop_listening") : buttonLabel("start_listening"),
                voiceModeEnabled ? t("stop_listening") : t("start_listening"));
        if (voiceModeEnabled) {
            listenButton.setBackgroundResource(R.drawable.recording_button);
        }
    }

    private void submitPromptFromInput(boolean autoContinue) {
        String prompt = inputText.getText().toString().trim();
        if (prompt.isEmpty()) {
            statusText.setText(t("assistant_empty"));
            return;
        }
        inputText.setText("");
        submitPromptText(prompt, autoContinue);
    }

    private void submitPromptText(String prompt, boolean autoContinue) {
        int token = ++assistantGenerationToken;
        setAssistantBusy(true);
        addAndRemember(t("user"), prompt);
        executor.execute(() -> {
            String answer = generateAssistantReplyNative(prompt, token);
            mainHandler.post(() -> {
                if (!isCurrentAssistantGeneration(token)) {
                    return;
                }
                addAndRemember(t("assistant"), answer);
                statusText.setText(t("assistant_answered"));
                speak(answer, () -> {
                    if (!isCurrentAssistantGeneration(token)) {
                        return;
                    }
                    setAssistantBusy(false);
                    if (autoContinue && voiceModeEnabled) {
                        listenOnce();
                    }
                });
            });
        });
    }

    private void setAssistantBusy(boolean busy) {
        assistantBusy = busy;
        if (sendButton != null) {
            sendButton.setEnabled(!busy);
            sendButton.setAlpha(busy ? 0.55f : 1.0f);
        }
        if (clearButton != null) {
            clearButton.setEnabled(!busy);
            clearButton.setAlpha(busy ? 0.55f : 1.0f);
        }
        updateStopSpeechButton();
    }

    private boolean isCurrentAssistantGeneration(int token) {
        return token == assistantGenerationToken;
    }

    private String generateAssistantReplyNative(String prompt, int token) {
        String answer = generateSingleAssistantReplyNative(prompt, token);
        int continuationAttempts = 0;
        while (isCurrentAssistantGeneration(token)
                && !isAssistantGenerationError(answer)
                && AssistantAnswerInspector.isLikelyIncomplete(answer)
                && continuationAttempts < MAX_CONTINUATION_ATTEMPTS) {
            continuationAttempts++;
            int attempt = continuationAttempts;
            mainHandler.post(() -> {
                if (isCurrentAssistantGeneration(token)) {
                    statusText.setText(isVietnameseUi()
                            ? "\u0110ang vi\u1ebft ti\u1ebfp... (" + attempt + "/" + MAX_CONTINUATION_ATTEMPTS + ")"
                            : "Continuing answer... (" + attempt + "/" + MAX_CONTINUATION_ATTEMPTS + ")");
                }
            });
            String continuationPrompt = AssistantAnswerInspector.continuationPrompt(languageName, prompt, answer);
            String continuation = generateSingleAssistantReplyNative(continuationPrompt, token);
            if (continuation.trim().isEmpty() || isAssistantGenerationError(continuation)) {
                break;
            }
            String merged = AssistantAnswerInspector.mergeContinuation(answer, continuation);
            if (merged.equals(answer)) {
                break;
            }
            answer = merged;
        }
        return answer;
    }

    private String generateSingleAssistantReplyNative(String prompt, int token) {
        if (!isCurrentAssistantGeneration(token)) {
            return t("assistant_empty_reply");
        }
        String onlineProvider = preferredModelValue("assistant");
        if (OnlineProviderConfig.isOnlineAssistantProvider(onlineProvider)) {
            try {
                mainHandler.post(() -> {
                    if (isCurrentAssistantGeneration(token)) {
                        statusText.setText("Đang xử lý bằng LLM online...");
                    }
                });
                String answer = OnlineAiClient.generateAssistantReply(this, onlineProvider, languageName,
                        memory.getString("conversation", ""), prompt);
                if (!isCurrentAssistantGeneration(token)) {
                    return t("assistant_empty_reply");
                }
                answer = cleanAssistantAnswer(answer);
                if (answer.isEmpty()) {
                    Log.w(TAG, "Online assistant provider returned an empty reply after cleanup.");
                    return t("assistant_empty_reply");
                }
                return answer;
            } catch (Exception e) {
                Log.e(TAG, "Online assistant provider failed.", e);
                return t("assistant_runtime_error") + ": " + safeMessage(e);
            }
        }
        List<File> models = availableAssistantModels();
        if (models.isEmpty()) {
            return t("assistant_model_missing");
        }
        String lastError = "";
        for (File model : models) {
            if (!isCurrentAssistantGeneration(token)) {
                break;
            }
            try {
                postProcessingModel(model, token);
                String answer = generateWithModelTimeout(model, prompt);
                if (!isAssistantGenerationError(answer)) {
                    return answer;
                }
                lastError = answer;
                Log.w(TAG, "Assistant model returned retryable empty/error result: " + model.getName());
            } catch (OutOfMemoryError e) {
                loadedAssistantModelPath = null;
                Log.e(TAG, "Assistant model out of memory: " + model.getName(), e);
                lastError = t("assistant_out_of_memory");
            } catch (TimeoutException e) {
                loadedAssistantModelPath = null;
                Log.e(TAG, "Assistant model timed out: " + model.getName(), e);
                lastError = t("assistant_runtime_error") + ": timeout";
            } catch (Throwable e) {
                loadedAssistantModelPath = null;
                Log.e(TAG, "Assistant model runtime error: " + model.getName(), e);
                lastError = t("assistant_runtime_error") + ": " + e.getClass().getSimpleName();
            }
            try {
                LlamaBridge.INSTANCE.nativeCancelGenerate();
            } catch (Throwable ignored) {
            }
        }
        return lastError.trim().isEmpty() ? t("assistant_empty_reply") : lastError;
    }

    private boolean isAssistantGenerationError(String answer) {
        if (answer == null) {
            return true;
        }
        String trimmed = answer.trim();
        return trimmed.isEmpty()
                || trimmed.equals(t("assistant_empty_reply"))
                || trimmed.equals(t("assistant_model_missing"))
                || trimmed.equals(t("assistant_model_load_failed"))
                || trimmed.equals(t("assistant_out_of_memory"))
                || trimmed.startsWith(t("assistant_runtime_error"));
    }

    private List<File> availableAssistantModels() {
        List<File> result = new ArrayList<>();
        AssistantModelChoice selected = getSelectedAssistantModel();
        if (selected != null && selected.file != null && selected.file.isFile()) {
            result.add(selected.file);
        }
        for (AssistantModelChoice choice : assistantModelChoices) {
            if (choice.file != null && choice.file.isFile() && !result.contains(choice.file)) {
                result.add(choice.file);
            }
        }
        return result;
    }

    private AssistantModelChoice getSelectedAssistantModel() {
        return selectedAssistantModelChoice != null ? selectedAssistantModelChoice
                : (assistantModelChoices.isEmpty() ? null : assistantModelChoices.get(0));
    }

    private TtsModelChoice getSelectedTtsModel() {
        return selectedTtsModelChoice != null ? selectedTtsModelChoice
                : (ttsModelChoices.isEmpty() ? null : ttsModelChoices.get(0));
    }

    private void postProcessingModel(File model, int token) {
        String message = "Đang xử lý bằng model '" + modelDisplayName(model) + "'";
        mainHandler.post(() -> {
            if (isCurrentAssistantGeneration(token)) {
                statusText.setText(message);
            }
        });
    }

    private String modelDisplayName(File model) {
        for (AssistantModelChoice choice : assistantModelChoices) {
            if (choice.file != null && choice.file.equals(model)) {
                return choice.label;
            }
        }
        return model.getName();
    }

    private String generateWithModelTimeout(File model, String prompt) throws Exception {
        Future<String> future = llmExecutor.submit(() -> generateWithLoadedModel(model, prompt));
        try {
            return future.get(LLM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            try {
                LlamaBridge.INSTANCE.nativeCancelGenerate();
            } catch (Throwable ignored) {
            }
            throw e;
        }
    }

    private String generateWithLoadedModel(File model, String prompt) {
        try {
            String modelPath = model.getAbsolutePath();
            if (!modelPath.equals(loadedAssistantModelPath)) {
                int threads = AcceleratorConfig.llmThreads();
                int batchOrGpuLayers = AcceleratorConfig.llmGpuLayers() > 0 ? -AcceleratorConfig.llmGpuLayers() : 512;
                LlamaBridge.INSTANCE.updateGenerateParams(0.6f, LOCAL_MAX_OUTPUT_TOKENS, 0.9f, 40, 1.1f,
                        LOCAL_CONTEXT_TOKENS, threads, true, false, batchOrGpuLayers);
                boolean ok = LlamaBridge.INSTANCE.initGenerateModel(modelPath);
                if (!ok) {
                    loadedAssistantModelPath = null;
                    Log.e(TAG, "Assistant model load failed: " + model.getName());
                    return t("assistant_model_load_failed");
                }
                loadedAssistantModelPath = modelPath;
            }

            String context = memory.getString("conversation", "");
            String systemPrompt = "You are an offline meeting assistant running directly on this Android device. "
                    + "Answer in " + languageName + ". Keep answers concise, helpful, and conversational. "
                    + "Use the conversation context when relevant. Do not mention cloud services.";
            String answer = generateWithStrategy(model, "chat_template",
                    () -> generateWithChatTemplate(systemPrompt, context, prompt));
            if (!answer.isEmpty()) {
                return answer;
            }

            answer = generateWithStrategy(model, "legacy_context",
                    () -> LlamaBridge.INSTANCE.generateWithContext(systemPrompt, context, prompt));
            if (!answer.isEmpty()) {
                return answer;
            }

            String plainPrompt = AssistantPromptFormatter.buildPlainPrompt(systemPrompt, context, prompt);
            answer = generateWithStrategy(model, "plain_instruction",
                    () -> LlamaBridge.INSTANCE.generate(plainPrompt));
            if (!answer.isEmpty()) {
                return answer;
            }

            Log.w(TAG, "Assistant model generated empty replies for all prompt strategies: " + model.getName());
            return t("assistant_empty_reply");
        } catch (OutOfMemoryError e) {
            loadedAssistantModelPath = null;
            throw e;
        } catch (Throwable e) {
            loadedAssistantModelPath = null;
            Log.e(TAG, "Assistant local generation failed.", e);
            throw new RuntimeException(e);
        }
    }

    private String generateWithStrategy(File model, String strategyName, AssistantRawGenerator generator) {
        String rawAnswer = null;
        String answer = "";
        try {
            rawAnswer = generator.generate();
            answer = cleanAssistantAnswer(rawAnswer);
            logAssistantStrategyResult(model, strategyName, rawAnswer, answer);
            return answer;
        } catch (OutOfMemoryError e) {
            throw e;
        } catch (Throwable e) {
            Log.w(TAG, "Assistant generation strategy failed: model=" + model.getName()
                    + ", strategy=" + strategyName + ", error=" + e.getClass().getSimpleName(), e);
            logAssistantStrategyResult(model, strategyName, rawAnswer, answer);
            return "";
        }
    }

    private void logAssistantStrategyResult(File model, String strategyName, String rawAnswer, String answer) {
        Log.i(TAG, "Assistant generation strategy result: model=" + model.getName()
                + ", strategy=" + strategyName
                + ", rawLength=" + lengthOf(rawAnswer)
                + ", cleanedLength=" + lengthOf(answer));
    }

    private int lengthOf(String value) {
        return value == null ? 0 : value.length();
    }

    private String generateWithChatTemplate(String systemPrompt, String context, String prompt) {
        try {
            String templatedPrompt = LlamaBridge.INSTANCE.applyChatTemplate(
                    AssistantPromptFormatter.buildMessages(systemPrompt, context, prompt),
                    true);
            if (templatedPrompt == null || templatedPrompt.trim().isEmpty()) {
                Log.w(TAG, "Assistant model returned an empty chat template.");
                return null;
            }
            return LlamaBridge.INSTANCE.generate(templatedPrompt);
        } catch (Throwable e) {
            Log.w(TAG, "Assistant chat template generation failed.", e);
            return null;
        }
    }

    private interface AssistantRawGenerator {
        String generate();
    }

    private String cleanAssistantAnswer(String rawAnswer) {
        return AssistantPromptFormatter.cleanAnswer(rawAnswer);
    }

    private String cleanAssistantAnswerLegacy(String rawAnswer) {
        if (rawAnswer == null) {
            return "";
        }
        String answer = rawAnswer.replace("\r\n", "\n").replace('\r', '\n');
        String lower = answer.toLowerCase(Locale.ROOT);
        String[] stopMarkers = {
                "<end_of_turn>",
                "<start_of_turn>",
                "<|endoftext|>",
                "<|eot_id|>",
                "<|im_end|>",
                "</s>",
                "\nquestion:",
                "\nuser:",
                "\nassistant:",
                "\nbạn:",
                "\ntrợ lý:"
        };
        int cutAt = -1;
        for (String marker : stopMarkers) {
            int index = lower.indexOf(marker);
            if (index >= 0 && (cutAt < 0 || index < cutAt)) {
                cutAt = index;
            }
        }
        if (cutAt >= 0) {
            answer = answer.substring(0, cutAt);
        }
        answer = answer
                .replace("<end_of_turn>", "")
                .replace("<start_of_turn>user", "")
                .replace("<start_of_turn>assistant", "")
                .replace("<start_of_turn>", "")
                .replace("<|endoftext|>", "")
                .replace("<|eot_id|>", "")
                .replace("<|im_end|>", "")
                .replace("</s>", "");
        return answer.trim();
    }

    private void listenOnce() {
        if (!voiceModeEnabled || assistantBusy) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO);
            return;
        }
        statusText.setText(t("assistant_listening"));
        executor.execute(() -> {
            try {
                List<short[]> audio = recordAudio();
                if (!voiceModeEnabled) {
                    return;
                }
                String sttProvider = preferredModelValue("stt");
                String text;
                if (OnlineProviderConfig.isOnlineSttProvider(sttProvider)) {
                    text = OnlineAiClient.transcribePcm(this, sttProvider, audio, SAMPLE_RATE, languageCode);
                } else {
                    File modelDir = new File(AppStorage.modelDir(this, "stt"), defaultSttModelName());
                    if (!modelDir.isDirectory()) {
                        mainHandler.post(() -> statusText.setText(String.format(t("assistant_no_stt_model"), languageName)));
                        return;
                    }
                    text = recognize(modelDir, audio);
                }
                mainHandler.post(() -> {
                    if (!voiceModeEnabled) {
                        return;
                    }
                    inputText.setText(text);
                    statusText.setText(text.isEmpty() ? t("assistant_no_speech") : t("assistant_speech_recognized"));
                    if (voiceModeEnabled && !text.isEmpty()) {
                        inputText.setText("");
                        submitPromptText(text, true);
                    } else if (voiceModeEnabled) {
                        mainHandler.postDelayed(this::listenOnce, 500);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    statusText.setText(t("assistant_listen_error") + ": " + safeMessage(e));
                    if (voiceModeEnabled) {
                        mainHandler.postDelayed(this::listenOnce, 800);
                    }
                });
            }
        });
    }

    private List<short[]> recordAudio() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("RECORD_AUDIO permission is not granted.");
        }
        List<short[]> chunks = new ArrayList<>();
        int minBufferSize = AudioRecord.getMinBufferSize((int) SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(minBufferSize, 4096);
        short[] buffer = new short[bufferSize / 2];
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, (int) SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        try {
            recorder.startRecording();
            long startedAt = System.currentTimeMillis();
            while (voiceModeEnabled && System.currentTimeMillis() - startedAt < 5000) {
                int read = recorder.read(buffer, 0, buffer.length);
                if (read > 0) {
                    chunks.add(Arrays.copyOf(buffer, read));
                }
            }
        } finally {
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop();
            }
            recorder.release();
        }
        return chunks;
    }

    private String recognize(File modelDir, List<short[]> chunks) throws IOException {
        Model model = new Model(modelDir.getAbsolutePath());
        try (Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {
            for (short[] chunk : chunks) {
                recognizer.acceptWaveForm(chunk, chunk.length);
            }
            return new JSONObject(recognizer.getFinalResult()).optString("text", "").trim();
        } catch (JSONException e) {
            return "";
        } finally {
            model.close();
        }
    }

    private void speak(String text, Runnable afterPlayback) {
        int token = ++speechPlaybackToken;
        speechBusy = true;
        updateStopSpeechButton();
        executor.execute(() -> {
            try {
                String preferredTts = preferredModelValue("tts");
                if (OnlineProviderConfig.isOnlineTtsProvider(preferredTts)) {
                    File audioFile = OnlineAiClient.synthesizeSpeech(this, preferredTts, text, languageCode);
                    mainHandler.post(() -> {
                        if (isCurrentSpeechToken(token)) {
                            play(audioFile, afterPlayback, token);
                        }
                    });
                    return;
                }
                TtsModelChoice selectedTts = getSelectedTtsModel();
                File modelDir = selectedTts == null ? null : selectedTts.file;
                if (!looksLikeTtsModel(modelDir)) {
                    mainHandler.post(() -> {
                        if (!isCurrentSpeechToken(token)) {
                            return;
                        }
                        speechBusy = false;
                        updateStopSpeechButton();
                        statusText.setText(t("assistant_no_tts"));
                        if (afterPlayback != null) {
                            afterPlayback.run();
                        }
                    });
                    return;
                }
                OfflineTts tts = createPreferredTts(modelDir);
                GeneratedAudio audio;
                try {
                    GenerationConfig generationConfig = new GenerationConfig();
                    generationConfig.setExtra(java.util.Collections.singletonMap("lang", languageCode));
                    audio = tts.generateWithConfig(text, generationConfig);
                } catch (Throwable e) {
                    tts.release();
                    if (!AcceleratorConfig.prefersNnapi()) {
                        throw new RuntimeException(e);
                    }
                    tts = createTts(modelDir, AcceleratorConfig.CPU_PROVIDER);
                    GenerationConfig generationConfig = new GenerationConfig();
                    generationConfig.setExtra(java.util.Collections.singletonMap("lang", languageCode));
                    audio = tts.generateWithConfig(text, generationConfig);
                }
                File wav = new File(getFilesDir(), "assistant-reply.wav");
                boolean saved = audio.getSamples().length > 0 && audio.save(wav.getAbsolutePath());
                tts.release();
                if (saved) {
                    mainHandler.post(() -> {
                        if (isCurrentSpeechToken(token)) {
                            play(wav, afterPlayback, token);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        if (!isCurrentSpeechToken(token)) {
                            return;
                        }
                        speechBusy = false;
                        updateStopSpeechButton();
                        if (afterPlayback != null) {
                            afterPlayback.run();
                        }
                    });
                }
            } catch (Throwable e) {
                mainHandler.post(() -> {
                    if (!isCurrentSpeechToken(token)) {
                        return;
                    }
                    speechBusy = false;
                    updateStopSpeechButton();
                    statusText.setText(t("assistant_tts_error") + ": " + e.getClass().getSimpleName());
                    if (afterPlayback != null) {
                        afterPlayback.run();
                    }
                });
            }
        });
    }

    private OfflineTts createTts(File modelDir, String provider) {
        OfflineTtsConfig config = TtsModelCatalog.createConfig(modelDir, provider, AcceleratorConfig.ttsThreads());
        return new OfflineTts(null, config);
    }

    private OfflineTts createPreferredTts(File modelDir) {
        String provider = AcceleratorConfig.preferredOnnxProvider();
        try {
            return createTts(modelDir, provider);
        } catch (Throwable e) {
            if (AcceleratorConfig.CPU_PROVIDER.equals(provider)) {
                throw new RuntimeException(e);
            }
            return createTts(modelDir, AcceleratorConfig.CPU_PROVIDER);
        }
    }

    private boolean isCurrentSpeechToken(int token) {
        return token == speechPlaybackToken;
    }

    private void play(File wav, Runnable afterPlayback, int token) {
        if (!isCurrentSpeechToken(token)) {
            return;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, Uri.fromFile(wav));
        if (mediaPlayer == null) {
            speechBusy = false;
            updateStopSpeechButton();
            if (afterPlayback != null) {
                afterPlayback.run();
            }
            return;
        }
        mediaPlayer.setOnCompletionListener(player -> {
            if (!isCurrentSpeechToken(token)) {
                return;
            }
            player.release();
            if (mediaPlayer == player) {
                mediaPlayer = null;
            }
            speechBusy = false;
            updateStopSpeechButton();
            if (afterPlayback != null) {
                afterPlayback.run();
            }
        });
        mediaPlayer.setOnErrorListener((player, what, extra) -> {
            if (!isCurrentSpeechToken(token)) {
                return true;
            }
            player.release();
            if (mediaPlayer == player) {
                mediaPlayer = null;
            }
            speechBusy = false;
            updateStopSpeechButton();
            if (afterPlayback != null) {
                afterPlayback.run();
            }
            return true;
        });
        mediaPlayer.start();
    }

    private void stopSpeechPlayback(boolean reportStatus) {
        speechPlaybackToken++;
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (IllegalStateException ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        speechBusy = false;
        updateStopSpeechButton();
        if (reportStatus) {
            statusText.setText(t("speech_stopped"));
            setAssistantBusy(false);
            if (voiceModeEnabled) {
                listenOnce();
            }
        }
    }

    private void updateStopSpeechButton() {
        if (stopSpeechButton == null) {
            return;
        }
        stopSpeechButton.setEnabled(speechBusy);
        stopSpeechButton.setAlpha(speechBusy ? 1.0f : 0.45f);
    }

    private void addAndRemember(String speaker, String text) {
        addBubble(speaker, text);
        String old = memory.getString("conversation", "");
        String updated = old + (old.isEmpty() ? "" : "\n\n") + speaker + ": " + text;
        memory.edit().putString("conversation", trimMemory(updated)).apply();
    }

    private void addBubble(String speaker, String text) {
        boolean user = isUserSpeaker(speaker);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(user ? Gravity.END : Gravity.START);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, 0, 0, dp(10));
        chatLayout.addView(row, rowParams);

        TextView label = new TextView(this);
        label.setText(speaker);
        label.setTextColor(getColor(user ? R.color.meeting_primary_dark : R.color.meeting_muted));
        label.setTextSize(12);
        label.setTypeface(null, Typeface.BOLD);
        row.addView(label, new LinearLayout.LayoutParams(-2, -2));

        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextColor(getColor(R.color.meeting_text));
        bubble.setTextSize(16);
        bubble.setBackgroundResource(user ? R.drawable.chat_bubble_user : R.drawable.chat_bubble_assistant);
        bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
        bubble.setMaxWidth(getResources().getDisplayMetrics().widthPixels - dp(56));
        LinearLayout.LayoutParams bubbleParams = new LinearLayout.LayoutParams(-2, -2);
        bubbleParams.setMargins(0, dp(4), 0, 0);
        row.addView(bubble, bubbleParams);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private boolean isUserSpeaker(String speaker) {
        return speaker != null && (speaker.equals(t("user")) || LocalizedText.matchesAnyLanguage("user", speaker));
    }

    private void clearMemory() {
        memory.edit().clear().apply();
        loadConversation();
        statusText.setText(t("assistant_memory_cleared"));
    }

    private void confirmClearMemory() {
        new android.app.AlertDialog.Builder(this)
                .setTitle(isVietnameseUi() ? "Xóa ngữ cảnh trợ lý?" : "Clear assistant memory?")
                .setMessage(isVietnameseUi()
                        ? "Cuộc trò chuyện đã lưu trên máy sẽ bị xóa."
                        : "The conversation context stored on this device will be cleared.")
                .setNegativeButton(isVietnameseUi() ? "Hủy" : "Cancel", null)
                .setPositiveButton(isVietnameseUi() ? "Xóa" : "Clear", (dialog, which) -> clearMemory())
                .show();
    }

    private String trimMemory(String value) {
        return value.length() <= MEMORY_MAX_CHARS ? value : value.substring(value.length() - MEMORY_MAX_CHARS);
    }

    private String defaultSttModelName() {
        switch (languageCode) {
            case "en": return "vosk-model-en-us-0.22";
            case "ja": return "vosk-model-ja-0.22";
            case "ko": return "vosk-model-small-ko-0.22";
            case "zh": return "vosk-model-cn-0.22";
            case "fr": return "vosk-model-fr-0.22";
            case "de": return "vosk-model-de-0.21";
            case "es": return "vosk-model-es-0.42";
            default: return "vosk-model-vn-0.4";
        }
    }

    private boolean looksLikeTtsModel(File dir) {
        return TtsModelCatalog.looksLikeUsableModel(dir);
    }

    private boolean isVietnameseUi() {
        return "vi".equals(languageCode);
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
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

    private String safeMessage(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && voiceModeEnabled) {
            listenOnce();
        }
    }

    private String t(String key) {
        return LocalizedText.get(languageCode, key);
    }

    private String buttonLabel(String key) {
        return LocalizedText.button(languageCode, key);
    }

    private static class AssistantModelChoice {
        final String label;
        final File file;

        AssistantModelChoice(String label, File file) {
            this.label = label;
            this.file = file;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class TtsModelChoice {
        final String label;
        final File file;

        TtsModelChoice(String label, File file) {
            this.label = label;
            this.file = file;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
