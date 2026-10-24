package com.example.aimeeting;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.k2fsa.sherpa.onnx.GenerationConfig;
import com.k2fsa.sherpa.onnx.OfflineTts;
import com.k2fsa.sherpa.onnx.OfflineTtsConfig;
import com.k2fsa.sherpa.onnx.TtsKt;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressLint({"SetTextI18n", "UseSwitchCompatOrMaterialCode"})
public class MainActivity extends Activity implements RecognitionListener {
    private static final String TAG = "AIMeeting";
    private static final int REQUEST_RECORD_AUDIO = 41;
    private static final int REQUEST_OPEN_FILE = 42;
    private static final int REQUEST_CAPTURE_PHOTO = 43;
    private static final float VOSK_SAMPLE_RATE = 16000.0f;
    private static final int MAX_RECOGNITION_RETRIES = 1;
    private static final long MAX_MEDIA_TRANSCRIPTION_DURATION_US = 15L * 60L * 1000L * 1000L;
    private static final String VOSK_BASE_URL = "https://alphacephei.com/vosk/models/";
    private static final String SUPERTONIC_MODEL_NAME = TtsModelCatalog.SUPERTONIC_MODEL_NAME;
    private static final String SUPERTONIC_MODEL_URL = TtsModelCatalog.SUPERTONIC_MODEL_URL;

    private interface ProgressReporter {
        void report(String phase, long doneBytes, long totalBytes);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final List<MeetingLanguage> languages = new ArrayList<>();
    private final List<UiLanguageChoice> uiLanguageChoices = new ArrayList<>();
    private final List<SttModelChoice> sttModels = new ArrayList<>();
    private final List<TranslationModelChoice> translationModels = new ArrayList<>();
    private final List<TtsVoiceChoice> ttsVoices = new ArrayList<>();

    private Spinner languageOneSpinner;
    private Spinner languageTwoSpinner;
    private Spinner uiLanguageSpinner;
    private Spinner sttModelSpinner;
    private Spinner translationModelSpinner;
    private Spinner ttsVoiceSpinner;
    private Switch autoDetectLanguageSwitch;
    private Button listenButton;
    private ImageButton meetingHeaderButton;
    private ImageButton assistantHeaderButton;
    private Button swapLanguagesButton;
    private Button manageModelsButton;
    private ImageButton manageModelsHeaderButton;
    private ImageButton settingsHeaderButton;
    private ImageButton runtimeHeaderButton;
    private Button openFileButton;
    private Button capturePhotoButton;
    private Button getTranscriptButton;
    private Button translateManualButton;
    private Button speakSourceButton;
    private Button shareSourceButton;
    private Button speakTargetButton;
    private Button shareTargetButton;
    private Button exportSessionButton;
    private Button resetSessionButton;
    private Button stopSpeechButton;
    private EditText manualTranscriptInput;
    private TextView appTitleText;
    private TextView commandCenterTitleText;
    private TextView commandCenterSubtitleText;
    private TextView stepLanguageText;
    private TextView stepReadinessText;
    private TextView stepCaptureText;
    private TextView meetingStateText;
    private TextView readinessSummaryText;
    private TextView modelModeText;
    private TextView readinessTitleText;
    private TextView sttReadinessText;
    private TextView translationReadinessText;
    private TextView ttsReadinessText;
    private TextView timelineTitleText;
    private TextView timelineEmptyText;
    private TextView manualTranscriptLabel;
    private TextView statusText;
    private TextView modelHelpText;
    private TextView sourceLanguageText;
    private TextView targetLanguageText;
    private TextView sourceText;
    private TextView targetText;
    private LinearLayout timelineListLayout;

    private LanguageIdentifier languageIdentifier;
    private ModelDownloadManager modelDownloadManager;
    private MeetingSessionStore meetingSessionStore;
    private SpeechService speechService;
    private SpeechRecognizer googleSpeechRecognizer;
    private TextToSpeech androidTts;
    private AudioRecord audioRecord;
    private Thread recognitionThread;
    private Model activeVoskModel;
    private File activeVoskModelDir;
    private OfflineTts internalTts;
    private File internalTtsModelDir;
    private String internalTtsProvider;
    private MediaPlayer mediaPlayer;

    private volatile boolean isListening;
    private volatile boolean recognitionActive;
    private volatile boolean recognitionInProgress;
    private volatile boolean ttsBusy;
    private int speechPlaybackToken;
    private int recognitionRetryCount;
    private Uri selectedFileUri;
    private String selectedFileMimeType;
    private String lastSourceText = "";
    private MeetingLanguage lastSourceLanguage;
    private String lastTranslatedText = "";
    private MeetingLanguage lastTargetLanguage;
    private String lastAppSpokenText = "";
    private long lastAppSpokenAt;
    private int baseContentTopPadding;
    private int baseScrollBottomPadding;
    private int timelineTurnCount;
    private String selectedUiLanguageCode = "vi";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        modelDownloadManager = new ModelDownloadManager(this, 15000, 30000);
        meetingSessionStore = new MeetingSessionStore(this);
        bindViews();
        applySystemBarInsets();
        seedLanguages();
        setupUiLanguageChoices();
        setupLanguagePickers();
        setupUiLanguagePicker();
        setupLanguageIdentifier();
        scanSttModels();
        setupTranslationModels();
        setupInternalTtsVoices();
        setupActions();
        applyUiLanguage();
        restoreSessionTimeline();
    }

    private void bindViews() {
        languageOneSpinner = findViewById(R.id.languageOneSpinner);
        languageTwoSpinner = findViewById(R.id.languageTwoSpinner);
        uiLanguageSpinner = findViewById(R.id.uiLanguageSpinner);
        sttModelSpinner = findViewById(R.id.sttModelSpinner);
        translationModelSpinner = findViewById(R.id.translationModelSpinner);
        ttsVoiceSpinner = findViewById(R.id.ttsVoiceSpinner);
        autoDetectLanguageSwitch = findViewById(R.id.autoDetectLanguageSwitch);
        listenButton = findViewById(R.id.listenButton);
        meetingHeaderButton = findViewById(R.id.meetingHeaderButton);
        assistantHeaderButton = findViewById(R.id.assistantHeaderButton);
        swapLanguagesButton = findViewById(R.id.swapLanguagesButton);
        manageModelsButton = findViewById(R.id.manageModelsButton);
        manageModelsHeaderButton = findViewById(R.id.manageModelsHeaderButton);
        settingsHeaderButton = findViewById(R.id.settingsHeaderButton);
        runtimeHeaderButton = findViewById(R.id.runtimeHeaderButton);
        openFileButton = findViewById(R.id.openFileButton);
        capturePhotoButton = findViewById(R.id.capturePhotoButton);
        getTranscriptButton = findViewById(R.id.getTranscriptButton);
        translateManualButton = findViewById(R.id.translateManualButton);
        speakSourceButton = findViewById(R.id.speakSourceButton);
        shareSourceButton = findViewById(R.id.shareSourceButton);
        speakTargetButton = findViewById(R.id.speakTargetButton);
        shareTargetButton = findViewById(R.id.shareTargetButton);
        exportSessionButton = findViewById(R.id.exportSessionButton);
        resetSessionButton = findViewById(R.id.resetSessionButton);
        stopSpeechButton = findViewById(R.id.stopSpeechButton);
        manualTranscriptInput = findViewById(R.id.manualTranscriptInput);
        appTitleText = findViewById(R.id.appTitleText);
        commandCenterTitleText = findViewById(R.id.commandCenterTitleText);
        commandCenterSubtitleText = findViewById(R.id.commandCenterSubtitleText);
        stepLanguageText = findViewById(R.id.stepLanguageText);
        stepReadinessText = findViewById(R.id.stepReadinessText);
        stepCaptureText = findViewById(R.id.stepCaptureText);
        meetingStateText = findViewById(R.id.meetingStateText);
        readinessSummaryText = findViewById(R.id.readinessSummaryText);
        modelModeText = findViewById(R.id.modelModeText);
        readinessTitleText = findViewById(R.id.readinessTitleText);
        sttReadinessText = findViewById(R.id.sttReadinessText);
        translationReadinessText = findViewById(R.id.translationReadinessText);
        ttsReadinessText = findViewById(R.id.ttsReadinessText);
        timelineTitleText = findViewById(R.id.timelineTitleText);
        timelineEmptyText = findViewById(R.id.timelineEmptyText);
        manualTranscriptLabel = findViewById(R.id.manualTranscriptLabel);
        statusText = findViewById(R.id.statusText);
        modelHelpText = findViewById(R.id.modelHelpText);
        sourceLanguageText = findViewById(R.id.sourceLanguageText);
        targetLanguageText = findViewById(R.id.targetLanguageText);
        sourceText = findViewById(R.id.sourceText);
        targetText = findViewById(R.id.targetText);
        timelineListLayout = findViewById(R.id.timelineListLayout);
        PremiumUi.styleFooterIconButton(this, meetingHeaderButton, true);
        PremiumUi.styleFooterIconButton(this, assistantHeaderButton, false);
        PremiumUi.styleFooterIconButton(this, manageModelsHeaderButton, false);
        PremiumUi.styleFooterIconButton(this, settingsHeaderButton, false);
        PremiumUi.styleFooterIconButton(this, runtimeHeaderButton, false);
    }

    private void applySystemBarInsets() {
        LinearLayout rootLayout = findViewById(R.id.rootLayout);
        ScrollView rootScroll = findViewById(R.id.rootScroll);
        LinearLayout headerLayout = findViewById(R.id.headerLayout);
        LinearLayout bottomNavLayout = findViewById(R.id.bottomNavLayout);
        PremiumUi.applyShell(this, rootLayout, headerLayout);
        int baseHeaderTopPadding = headerLayout.getPaddingTop();
        int baseBottomNavBottomPadding = bottomNavLayout.getPaddingBottom();
        baseScrollBottomPadding = rootScroll.getPaddingBottom();
        rootLayout.setOnApplyWindowInsetsListener((view, insets) -> {
            headerLayout.setPadding(
                    headerLayout.getPaddingLeft(),
                    baseHeaderTopPadding + InsetsCompat.systemBarTop(insets),
                    headerLayout.getPaddingRight(),
                    headerLayout.getPaddingBottom()
            );
            rootScroll.setPadding(
                    rootScroll.getPaddingLeft(),
                    rootScroll.getPaddingTop(),
                    rootScroll.getPaddingRight(),
                    baseScrollBottomPadding + InsetsCompat.systemBarBottom(insets)
            );
            bottomNavLayout.setPadding(
                    bottomNavLayout.getPaddingLeft(),
                    bottomNavLayout.getPaddingTop(),
                    bottomNavLayout.getPaddingRight(),
                    baseBottomNavBottomPadding + InsetsCompat.systemBarBottom(insets)
            );
            return insets;
        });
        rootLayout.requestApplyInsets();
    }

    private void seedLanguages() {
        languages.add(new MeetingLanguage("Tiếng Việt", "vi", TranslateLanguage.VIETNAMESE, Locale.forLanguageTag("vi-VN")));
        languages.add(new MeetingLanguage("English", "en", TranslateLanguage.ENGLISH, Locale.US));
        languages.add(new MeetingLanguage("日本語", "ja", TranslateLanguage.JAPANESE, Locale.JAPAN));
        languages.add(new MeetingLanguage("한국어", "ko", TranslateLanguage.KOREAN, Locale.KOREA));
        languages.add(new MeetingLanguage("中文", "zh", TranslateLanguage.CHINESE, Locale.SIMPLIFIED_CHINESE));
        languages.add(new MeetingLanguage("Français", "fr", TranslateLanguage.FRENCH, Locale.FRANCE));
        languages.add(new MeetingLanguage("Deutsch", "de", TranslateLanguage.GERMAN, Locale.GERMANY));
        languages.add(new MeetingLanguage("Español", "es", TranslateLanguage.SPANISH, Locale.forLanguageTag("es-ES")));
        languages.add(new MeetingLanguage("ไทย", "th", TranslateLanguage.THAI, Locale.forLanguageTag("th-TH")));
    }

    private void setupUiLanguageChoices() {
        uiLanguageChoices.clear();
        uiLanguageChoices.add(new UiLanguageChoice("\uD83C\uDDFB\uD83C\uDDF3", "vi", "Ti\u1ebfng Vi\u1ec7t"));
        uiLanguageChoices.add(new UiLanguageChoice("\uD83C\uDDFA\uD83C\uDDF8", "en", "English"));
        uiLanguageChoices.add(new UiLanguageChoice("\uD83C\uDDEF\uD83C\uDDF5", "ja", "\u65e5\u672c\u8a9e"));
        uiLanguageChoices.add(new UiLanguageChoice("\uD83C\uDDF0\uD83C\uDDF7", "ko", "\ud55c\uad6d\uc5b4"));
        uiLanguageChoices.add(new UiLanguageChoice("\uD83C\uDDE8\uD83C\uDDF3", "zh", "\u4e2d\u6587"));
        uiLanguageChoices.add(new UiLanguageChoice("\uD83C\uDDEB\uD83C\uDDF7", "fr", "Fran\u00e7ais"));
        uiLanguageChoices.add(new UiLanguageChoice("\uD83C\uDDE9\uD83C\uDDEA", "de", "Deutsch"));
        uiLanguageChoices.add(new UiLanguageChoice("\uD83C\uDDEA\uD83C\uDDF8", "es", "Espa\u00f1ol"));
        uiLanguageChoices.add(new UiLanguageChoice("\uD83C\uDDF9\uD83C\uDDED", "th", "\u0e44\u0e17\u0e22"));
    }

    private void setupLanguagePickers() {
        ArrayAdapter<MeetingLanguage> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageOneSpinner.setAdapter(adapter);
        languageTwoSpinner.setAdapter(adapter);
        languageTwoSpinner.setSelection(1);
        AdapterView.OnItemSelectedListener languageChangeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateReadinessSummary();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        languageOneSpinner.setOnItemSelectedListener(languageChangeListener);
        languageTwoSpinner.setOnItemSelectedListener(languageChangeListener);
    }

    private void setupUiLanguagePicker() {
        ArrayAdapter<UiLanguageChoice> adapter = new ArrayAdapter<UiLanguageChoice>(this,
                android.R.layout.simple_spinner_item, uiLanguageChoices) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return flagView(position);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return flagView(position);
            }

            private View flagView(int position) {
                TextView text = new TextView(MainActivity.this);
                UiLanguageChoice choice = getItem(position);
                text.setText(choice == null ? "" : choice.flag);
                text.setContentDescription(choice == null ? "" : choice.name);
                text.setGravity(Gravity.CENTER);
                text.setTextSize(22);
                text.setSingleLine(true);
                text.setMinHeight(dp(36));
                text.setPadding(0, 0, 0, 0);
                return text;
            }
        };
        uiLanguageSpinner.setAdapter(adapter);
        uiLanguageSpinner.setSelection(uiLanguageIndex(selectedUiLanguageCode));
        uiLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                UiLanguageChoice choice = uiLanguageChoices.get(position);
                if (choice.code.equals(selectedUiLanguageCode)) {
                    return;
                }
                selectedUiLanguageCode = choice.code;
                applyUiLanguage();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupLanguageIdentifier() {
        languageIdentifier = LanguageIdentification.getClient(
                new LanguageIdentificationOptions.Builder()
                        .setConfidenceThreshold(0.20f)
                        .build()
        );
    }

    private void scanSttModels() {
        sttModels.clear();
        File sttDir = AppStorage.modelDir(this, "stt");
        if (!sttDir.exists()) {
            sttDir.mkdirs();
        }

        File[] children = sttDir.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                if (looksLikeVoskModel(child)) {
                    sttModels.add(SttModelChoice.local(child.getName(), child));
                }
            }
        }

        addDownloadableSttModels(sttDir);
        listenButton.setEnabled(true);
        modelHelpText.setText("");
        setStatus(t("ready"));

        ArrayAdapter<SttModelChoice> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sttModels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sttModelSpinner.setAdapter(adapter);
        updateReadinessSummary();
    }

    private void addDownloadableSttModels(File sttDir) {
        addDownloadableSttModel(sttDir, "Tiếng Việt - Vosk accurate", "vi", "vosk-model-vn-0.4");
        addDownloadableSttModel(sttDir, "English - Vosk best", "en", "vosk-model-en-us-0.22");
        addDownloadableSttModel(sttDir, "Japanese - Vosk best", "ja", "vosk-model-ja-0.22");
        addDownloadableSttModel(sttDir, "Chinese - Vosk best", "zh", "vosk-model-cn-0.22");
        addDownloadableSttModel(sttDir, "French - Vosk best", "fr", "vosk-model-fr-0.22");
        addDownloadableSttModel(sttDir, "German - Vosk best", "de", "vosk-model-de-0.21");
        addDownloadableSttModel(sttDir, "Spanish - Vosk best", "es", "vosk-model-es-0.42");
        addDownloadableSttModel(sttDir, "Tiếng Việt - Vosk small", "vi", "vosk-model-small-vn-0.4");
        addDownloadableSttModel(sttDir, "English - Vosk small", "en", "vosk-model-small-en-us-0.15");
        addDownloadableSttModel(sttDir, "日本語 - Vosk small", "ja", "vosk-model-small-ja-0.22");
        addDownloadableSttModel(sttDir, "한국어 - Vosk small", "ko", "vosk-model-small-ko-0.22");
        addDownloadableSttModel(sttDir, "中文 - Vosk small", "zh", "vosk-model-small-cn-0.22");
        addDownloadableSttModel(sttDir, "Français - Vosk small", "fr", "vosk-model-small-fr-0.22");
        addDownloadableSttModel(sttDir, "Deutsch - Vosk small", "de", "vosk-model-small-de-0.15");
        addDownloadableSttModel(sttDir, "Español - Vosk small", "es", "vosk-model-small-es-0.42");
    }

    private void addDownloadableSttModel(File sttDir, String label, String languageCode, String modelName) {
        File targetDir = new File(sttDir, modelName);
        if (looksLikeVoskModel(targetDir)) {
            return;
        }
        sttModels.add(SttModelChoice.downloadable(
                label,
                languageCode,
                modelName,
                targetDir,
                VOSK_BASE_URL + modelName + ".zip"
        ));
    }

    private boolean looksLikeVoskModel(File dir) {
        return dir != null && dir.isDirectory()
                && (new File(dir, "conf").isDirectory()
                || new File(dir, "am").isDirectory()
                || new File(dir, "graph").isDirectory());
    }

    private void setupTranslationModels() {
        translationModels.clear();
        translationModels.add(new TranslationModelChoice("ML Kit on-device - tự tải lần đầu", "mlkit"));
        ArrayAdapter<TranslationModelChoice> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, translationModels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        translationModelSpinner.setAdapter(adapter);
    }

    private void setupInternalTtsVoices() {
        ttsVoices.clear();
        ttsVoices.add(new TtsVoiceChoice("Ti\u1ebfng Vi\u1ec7t - Piper VAIS1000 medium offline", "piper-vi-vais1000", TtsModelCatalog.VIETNAMESE_PIPER_VAIS1000_MODEL_NAME));
        ttsVoices.add(new TtsVoiceChoice("Tiếng Việt - VITS Mimic3 VAIS1000 offline", "vits-vi-vn", TtsModelCatalog.VIETNAMESE_VITS_MODEL_NAME));
        ttsVoices.add(new TtsVoiceChoice("Ti\u1ebfng Vi\u1ec7t - Piper VIVOS X low offline", "piper-vi-vivos", TtsModelCatalog.VIETNAMESE_PIPER_VIVOS_MODEL_NAME));
        ttsVoices.add(new TtsVoiceChoice("Ti\u1ebfng Vi\u1ec7t - Piper 25 hours single low offline", "piper-vi-25h", TtsModelCatalog.VIETNAMESE_PIPER_25H_MODEL_NAME));
        ttsVoices.add(new TtsVoiceChoice("Supertonic 3 multilingual", "supertonic3", SUPERTONIC_MODEL_NAME));
        ArrayAdapter<TtsVoiceChoice> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ttsVoices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ttsVoiceSpinner.setAdapter(adapter);
    }

    private void setupActions() {
        listenButton.setOnClickListener(v -> {
            if (isListening) {
                stopListening();
            } else {
                startListeningWithPermission();
            }
        });

        openFileButton.setOnClickListener(v -> openFilePicker());
        capturePhotoButton.setOnClickListener(v -> capturePhoto());
        getTranscriptButton.setOnClickListener(v -> getTranscriptFromSelectedFile());
        translateManualButton.setOnClickListener(v -> translateManualTranscript());
        speakSourceButton.setOnClickListener(v -> speakLastSource());
        shareSourceButton.setOnClickListener(v -> shareSourceText());
        speakTargetButton.setOnClickListener(v -> speakLastTarget());
        shareTargetButton.setOnClickListener(v -> shareTargetText());
        exportSessionButton.setOnClickListener(v -> exportMeetingSession());
        resetSessionButton.setOnClickListener(v -> confirmResetMeetingSession());
        stopSpeechButton.setOnClickListener(v -> stopSpeechPlayback(true));
        swapLanguagesButton.setOnClickListener(v -> swapSelectedLanguages());
        View.OnClickListener openModelManager = v -> {
            Intent intent = new Intent(this, ModelManagerActivity.class);
            intent.putExtra("ui_language_code", getUiLanguageCode());
            intent.putExtra("ui_language_name", getUiLanguageName());
            startActivity(intent);
        };
        manageModelsButton.setOnClickListener(openModelManager);
        manageModelsHeaderButton.setOnClickListener(openModelManager);
        settingsHeaderButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            MeetingLanguage first = getLanguageOne();
            MeetingLanguage second = getLanguageTwo();
            intent.putExtra("ui_language_code", getUiLanguageCode());
            intent.putExtra("language_1_name", first.name);
            intent.putExtra("language_1_code", first.code);
            intent.putExtra("language_2_name", second.name);
            intent.putExtra("language_2_code", second.code);
            startActivity(intent);
        });
        runtimeHeaderButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, RuntimeActivity.class);
            MeetingLanguage first = getLanguageOne();
            MeetingLanguage second = getLanguageTwo();
            intent.putExtra("ui_language_code", getUiLanguageCode());
            intent.putExtra("language_1_name", first.name);
            intent.putExtra("language_1_code", first.code);
            intent.putExtra("language_2_name", second.name);
            intent.putExtra("language_2_code", second.code);
            startActivity(intent);
        });
        assistantHeaderButton.setOnClickListener(v -> {
            MeetingLanguage language = getLanguageOne();
            Intent intent = new Intent(this, AssistantActivity.class);
            intent.putExtra("language_name", language.name);
            intent.putExtra("language_code", language.code);
            startActivity(intent);
        });
        autoDetectLanguageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            MeetingLanguage first = getLanguageOne();
            MeetingLanguage second = getLanguageTwo();
            if (isChecked) {
                setStatus("T\u1ef1 ph\u00e1t hi\u1ec7n ng\u00f4n ng\u1eef: b\u1eadt.");
            } else {
                setStatus("T\u1ef1 ph\u00e1t hi\u1ec7n ng\u00f4n ng\u1eef: t\u1eaft. Ngu\u1ed3n " + first.name + " -> \u0111\u00edch " + second.name + ".");
            }
            updateReadinessSummary();
        });
    }

    private void swapSelectedLanguages() {
        int first = languageOneSpinner.getSelectedItemPosition();
        int second = languageTwoSpinner.getSelectedItemPosition();
        if (first < 0 || second < 0 || first == second) {
            return;
        }
        languageOneSpinner.setSelection(second);
        languageTwoSpinner.setSelection(first);
        MeetingLanguage source = getLanguageOne();
        MeetingLanguage target = getLanguageTwo();
        setStatus(t("source") + " " + source.name + " ↔ " + t("target") + " " + target.name + ".");
    }

    private void applyUiLanguage() {
        appTitleText.setText("AI Meeting");
        commandCenterTitleText.setText(uiLabel("command_center_title"));
        commandCenterSubtitleText.setText(uiLabel("command_center_subtitle"));
        stepLanguageText.setText(uiLabel("step_language"));
        stepReadinessText.setText(uiLabel("step_readiness"));
        stepCaptureText.setText(uiLabel("step_capture"));
        meetingHeaderButton.setContentDescription("AI Meeting");
        assistantHeaderButton.setContentDescription(t("assistant"));
        manageModelsHeaderButton.setContentDescription(uiLabel("models"));
        settingsHeaderButton.setContentDescription(uiLabel("settings"));
        runtimeHeaderButton.setContentDescription(uiLabel("runtime"));
        readinessTitleText.setText(uiLabel("readiness_title"));
        modelModeText.setText(uiLabel("offline_first"));
        timelineTitleText.setText(uiLabel("timeline_title"));
        if (timelineTurnCount == 0) {
            timelineEmptyText.setText(uiLabel("timeline_empty"));
        }
        PremiumUi.secondaryIconTextButton(this, exportSessionButton, R.drawable.ic_action_share,
                sessionExportLabel(), sessionExportLabel());
        PremiumUi.dangerIconTextButton(this, resetSessionButton, R.drawable.ic_action_trash,
                resetSessionLabel(), resetSessionDescription());
        PremiumUi.secondaryIconTextButton(this, stopSpeechButton, R.drawable.ic_action_stop,
                stopSpeechLabel(), stopSpeechDescription());
        autoDetectLanguageSwitch.setText(t("auto_detect"));
        updateMeetingState();
        PremiumUi.primaryIconButton(this, swapLanguagesButton, R.drawable.ic_action_swap, t("swap_languages"));
        PremiumUi.secondaryIconTextButton(this, openFileButton, R.drawable.ic_action_folder,
                buttonLabel("open_file"), t("open_file"));
        PremiumUi.secondaryIconTextButton(this, capturePhotoButton, R.drawable.ic_action_camera,
                buttonLabel("capture_photo"), t("capture_photo"));
        PremiumUi.primaryIconTextButton(this, manageModelsButton, R.drawable.ic_nav_models,
                buttonLabel("manage_models"), t("manage_models"));
        manualTranscriptLabel.setText(t("manual_label"));
        manualTranscriptInput.setHint(t("manual_hint"));
        PremiumUi.primaryIconTextButton(this, getTranscriptButton, R.drawable.ic_action_transcript,
                buttonLabel("get_transcript"), t("get_transcript"));
        PremiumUi.primaryIconTextButton(this, translateManualButton, R.drawable.ic_action_translate,
                buttonLabel("translate_manual"), t("translate_manual"));
        PremiumUi.primaryIconTextButton(this, speakSourceButton, R.drawable.ic_action_volume,
                buttonLabel("speak_source"), t("speak_source"));
        PremiumUi.secondaryIconTextButton(this, shareSourceButton, R.drawable.ic_action_share,
                buttonLabel("share"), t("share"));
        PremiumUi.primaryIconTextButton(this, speakTargetButton, R.drawable.ic_action_volume,
                buttonLabel("speak_target"), t("speak_target"));
        PremiumUi.secondaryIconTextButton(this, shareTargetButton, R.drawable.ic_action_share,
                buttonLabel("share"), t("share"));
        if (isPlaceholderSource()) {
            sourceText.setText(t("source_placeholder"));
        }
        if (isPlaceholderTarget()) {
            targetText.setText(t("target_placeholder"));
        }
        updateLanguageLabels();
        updateShareButtons();
        updateSessionExportButton();
        updateStopSpeechButton();
        if (!isListening) {
            setStatus(t("ready"));
        }
        updateReadinessSummary();
    }

    private void updateLanguageLabels() {
        String sourceName = "--";
        String targetName = "--";
        if (sourceLanguageText.getText() != null && sourceLanguageText.getText().toString().contains(":")) {
            String[] parts = sourceLanguageText.getText().toString().split(":", 2);
            sourceName = parts.length > 1 ? parts[1].trim() : sourceName;
        }
        if (targetLanguageText.getText() != null && targetLanguageText.getText().toString().contains(":")) {
            String[] parts = targetLanguageText.getText().toString().split(":", 2);
            targetName = parts.length > 1 ? parts[1].trim() : targetName;
        }
        sourceLanguageText.setText(t("source") + ": " + sourceName);
        targetLanguageText.setText(t("target") + ": " + targetName);
    }

    private boolean isPlaceholderSource() {
        String value = sourceText.getText() == null ? "" : sourceText.getText().toString();
        return value.contains("Transcript") || value.equals(LocalizedText.get("en", "source_placeholder"))
                || LocalizedText.matchesAnyLanguage("source_placeholder", value);
    }

    private boolean isPlaceholderTarget() {
        String value = targetText.getText() == null ? "" : targetText.getText().toString();
        return value.contains("Translation") || value.contains("Bản dịch")
                || LocalizedText.matchesAnyLanguage("target_placeholder", value);
    }

    private String t(String key) {
        return LocalizedText.get(getUiLanguageCode(), key);
    }

    private String buttonLabel(String key) {
        return LocalizedText.button(getUiLanguageCode(), key);
    }

    private String uiLabel(String key) {
        String code = getUiLanguageCode();
        boolean vietnamese = "vi".equals(code);
        switch (key) {
            case "models":
                return vietnamese ? "Models" : "Models";
            case "settings":
                return vietnamese ? "Cài đặt" : "Settings";
            case "runtime":
                return vietnamese ? "Runtime" : "Runtime";
            case "readiness_title":
                return vietnamese ? "Sẵn sàng cho cuộc họp" : "Meeting readiness";
            case "offline_first":
                return vietnamese ? "Offline trước" : "Offline-first";
            case "timeline_title":
                return vietnamese ? "Cuộc hội thoại" : "Conversation";
            case "timeline_empty":
                return vietnamese ? "Các lượt đã dịch sẽ hiển thị tại đây." : "Translated turns will appear here.";
            case "export_session":
                return sessionExportLabel();
            case "command_center_title":
                return vietnamese ? "Trung tâm điều khiển cuộc họp" : "Meeting command center";
            case "command_center_subtitle":
                return vietnamese ? "Chọn cặp ngôn ngữ, kiểm tra model, rồi bắt đầu nghe trực tiếp." : "Choose language pair, check readiness, then start live listening.";
            case "step_language":
                return vietnamese ? "1  Ngôn ngữ" : "1  Languages";
            case "step_readiness":
                return vietnamese ? "2  Sẵn sàng" : "2  Readiness";
            case "step_capture":
                return vietnamese ? "3  Thu âm" : "3  Capture";
            case "turn":
                return vietnamese ? "Lượt" : "Turn";
            case "speak_source_short":
                return vietnamese ? "Phát nguồn" : "Speak source";
            case "speak_target_short":
                return vietnamese ? "Phát dịch" : "Speak translation";
            case "ready_short":
                return vietnamese ? "Sẵn sàng" : "Ready";
            case "live_short":
                return vietnamese ? "Đang nghe" : "Listening";
            case "all_ready":
                return vietnamese ? "Sẵn sàng họp" : "Ready for meeting";
            case "needs_models":
                return vietnamese ? "Cần tải model" : "Models needed";
            default:
                return key;
        }
    }

    private void updateMeetingState() {
        if (listenButton != null) {
            PremiumUi.primaryIconTextButton(this, listenButton,
                    isListening ? R.drawable.ic_action_stop : R.drawable.ic_action_mic,
                    isListening ? buttonLabel("stop_listening") : buttonLabel("start_listening"),
                    isListening ? t("stop_listening") : t("start_listening"));
            if (isListening) {
                listenButton.setBackgroundResource(R.drawable.recording_button);
            }
        }
        if (meetingStateText != null) {
            meetingStateText.setText(isListening ? uiLabel("live_short") : uiLabel("ready_short"));
            meetingStateText.setBackgroundResource(isListening ? R.drawable.status_chip_live : R.drawable.status_chip_neutral);
            meetingStateText.setTextColor(getColor(isListening ? android.R.color.holo_red_dark : R.color.meeting_primary_dark));
        }
        if (stepCaptureText != null) {
            stepCaptureText.setBackgroundResource(isListening ? R.drawable.status_chip_live : R.drawable.status_chip_neutral);
            stepCaptureText.setTextColor(getColor(isListening ? android.R.color.holo_red_dark : R.color.meeting_primary_dark));
        }
    }

    private void updateReadinessSummary() {
        if (sttReadinessText == null || translationReadinessText == null || ttsReadinessText == null) {
            return;
        }
        MeetingLanguage first = getLanguageOne();
        MeetingLanguage second = getLanguageTwo();
        if (first == null || second == null) {
            return;
        }

        boolean autoDetect = isAutoLanguageDetectionEnabled();
        boolean firstSttReady = isSpeechReadyForLanguage(1, first);
        boolean secondSttReady = !autoDetect || isSpeechReadyForLanguage(2, second);
        boolean sttReady = firstSttReady && secondSttReady;
        boolean ttsReady = isTtsReadyForLanguage(2, second) && (!autoDetect || isTtsReadyForLanguage(1, first));
        boolean allReady = sttReady && ttsReady;

        boolean vietnamese = isVietnameseUi();
        String pair = first.name + " ↔ " + second.name;
        sttReadinessText.setText(sttReady
                ? (vietnamese ? "STT: sẵn sàng cho " : "STT: ready for ") + pair
                : (vietnamese ? "STT: cần tải model cho " : "STT: model needed for ") + pair);
        translationReadinessText.setText(vietnamese
                ? "Dịch: ML Kit on-device, tự tải model khi cần"
                : "Translation: ML Kit on-device, downloads when needed");
        ttsReadinessText.setText(ttsReady
                ? (vietnamese ? "Phát âm: voice offline đã sẵn sàng" : "Speech: offline voice ready")
                : (vietnamese ? "Phát âm: voice sẽ tự tải trong lần phát đầu" : "Speech: voice downloads on first playback"));

        if (readinessSummaryText != null) {
            readinessSummaryText.setText(allReady ? uiLabel("all_ready") : uiLabel("needs_models"));
            readinessSummaryText.setBackgroundResource(allReady ? R.drawable.status_chip_neutral : R.drawable.status_chip_warning);
            readinessSummaryText.setTextColor(getColor(allReady ? R.color.meeting_primary_dark : android.R.color.holo_orange_dark));
        }
        if (stepReadinessText != null) {
            stepReadinessText.setBackgroundResource(allReady ? R.drawable.status_chip_neutral : R.drawable.status_chip_warning);
            stepReadinessText.setTextColor(getColor(allReady ? R.color.meeting_primary_dark : R.color.meeting_gold));
        }
        if (stepCaptureText != null) {
            stepCaptureText.setBackgroundResource(isListening ? R.drawable.status_chip_live : R.drawable.status_chip_neutral);
            stepCaptureText.setTextColor(getColor(isListening ? android.R.color.holo_red_dark : R.color.meeting_primary_dark));
        }
        modelModeText.setText(autoDetect
                ? uiLabel("offline_first") + " · Auto language"
                : uiLabel("offline_first") + " · " + first.code + " → " + second.code);
    }

    private boolean isSpeechReadyForLanguage(int languageIndex, MeetingLanguage language) {
        String preferredValue = getPreferredModelValue("stt", languageIndex);
        if (usesGoogleSpeechRecognizer(languageIndex) || OnlineProviderConfig.isOnlineSttProvider(preferredValue)) {
            return true;
        }
        File preferred = getPreferredLocalModel("stt", languageIndex);
        if (preferred != null && looksLikeVoskModel(preferred)) {
            return true;
        }
        SttModelChoice model = getSttModelForLanguage(language.code);
        return model != null && model.isAvailable();
    }

    private boolean isTtsReadyForLanguage(int languageIndex, MeetingLanguage language) {
        String preferredValue = getPreferredModelValue("tts", languageIndex);
        if (OnlineProviderConfig.isOnlineTtsProvider(preferredValue)) {
            return true;
        }
        File preferred = getPreferredLocalModel("tts", languageIndex);
        if (preferred != null && TtsModelCatalog.looksLikeUsableModel(preferred)) {
            return true;
        }
        File ttsParent = AppStorage.modelDir(this, "tts");
        return TtsModelCatalog.looksLikeUsableModel(TtsModelCatalog.defaultModelDir(ttsParent, language.code));
    }

    private void startListeningWithPermission() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
            return;
        }

        if (!validateLanguagePair()) {
            return;
        }

        isListening = true;
        recognitionRetryCount = 0;
        updateMeetingState();
        targetText.setText(t("target_placeholder"));
        speakTargetButton.setEnabled(false);
        updateShareButtons();
        ensureSttModelThenStart();
    }

    private void ensureSttModelThenStart() {
        if (!isListening || recognitionInProgress) {
            return;
        }

        SttModelChoice firstModel = getPreferredSttModelForLanguage(1, getLanguageOne().code);
        SttModelChoice secondModel = getPreferredSttModelForLanguage(2, getLanguageTwo().code);
        boolean autoDetect = isAutoLanguageDetectionEnabled();
        if (usesGoogleSpeechRecognizer(1) && !autoDetect) {
            startGoogleSpeechRecognition(getLanguageOne());
            return;
        }
        String firstProvider = getPreferredModelValue("stt", 1);
        String secondProvider = getPreferredModelValue("stt", 2);
        if (OnlineProviderConfig.isOnlineSttProvider(firstProvider)
                || (autoDetect && OnlineProviderConfig.isOnlineSttProvider(secondProvider))) {
            startPreferredSpeechRecognition(firstProvider, secondProvider);
            return;
        }
        if (firstModel == null || (autoDetect && secondModel == null)) {
            setStatus("Chưa có model STT cho cặp ngôn ngữ này.");
            stopListeningSilently();
            return;
        }
        if (firstModel.isAvailable() && (!autoDetect || secondModel.isAvailable())) {
            startOfflineSpeechRecognition();
            return;
        }

        listenButton.setEnabled(false);
        setStatus("Đang tải model STT lần đầu cho cặp ngôn ngữ...");
        backgroundExecutor.execute(() -> {
            try {
                if (!firstModel.isAvailable()) {
                    downloadAndUnzipWithProgress(firstModel.downloadUrl, firstModel.modelDir.getParentFile(), firstModel.name);
                }
                if (autoDetect && !secondModel.isAvailable()) {
                    downloadAndUnzipWithProgress(secondModel.downloadUrl, secondModel.modelDir.getParentFile(), secondModel.name);
                }
                mainHandler.post(() -> {
                    listenButton.setEnabled(true);
                    updateReadinessSummary();
                    setStatus("Đã tải model STT. Đang nghe...");
                    startOfflineSpeechRecognition();
                });
            } catch (IOException e) {
                mainHandler.post(() -> {
                    listenButton.setEnabled(true);
                    setStatus("Không tải được model STT: " + e.getMessage());
                    stopListeningSilently();
                });
            }
        });
    }

    private void downloadAndUnzip(String urlString, File destinationParent) throws IOException {
        if (modelDownloadManager != null) {
            modelDownloadManager.downloadZip(
                    urlString,
                    destinationParent,
                    "vosk-model",
                    "tải ZIP",
                    "giải nén ZIP",
                    "hoàn tất giải nén",
                    "tiếp tục từ",
                    "đã có sẵn",
                    null
            );
            return;
        }
        if (urlString == null) {
            throw new IOException("Không có URL tải model.");
        }
        if (!destinationParent.exists() && !destinationParent.mkdirs()) {
            throw new IOException("Không tạo được thư mục model.");
        }

        File zipFile = resumableCacheFile("vosk-model", urlString, ".zip.part");
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
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
        if (!zipFile.delete()) {
            zipFile.deleteOnExit();
        }
    }

    private void downloadAndUnzipWithProgress(String urlString, File destinationParent, String label) throws IOException {
        if (modelDownloadManager != null) {
            modelDownloadManager.downloadZip(
                    urlString,
                    destinationParent,
                    "vosk-model",
                    "tải ZIP",
                    "giải nén ZIP",
                    "hoàn tất giải nén",
                    "tiếp tục từ",
                    "đã có sẵn",
                    (phase, doneBytes, totalBytes) -> postDownloadProgress(label, phase, doneBytes, totalBytes)
            );
            return;
        }
        if (urlString == null) {
            throw new IOException("Không có URL tải model.");
        }
        if (!destinationParent.exists() && !destinationParent.mkdirs()) {
            throw new IOException("Không tạo được thư mục model.");
        }
        File zipFile = File.createTempFile("vosk-model", ".zip", getCacheDir());
        downloadArchiveWithProgress(urlString, zipFile, label, "tải ZIP");
        postDownloadProgress(label, "giải nén ZIP", zipFile.length(), zipFile.length());
        unzip(zipFile, destinationParent);
        postDownloadProgress(label, "hoàn tất giải nén", zipFile.length(), zipFile.length());
        if (!zipFile.delete()) {
            zipFile.deleteOnExit();
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
        if (modelDownloadManager != null) {
            modelDownloadManager.downloadTarBz2(
                    urlString,
                    destinationParent,
                    "tts-model",
                    "tải TAR.BZ2",
                    "giải nén TAR.BZ2",
                    "hoàn tất giải nén",
                    "tiếp tục từ",
                    "đã có sẵn",
                    null
            );
            return;
        }
        if (!destinationParent.exists() && !destinationParent.mkdirs()) {
            throw new IOException("Không tạo được thư mục model TTS.");
        }

        File archiveFile = resumableCacheFile("tts-model", urlString, ".tar.bz2.part");
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
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
        if (!archiveFile.delete()) {
            archiveFile.deleteOnExit();
        }
    }

    private void downloadAndExtractTarBz2WithProgress(String urlString, File destinationParent, String label) throws IOException {
        if (modelDownloadManager != null) {
            modelDownloadManager.downloadTarBz2(
                    urlString,
                    destinationParent,
                    "tts-model",
                    "tải TAR.BZ2",
                    "giải nén TAR.BZ2",
                    "hoàn tất giải nén",
                    "tiếp tục từ",
                    "đã có sẵn",
                    (phase, doneBytes, totalBytes) -> postDownloadProgress(label, phase, doneBytes, totalBytes)
            );
            return;
        }
        if (!destinationParent.exists() && !destinationParent.mkdirs()) {
            throw new IOException("Không tạo được thư mục model TTS.");
        }
        File archiveFile = File.createTempFile("tts-model", ".tar.bz2", getCacheDir());
        downloadArchiveWithProgress(urlString, archiveFile, label, "tải TAR.BZ2");
        postDownloadProgress(label, "giải nén TAR.BZ2", archiveFile.length(), archiveFile.length());
        extractTarBz2(archiveFile, destinationParent);
        postDownloadProgress(label, "hoàn tất giải nén", archiveFile.length(), archiveFile.length());
        if (!archiveFile.delete()) {
            archiveFile.deleteOnExit();
        }
    }

    private void downloadArchiveWithProgress(String urlString, File outputFile, String label, String phase) throws IOException {
        long resumeBytes = outputFile.isFile() ? outputFile.length() : 0L;
        String rangeHeader = resumeBytes > 0 ? "bytes=" + resumeBytes + "-" : null;
        HttpURLConnection connection = DownloadConnection.open(urlString, 15000, 30000, rangeHeader);
        int responseCode;
        try {
            connection.connect();
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            connection.disconnect();
            connection = DownloadConnection.reopenAfterTlsFailure(urlString, 15000, 30000, rangeHeader, e);
            connection.connect();
            responseCode = connection.getResponseCode();
        }
        if (responseCode == 416 && resumeBytes > 0) {
            postDownloadProgress(label, phase + " đã có sẵn", resumeBytes, resumeBytes);
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
                    postDownloadProgress(label, resumedPhase(phase, resumeBytes) + speedSuffix(doneBytes - resumeBytes, startedAt), doneBytes, totalBytes);
                    lastReportAt = now;
                }
            }
        } finally {
            connection.disconnect();
        }
        postDownloadProgress(label, resumedPhase(phase, resumeBytes) + speedSuffix(doneBytes - resumeBytes, startedAt), doneBytes, totalBytes);
    }

    private File resumableCacheFile(String prefix, String urlString, String suffix) {
        String key = Integer.toHexString(urlString.hashCode());
        return new File(getCacheDir(), prefix + "-" + key + suffix);
    }

    private String resumedPhase(String phase, long resumeBytes) {
        return resumeBytes > 0 ? phase + " • tiếp tục từ " + formatBytes(resumeBytes) : phase;
    }

    private void postDownloadProgress(String label, String phase, long doneBytes, long totalBytes) {
        mainHandler.post(() -> setStatus(downloadProgressText(label, phase, doneBytes, totalBytes)));
    }

    private String downloadProgressText(String label, String phase, long doneBytes, long totalBytes) {
        StringBuilder builder = new StringBuilder("Đang tải ").append(label).append(": ").append(phase);
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

    private String speedSuffix(long doneBytes, long startedAt) {
        long elapsedMs = Math.max(1L, System.currentTimeMillis() - startedAt);
        long bytesPerSecond = doneBytes * 1000L / elapsedMs;
        return " • " + formatBytes(bytesPerSecond) + "/s";
    }

    private void extractTarBz2(File archiveFile, File destinationParent) throws IOException {
        byte[] buffer = new byte[32 * 1024];
        String destinationRoot = destinationParent.getCanonicalPath() + File.separator;
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(
                new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(archiveFile))))) {
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextEntry()) != null) {
                if (entry.isSymbolicLink() || entry.isLink() || (!entry.isDirectory() && !entry.isFile())) {
                    throw new IOException("Invalid TAR.BZ2 archive.");
                }
                File outputFile = new File(destinationParent, entry.getName());
                String outputPath = outputFile.getCanonicalPath();
                if (!outputPath.startsWith(destinationRoot)) {
                    throw new IOException("Archive TTS không hợp lệ.");
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

    private void startOfflineSpeechRecognition() {
        if (!isListening || recognitionInProgress) {
            return;
        }
        stopSpeechServiceOnly();
        stopAudioRecordOnly();
        SttModelChoice firstModel = getPreferredSttModelForLanguage(1, getLanguageOne().code);
        SttModelChoice secondModel = getPreferredSttModelForLanguage(2, getLanguageTwo().code);
        boolean autoDetect = isAutoLanguageDetectionEnabled();
        if (firstModel == null || (autoDetect && secondModel == null)
                || !firstModel.isAvailable() || (autoDetect && !secondModel.isAvailable())) {
            ensureSttModelThenStart();
            return;
        }

        setStatus("Đang nghe và tự nhận dạng ngôn ngữ...");
        recognitionInProgress = true;
        recognitionActive = true;
        recognitionThread = new Thread(() -> {
            try {
                List<short[]> audioChunks = recordAudioChunks();
                if (!isListening) {
                    return;
                }
                if (audioChunks.isEmpty()) {
                    mainHandler.post(() -> {
                        setStatus("Không thu được âm thanh từ micro.");
                        scheduleRestart();
                    });
                    return;
                }
                mainHandler.post(() -> setStatus("Đã thu âm. Đang nhận dạng ngôn ngữ 1..."));
                String firstText = recognizeWithModel(firstModel, audioChunks);
                if (!isListening) {
                    return;
                }
                mainHandler.post(() -> setStatus("Đang nhận dạng ngôn ngữ 2..."));
                String secondText = autoDetect ? recognizeWithModel(secondModel, audioChunks) : "";
                if (!isListening) {
                    return;
                }
                mainHandler.post(() -> chooseTranscriptAndTranslate(firstText, secondText));
            } catch (Exception e) {
                if (!isListening) {
                    return;
                }
                mainHandler.post(() -> {
                    setStatus("Lỗi nhận dạng offline: " + e.getMessage());
                    stopListeningSilently();
                });
            } finally {
                recognitionInProgress = false;
                stopAudioRecordOnly();
            }
        }, "dual-vosk-recognition");
        recognitionThread.start();
    }

    private void startPreferredSpeechRecognition(String firstProvider, String secondProvider) {
        if (!isListening || recognitionInProgress) {
            return;
        }
        stopSpeechServiceOnly();
        stopAudioRecordOnly();
        boolean autoDetect = isAutoLanguageDetectionEnabled();
        setStatus("Đang nghe bằng STT ưu tiên...");
        recognitionInProgress = true;
        recognitionActive = true;
        recognitionThread = new Thread(() -> {
            try {
                List<short[]> audioChunks = recordAudioChunks();
                if (!isListening) {
                    return;
                }
                if (audioChunks.isEmpty()) {
                    mainHandler.post(() -> {
                        setStatus("Không thu được âm thanh từ micro.");
                        scheduleRestart();
                    });
                    return;
                }
                mainHandler.post(() -> setStatus("Đã thu âm. Đang nhận dạng bằng provider ưu tiên..."));
                String firstText = recognizeWithPreferredSttSync(1, getLanguageOne(), firstProvider, audioChunks, VOSK_SAMPLE_RATE);
                if (!isListening) {
                    return;
                }
                String secondText = autoDetect
                        ? recognizeWithPreferredSttSync(2, getLanguageTwo(), secondProvider, audioChunks, VOSK_SAMPLE_RATE)
                        : "";
                if (!isListening) {
                    return;
                }
                mainHandler.post(() -> chooseTranscriptAndTranslate(firstText, secondText));
            } catch (Exception e) {
                if (!isListening) {
                    return;
                }
                mainHandler.post(() -> {
                    setStatus("Lỗi STT online/local: " + safeErrorMessage(e));
                    stopListeningSilently();
                });
            } finally {
                recognitionInProgress = false;
                stopAudioRecordOnly();
            }
        }, "preferred-stt-recognition");
        recognitionThread.start();
    }

    private void startGoogleSpeechRecognition(MeetingLanguage language) {
        if (recognitionInProgress) {
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            setStatus("Google Speech Recognizer không khả dụng trên máy này. Tạm dùng model local nếu có.");
            isListening = false;
            updateMeetingState();
            return;
        }
        stopSpeechServiceOnly();
        stopAudioRecordOnly();
        recognitionInProgress = true;
        if (googleSpeechRecognizer != null) {
            googleSpeechRecognizer.destroy();
        }
        googleSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        googleSpeechRecognizer.setRecognitionListener(new android.speech.RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                if (!isListening) {
                    return;
                }
                setStatus("Google Speech Recognizer đang nghe...");
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                if (!isListening) {
                    return;
                }
                setStatus("Google Speech Recognizer đang xử lý...");
            }

            @Override
            public void onError(int error) {
                if (!isListening) {
                    return;
                }
                recognitionInProgress = false;
                setStatus("Lỗi Google Speech Recognizer: " + error);
                stopListeningSilently();
            }

            @Override
            public void onResults(Bundle results) {
                if (!isListening) {
                    return;
                }
                recognitionInProgress = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = matches == null || matches.isEmpty() ? "" : matches.get(0);
                if (text.trim().isEmpty()) {
                    setStatus("Google Speech Recognizer chưa nhận dạng được giọng nói.");
                    scheduleRestart();
                    return;
                }
                sourceText.setText(text);
                rememberSource(text, language);
                translateWithFixedDirection(text);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                if (!isListening) {
                    return;
                }
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    sourceText.setText(matches.get(0));
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.ttsLocale.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        googleSpeechRecognizer.startListening(intent);
        setStatus("Đang nghe bằng Google Speech Recognizer...");
    }

    private List<short[]> recordAudioChunks() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw new SecurityException("RECORD_AUDIO permission is not granted.");
        }
        List<short[]> chunks = new ArrayList<>();
        int minBufferSize = AudioRecord.getMinBufferSize(
                (int) VOSK_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        if (minBufferSize <= 0) {
            throw new IllegalStateException("Thiết bị không hỗ trợ cấu hình micro 16 kHz mono.");
        }
        int bufferSize = Math.max(minBufferSize, 4096);
        short[] buffer = new short[bufferSize / 2];
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                (int) VOSK_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalStateException("Không khởi tạo được micro.");
        }
        try {
            audioRecord.startRecording();
            long startedAt = System.currentTimeMillis();
            long lastLevelUpdateAt = 0L;
            int peakLevel = 0;
            while (recognitionActive && System.currentTimeMillis() - startedAt < 6000) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    int localPeak = 0;
                    for (int i = 0; i < read; i++) {
                        localPeak = Math.max(localPeak, Math.abs(buffer[i]));
                    }
                    peakLevel = Math.max(peakLevel, localPeak);
                    long now = System.currentTimeMillis();
                    if (now - lastLevelUpdateAt > 800) {
                        lastLevelUpdateAt = now;
                        int levelPercent = Math.min(100, Math.round((peakLevel / 32767f) * 100f));
                        mainHandler.post(() -> {
                            if (isListening && recognitionActive) {
                                setStatus("\u0110ang nghe... m\u1ee9c \u00e2m " + levelPercent + "%");
                            }
                        });
                    }
                    chunks.add(Arrays.copyOf(buffer, read));
                }
            }
        } finally {
            stopAudioRecordOnly();
        }
        return chunks;
    }

    private String recognizeWithModel(SttModelChoice modelChoice, List<short[]> chunks) throws IOException {
        return recognizeWithModel(modelChoice, chunks, VOSK_SAMPLE_RATE);
    }

    private String recognizeWithModel(SttModelChoice modelChoice, List<short[]> chunks, float sampleRate) throws IOException {
        Model model = new Model(modelChoice.modelDir.getAbsolutePath());
        try (Recognizer recognizer = new Recognizer(model, sampleRate)) {
            for (short[] chunk : chunks) {
                recognizer.acceptWaveForm(chunk, chunk.length);
            }
            return extractVoskText(recognizer.getFinalResult(), "text");
        } finally {
            model.close();
        }
    }

    private String recognizeWithPreferredSttSync(int languageIndex, MeetingLanguage language, String providerValue,
                                                 List<short[]> chunks, float sampleRate)
            throws IOException, JSONException {
        if (OnlineProviderConfig.isOnlineSttProvider(providerValue)) {
            return OnlineAiClient.transcribePcm(this, providerValue, chunks, sampleRate, language.code);
        }
        SttModelChoice model = getPreferredSttModelForLanguage(languageIndex, language.code);
        if (model == null) {
            throw new IOException("No STT model for " + language.name);
        }
        ensureModelAvailableSync(model);
        return recognizeWithModel(model, chunks, sampleRate);
    }

    private void chooseTranscriptAndTranslate(String firstText, String secondText) {
        MeetingLanguage first = getLanguageOne();
        MeetingLanguage second = getLanguageTwo();
        if (!isAutoLanguageDetectionEnabled()) {
            if (firstText == null || firstText.trim().isEmpty()) {
                setStatus("Chưa nhận dạng được giọng nói. Hãy nói gần micro hơn rồi bấm nghe lại.");
                scheduleRestart();
                return;
            }
            if (shouldIgnoreRecognizedTranscript(firstText)) {
                setStatus("Bỏ qua âm thanh phát ra từ ứng dụng. Đang nghe tiếp...");
                queueNextListeningTurn();
                return;
            }
            sourceText.setText(firstText);
            rememberSource(firstText, first);
            translateWithFixedDirection(firstText);
            return;
        }
        sourceText.setText(
                first.name + ": " + emptyMarker(firstText) + "\n"
                        + second.name + ": " + emptyMarker(secondText)
        );
        scoreTranscriptForLanguage(firstText, first, firstScore ->
                scoreTranscriptForLanguage(secondText, second, secondScore -> {
                    boolean useFirst = shouldUseFirstTranscript(firstText, firstScore, secondText, secondScore);
                    String transcript = useFirst ? firstText : secondText;
                    if (transcript == null || transcript.trim().isEmpty()) {
                        setStatus("Chưa nhận dạng được giọng nói. Hãy nói gần micro hơn rồi bấm nghe lại.");
                        scheduleRestart();
                        return;
                    }
                    if (shouldIgnoreRecognizedTranscript(transcript)) {
                        setStatus("Bỏ qua âm thanh phát ra từ ứng dụng. Đang nghe tiếp...");
                        queueNextListeningTurn();
                        return;
                    }
                    MeetingLanguage sourceLanguage = useFirst ? first : second;
                    MeetingLanguage targetLanguage = useFirst ? second : first;
                    sourceText.setText(transcript);
                    sourceLanguageText.setText(t("source") + ": " + sourceLanguage.name);
                    targetLanguageText.setText(t("target") + ": " + targetLanguage.name);
                    rememberSource(transcript, sourceLanguage);
                    translateWithAutoDownload(transcript, sourceLanguage, targetLanguage);
                })
        );
    }

    private boolean shouldUseFirstTranscript(String firstText, float firstScore, String secondText, float secondScore) {
        if (modelDownloadManager != null) {
            return TranscriptSelector.shouldUseFirst(firstText, firstScore, secondText, secondScore);
        }
        boolean firstEmpty = firstText == null || firstText.trim().isEmpty();
        boolean secondEmpty = secondText == null || secondText.trim().isEmpty();
        if (firstEmpty != secondEmpty) {
            return !firstEmpty;
        }
        if (Math.abs(firstScore - secondScore) >= 0.10f) {
            return firstScore > secondScore;
        }
        int firstLength = firstText == null ? 0 : firstText.trim().length();
        int secondLength = secondText == null ? 0 : secondText.trim().length();
        if (Math.abs(firstLength - secondLength) >= 4) {
            return firstLength > secondLength;
        }
        return firstScore >= secondScore;
    }

    private String emptyMarker(String text) {
        if (modelDownloadManager != null) {
            return TranscriptSelector.emptyMarker(text);
        }
        return text == null || text.trim().isEmpty() ? "--" : text;
    }

    private void scoreTranscriptForLanguage(String text, MeetingLanguage expectedLanguage, Consumer<Float> callback) {
        if (text == null || text.trim().isEmpty()) {
            callback.accept(-1f);
            return;
        }
        languageIdentifier.identifyPossibleLanguages(text)
                .addOnSuccessListener(candidates -> {
                    float score = 0f;
                    for (IdentifiedLanguage candidate : candidates) {
                        if (normalizeLanguageTag(candidate.getLanguageTag()).equals(expectedLanguage.code)) {
                            score = Math.max(score, candidate.getConfidence());
                        }
                    }
                    MeetingLanguage scriptLanguage = inferLanguageByScript(text, getLanguageOne(), getLanguageTwo());
                    if (scriptLanguage != null && scriptLanguage.code.equals(expectedLanguage.code)) {
                        score = Math.max(score, 0.95f);
                    }
                    callback.accept(score);
                })
                .addOnFailureListener(e -> callback.accept(0f));
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "audio/*", "video/*", "image/*", "text/*", "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        });
        startActivityForResult(intent, REQUEST_OPEN_FILE);
    }

    private void capturePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(intent, REQUEST_CAPTURE_PHOTO);
        } catch (Exception e) {
            setStatus("Không mở được camera: " + safeErrorMessage(e));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAPTURE_PHOTO) {
            handleCapturedPhoto(resultCode, data);
            return;
        }
        if (requestCode != REQUEST_OPEN_FILE || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        selectedFileUri = data.getData();
        selectedFileMimeType = getContentResolver().getType(selectedFileUri);
        try {
            if ((data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                getContentResolver().takePersistableUriPermission(
                        selectedFileUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            }
        } catch (SecurityException ignored) {
        }
        setStatus(t("file_selected"));
    }

    private void handleCapturedPhoto(int resultCode, Intent data) {
        if (resultCode != RESULT_OK || data == null || data.getExtras() == null) {
            return;
        }
        Bitmap bitmap = getCapturedThumbnail(data);
        if (bitmap == null) {
            setStatus("Không nhận được ảnh từ camera.");
            return;
        }
        stopListeningSilently();
        setStatus("Đang OCR ảnh vừa chụp...");
        backgroundExecutor.execute(() -> processOcrBitmap(bitmap));
    }

    private Bitmap getCapturedThumbnail(Intent data) {
        Bundle extras = data == null ? null : data.getExtras();
        if (extras == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return extras.getParcelable("data", Bitmap.class);
        }
        return getCapturedThumbnailLegacy(extras);
    }

    @SuppressWarnings("deprecation")
    private Bitmap getCapturedThumbnailLegacy(Bundle extras) {
        Object value = extras.get("data");
        return value instanceof Bitmap ? (Bitmap) value : null;
    }

    private void getTranscriptFromSelectedFile() {
        if (selectedFileUri == null) {
            setStatus("Vui lòng chọn tệp trước.");
            return;
        }
        stopListeningSilently();
        setStatus("Đang lấy transcript từ tệp...");
        getTranscriptButton.setEnabled(false);
        backgroundExecutor.execute(() -> {
            try {
                String mimeType = selectedFileMimeType == null ? "" : selectedFileMimeType;
                if (mimeType.startsWith("audio/") || mimeType.startsWith("video/")) {
                    transcribeMediaFile(selectedFileUri);
                    return;
                }
                if (mimeType.startsWith("image/")) {
                    runOcrOnImage(selectedFileUri);
                    return;
                }
                if ("application/pdf".equals(mimeType)) {
                    runOcrOnPdf(selectedFileUri);
                    return;
                }
                if (mimeType.startsWith("text/")) {
                    String text = readTextUri(selectedFileUri);
                    mainHandler.post(() -> acceptFileTranscript(text));
                    return;
                }
                if (isWordDocument(selectedFileUri, mimeType)) {
                    String text = readWordDocument(selectedFileUri, mimeType);
                    mainHandler.post(() -> acceptFileTranscript(text));
                    return;
                }
                mainHandler.post(() -> setStatus("Chưa hỗ trợ lấy transcript cho định dạng tệp này."));
            } catch (Exception e) {
                mainHandler.post(() -> setStatus("Lỗi lấy transcript: " + safeErrorMessage(e)));
            } finally {
                mainHandler.post(() -> getTranscriptButton.setEnabled(true));
            }
        });
    }

    private void transcribeMediaFile(Uri uri) throws Exception {
        if (!validateLanguagePair()) {
            mainHandler.post(() -> getTranscriptButton.setEnabled(true));
            return;
        }
        SttModelChoice firstModel = getPreferredSttModelForLanguage(1, getLanguageOne().code);
        SttModelChoice secondModel = getPreferredSttModelForLanguage(2, getLanguageTwo().code);
        boolean autoDetect = isAutoLanguageDetectionEnabled();
        String firstProvider = getPreferredModelValue("stt", 1);
        String secondProvider = getPreferredModelValue("stt", 2);
        if (OnlineProviderConfig.isOnlineSttProvider(firstProvider)
                || (autoDetect && OnlineProviderConfig.isOnlineSttProvider(secondProvider))) {
            DecodedAudio decodedAudio = decodeAudioFromMedia(uri);
            if (decodedAudio.chunks.isEmpty()) {
                mainHandler.post(() -> setStatus("Không đọc được âm thanh từ tệp đã chọn."));
                return;
            }
            String firstText = recognizeWithPreferredSttSync(1, getLanguageOne(), firstProvider,
                    decodedAudio.chunks, decodedAudio.sampleRate);
            String secondText = autoDetect
                    ? recognizeWithPreferredSttSync(2, getLanguageTwo(), secondProvider,
                    decodedAudio.chunks, decodedAudio.sampleRate)
                    : "";
            mainHandler.post(() -> {
                acceptFileTranscript(autoDetect && secondText.trim().length() > firstText.trim().length() ? secondText : firstText);
                chooseTranscriptAndTranslate(firstText, secondText);
            });
            return;
        }
        if (firstModel == null || (autoDetect && secondModel == null)) {
            mainHandler.post(() -> setStatus("Chưa có model STT cho cặp ngôn ngữ này."));
            return;
        }
        ensureModelAvailableSync(firstModel);
        if (autoDetect) {
            ensureModelAvailableSync(secondModel);
        }
        DecodedAudio decodedAudio = decodeAudioFromMedia(uri);
        if (decodedAudio.chunks.isEmpty()) {
            mainHandler.post(() -> setStatus("Không đọc được âm thanh từ tệp đã chọn."));
            return;
        }
        String firstText = recognizeWithModel(firstModel, decodedAudio.chunks, decodedAudio.sampleRate);
        String secondText = autoDetect ? recognizeWithModel(secondModel, decodedAudio.chunks, decodedAudio.sampleRate) : "";
        mainHandler.post(() -> {
            acceptFileTranscript(autoDetect && secondText.trim().length() > firstText.trim().length() ? secondText : firstText);
            chooseTranscriptAndTranslate(firstText, secondText);
        });
    }

    private void ensureModelAvailableSync(SttModelChoice modelChoice) throws IOException {
        if (!modelChoice.isAvailable()) {
            downloadAndUnzipWithProgress(modelChoice.downloadUrl, modelChoice.modelDir.getParentFile(), modelChoice.name);
        }
    }

    private DecodedAudio decodeAudioFromMedia(Uri uri) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        boolean codecStarted = false;
        try {
            extractor.setDataSource(this, uri, null);
            int trackIndex = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat candidate = extractor.getTrackFormat(i);
                String mime = getMediaFormatString(candidate, MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    trackIndex = i;
                    format = candidate;
                    break;
                }
            }
            if (trackIndex < 0 || format == null) {
                throw new IOException("Tệp không có track âm thanh.");
            }
            extractor.selectTrack(trackIndex);
            String mime = getMediaFormatString(format, MediaFormat.KEY_MIME);
            if (mime == null || !mime.startsWith("audio/")) {
                throw new IOException("Track âm thanh không hợp lệ.");
            }
            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            codec.start();
            codecStarted = true;

            List<short[]> chunks = new ArrayList<>();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            boolean outputDone = false;
            float sampleRate = safeMediaFormatInt(format, MediaFormat.KEY_SAMPLE_RATE, (int) VOSK_SAMPLE_RATE);
            int channels = Math.max(1, safeMediaFormatInt(format, MediaFormat.KEY_CHANNEL_COUNT, 1));
            int pcmEncoding = safeMediaFormatInt(format, MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT);

            while (!outputDone) {
                if (!inputDone) {
                    int inputIndex = codec.dequeueInputBuffer(10000);
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = codec.getInputBuffer(inputIndex);
                        if (inputBuffer == null) {
                            continue;
                        }
                        int sampleSize = extractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else if (extractor.getSampleTime() > MAX_MEDIA_TRANSCRIPTION_DURATION_US) {
                            codec.queueInputBuffer(inputIndex, 0, 0, extractor.getSampleTime(), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }
                int outputIndex = codec.dequeueOutputBuffer(info, 10000);
                if (outputIndex >= 0) {
                    try {
                        if (pcmEncoding != AudioFormat.ENCODING_PCM_16BIT) {
                            throw new IOException("Codec âm thanh trả về PCM không được hỗ trợ: " + pcmEncoding);
                        }
                        ByteBuffer outputBuffer = codec.getOutputBuffer(outputIndex);
                        if (outputBuffer != null && info.size > 0) {
                            outputBuffer.position(info.offset);
                            outputBuffer.limit(info.offset + info.size);
                            chunks.addAll(readPcm16MonoChunks(outputBuffer, channels));
                        }
                        outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    } finally {
                        codec.releaseOutputBuffer(outputIndex, false);
                    }
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat outputFormat = codec.getOutputFormat();
                    sampleRate = safeMediaFormatInt(outputFormat, MediaFormat.KEY_SAMPLE_RATE, (int) sampleRate);
                    channels = Math.max(1, safeMediaFormatInt(outputFormat, MediaFormat.KEY_CHANNEL_COUNT, channels));
                    pcmEncoding = safeMediaFormatInt(outputFormat, MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
                }
            }
            return new DecodedAudio(chunks, sampleRate);
        } finally {
            if (codec != null) {
                if (codecStarted) {
                    try {
                        codec.stop();
                    } catch (IllegalStateException ignored) {
                    }
                }
                codec.release();
            }
            extractor.release();
        }
    }

    private String getMediaFormatString(MediaFormat format, String key) {
        if (format == null || !format.containsKey(key)) {
            return null;
        }
        try {
            return format.getString(key);
        } catch (ClassCastException | NullPointerException e) {
            return null;
        }
    }

    private int safeMediaFormatInt(MediaFormat format, String key, int fallback) {
        if (format == null || !format.containsKey(key)) {
            return fallback;
        }
        try {
            return format.getInteger(key);
        } catch (ClassCastException | NullPointerException e) {
            return fallback;
        }
    }

    private List<short[]> readPcm16MonoChunks(ByteBuffer buffer, int channels) {
        List<short[]> chunks = new ArrayList<>();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        channels = Math.max(1, channels);
        int frameCount = buffer.remaining() / 2 / channels;
        short[] chunk = new short[Math.min(4096, Math.max(1, frameCount))];
        int index = 0;
        for (int frame = 0; frame < frameCount; frame++) {
            int mixed = 0;
            for (int channel = 0; channel < channels; channel++) {
                mixed += buffer.getShort();
            }
            chunk[index++] = (short) (mixed / channels);
            if (index == chunk.length) {
                chunks.add(Arrays.copyOf(chunk, index));
                index = 0;
            }
        }
        if (index > 0) {
            chunks.add(Arrays.copyOf(chunk, index));
        }
        return chunks;
    }

    private void runOcrOnImage(Uri uri) throws IOException {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (bitmap == null) {
                throw new IOException("Không đọc được ảnh.");
            }
            processOcrBitmap(bitmap);
        }
    }

    private void runOcrOnPdf(Uri uri) throws IOException {
        try (ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(uri, "r");
             android.graphics.pdf.PdfRenderer renderer = new android.graphics.pdf.PdfRenderer(descriptor)) {
            if (renderer.getPageCount() == 0) {
                throw new IOException("PDF không có trang.");
            }
            StringBuilder text = new StringBuilder();
            int pageCount = renderer.getPageCount();
            for (int i = 0; i < pageCount; i++) {
                int pageNumber = i + 1;
                mainHandler.post(() -> setStatus("OCR PDF page " + pageNumber + "/" + pageCount + "..."));
                android.graphics.pdf.PdfRenderer.Page page = renderer.openPage(i);
                Bitmap bitmap = null;
                try {
                    bitmap = Bitmap.createBitmap(page.getWidth() * 2, page.getHeight() * 2, Bitmap.Config.ARGB_8888);
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    String pageText = processOcrBitmapSync(bitmap);
                    if (!pageText.trim().isEmpty()) {
                        if (text.length() > 0) {
                            text.append("\n\n");
                        }
                        text.append(pageText.trim());
                    }
                } catch (Exception e) {
                    throw new IOException("OCR PDF page " + pageNumber + " failed: " + safeErrorMessage(e), e);
                } finally {
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    page.close();
                }
            }
            String result = text.toString();
            mainHandler.post(() -> acceptFileTranscript(result));
        }
    }

    private void processOcrBitmap(Bitmap bitmap) {
        String preferredOcr = getPreferredModelValue("ocr", languageIndexFor(getLanguageOne()));
        if (OnlineProviderConfig.isOnlineOcrProvider(preferredOcr)) {
            try {
                String text = OnlineAiClient.recognizeText(this, preferredOcr, bitmap, getLanguageOne().code);
                mainHandler.post(() -> acceptFileTranscript(text));
            } catch (Exception e) {
                mainHandler.post(() -> setStatus("Lỗi OCR online: " + safeErrorMessage(e)));
            }
            return;
        }
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = createTextRecognizer();
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    recognizer.close();
                    acceptFileTranscript(result.getText());
                })
                .addOnFailureListener(e -> {
                    recognizer.close();
                    setStatus("Lỗi OCR: " + safeErrorMessage(e));
                });
    }

    private String processOcrBitmapSync(Bitmap bitmap) throws Exception {
        String preferredOcr = getPreferredModelValue("ocr", languageIndexFor(getLanguageOne()));
        if (OnlineProviderConfig.isOnlineOcrProvider(preferredOcr)) {
            return OnlineAiClient.recognizeText(this, preferredOcr, bitmap, getLanguageOne().code);
        }
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = createTextRecognizer();
        try {
            return Tasks.await(recognizer.process(image)).getText();
        } finally {
            recognizer.close();
        }
    }

    private TextRecognizer createTextRecognizer() {
        String code = getLanguageOne().code;
        if ("zh".equals(code)) {
            return TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
        }
        if ("ja".equals(code)) {
            return TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build());
        }
        if ("ko".equals(code)) {
            return TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());
        }
        return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    private String readTextUri(Uri uri) throws IOException {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IOException("Không mở được tệp.");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString("UTF-8");
        }
    }

    private boolean isWordDocument(Uri uri, String mimeType) {
        String normalized = mimeType == null ? "" : mimeType.toLowerCase(Locale.US);
        if ("application/msword".equals(normalized)
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(normalized)) {
            return true;
        }
        String name = getDisplayName(uri).toLowerCase(Locale.US);
        return name.endsWith(".doc") || name.endsWith(".docx");
    }

    private String readWordDocument(Uri uri, String mimeType) throws IOException {
        String name = getDisplayName(uri).toLowerCase(Locale.US);
        String normalized = mimeType == null ? "" : mimeType.toLowerCase(Locale.US);
        if (name.endsWith(".docx")
                || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(normalized)) {
            return readDocxText(uri);
        }
        if (name.endsWith(".doc") || "application/msword".equals(normalized)) {
            return readDocText(uri);
        }
        throw new IOException("Không nhận diện được định dạng Word.");
    }

    private String readDocxText(Uri uri) throws IOException {
        InputStream input = getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new IOException("Không mở được tệp DOCX.");
        }
        try (InputStream inputStream = input;
             ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(inputStream))) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    return parseDocxDocumentXml(zipInput);
                }
                zipInput.closeEntry();
            }
        } catch (Exception e) {
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("Không đọc được DOCX.", e);
        }
        throw new IOException("DOCX không có nội dung văn bản.");
    }

    private String parseDocxDocumentXml(InputStream input) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(input, "UTF-8");
        StringBuilder text = new StringBuilder();
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                String tag = parser.getName();
                if ("t".equals(tag)) {
                    text.append(parser.nextText());
                } else if ("tab".equals(tag)) {
                    text.append('\t');
                } else if ("br".equals(tag) || "cr".equals(tag) || "p".equals(tag)) {
                    appendLineBreak(text);
                }
            }
            event = parser.next();
        }
        return text.toString().replaceAll("[ \\t]+\\n", "\n").trim();
    }

    private String readDocText(Uri uri) throws IOException {
        InputStream input = getContentResolver().openInputStream(uri);
        if (input == null) {
            throw new IOException("Không mở được tệp DOC.");
        }
        try (InputStream inputStream = input;
             HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText().trim();
        }
    }

    private void appendLineBreak(StringBuilder text) {
        int length = text.length();
        if (length > 0 && text.charAt(length - 1) != '\n') {
            text.append('\n');
        }
    }

    private String getDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    return name == null ? "" : name;
                }
            }
        } catch (Exception ignored) {
        }
        String path = uri.getPath();
        return path == null ? "" : path;
    }

    private void acceptFileTranscript(String transcript) {
        String clean = transcript == null ? "" : transcript.trim();
        if (clean.isEmpty()) {
            setStatus("Không tìm thấy transcript trong tệp.");
            return;
        }
        manualTranscriptInput.setText(clean);
        sourceText.setText(clean);
        rememberSource(clean, getLanguageOne());
        updateShareButtons();
        setStatus("Đã dán transcript vào ô nhập transcript.");
    }

    private void translateManualTranscript() {
        String transcript = manualTranscriptInput.getText().toString().trim();
        if (!validateLanguagePair()) {
            return;
        }
        if (transcript.isEmpty()) {
            setStatus("Vui lòng nhập transcript trước khi dịch.");
            return;
        }

        stopListeningSilently();
        sourceText.setText(transcript);
        targetText.setText(t("translating"));
        lastSourceText = transcript;
        lastSourceLanguage = getLanguageOne();
        speakSourceButton.setEnabled(true);
        speakTargetButton.setEnabled(false);
        updateShareButtons();
        detectAndTranslate(transcript);
    }

    private boolean validateLanguagePair() {
        MeetingLanguage first = getLanguageOne();
        MeetingLanguage second = getLanguageTwo();
        if (first.code.equals(second.code)) {
            setStatus("Vui lòng chọn hai ngôn ngữ khác nhau.");
            return false;
        }
        return true;
    }

    @Override
    public void onPartialResult(String hypothesis) {
        String text = extractVoskText(hypothesis, "partial");
        if (!text.isEmpty()) {
            sourceText.setText(text);
        }
    }

    @Override
    public void onResult(String hypothesis) {
        String text = extractVoskText(hypothesis, "text");
        if (!text.isEmpty()) {
            sourceText.setText(text);
            stopSpeechServiceOnly();
            detectAndTranslate(text);
        }
    }

    @Override
    public void onFinalResult(String hypothesis) {
        String text = extractVoskText(hypothesis, "text");
        if (!text.isEmpty()) {
            sourceText.setText(text);
            detectAndTranslate(text);
        }
    }

    @Override
    public void onError(Exception exception) {
        setStatus("Lỗi nhận dạng offline: " + exception.getMessage());
        stopListeningSilently();
    }

    @Override
    public void onTimeout() {
        if (isListening) {
            setStatus("Chưa nghe rõ, đang nghe lại...");
            ensureSttModelThenStart();
        }
    }

    private String extractVoskText(String json, String field) {
        try {
            return new JSONObject(json).optString(field, "").trim();
        } catch (JSONException e) {
            return "";
        }
    }

    private void detectAndTranslate(String transcript) {
        if (shouldIgnoreRecognizedTranscript(transcript)) {
            setStatus("Bỏ qua âm thanh phát ra từ ứng dụng. Đang nghe tiếp...");
            queueNextListeningTurn();
            return;
        }
        if (!isAutoLanguageDetectionEnabled()) {
            translateWithFixedDirection(transcript);
            return;
        }

        MeetingLanguage first = getLanguageOne();
        MeetingLanguage second = getLanguageTwo();

        setStatus("Đang phát hiện ngôn ngữ...");
        languageIdentifier.identifyPossibleLanguages(transcript)
                .addOnSuccessListener(candidates -> {
                    MeetingLanguage source = pickSourceLanguage(candidates, first, second);
                    MeetingLanguage target = source.code.equals(first.code) ? second : first;
                    sourceLanguageText.setText(t("source") + ": " + source.name);
                    targetLanguageText.setText(t("target") + ": " + target.name);
                    rememberSource(transcript, source);
                    translateWithAutoDownload(transcript, source, target);
                })
                .addOnFailureListener(e -> {
                    setStatus("Không nhận diện được ngôn ngữ: " + e.getMessage());
                    scheduleRestart();
                });
    }

    private void translateWithFixedDirection(String transcript) {
        if (shouldIgnoreRecognizedTranscript(transcript)) {
            setStatus("Bỏ qua âm thanh phát ra từ ứng dụng. Đang nghe tiếp...");
            queueNextListeningTurn();
            return;
        }
        MeetingLanguage source = getLanguageOne();
        MeetingLanguage target = getLanguageTwo();
        sourceLanguageText.setText(t("source") + ": " + source.name);
        targetLanguageText.setText(t("target") + ": " + target.name);
        setStatus("T\u1ef1 ph\u00e1t hi\u1ec7n ng\u00f4n ng\u1eef \u0111ang t\u1eaft. D\u1ecbch " + source.name + " -> " + target.name + ".");
        rememberSource(transcript, source);
        translateWithAutoDownload(transcript, source, target);
    }

    private MeetingLanguage pickSourceLanguage(List<IdentifiedLanguage> candidates, MeetingLanguage first, MeetingLanguage second) {
        float firstScore = 0f;
        float secondScore = 0f;

        for (IdentifiedLanguage candidate : candidates) {
            String tag = normalizeLanguageTag(candidate.getLanguageTag());
            if (tag.equals(first.code)) {
                firstScore = Math.max(firstScore, candidate.getConfidence());
            }
            if (tag.equals(second.code)) {
                secondScore = Math.max(secondScore, candidate.getConfidence());
            }
        }

        String text = sourceText.getText() == null ? "" : sourceText.getText().toString();
        MeetingLanguage scriptLanguage = inferLanguageByScript(text, first, second);
        if (scriptLanguage != null && (Math.max(firstScore, secondScore) < 0.50f || Math.abs(firstScore - secondScore) < 0.10f)) {
            return scriptLanguage;
        }
        return secondScore > firstScore ? second : first;
    }

    private MeetingLanguage inferLanguageByScript(String text, MeetingLanguage first, MeetingLanguage second) {
        String inferredCode = inferLanguageCodeByScript(text);
        if (inferredCode.isEmpty()) {
            return null;
        }
        if (first != null && first.code.equals(inferredCode)) {
            return first;
        }
        if (second != null && second.code.equals(inferredCode)) {
            return second;
        }
        return null;
    }

    private String inferLanguageCodeByScript(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        int vietnameseMarks = 0;
        int thai = 0;
        int hangul = 0;
        int kana = 0;
        int han = 0;
        String vietnameseCharacters = "ăâđêôơưĂÂĐÊÔƠƯáàảãạấầẩẫậắằẳẵặéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵÁÀẢÃẠẤẦẨẪẬẮẰẲẴẶÉÈẺẼẸẾỀỂỄỆÍÌỈĨỊÓÒỎÕỌỐỒỔỖỘỚỜỞỠỢÚÙỦŨỤỨỪỬỮỰÝỲỶỸỴ";
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            if (block == Character.UnicodeBlock.THAI) {
                thai++;
            } else if (block == Character.UnicodeBlock.HANGUL_SYLLABLES
                    || block == Character.UnicodeBlock.HANGUL_JAMO
                    || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
                hangul++;
            } else if (block == Character.UnicodeBlock.HIRAGANA
                    || block == Character.UnicodeBlock.KATAKANA) {
                kana++;
            } else if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) {
                han++;
            }
            if (vietnameseCharacters.indexOf(c) >= 0) {
                vietnameseMarks++;
            }
        }
        if (thai > 0) {
            return "th";
        }
        if (hangul > 0) {
            return "ko";
        }
        if (kana > 0) {
            return "ja";
        }
        if (han > 0) {
            return "zh";
        }
        if (vietnameseMarks > 0) {
            return "vi";
        }
        return "";
    }

    private String normalizeLanguageTag(String tag) {
        if (tag == null || tag.equals("und")) {
            return "";
        }
        int separator = tag.indexOf('-');
        return separator >= 0 ? tag.substring(0, separator) : tag;
    }

    private void translateWithAutoDownload(String transcript, MeetingLanguage source, MeetingLanguage target) {
        String preferredTranslation = getPreferredModelValue("translation", languageIndexFor(target));
        if ("provider:mymemory-free".equals(preferredTranslation)
                || "provider:libretranslate-free".equals(preferredTranslation)
                || "google:translate-service".equals(preferredTranslation)) {
            translateWithHttpProvider(preferredTranslation, transcript, source, target);
            return;
        }
        if (isConfiguredOnlineProvider(preferredTranslation) && !"google:mlkit-translate".equals(preferredTranslation)) {
            setStatus("Model dịch ưu tiên cần cấu hình API/adapter riêng. Tạm dùng ML Kit on-device.");
        }
        setStatus("Đang tải model dịch nếu cần...");
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(source.translateCode)
                .setTargetLanguage(target.translateCode)
                .build();
        Translator translator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder().build();

        translator.downloadModelIfNeeded(conditions)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return translator.translate(transcript);
                })
                .addOnSuccessListener(translated -> {
                    translator.close();
                    String repaired = repairTranslation(transcript, translated, source, target);
                    targetText.setText(repaired);
                    lastTranslatedText = repaired;
                    lastTargetLanguage = target;
                    speakTargetButton.setEnabled(!repaired.trim().isEmpty());
                    updateShareButtons();
                    addTimelineTurn(transcript, repaired, source, target);
                    setStatus("Đã dịch. Lần sau có thể dùng offline với model này.");
                    speakTranslation(repaired, target);
                })
                .addOnFailureListener(e -> {
                    translator.close();
                    targetText.setText("Không tải hoặc dịch được model.");
                    speakTargetButton.setEnabled(false);
                    updateShareButtons();
                    setStatus("Lỗi dịch: " + e.getMessage());
                    scheduleRestart();
                });
    }

    private void translateWithHttpProvider(String provider, String transcript, MeetingLanguage source, MeetingLanguage target) {
        setStatus("Đang dịch bằng provider trực tuyến...");
        backgroundExecutor.execute(() -> {
            try {
                String translated;
                if ("provider:mymemory-free".equals(provider)) {
                    translated = translateWithMyMemory(transcript, source, target);
                } else if ("google:translate-service".equals(provider)) {
                    translated = translateWithGoogleTranslateService(transcript, source, target);
                } else {
                    translated = translateWithLibreTranslate(transcript, source, target);
                }
                String repaired = repairTranslation(transcript, translated, source, target);
                mainHandler.post(() -> {
                    targetText.setText(repaired);
                    lastTranslatedText = repaired;
                    lastTargetLanguage = target;
                    speakTargetButton.setEnabled(!repaired.trim().isEmpty());
                    updateShareButtons();
                    addTimelineTurn(transcript, repaired, source, target);
                    setStatus("Đã dịch bằng provider trực tuyến.");
                    speakTranslation(repaired, target);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setStatus("Provider dịch lỗi, tạm dùng ML Kit: " + safeErrorMessage(e));
                    translateWithMlKit(transcript, source, target);
                });
            }
        });
    }

    private void translateWithMlKit(String transcript, MeetingLanguage source, MeetingLanguage target) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(source.translateCode)
                .setTargetLanguage(target.translateCode)
                .build();
        Translator translator = Translation.getClient(options);
        DownloadConditions conditions = new DownloadConditions.Builder().build();
        translator.downloadModelIfNeeded(conditions)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return translator.translate(transcript);
                })
                .addOnSuccessListener(translated -> {
                    translator.close();
                    String repaired = repairTranslation(transcript, translated, source, target);
                    targetText.setText(repaired);
                    lastTranslatedText = repaired;
                    lastTargetLanguage = target;
                    speakTargetButton.setEnabled(!repaired.trim().isEmpty());
                    updateShareButtons();
                    addTimelineTurn(transcript, repaired, source, target);
                    setStatus("Đã dịch bằng ML Kit.");
                    speakTranslation(repaired, target);
                })
                .addOnFailureListener(e -> {
                    translator.close();
                    targetText.setText("Không tải hoặc dịch được model.");
                    speakTargetButton.setEnabled(false);
                    updateShareButtons();
                    setStatus("Lỗi dịch: " + e.getMessage());
                    scheduleRestart();
                });
    }

    private void addTimelineTurn(String original, String translated, MeetingLanguage source, MeetingLanguage target) {
        if (timelineListLayout == null || source == null || target == null) {
            return;
        }
        String cleanOriginal = original == null ? "" : original.trim();
        String cleanTranslated = translated == null ? "" : translated.trim();
        if (cleanOriginal.isEmpty() && cleanTranslated.isEmpty()) {
            return;
        }
        if (meetingSessionStore != null) {
            meetingSessionStore.addTurn(cleanOriginal, cleanTranslated, source.name, target.name, source.code, target.code);
        }

        timelineTurnCount++;
        if (timelineEmptyText != null) {
            timelineEmptyText.setVisibility(View.GONE);
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.elevateCard(this, card);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        if (timelineListLayout.getChildCount() > 0) {
            View previousFirst = timelineListLayout.getChildAt(0);
            LinearLayout.LayoutParams previousParams = (LinearLayout.LayoutParams) previousFirst.getLayoutParams();
            previousParams.setMargins(0, dp(10), 0, 0);
            previousFirst.setLayoutParams(previousParams);
        }
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, 0);
        timelineListLayout.addView(card, 0, cardParams);

        TextView meta = new TextView(this);
        meta.setText(uiLabel("turn") + " " + timelineTurnCount + " · " + source.name + " → " + target.name);
        meta.setTextColor(getColor(R.color.meeting_primary_dark));
        meta.setTextSize(13);
        meta.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(meta, new LinearLayout.LayoutParams(-1, -2));

        TextView sourceValue = timelineText(cleanOriginal, 16, false);
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(-1, -2);
        sourceParams.setMargins(0, dp(8), 0, 0);
        card.addView(sourceValue, sourceParams);

        TextView targetValue = timelineText(cleanTranslated, 18, true);
        LinearLayout.LayoutParams targetParams = new LinearLayout.LayoutParams(-1, -2);
        targetParams.setMargins(0, dp(8), 0, 0);
        card.addView(targetValue, targetParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, -2);
        actionParams.setMargins(0, dp(10), 0, 0);
        card.addView(actions, actionParams);

        Button speakOriginal = compactTimelineButton(uiLabel("speak_source_short"));
        speakOriginal.setOnClickListener(v -> speakTranslation(cleanOriginal, source));
        actions.addView(speakOriginal, weightedParams(1, 0));

        Button speakTranslated = compactTimelineButton(uiLabel("speak_target_short"));
        speakTranslated.setOnClickListener(v -> speakTranslation(cleanTranslated, target));
        actions.addView(speakTranslated, weightedParams(1, dp(8)));

        trimTimelineTurns();
        updateSessionExportButton();
    }

    private TextView timelineText(String value, int sizeSp, boolean emphasized) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextColor(getColor(emphasized ? R.color.meeting_text : R.color.meeting_muted));
        text.setTextSize(sizeSp);
        if (emphasized) {
            text.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        return text;
    }

    private Button compactTimelineButton(String label) {
        Button button = new Button(this);
        button.setTransformationMethod(null);
        button.setTextSize(13);
        PremiumUi.secondaryIconButton(this, button, R.drawable.ic_action_volume, label);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        return button;
    }

    private LinearLayout.LayoutParams weightedParams(int weight, int startMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(34), weight);
        params.setMargins(startMargin, 0, 0, 0);
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void trimTimelineTurns() {
        int maxTurns = 20;
        while (timelineListLayout.getChildCount() > maxTurns) {
            timelineListLayout.removeViewAt(timelineListLayout.getChildCount() - 1);
        }
    }

    private String translateWithMyMemory(String text, MeetingLanguage source, MeetingLanguage target) throws IOException, JSONException {
        String query = URLEncoder.encode(text, "UTF-8");
        String langPair = URLEncoder.encode(source.code + "|" + target.code, "UTF-8");
        URL url = new URL("https://api.mymemory.translated.net/get?q=" + query + "&langpair=" + langPair);
        JSONObject json = new JSONObject(readHttpText(url, "GET", null, "application/json"));
        JSONObject responseData = json.optJSONObject("responseData");
        String translated = responseData == null ? "" : responseData.optString("translatedText", "");
        if (translated.trim().isEmpty()) {
            throw new IOException("MyMemory empty response");
        }
        return translated;
    }

    private String translateWithLibreTranslate(String text, MeetingLanguage source, MeetingLanguage target) throws IOException, JSONException {
        String body = "q=" + URLEncoder.encode(text, "UTF-8")
                + "&source=" + URLEncoder.encode(source.code, "UTF-8")
                + "&target=" + URLEncoder.encode(target.code, "UTF-8")
                + "&format=text";
        URL url = new URL("https://libretranslate.de/translate");
        JSONObject json = new JSONObject(readHttpText(url, "POST", body, "application/x-www-form-urlencoded"));
        String translated = json.optString("translatedText", "");
        if (translated.trim().isEmpty()) {
            throw new IOException("LibreTranslate empty response");
        }
        return translated;
    }

    private String translateWithGoogleTranslateService(String text, MeetingLanguage source, MeetingLanguage target)
            throws IOException, JSONException {
        OnlineProviderConfig.ProviderSettings settings =
                OnlineProviderConfig.settings(this, "google:translate-service");
        settings.requireApiKey();
        String body = "q=" + URLEncoder.encode(text, "UTF-8")
                + "&source=" + URLEncoder.encode(source.code, "UTF-8")
                + "&target=" + URLEncoder.encode(target.code, "UTF-8")
                + "&format=text";
        URL url = new URL("https://translation.googleapis.com/language/translate/v2?key="
                + URLEncoder.encode(settings.apiKey, "UTF-8"));
        JSONObject json = new JSONObject(readHttpText(url, "POST", body, "application/x-www-form-urlencoded"));
        JSONObject data = json.optJSONObject("data");
        JSONArray translations = data == null ? null : data.optJSONArray("translations");
        JSONObject first = translations == null || translations.length() == 0 ? null : translations.optJSONObject(0);
        String translated = first == null ? "" : first.optString("translatedText", "");
        if (translated.trim().isEmpty()) {
            throw new IOException("Google Translate empty response");
        }
        return translated;
    }

    private String repairTranslation(String sourceTextValue, String translated, MeetingLanguage source, MeetingLanguage target) {
        String cleanSource = sourceTextValue == null ? "" : sourceTextValue.trim();
        String cleanTranslated = translated == null ? "" : translated.trim();
        if (cleanSource.isEmpty() || cleanTranslated.isEmpty()) {
            return cleanTranslated;
        }

        if ("vi".equals(source.code) && "en".equals(target.code)) {
            String lowerSource = cleanSource.toLowerCase(Locale.ROOT);
            String lowerTranslated = cleanTranslated.toLowerCase(Locale.ROOT);
            String name = tailAfterAny(lowerSource, cleanSource,
                    "tôi tên là ", "tôi tên ", "tên tôi là ", "mình tên là ", "mình tên ");
            if (!name.isEmpty() && (lowerTranslated.equals("my name") || lowerTranslated.equals("my name is")
                    || lowerTranslated.endsWith("my name"))) {
                return "my name is " + name;
            }
            String identity = tailAfterAny(lowerSource, cleanSource, "tôi là ", "mình là ");
            if (!identity.isEmpty() && (lowerTranslated.equals("i am") || lowerTranslated.equals("i'm")
                    || lowerTranslated.equals("i"))) {
                return "I am " + identity;
            }
        }

        return cleanTranslated;
    }

    private String tailAfterAny(String lowerSource, String originalSource, String... prefixes) {
        for (String prefix : prefixes) {
            if (lowerSource.startsWith(prefix)) {
                return originalSource.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private void speakTranslation(String translated, MeetingLanguage target) {
        if (translated.trim().isEmpty()) {
            setStatus("Không có nội dung để phát âm.");
            scheduleRestart();
            return;
        }

        rememberAppSpeech(translated);
        int token = ++speechPlaybackToken;
        ensureInternalTtsThenSpeak(translated, target, token);
    }

    private void ensureInternalTtsThenSpeak(String text, MeetingLanguage target, int token) {
        if (ttsBusy) {
            setStatus("TTS đang phát âm, vui lòng chờ.");
            return;
        }
        String preferredTts = getPreferredModelValue("tts", languageIndexFor(target));
        if ("google:text-to-speech".equals(preferredTts)) {
            speakWithAndroidTts(text, target, token);
            return;
        }
        if ("provider:streamelements-tts-free".equals(preferredTts)) {
            playRemoteTtsUrl(streamElementsTtsUrl(text), "StreamElements TTS", token);
            return;
        }
        if (OnlineProviderConfig.isOnlineTtsProvider(preferredTts)) {
            speakWithOnlineTts(preferredTts, text, target, token);
            return;
        }
        if (ttsBusy) {
            setStatus("TTS đang phát âm, vui lòng chờ.");
            return;
        }
        File ttsParent = AppStorage.modelDir(this, "tts");
        File modelDir = getPreferredTtsModelDir(target);
        File defaultModelDir = TtsModelCatalog.defaultModelDir(ttsParent, target.code);
        if (!TtsModelCatalog.looksLikeUsableModel(modelDir) && !modelDir.equals(defaultModelDir)) {
            setStatus("Model phát âm ưu tiên không khả dụng. Tạm dùng " + TtsModelCatalog.displayName(defaultModelDir) + ".");
            modelDir = defaultModelDir;
        }
        if (!TtsModelCatalog.looksLikeUsableModel(modelDir)) {
            ttsBusy = true;
            setSpeakButtonsEnabled(false);
            setStatus("Đang tải TTS engine/voice lần đầu...");
            File finalModelDir = modelDir;
            backgroundExecutor.execute(() -> {
                try {
                    downloadAndExtractTarBz2WithProgress(TtsModelCatalog.defaultModelUrl(finalModelDir.getName()),
                            ttsParent, "TTS " + TtsModelCatalog.displayName(finalModelDir));
                    mainHandler.post(() -> {
                        if (!isCurrentSpeechToken(token)) {
                            return;
                        }
                        ttsBusy = false;
                        restoreSpeakButtonsEnabled();
                        updateReadinessSummary();
                        synthesizeAndPlay(text, target, finalModelDir, token);
                    });
                } catch (IOException e) {
                    mainHandler.post(() -> {
                        if (!isCurrentSpeechToken(token)) {
                            return;
                        }
                        ttsBusy = false;
                        restoreSpeakButtonsEnabled();
                        setStatus("Không tải được TTS model: " + e.getMessage());
                        scheduleRestart();
                    });
                }
            });
            return;
        }

        synthesizeAndPlay(text, target, modelDir, token);
    }

    private boolean looksLikeSupertonicModel(File modelDir) {
        return TtsModelCatalog.looksLikeSupertonicModel(modelDir);
    }

    private boolean isCurrentSpeechToken(int token) {
        return token == speechPlaybackToken;
    }

    private void synthesizeAndPlay(String text, MeetingLanguage target, File modelDir, int token) {
        ttsBusy = true;
        setSpeakButtonsEnabled(false);
        setStatus("Đang sinh giọng nói bằng TTS nội bộ...");
        backgroundExecutor.execute(() -> {
            try {
                GenerationConfig generationConfig = new GenerationConfig();
                generationConfig.setSpeed(1.0f);
                generationConfig.setExtra(java.util.Collections.singletonMap("lang", target.code));
                GeneratedAudio audio;
                try {
                    OfflineTts tts = getInternalTts(modelDir, AcceleratorConfig.preferredOnnxProvider());
                    audio = tts.generateWithConfig(text, generationConfig);
                } catch (Throwable e) {
                    if (!AcceleratorConfig.prefersNnapi()) {
                        throw new RuntimeException(e);
                    }
                    releaseInternalTts();
                    OfflineTts tts = getInternalTts(modelDir, AcceleratorConfig.CPU_PROVIDER);
                    audio = tts.generateWithConfig(text, generationConfig);
                }
                File wavFile = new File(getFilesDir(), "last-translation.wav");
                boolean saved = audio.getSamples().length > 0 && audio.save(wavFile.getAbsolutePath());
                mainHandler.post(() -> {
                    if (!isCurrentSpeechToken(token)) {
                        return;
                    }
                    if (!saved) {
                        ttsBusy = false;
                        restoreSpeakButtonsEnabled();
                        setStatus("TTS nội bộ không tạo được âm thanh.");
                        scheduleRestart();
                        return;
                    }
                    playGeneratedSpeech(wavFile, token);
                });
            } catch (Error e) {
                Log.e(TAG, "Internal TTS failed", e);
                mainHandler.post(() -> {
                    if (!isCurrentSpeechToken(token)) {
                        return;
                    }
                    ttsBusy = false;
                    restoreSpeakButtonsEnabled();
                    setStatus("L\u1ed7i TTS n\u1ed9i b\u1ed9: " + safeErrorMessage(e));
                    scheduleRestart();
                });
            } catch (Exception e) {
                Log.e(TAG, "Internal TTS failed", e);
                mainHandler.post(() -> {
                    if (!isCurrentSpeechToken(token)) {
                        return;
                    }
                    ttsBusy = false;
                    restoreSpeakButtonsEnabled();
                    setStatus("Lỗi TTS nội bộ: " + e.getMessage());
                    scheduleRestart();
                });
            }
        });
    }

    private String safeErrorMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = error.getClass().getSimpleName();
        }
        return message;
    }

    private String readHttpText(URL url, String method, String body, String contentType) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", "application/json,text/plain,*/*");
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", contentType);
            try (java.io.OutputStream output = connection.getOutputStream()) {
                output.write(body.getBytes("UTF-8"));
            }
        }
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        StringBuilder builder = new StringBuilder();
        try {
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                }
            }
        } finally {
            connection.disconnect();
        }
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + ": " + builder);
        }
        return builder.toString();
    }

    private void ensureDirectory(File directory, String message) throws IOException {
        if (directory == null || (directory.exists() && !directory.isDirectory())) {
            throw new IOException(message);
        }
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException(message);
        }
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

    private OfflineTts getInternalTts(File modelDir, String provider) {
        if (internalTts != null && modelDir.equals(internalTtsModelDir) && provider.equals(internalTtsProvider)) {
            return internalTts;
        }
        releaseInternalTts();
        OfflineTtsConfig config = TtsModelCatalog.createConfig(modelDir, provider, AcceleratorConfig.ttsThreads());
        internalTts = new OfflineTts(null, config);
        internalTtsModelDir = modelDir;
        internalTtsProvider = provider;
        return internalTts;
    }

    private void releaseInternalTts() {
        if (internalTts != null) {
            internalTts.release();
            internalTts = null;
        }
        internalTtsModelDir = null;
        internalTtsProvider = null;
    }

    private void playGeneratedSpeech(File wavFile, int token) {
        try {
            if (!isCurrentSpeechToken(token)) {
                return;
            }
            ttsBusy = true;
            setSpeakButtonsEnabled(false);
            updateStopSpeechButton();
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            mediaPlayer = MediaPlayer.create(this, Uri.fromFile(wavFile));
            if (mediaPlayer == null) {
                throw new IOException("MediaPlayer could not open generated audio.");
            }
            mediaPlayer.setOnCompletionListener(player -> {
                if (!isCurrentSpeechToken(token)) {
                    return;
                }
                player.release();
                if (mediaPlayer == player) {
                    mediaPlayer = null;
                }
                ttsBusy = false;
                restoreSpeakButtonsEnabled();
                if (isListening) {
                    ensureSttModelThenStart();
                } else {
                    setStatus("Đã phát âm xong.");
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
                ttsBusy = false;
                restoreSpeakButtonsEnabled();
                setStatus("Lỗi phát âm bản dịch.");
                scheduleRestart();
                return true;
            });
            mediaPlayer.start();
            setStatus("Đang phát âm bản dịch...");
        } catch (Exception e) {
            ttsBusy = false;
            restoreSpeakButtonsEnabled();
            updateStopSpeechButton();
            setStatus("Không phát được âm thanh TTS: " + e.getMessage());
            scheduleRestart();
        }
    }

    private void speakWithAndroidTts(String text, MeetingLanguage target, int token) {
        ttsBusy = true;
        setSpeakButtonsEnabled(false);
        setStatus("Đang phát âm bằng Android/Google Text-to-Speech...");
        if (androidTts != null) {
            androidTts.shutdown();
            androidTts = null;
        }
        androidTts = new TextToSpeech(this, status -> {
            if (!isCurrentSpeechToken(token)) {
                return;
            }
            if (status != TextToSpeech.SUCCESS || androidTts == null) {
                ttsBusy = false;
                restoreSpeakButtonsEnabled();
                setStatus("Android Text-to-Speech không sẵn sàng.");
                scheduleRestart();
                return;
            }
            int languageStatus = androidTts.setLanguage(target.ttsLocale);
            if (languageStatus == TextToSpeech.LANG_MISSING_DATA || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsBusy = false;
                restoreSpeakButtonsEnabled();
                setStatus("Android Text-to-Speech chưa có voice cho " + target.name + ".");
                scheduleRestart();
                return;
            }
            androidTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                }

                @Override
                public void onDone(String utteranceId) {
                    mainHandler.post(() -> {
                        if (!isCurrentSpeechToken(token)) {
                            return;
                        }
                        ttsBusy = false;
                        restoreSpeakButtonsEnabled();
                        setStatus("Đã phát âm xong.");
                        if (isListening) {
                            ensureSttModelThenStart();
                        }
                    });
                }

                @Override
                @SuppressWarnings("deprecation")
                public void onError(String utteranceId) {
                    mainHandler.post(() -> {
                        if (!isCurrentSpeechToken(token)) {
                            return;
                        }
                        ttsBusy = false;
                        restoreSpeakButtonsEnabled();
                        setStatus("Lỗi Android Text-to-Speech.");
                        scheduleRestart();
                    });
                }
            });
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "meeting-tts");
            androidTts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "meeting-tts");
        });
    }

    private void speakWithOnlineTts(String providerValue, String text, MeetingLanguage target, int token) {
        ttsBusy = true;
        setSpeakButtonsEnabled(false);
        setStatus("Đang tạo âm thanh bằng TTS online...");
        backgroundExecutor.execute(() -> {
            try {
                File audioFile = OnlineAiClient.synthesizeSpeech(this, providerValue, text, target.code);
                mainHandler.post(() -> {
                    if (isCurrentSpeechToken(token)) {
                        playGeneratedSpeech(audioFile, token);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isCurrentSpeechToken(token)) {
                        return;
                    }
                    ttsBusy = false;
                    restoreSpeakButtonsEnabled();
                    setStatus("Lỗi TTS online: " + safeErrorMessage(e));
                    scheduleRestart();
                });
            }
        });
    }

    private String streamElementsTtsUrl(String text) {
        try {
            return "https://api.streamelements.com/kappa/v2/speech?voice=Brian&text="
                    + URLEncoder.encode(text, "UTF-8");
        } catch (Exception e) {
            return "https://api.streamelements.com/kappa/v2/speech?voice=Brian&text=";
        }
    }

    private void playRemoteTtsUrl(String url, String providerName, int token) {
        ttsBusy = true;
        setSpeakButtonsEnabled(false);
        setStatus("Đang phát âm bằng " + providerName + "...");
        try {
            if (!isCurrentSpeechToken(token)) {
                return;
            }
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnCompletionListener(player -> {
                if (!isCurrentSpeechToken(token)) {
                    return;
                }
                player.release();
                if (mediaPlayer == player) {
                    mediaPlayer = null;
                }
                ttsBusy = false;
                restoreSpeakButtonsEnabled();
                setStatus("Đã phát âm xong.");
                if (isListening) {
                    ensureSttModelThenStart();
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
                ttsBusy = false;
                restoreSpeakButtonsEnabled();
                setStatus(providerName + " lỗi, tạm dừng phát âm.");
                scheduleRestart();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            ttsBusy = false;
            restoreSpeakButtonsEnabled();
            setStatus(providerName + " lỗi: " + safeErrorMessage(e));
            scheduleRestart();
        }
    }

    private void rememberSource(String text, MeetingLanguage language) {
        lastSourceText = text == null ? "" : text;
        lastSourceLanguage = language;
        speakSourceButton.setEnabled(!lastSourceText.trim().isEmpty() && lastSourceLanguage != null);
        updateShareButtons();
    }

    private void setSpeakButtonsEnabled(boolean enabled) {
        speakSourceButton.setEnabled(enabled && !lastSourceText.trim().isEmpty() && lastSourceLanguage != null);
        speakTargetButton.setEnabled(enabled && !lastTranslatedText.trim().isEmpty() && lastTargetLanguage != null);
        updateShareButtons();
        updateStopSpeechButton();
    }

    private void restoreSpeakButtonsEnabled() {
        setSpeakButtonsEnabled(true);
    }

    private void updateShareButtons() {
        if (shareSourceButton != null) {
            shareSourceButton.setEnabled(!lastSourceText.trim().isEmpty() && lastSourceLanguage != null);
            shareSourceButton.setAlpha(shareSourceButton.isEnabled() ? 1.0f : 0.45f);
        }
        if (shareTargetButton != null) {
            shareTargetButton.setEnabled(!lastTranslatedText.trim().isEmpty() && lastTargetLanguage != null);
            shareTargetButton.setAlpha(shareTargetButton.isEnabled() ? 1.0f : 0.45f);
        }
    }

    private void updateSessionExportButton() {
        if (meetingSessionStore == null) {
            return;
        }
        boolean enabled = meetingSessionStore.hasTurns();
        if (exportSessionButton != null) {
            exportSessionButton.setEnabled(enabled);
            exportSessionButton.setAlpha(enabled ? 1.0f : 0.45f);
        }
        if (resetSessionButton != null) {
            resetSessionButton.setEnabled(enabled);
            resetSessionButton.setAlpha(enabled ? 1.0f : 0.45f);
        }
    }

    private void updateStopSpeechButton() {
        if (stopSpeechButton == null) {
            return;
        }
        stopSpeechButton.setEnabled(ttsBusy);
        stopSpeechButton.setAlpha(ttsBusy ? 1.0f : 0.45f);
    }

    private void restoreSessionTimeline() {
        if (meetingSessionStore == null || timelineListLayout == null) {
            return;
        }
        List<MeetingSessionStore.Turn> turns = meetingSessionStore.turns();
        if (turns.isEmpty()) {
            updateSessionExportButton();
            return;
        }
        timelineListLayout.removeAllViews();
        timelineTurnCount = 0;
        for (MeetingSessionStore.Turn turn : turns) {
            renderStoredTimelineTurn(turn);
        }
        updateSessionExportButton();
    }

    private void renderStoredTimelineTurn(MeetingSessionStore.Turn turn) {
        timelineTurnCount++;
        if (timelineEmptyText != null) {
            timelineEmptyText.setVisibility(View.GONE);
        }

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        PremiumUi.elevateCard(this, card);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        if (timelineListLayout.getChildCount() > 0) {
            View previousFirst = timelineListLayout.getChildAt(0);
            LinearLayout.LayoutParams previousParams = (LinearLayout.LayoutParams) previousFirst.getLayoutParams();
            previousParams.setMargins(0, dp(10), 0, 0);
            previousFirst.setLayoutParams(previousParams);
        }
        timelineListLayout.addView(card, 0, new LinearLayout.LayoutParams(-1, -2));

        TextView meta = new TextView(this);
        meta.setText(uiLabel("turn") + " " + timelineTurnCount + " · " + turn.sourceName + " → " + turn.targetName);
        meta.setTextColor(getColor(R.color.meeting_primary_dark));
        meta.setTextSize(13);
        meta.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(meta, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(-1, -2);
        sourceParams.setMargins(0, dp(8), 0, 0);
        card.addView(timelineText(turn.original, 16, false), sourceParams);

        LinearLayout.LayoutParams targetParams = new LinearLayout.LayoutParams(-1, -2);
        targetParams.setMargins(0, dp(8), 0, 0);
        card.addView(timelineText(turn.translated, 18, true), targetParams);

        MeetingLanguage source = findLanguageByCode(turn.sourceCode);
        MeetingLanguage target = findLanguageByCode(turn.targetCode);
        if (source != null || target != null) {
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, -2);
            actionParams.setMargins(0, dp(10), 0, 0);
            card.addView(actions, actionParams);

            Button speakOriginal = compactTimelineButton(uiLabel("speak_source_short"));
            speakOriginal.setEnabled(source != null);
            speakOriginal.setAlpha(source == null ? 0.45f : 1.0f);
            speakOriginal.setOnClickListener(v -> {
                if (source != null) {
                    speakTranslation(turn.original, source);
                }
            });
            actions.addView(speakOriginal, weightedParams(1, 0));

            Button speakTranslated = compactTimelineButton(uiLabel("speak_target_short"));
            speakTranslated.setEnabled(target != null);
            speakTranslated.setAlpha(target == null ? 0.45f : 1.0f);
            speakTranslated.setOnClickListener(v -> {
                if (target != null) {
                    speakTranslation(turn.translated, target);
                }
            });
            actions.addView(speakTranslated, weightedParams(1, dp(8)));
        }
    }

    private MeetingLanguage findLanguageByCode(String code) {
        if (code == null) {
            return null;
        }
        for (MeetingLanguage language : languages) {
            if (code.equals(language.code)) {
                return language;
            }
        }
        return null;
    }

    private void shareSourceText() {
        if (lastSourceText.trim().isEmpty() || lastSourceLanguage == null) {
            setStatus("Chưa có transcript nguồn để chia sẻ.");
            return;
        }
        shareText(t("source") + ": " + lastSourceLanguage.name, lastSourceText);
    }

    private void shareTargetText() {
        if (lastTranslatedText.trim().isEmpty() || lastTargetLanguage == null) {
            setStatus("Chưa có bản dịch để chia sẻ.");
            return;
        }
        shareText(t("target") + ": " + lastTargetLanguage.name, lastTranslatedText);
    }

    private void shareText(String title, String text) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(sendIntent, t("share")));
    }

    private void exportMeetingSession() {
        if (meetingSessionStore == null || !meetingSessionStore.hasTurns()) {
            setStatus("Chưa có phiên họp để xuất.");
            return;
        }
        String title = sessionExportTitle();
        shareText(title, meetingSessionStore.exportText(title));
    }

    private String sessionExportTitle() {
        MeetingLanguage first = getLanguageOne();
        MeetingLanguage second = getLanguageTwo();
        return "AI Meeting - " + first.name + " / " + second.name;
    }

    private String sessionExportLabel() {
        if (isVietnameseUi()) {
            return "Xuất phiên";
        }
        if (!isVietnameseUi()) {
            return "Export";
        }
        MeetingLanguage language = getLanguageOne();
        return language != null && "vi".equals(language.code) ? "Xuất phiên" : "Export";
    }

    private String resetSessionLabel() {
        return "Reset";
    }

    private String resetSessionDescription() {
        if (isVietnameseUi()) {
            return "Reset cu\u1ed9c h\u1ed9i tho\u1ea1i";
        }
        if (!isVietnameseUi()) {
            return "Reset conversation";
        }
        MeetingLanguage language = getLanguageOne();
        return language != null && "vi".equals(language.code) ? "Reset cu\u1ed9c h\u1ed9i tho\u1ea1i" : "Reset conversation";
    }

    private String stopSpeechLabel() {
        if (isVietnameseUi()) {
            return "D\u1eebng ph\u00e1t";
        }
        if (!isVietnameseUi()) {
            return "Stop speech";
        }
        MeetingLanguage language = getLanguageOne();
        return language != null && "vi".equals(language.code) ? "D\u1eebng ph\u00e1t" : "Stop speech";
    }

    private String stopSpeechDescription() {
        if (isVietnameseUi()) {
            return "Ng\u1eebng ph\u00e1t \u00e2m";
        }
        if (!isVietnameseUi()) {
            return "Stop speech playback";
        }
        MeetingLanguage language = getLanguageOne();
        return language != null && "vi".equals(language.code) ? "Ng\u1eebng ph\u00e1t \u00e2m" : "Stop speech playback";
    }

    private void confirmResetMeetingSession() {
        boolean vietnamese = isVietnameseUi();
        new android.app.AlertDialog.Builder(this)
                .setTitle(vietnamese ? "Reset cu\u1ed9c h\u1ed9i tho\u1ea1i?" : "Reset conversation?")
                .setMessage(vietnamese
                        ? "To\u00e0n b\u1ed9 transcript v\u00e0 b\u1ea3n d\u1ecbch trong phi\u00ean hi\u1ec7n t\u1ea1i s\u1ebd b\u1ecb x\u00f3a."
                        : "All transcripts and translations in the current session will be cleared.")
                .setNegativeButton(vietnamese ? "H\u1ee7y" : "Cancel", null)
                .setPositiveButton("Reset", (dialog, which) -> resetMeetingSession())
                .show();
    }

    private void resetMeetingSession() {
        stopListeningSilently();
        stopSpeechPlayback(false);
        if (meetingSessionStore != null) {
            meetingSessionStore.reset();
        }
        if (timelineListLayout != null) {
            timelineListLayout.removeAllViews();
        }
        timelineTurnCount = 0;
        lastSourceText = "";
        lastTranslatedText = "";
        lastSourceLanguage = null;
        lastTargetLanguage = null;
        selectedFileUri = null;
        selectedFileMimeType = null;
        manualTranscriptInput.setText("");
        sourceLanguageText.setText(t("source") + ": --");
        targetLanguageText.setText(t("target") + ": --");
        sourceText.setText(t("source_placeholder"));
        targetText.setText(t("target_placeholder"));
        if (timelineEmptyText != null) {
            timelineEmptyText.setVisibility(View.VISIBLE);
            timelineEmptyText.setText(uiLabel("timeline_empty"));
        }
        restoreSpeakButtonsEnabled();
        updateSessionExportButton();
        setStatus(getLanguageOne() != null && "vi".equals(getLanguageOne().code)
                ? "\u0110\u00e3 reset cu\u1ed9c h\u1ed9i tho\u1ea1i."
                : "Conversation reset.");
    }

    private void speakLastSource() {
        if (lastSourceText.trim().isEmpty() || lastSourceLanguage == null) {
            setStatus("Chưa có transcript nguồn để phát âm.");
            return;
        }
        speakTranslation(lastSourceText, lastSourceLanguage);
    }

    private void speakLastTarget() {
        if (lastTranslatedText.trim().isEmpty() || lastTargetLanguage == null) {
            setStatus("Chưa có bản dịch để phát âm.");
            return;
        }
        speakTranslation(lastTranslatedText, lastTargetLanguage);
    }

    private void replayLastTranslation() {
        if (lastTranslatedText.trim().isEmpty() || lastTargetLanguage == null) {
            setStatus("Chưa có bản dịch để phát âm.");
            return;
        }
        speakTranslation(lastTranslatedText, lastTargetLanguage);
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
        if (androidTts != null) {
            androidTts.stop();
            androidTts.shutdown();
            androidTts = null;
        }
        ttsBusy = false;
        restoreSpeakButtonsEnabled();
        updateStopSpeechButton();
        if (reportStatus) {
            setStatus(getLanguageOne() != null && "vi".equals(getLanguageOne().code)
                    ? "\u0110\u00e3 ng\u1eebng ph\u00e1t \u00e2m."
                    : "Speech playback stopped.");
            if (isListening) {
                ensureSttModelThenStart();
            }
        }
    }

    private void stopListening() {
        stopListeningSilently();
        setStatus("Đã dừng nghe.");
    }

    private void stopListeningSilently() {
        isListening = false;
        recognitionInProgress = false;
        updateMeetingState();
        stopSpeechServiceOnly();
        stopAudioRecordOnly();
        stopSpeechPlayback(false);
    }

    private void stopSpeechServiceOnly() {
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
        }
        if (googleSpeechRecognizer != null) {
            googleSpeechRecognizer.cancel();
            googleSpeechRecognizer.destroy();
            googleSpeechRecognizer = null;
        }
    }

    private void stopAudioRecordOnly() {
        recognitionActive = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (IllegalStateException ignored) {
            }
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void closeActiveVoskModel() {
        if (activeVoskModel != null) {
            activeVoskModel.close();
            activeVoskModel = null;
        }
        activeVoskModelDir = null;
    }

    private void scheduleRestart() {
        mainHandler.postDelayed(() -> {
            if (isListening && !recognitionInProgress) {
                if (recognitionRetryCount >= MAX_RECOGNITION_RETRIES) {
                    stopListeningSilently();
                    setStatus("Đã dừng vì chưa nhận dạng được. Hãy thử nói rõ hơn hoặc kiểm tra quyền micro.");
                    return;
                }
                recognitionRetryCount++;
                ensureSttModelThenStart();
            }
        }, 650);
    }

    private void queueNextListeningTurn() {
        if (!isListening || recognitionInProgress) {
            return;
        }
        mainHandler.postDelayed(() -> {
            if (isListening && !recognitionInProgress) {
                ensureSttModelThenStart();
            }
        }, 120);
    }

    private void rememberAppSpeech(String text) {
        lastAppSpokenText = text == null ? "" : text;
        lastAppSpokenAt = System.currentTimeMillis();
    }

    private boolean shouldIgnoreRecognizedTranscript(String transcript) {
        if (!isListening || transcript == null || transcript.trim().isEmpty()) {
            return false;
        }
        if (lastAppSpokenText == null || lastAppSpokenText.trim().isEmpty()) {
            return false;
        }
        if (System.currentTimeMillis() - lastAppSpokenAt > 20000L) {
            return false;
        }
        String heard = normalizeForEchoCheck(transcript);
        String spoken = normalizeForEchoCheck(lastAppSpokenText);
        if (heard.length() < 12 || spoken.length() < 12) {
            return false;
        }
        return heard.equals(spoken) || heard.contains(spoken) || spoken.contains(heard);
    }

    private String normalizeForEchoCheck(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private MeetingLanguage getLanguageOne() {
        Object selected = languageOneSpinner == null ? null : languageOneSpinner.getSelectedItem();
        if (selected instanceof MeetingLanguage) {
            return (MeetingLanguage) selected;
        }
        return languages.isEmpty() ? null : languages.get(0);
    }

    private MeetingLanguage getLanguageTwo() {
        Object selected = languageTwoSpinner == null ? null : languageTwoSpinner.getSelectedItem();
        if (selected instanceof MeetingLanguage) {
            return (MeetingLanguage) selected;
        }
        if (languages.size() > 1) {
            return languages.get(1);
        }
        return languages.isEmpty() ? null : languages.get(0);
    }

    private String getUiLanguageCode() {
        return selectedUiLanguageCode == null || selectedUiLanguageCode.trim().isEmpty()
                ? "vi" : selectedUiLanguageCode;
    }

    private String getUiLanguageName() {
        UiLanguageChoice choice = getUiLanguageChoice();
        return choice == null ? "Ti\u1ebfng Vi\u1ec7t" : choice.name;
    }

    private UiLanguageChoice getUiLanguageChoice() {
        String code = getUiLanguageCode();
        for (UiLanguageChoice choice : uiLanguageChoices) {
            if (code.equals(choice.code)) {
                return choice;
            }
        }
        return uiLanguageChoices.isEmpty() ? null : uiLanguageChoices.get(0);
    }

    private int uiLanguageIndex(String code) {
        for (int i = 0; i < uiLanguageChoices.size(); i++) {
            if (uiLanguageChoices.get(i).code.equals(code)) {
                return i;
            }
        }
        return 0;
    }

    private boolean isVietnameseUi() {
        return "vi".equals(getUiLanguageCode());
    }

    private boolean isAutoLanguageDetectionEnabled() {
        return autoDetectLanguageSwitch == null || autoDetectLanguageSwitch.isChecked();
    }

    private SttModelChoice getSelectedSttModel() {
        return (SttModelChoice) sttModelSpinner.getSelectedItem();
    }

    private SttModelChoice getPreferredSttModelForLanguage(int languageIndex, String languageCode) {
        File preferred = getPreferredLocalModel("stt", languageIndex);
        if (preferred != null && looksLikeVoskModel(preferred)) {
            return SttModelChoice.local(preferred.getName(), preferred);
        }
        String preferredValue = getPreferredModelValue("stt", languageIndex);
        if (isConfiguredOnlineProvider(preferredValue) && !OnlineProviderConfig.isOnlineSttProvider(preferredValue)) {
            setStatus("Model nhận dạng ưu tiên chưa có runtime trong app. Tạm dùng Vosk local.");
        }
        return getSttModelForLanguage(languageCode);
    }

    private SttModelChoice getSttModelForLanguage(String languageCode) {
        String expectedModelName = getDefaultSttModelNameForLanguage(languageCode);
        if (expectedModelName == null) {
            return null;
        }
        for (SttModelChoice choice : sttModels) {
            if (expectedModelName.equals(choice.modelName)) {
                return choice;
            }
            if (choice.modelDir != null && expectedModelName.equals(choice.modelDir.getName())) {
                return choice;
            }
        }
        return null;
    }

    private File getPreferredTtsModelDir(MeetingLanguage language) {
        File preferred = getPreferredLocalModel("tts", languageIndexFor(language));
        if (preferred != null) {
            return preferred;
        }
        String preferredValue = getPreferredModelValue("tts", languageIndexFor(language));
        if (isConfiguredOnlineProvider(preferredValue) && !OnlineProviderConfig.isOnlineTtsProvider(preferredValue)) {
            setStatus("Model phát âm ưu tiên chưa có runtime trong app. Tạm dùng TTS offline mặc định.");
        }
        return TtsModelCatalog.defaultModelDir(AppStorage.modelDir(this, "tts"), language.code);
    }

    private File getPreferredLocalModel(String tab, int languageIndex) {
        String value = getPreferredModelValue(tab, languageIndex);
        if (value.startsWith(SettingsActivity.LOCAL_PREFIX)) {
            String path = value.substring(SettingsActivity.LOCAL_PREFIX.length()).trim();
            return path.isEmpty() ? null : new File(path);
        }
        return null;
    }

    private String getPreferredModelValue(String tab, int languageIndex) {
        return OnlineProviderConfig.safeString(
                getSharedPreferences(SettingsActivity.PREFS, MODE_PRIVATE),
                SettingsActivity.preferenceKey(tab, languageIndex),
                "");
    }

    private boolean isConfiguredOnlineProvider(String value) {
        return value != null && (value.startsWith("google:") || value.startsWith("provider:"));
    }

    private boolean usesGoogleSpeechRecognizer(int languageIndex) {
        return "google:speech-recognizer".equals(getPreferredModelValue("stt", languageIndex));
    }

    private int languageIndexFor(MeetingLanguage language) {
        MeetingLanguage first = getLanguageOne();
        MeetingLanguage second = getLanguageTwo();
        if (language != null && second != null && language.code.equals(second.code)) {
            return 2;
        }
        if (language != null && first != null && language.code.equals(first.code)) {
            return 1;
        }
        return 1;
    }

    private String getDefaultSttModelNameForLanguage(String languageCode) {
        switch (languageCode) {
            case "vi":
                return "vosk-model-vn-0.4";
            case "en":
                return "vosk-model-en-us-0.22";
            case "ja":
                return "vosk-model-ja-0.22";
            case "ko":
                return "vosk-model-small-ko-0.22";
            case "zh":
                return "vosk-model-cn-0.22";
            case "fr":
                return "vosk-model-fr-0.22";
            case "de":
                return "vosk-model-de-0.21";
            case "es":
                return "vosk-model-es-0.42";
            default:
                return null;
        }
    }

    private TtsVoiceChoice getSelectedTtsVoice() {
        if (ttsVoiceSpinner.getSelectedItem() == null) {
            return null;
        }
        return (TtsVoiceChoice) ttsVoiceSpinner.getSelectedItem();
    }

    private void setStatus(String message) {
        statusText.setText(message);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListeningWithPermission();
        } else if (requestCode == REQUEST_RECORD_AUDIO) {
            setStatus("Ứng dụng cần quyền micro để hỗ trợ meeting.");
        }
    }

    @Override
    protected void onStop() {
        if (isListening || recognitionInProgress || ttsBusy) {
            stopListeningSilently();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSpeechServiceOnly();
        closeActiveVoskModel();
        releaseInternalTts();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (androidTts != null) {
            androidTts.shutdown();
            androidTts = null;
        }
        backgroundExecutor.shutdownNow();
        if (languageIdentifier != null) {
            languageIdentifier.close();
        }
    }

    private static class MeetingLanguage {
        final String name;
        final String code;
        final String translateCode;
        final Locale ttsLocale;

        MeetingLanguage(String name, String code, String translateCode, Locale ttsLocale) {
            this.name = name;
            this.code = code;
            this.translateCode = translateCode;
            this.ttsLocale = ttsLocale;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class UiLanguageChoice {
        final String flag;
        final String code;
        final String name;

        UiLanguageChoice(String flag, String code, String name) {
            this.flag = flag;
            this.code = code;
            this.name = name;
        }

        @Override
        public String toString() {
            return flag;
        }
    }

    private static class SttModelChoice {
        final String name;
        final String languageCode;
        final String modelName;
        final File modelDir;
        final String downloadUrl;

        private SttModelChoice(String name, String languageCode, String modelName, File modelDir, String downloadUrl) {
            this.name = name;
            this.languageCode = languageCode;
            this.modelName = modelName;
            this.modelDir = modelDir;
            this.downloadUrl = downloadUrl;
        }

        static SttModelChoice local(String name, File modelDir) {
            return new SttModelChoice(name + " (đã tải)", "", modelDir.getName(), modelDir, null);
        }

        static SttModelChoice downloadable(String label, String languageCode, String modelName, File modelDir, String downloadUrl) {
            return new SttModelChoice(label + " (tự tải lần đầu)", languageCode, modelName, modelDir, downloadUrl);
        }

        boolean isAvailable() {
            return modelDir != null && modelDir.isDirectory()
                    && (new File(modelDir, "conf").isDirectory()
                    || new File(modelDir, "am").isDirectory()
                    || new File(modelDir, "graph").isDirectory());
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class TranslationModelChoice {
        final String name;
        final String engineId;

        TranslationModelChoice(String name, String engineId) {
            this.name = name;
            this.engineId = engineId;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class TtsVoiceChoice {
        final String name;
        final String engineId;
        final String modelName;

        TtsVoiceChoice(String name, String engineId, String modelName) {
            this.name = name;
            this.engineId = engineId;
            this.modelName = modelName;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class DecodedAudio {
        final List<short[]> chunks;
        final float sampleRate;

        DecodedAudio(List<short[]> chunks, float sampleRate) {
            this.chunks = chunks;
            this.sampleRate = sampleRate;
        }
    }
}
