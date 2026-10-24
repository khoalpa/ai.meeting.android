package com.example.aimeeting;

import java.util.HashMap;
import java.util.Map;

final class LocalizedText {
    private static final Map<String, Map<String, String>> TEXT = new HashMap<>();
    private static final Map<String, Map<String, String>> BUTTON_TEXT = new HashMap<>();

    static {
        add("vi", "language_1", "Ngôn ngữ nguồn");
        add("vi", "language_2", "Ngôn ngữ đích");
        add("vi", "auto_detect", "Tự phát hiện ngôn ngữ");
        add("vi", "start_listening", "Bắt đầu nghe");
        add("vi", "stop_listening", "Dừng nghe");
        add("vi", "manual_label", "Nhập transcript");
        add("vi", "manual_hint", "Ví dụ: Xin chào mọi người");
        add("vi", "translate_manual", "Dịch transcript");
        add("vi", "source", "Nguồn");
        add("vi", "target", "Đích");
        add("vi", "source_placeholder", "Transcript sẽ hiển thị tại đây.");
        add("vi", "target_placeholder", "Bản dịch sẽ hiển thị tại đây.");
        add("vi", "replay", "Phát âm lại");
        add("vi", "ready", "Sẵn sàng nghe, dịch và phát âm tự động.");
        add("vi", "listening", "Đang nghe...");
        add("vi", "translating", "Đang dịch...");
        add("vi", "manage_models", "Quản lý models");
        add("vi", "assistant", "Trợ lý");

        add("en", "language_1", "Language 1");
        add("en", "language_2", "Language 2");
        add("en", "auto_detect", "Auto-detect language");
        add("en", "start_listening", "Start listening");
        add("en", "stop_listening", "Stop listening");
        add("en", "manual_label", "Enter transcript");
        add("en", "manual_hint", "Example: Hello everyone");
        add("en", "translate_manual", "Translate transcript");
        add("en", "source", "Source");
        add("en", "target", "Target");
        add("en", "source_placeholder", "Transcript will appear here.");
        add("en", "target_placeholder", "Translation will appear here.");
        add("en", "replay", "Speak again");
        add("en", "ready", "Ready to listen, translate, and speak automatically.");
        add("en", "listening", "Listening...");
        add("en", "translating", "Translating...");
        add("en", "manage_models", "Manage models");
        add("en", "assistant", "Assistant");

        add("ja", "language_1", "言語 1");
        add("ja", "language_2", "言語 2");
        add("ja", "auto_detect", "言語を自動検出");
        add("ja", "start_listening", "聞き取り開始");
        add("ja", "stop_listening", "聞き取り停止");
        add("ja", "manual_label", "文字起こしを入力");
        add("ja", "manual_hint", "例: こんにちは、皆さん");
        add("ja", "translate_manual", "文字起こしを翻訳");
        add("ja", "source", "入力");
        add("ja", "target", "翻訳");
        add("ja", "source_placeholder", "文字起こしがここに表示されます。");
        add("ja", "target_placeholder", "翻訳がここに表示されます。");
        add("ja", "replay", "もう一度再生");
        add("ja", "ready", "聞き取り、翻訳、読み上げの準備ができました。");
        add("ja", "listening", "聞き取り中...");
        add("ja", "translating", "翻訳中...");
        add("ja", "manage_models", "モデル管理");
        add("ja", "assistant", "アシスタント");

        add("ko", "language_1", "언어 1");
        add("ko", "language_2", "언어 2");
        add("ko", "auto_detect", "언어 자동 감지");
        add("ko", "start_listening", "듣기 시작");
        add("ko", "stop_listening", "듣기 중지");
        add("ko", "manual_label", "대화 내용 입력");
        add("ko", "manual_hint", "예: 안녕하세요 여러분");
        add("ko", "translate_manual", "대화 내용 번역");
        add("ko", "source", "원문");
        add("ko", "target", "번역");
        add("ko", "source_placeholder", "대화 내용이 여기에 표시됩니다.");
        add("ko", "target_placeholder", "번역이 여기에 표시됩니다.");
        add("ko", "replay", "다시 말하기");
        add("ko", "ready", "듣기, 번역, 음성 재생 준비 완료.");
        add("ko", "listening", "듣는 중...");
        add("ko", "translating", "번역 중...");
        add("ko", "manage_models", "모델 관리");
        add("ko", "assistant", "어시스턴트");

        add("zh", "language_1", "语言 1");
        add("zh", "language_2", "语言 2");
        add("zh", "auto_detect", "自动检测语言");
        add("zh", "start_listening", "开始收听");
        add("zh", "stop_listening", "停止收听");
        add("zh", "manual_label", "输入转写文本");
        add("zh", "manual_hint", "例如：大家好");
        add("zh", "translate_manual", "翻译转写文本");
        add("zh", "source", "源语言");
        add("zh", "target", "目标语言");
        add("zh", "source_placeholder", "转写文本将显示在这里。");
        add("zh", "target_placeholder", "译文将显示在这里。");
        add("zh", "replay", "再次朗读");
        add("zh", "ready", "已准备好自动收听、翻译和朗读。");
        add("zh", "listening", "正在收听...");
        add("zh", "translating", "正在翻译...");
        add("zh", "manage_models", "管理模型");
        add("zh", "assistant", "助手");

        addRomance("fr", "Langue 1", "Langue 2", "Détection automatique", "Démarrer l'écoute",
                "Arrêter l'écoute", "Saisir le transcript", "Exemple : Bonjour à tous",
                "Traduire le transcript", "Source", "Cible", "Le transcript s'affichera ici.",
                "La traduction s'affichera ici.", "Relire", "Prêt à écouter, traduire et parler automatiquement.",
                "Écoute...", "Traduction...", "Gérer les modèles", "Assistant");
        addRomance("de", "Sprache 1", "Sprache 2", "Sprache automatisch erkennen", "Zuhören starten",
                "Zuhören stoppen", "Transkript eingeben", "Beispiel: Hallo zusammen",
                "Transkript übersetzen", "Quelle", "Ziel", "Das Transkript erscheint hier.",
                "Die Übersetzung erscheint hier.", "Erneut sprechen", "Bereit zum automatischen Zuhören, Übersetzen und Sprechen.",
                "Hört zu...", "Übersetzt...", "Modelle verwalten", "Assistent");
        addRomance("es", "Idioma 1", "Idioma 2", "Detectar idioma automáticamente", "Empezar a escuchar",
                "Dejar de escuchar", "Ingresar transcripción", "Ejemplo: Hola a todos",
                "Traducir transcripción", "Origen", "Destino", "La transcripción aparecerá aquí.",
                "La traducción aparecerá aquí.", "Reproducir de nuevo", "Listo para escuchar, traducir y hablar automáticamente.",
                "Escuchando...", "Traduciendo...", "Gestionar modelos", "Asistente");
        addRomance("th", "ภาษา 1", "ภาษา 2", "ตรวจจับภาษาอัตโนมัติ", "เริ่มฟัง",
                "หยุดฟัง", "ป้อนข้อความถอดเสียง", "ตัวอย่าง: สวัสดีทุกคน",
                "แปลข้อความถอดเสียง", "ต้นทาง", "ปลายทาง", "ข้อความถอดเสียงจะแสดงที่นี่",
                "คำแปลจะแสดงที่นี่", "เล่นเสียงอีกครั้ง", "พร้อมฟัง แปล และออกเสียงอัตโนมัติ",
                "กำลังฟัง...", "กำลังแปล...", "จัดการโมเดล", "ผู้ช่วย");
        add("vi", "capture_photo", "Chụp ảnh");
        add("en", "capture_photo", "Capture photo");
        add("ja", "capture_photo", "写真を撮る");
        add("ko", "capture_photo", "사진 촬영");
        add("zh", "capture_photo", "拍照");
        add("fr", "capture_photo", "Prendre photo");
        add("de", "capture_photo", "Foto aufnehmen");
        add("es", "capture_photo", "Tomar foto");
        add("th", "capture_photo", "ถ่ายภาพ");
        add("vi", "share", "Chia sẻ");
        add("vi", "stop_speech", "Ngừng phát âm");
        add("vi", "speech_stopped", "Đã ngừng phát âm.");
        add("en", "share", "Share");
        add("en", "stop_speech", "Stop speech");
        add("en", "speech_stopped", "Speech playback stopped.");
        add("ja", "share", "共有");
        add("ja", "stop_speech", "Stop speech");
        add("ja", "speech_stopped", "Speech playback stopped.");
        add("ko", "share", "공유");
        add("ko", "stop_speech", "Stop speech");
        add("ko", "speech_stopped", "Speech playback stopped.");
        add("zh", "share", "分享");
        add("zh", "stop_speech", "Stop speech");
        add("zh", "speech_stopped", "Speech playback stopped.");
        add("fr", "share", "Partager");
        add("fr", "stop_speech", "Stop speech");
        add("fr", "speech_stopped", "Speech playback stopped.");
        add("de", "share", "Teilen");
        add("de", "stop_speech", "Stop speech");
        add("de", "speech_stopped", "Speech playback stopped.");
        add("es", "share", "Compartir");
        add("es", "stop_speech", "Stop speech");
        add("es", "speech_stopped", "Speech playback stopped.");
        add("th", "share", "แชร์");
        add("th", "stop_speech", "Stop speech");
        add("th", "speech_stopped", "Speech playback stopped.");
    }

    static {
        addCommonActions("vi", "Mở tệp", "Lấy transcript", "Phát âm nguồn", "Phát âm đích", "Đã chọn tệp. Bấm Lấy transcript để nhận dạng.");
        addCommonActions("en", "Open file", "Get transcript", "Speak source", "Speak target", "File selected. Tap Get transcript to recognize it.");
        addCommonActions("ja", "ファイルを開く", "文字起こし取得", "入力を読み上げ", "翻訳を読み上げ", "ファイルを選択しました。文字起こし取得を押してください。");
        addCommonActions("ko", "파일 열기", "대본 가져오기", "원문 말하기", "번역 말하기", "파일을 선택했습니다. 대본 가져오기를 누르세요.");
        addCommonActions("zh", "打开文件", "获取转写", "朗读源文", "朗读译文", "已选择文件。点击获取转写进行识别。");
        addCommonActions("fr", "Ouvrir fichier", "Obtenir transcript", "Lire la source", "Lire la cible", "Fichier sélectionné. Touchez Obtenir transcript.");
        addCommonActions("de", "Datei öffnen", "Transkript holen", "Quelle sprechen", "Ziel sprechen", "Datei ausgewählt. Tippen Sie auf Transkript holen.");
        addCommonActions("es", "Abrir archivo", "Obtener transcripción", "Leer origen", "Leer destino", "Archivo seleccionado. Toca Obtener transcripción.");
        addCommonActions("th", "เปิดไฟล์", "รับทรานสคริปต์", "พูดต้นทาง", "พูดปลายทาง", "เลือกไฟล์แล้ว แตะรับทรานสคริปต์เพื่อรู้จำ");
    }

    private LocalizedText() {
    }

    static String get(String languageCode, String key) {
        Map<String, String> language = TEXT.get(languageCode);
        if (language != null && language.containsKey(key)) {
            return language.get(key);
        }
        return TEXT.get("vi").getOrDefault(key, key);
    }

    static String button(String languageCode, String key) {
        Map<String, String> language = BUTTON_TEXT.get(languageCode);
        if (language != null && language.containsKey(key)) {
            return language.get(key);
        }
        Map<String, String> english = BUTTON_TEXT.get("en");
        if (english != null && english.containsKey(key)) {
            return english.get(key);
        }
        return get(languageCode, key);
    }

    static boolean matchesAnyLanguage(String key, String value) {
        for (Map<String, String> language : TEXT.values()) {
            if (value.equals(language.get(key))) {
                return true;
            }
        }
        return false;
    }

    private static void addRomance(String code, String language1, String language2, String autoDetect,
                                   String start, String stop, String manual, String hint,
                                   String translateManual, String source, String target,
                                   String sourcePlaceholder, String targetPlaceholder, String replay,
                                   String ready, String listening, String translating,
                                   String manageModels, String assistant) {
        add(code, "language_1", language1);
        add(code, "language_2", language2);
        add(code, "auto_detect", autoDetect);
        add(code, "start_listening", start);
        add(code, "stop_listening", stop);
        add(code, "manual_label", manual);
        add(code, "manual_hint", hint);
        add(code, "translate_manual", translateManual);
        add(code, "source", source);
        add(code, "target", target);
        add(code, "source_placeholder", sourcePlaceholder);
        add(code, "target_placeholder", targetPlaceholder);
        add(code, "replay", replay);
        add(code, "ready", ready);
        add(code, "listening", listening);
        add(code, "translating", translating);
        add(code, "manage_models", manageModels);
        add(code, "assistant", assistant);
    }

    private static void add(String code, String key, String value) {
        TEXT.computeIfAbsent(code, unused -> new HashMap<>()).put(key, value);
    }

    private static void addButtonLabel(String code, String key, String value) {
        BUTTON_TEXT.computeIfAbsent(code, unused -> new HashMap<>()).put(key, value);
    }

    private static void addButton(String key, String vi, String en, String ja, String ko, String zh,
                                  String fr, String de, String es, String th) {
        addButtonLabel("vi", key, vi);
        addButtonLabel("en", key, en);
        addButtonLabel("ja", key, ja);
        addButtonLabel("ko", key, ko);
        addButtonLabel("zh", key, zh);
        addButtonLabel("fr", key, fr);
        addButtonLabel("de", key, de);
        addButtonLabel("es", key, es);
        addButtonLabel("th", key, th);
    }

    private static void addCommonActions(String code, String openFile, String getTranscript,
                                         String speakSource, String speakTarget, String fileSelected) {
        add(code, "open_file", openFile);
        add(code, "get_transcript", getTranscript);
        add(code, "speak_source", speakSource);
        add(code, "speak_target", speakTarget);
        add(code, "file_selected", fileSelected);
    }

    static {
        addButton("start_listening", "Nghe", "Listen", "\u805e\u304f", "\ub4e3\uae30", "\u542c", "\u00c9couter", "H\u00f6ren", "O\u00edr", "\u0e1f\u0e31\u0e07");
        addButton("stop_listening", "D\u1eebng", "Stop", "\u505c\u6b62", "\uc911\uc9c0", "\u505c", "Stop", "Stopp", "Parar", "\u0e2b\u0e22\u0e38\u0e14");
        addButton("open_file", "T\u1ec7p", "File", "\u30d5\u30a1\u30a4\u30eb", "\ud30c\uc77c", "\u6587\u4ef6", "Fichier", "Datei", "Archivo", "\u0e44\u0e1f\u0e25\u0e4c");
        addButton("capture_photo", "\u1ea2nh", "Photo", "\u5199\u771f", "\uc0ac\uc9c4", "\u62cd\u7167", "Photo", "Foto", "Foto", "\u0e20\u0e32\u0e1e");
        addButton("get_transcript", "L\u1ea5y ch\u1eef", "Text", "\u6587\u5b57", "\ub300\ubcf8", "\u8f6c\u5199", "Texte", "Text", "Texto", "\u0e02\u0e49\u0e2d\u0e04\u0e27\u0e32\u0e21");
        addButton("translate_manual", "D\u1ecbch", "Translate", "\u7ffb\u8a33", "\ubc88\uc5ed", "\u7ffb\u8bd1", "Traduire", "Ubers.", "Traducir", "\u0e41\u0e1b\u0e25");
        addButton("speak_source", "Ngu\u1ed3n", "Source", "\u5165\u529b", "\uc6d0\ubb38", "\u6e90\u6587", "Source", "Quelle", "Origen", "\u0e15\u0e49\u0e19\u0e17\u0e32\u0e07");
        addButton("speak_target", "\u0110\u00edch", "Target", "\u7ffb\u8a33", "\ubc88\uc5ed", "\u8bd1\u6587", "Cible", "Ziel", "Destino", "\u0e1b\u0e25\u0e32\u0e22\u0e17\u0e32\u0e07");
        addButton("share", "Chia s\u1ebb", "Share", "\u5171\u6709", "\uacf5\uc720", "\u5206\u4eab", "Partager", "Teilen", "Compartir", "\u0e41\u0e0a\u0e23\u0e4c");
        addButton("send", "G\u1eedi", "Send", "\u9001\u4fe1", "\uc804\uc1a1", "\u53d1\u9001", "Envoyer", "Senden", "Enviar", "\u0e2a\u0e48\u0e07");
        addButton("clear_memory", "X\u00f3a nh\u1edb", "Clear", "\u6d88\u53bb", "\uc0ad\uc81c", "\u6e05\u9664", "Effacer", "Leeren", "Borrar", "\u0e25\u0e49\u0e32\u0e07");
        addButton("stop_speech", "D\u1eebng ph\u00e1t", "Stop speech", "\u505c\u6b62", "\uc911\uc9c0", "\u505c", "Stop", "Stopp", "Parar", "\u0e2b\u0e22\u0e38\u0e14");
        addButton("refresh", "L\u00e0m m\u1edbi", "Refresh", "\u66f4\u65b0", "\uc0c8\ub85c\uace0\uce68", "\u5237\u65b0", "Rafraichir", "Neu", "Actualizar", "\u0e23\u0e35\u0e40\u0e1f\u0e23\u0e0a");
        addButton("download_best", "T\u1ea3i t\u1ed1t", "Best", "\u6700\u9069", "\ucd94\ucc9c", "\u6700\u4f73", "Meilleur", "Beste", "Mejor", "\u0e14\u0e35\u0e17\u0e35\u0e48\u0e2a\u0e38\u0e14");
        addButton("download", "T\u1ea3i", "Get", "\u53d6\u5f97", "\ubc1b\uae30", "\u4e0b\u8f7d", "Obtenir", "Laden", "Bajar", "\u0e42\u0e2b\u0e25\u0e14");
        addButton("delete", "X\u00f3a", "Delete", "\u524a\u9664", "\uc0ad\uc81c", "\u5220\u9664", "Supprimer", "Loschen", "Eliminar", "\u0e25\u0e1a");
        addButton("tab_stt", "STT", "STT", "STT", "STT", "STT", "STT", "STT", "STT", "STT");
        addButton("tab_translation", "D\u1ecbch", "Translate", "\u7ffb\u8a33", "\ubc88\uc5ed", "\u7ffb\u8bd1", "Traduire", "Ubers.", "Traducir", "\u0e41\u0e1b\u0e25");
        addButton("tab_tts", "TTS", "TTS", "TTS", "TTS", "TTS", "TTS", "TTS", "TTS", "TTS");
        addButton("assistant", "Tr\u1ee3 l\u00fd", "Assistant", "\u52a9\u624b", "\ub3c4\uc6b0\ubbf8", "\u52a9\u624b", "Assistant", "Assistent", "Asistente", "\u0e1c\u0e39\u0e49\u0e0a\u0e48\u0e27\u0e22");
        addButton("manage_models", "Models", "Models", "\u30e2\u30c7\u30eb", "\ubaa8\ub378", "\u6a21\u578b", "Modeles", "Modelle", "Modelos", "\u0e42\u0e21\u0e40\u0e14\u0e25");
    }

    static {
        addScreenText("vi", "Gửi", "Xóa nhớ", "Bạn", "Xin chào. Tôi sẽ trả lời bằng %s và ghi nhớ nội dung trong cuộc trò chuyện này.",
                "Vui lòng nhập nội dung hoặc bấm Nói.", "Đang xử lý bằng model Trợ lý...", "Đã trả lời. Ngữ cảnh đã được lưu trong máy.",
                "Chưa có model Trợ lý. Hãy mở Quản lý models > tab Trợ lý và tải model mặc định trước.",
                "Không thể nạp model Trợ lý GGUF. Hãy xóa và tải lại model trong Quản lý models.",
                "Model Trợ lý chưa tạo được câu trả lời. Hãy thử lại với câu hỏi ngắn hơn.",
                "Thiết bị không đủ RAM để chạy model Trợ lý hiện tại. Hãy chọn model GGUF nhỏ hơn trong Quản lý models.",
                "Lỗi runtime GGUF/LLM", "Đang nghe...", "Chưa có model nhận dạng cho %s.", "Chưa nhận dạng được giọng nói.",
                "Đã nhận dạng giọng nói.", "Lỗi nghe", "Chưa có model phát âm. Hãy tải trong tab Phát âm.",
                "Lỗi phát âm Trợ lý", "Đã xóa ngữ cảnh Trợ lý.", "có ghi nhớ ngữ cảnh trong máy.",
                "Nhận dạng", "Dịch thuật", "Phát âm", "Làm mới", "Tải tốt nhất", "Tải", "Xóa", "Đã tải", "Chưa tải",
                "mặc định tốt nhất", "Dung lượng app models", "model. Có thể tải, xóa và kiểm tra từng model.",
                "Đang tải model tốt nhất trong tab %s...", "Đã tải xong model tốt nhất trong tab %s.",
                "Đang tải %s...", "Đã tải %s.", "Không tải được %s", "Đang xóa %s...", "Đã xóa %s.", "Không xóa được %s",
                "Đang tải %s: %s", "tiếp tục từ", "đã có sẵn", "tải ZIP", "giải nén ZIP", "tải TAR.BZ2", "giải nén TAR.BZ2",
                "tải GGUF", "lưu model GGUF", "hoàn tất giải nén", "ML Kit đang tải model dịch", "ML Kit đã tải xong",
                "Đang kiểm tra model...");
        addScreenText("en", "Send", "Clear memory", "You", "Hello. I will answer in %s and remember this conversation.",
                "Enter a message or tap Speak.", "Processing with the Assistant model...", "Answered. Context is stored on this device.",
                "No Assistant model yet. Open Manage models > Assistant and download the default model first.",
                "Cannot load the Assistant GGUF model. Delete and download the model again in Manage models.",
                "The Assistant model did not generate a reply. Try a shorter question.",
                "This device does not have enough RAM for the current Assistant model. Choose a smaller GGUF model in Manage models.",
                "GGUF/LLM runtime error", "Listening...", "No speech model for %s.", "Speech was not recognized.",
                "Speech recognized.", "Listening error", "No voice model. Download it in the Speech tab.",
                "Assistant speech error", "Assistant context cleared.", "stores context on this device.",
                "Speech", "Translation", "Voice", "Refresh", "Download best", "Download", "Delete", "Downloaded", "Not downloaded",
                "best default", "App model storage", "models. You can download, delete, and check each model.",
                "Downloading best models in %s...", "Finished downloading best models in %s.",
                "Downloading %s...", "Downloaded %s.", "Could not download %s", "Deleting %s...", "Deleted %s.", "Could not delete %s",
                "Downloading %s: %s", "resume from", "already available", "download ZIP", "extract ZIP", "download TAR.BZ2", "extract TAR.BZ2",
                "download GGUF", "save GGUF model", "finished extracting", "ML Kit is downloading the translation model", "ML Kit download complete",
                "Checking models...");
        addScreenText("ja", "送信", "記憶を消去", "あなた", "こんにちは。%sで回答し、この会話を記憶します。",
                "メッセージを入力するか、話すを押してください。", "アシスタントモデルで処理中...", "回答しました。文脈は端末内に保存されました。",
                "アシスタントモデルがありません。モデル管理 > アシスタントで既定モデルをダウンロードしてください。",
                "アシスタントGGUFモデルを読み込めません。モデル管理で削除して再ダウンロードしてください。",
                "アシスタントモデルが回答を生成できませんでした。短い質問で試してください。",
                "この端末のRAMでは現在のアシスタントモデルを実行できません。より小さいGGUFモデルを選んでください。",
                "GGUF/LLMランタイムエラー", "聞き取り中...", "%s の音声モデルがありません。", "音声を認識できませんでした。",
                "音声を認識しました。", "聞き取りエラー", "音声モデルがありません。音声タブでダウンロードしてください。",
                "アシスタント音声エラー", "アシスタントの文脈を消去しました。", "端末内に文脈を保存します。",
                "音声認識", "翻訳", "音声", "更新", "最適を取得", "取得", "削除", "取得済み", "未取得",
                "既定の最適", "アプリモデル容量", "モデル。各モデルを取得、削除、確認できます。",
                "%s の最適モデルを取得中...", "%s の最適モデルを取得しました。",
                "%s を取得中...", "%s を取得しました。", "%s を取得できません", "%s を削除中...", "%s を削除しました。", "%s を削除できません",
                "%s を取得中: %s", "再開位置", "既に利用可能", "ZIPを取得", "ZIPを展開", "TAR.BZ2を取得", "TAR.BZ2を展開",
                "GGUFを取得", "GGUFモデルを保存", "展開完了", "ML Kitが翻訳モデルを取得中", "ML Kitの取得完了",
                "モデルを確認中...");
        addScreenText("ko", "보내기", "기억 지우기", "사용자", "안녕하세요. %s로 답변하고 이 대화를 기억합니다.",
                "내용을 입력하거나 말하기를 누르세요.", "어시스턴트 모델로 처리 중...", "답변했습니다. 문맥이 기기에 저장되었습니다.",
                "어시스턴트 모델이 없습니다. 모델 관리 > 어시스턴트에서 기본 모델을 먼저 다운로드하세요.",
                "어시스턴트 GGUF 모델을 불러올 수 없습니다. 모델 관리에서 삭제 후 다시 다운로드하세요.",
                "어시스턴트 모델이 답변을 생성하지 못했습니다. 더 짧게 질문해 보세요.",
                "현재 어시스턴트 모델을 실행하기에 RAM이 부족합니다. 더 작은 GGUF 모델을 선택하세요.",
                "GGUF/LLM 런타임 오류", "듣는 중...", "%s 음성 모델이 없습니다.", "음성을 인식하지 못했습니다.",
                "음성을 인식했습니다.", "듣기 오류", "음성 모델이 없습니다. 음성 탭에서 다운로드하세요.",
                "어시스턴트 음성 오류", "어시스턴트 문맥을 지웠습니다.", "기기에 문맥을 저장합니다.",
                "음성 인식", "번역", "음성", "새로고침", "최고 모델 받기", "다운로드", "삭제", "다운로드됨", "미다운로드",
                "기본 최고", "앱 모델 저장공간", "개 모델. 각 모델을 다운로드, 삭제, 확인할 수 있습니다.",
                "%s 탭의 최고 모델 다운로드 중...", "%s 탭의 최고 모델 다운로드 완료.",
                "%s 다운로드 중...", "%s 다운로드 완료.", "%s 다운로드 실패", "%s 삭제 중...", "%s 삭제 완료.", "%s 삭제 실패",
                "%s 다운로드 중: %s", "이어받기 위치", "이미 있음", "ZIP 다운로드", "ZIP 압축 해제", "TAR.BZ2 다운로드", "TAR.BZ2 압축 해제",
                "GGUF 다운로드", "GGUF 모델 저장", "압축 해제 완료", "ML Kit 번역 모델 다운로드 중", "ML Kit 다운로드 완료",
                "모델 확인 중...");
        addScreenText("zh", "发送", "清除记忆", "你", "你好。我会用%s回答，并记住本次对话。",
                "请输入内容或点击说话。", "正在使用助手模型处理...", "已回答。上下文已保存在本机。",
                "还没有助手模型。请打开管理模型 > 助手并先下载默认模型。",
                "无法加载助手 GGUF 模型。请在管理模型中删除并重新下载。",
                "助手模型未能生成回复。请尝试更短的问题。",
                "设备内存不足，无法运行当前助手模型。请在管理模型中选择更小的 GGUF 模型。",
                "GGUF/LLM 运行时错误", "正在听...", "没有 %s 的语音模型。", "未识别到语音。",
                "已识别语音。", "听写错误", "没有语音模型。请在发音标签下载。",
                "助手发音错误", "已清除助手上下文。", "在本机保存上下文。",
                "语音识别", "翻译", "发音", "刷新", "下载最佳", "下载", "删除", "已下载", "未下载",
                "默认最佳", "应用模型存储", "个模型。可下载、删除并检查每个模型。",
                "正在下载 %s 中的最佳模型...", "%s 中的最佳模型已下载完成。",
                "正在下载 %s...", "已下载 %s。", "无法下载 %s", "正在删除 %s...", "已删除 %s。", "无法删除 %s",
                "正在下载 %s：%s", "继续自", "已存在", "下载 ZIP", "解压 ZIP", "下载 TAR.BZ2", "解压 TAR.BZ2",
                "下载 GGUF", "保存 GGUF 模型", "解压完成", "ML Kit 正在下载翻译模型", "ML Kit 下载完成",
                "正在检查模型...");
        addScreenText("fr", "Envoyer", "Effacer mémoire", "Vous", "Bonjour. Je répondrai en %s et mémoriserai cette conversation.",
                "Saisissez un message ou touchez Parler.", "Traitement avec le modèle Assistant...", "Réponse prête. Le contexte est stocké sur l'appareil.",
                "Aucun modèle Assistant. Ouvrez Gérer les modèles > Assistant et téléchargez le modèle par défaut.",
                "Impossible de charger le modèle GGUF Assistant. Supprimez-le puis téléchargez-le à nouveau.",
                "Le modèle Assistant n'a pas généré de réponse. Essayez une question plus courte.",
                "RAM insuffisante pour ce modèle Assistant. Choisissez un modèle GGUF plus petit.",
                "Erreur runtime GGUF/LLM", "Écoute...", "Aucun modèle vocal pour %s.", "Voix non reconnue.",
                "Voix reconnue.", "Erreur d'écoute", "Aucun modèle vocal. Téléchargez-le dans l'onglet Voix.",
                "Erreur vocale Assistant", "Contexte Assistant effacé.", "mémorise le contexte sur cet appareil.",
                "Reconnaissance", "Traduction", "Voix", "Actualiser", "Télécharger le meilleur", "Télécharger", "Supprimer", "Téléchargé", "Non téléchargé",
                "meilleur par défaut", "Stockage des modèles", "modèles. Vous pouvez télécharger, supprimer et vérifier chaque modèle.",
                "Téléchargement des meilleurs modèles dans %s...", "Meilleurs modèles téléchargés dans %s.",
                "Téléchargement de %s...", "%s téléchargé.", "Impossible de télécharger %s", "Suppression de %s...", "%s supprimé.", "Impossible de supprimer %s",
                "Téléchargement de %s : %s", "reprise depuis", "déjà disponible", "téléchargement ZIP", "extraction ZIP", "téléchargement TAR.BZ2", "extraction TAR.BZ2",
                "téléchargement GGUF", "enregistrement GGUF", "extraction terminée", "ML Kit télécharge le modèle de traduction", "Téléchargement ML Kit terminé",
                "Vérification des modèles...");
        addScreenText("de", "Senden", "Speicher löschen", "Sie", "Hallo. Ich antworte auf %s und merke mir dieses Gespräch.",
                "Nachricht eingeben oder Sprechen tippen.", "Verarbeite mit Assistent-Modell...", "Antwort fertig. Kontext ist auf dem Gerät gespeichert.",
                "Kein Assistent-Modell vorhanden. Öffnen Sie Modelle verwalten > Assistent und laden Sie das Standardmodell.",
                "Assistent-GGUF-Modell kann nicht geladen werden. Bitte löschen und erneut herunterladen.",
                "Das Assistent-Modell hat keine Antwort erzeugt. Versuchen Sie eine kürzere Frage.",
                "Nicht genug RAM für dieses Assistent-Modell. Wählen Sie ein kleineres GGUF-Modell.",
                "GGUF/LLM-Laufzeitfehler", "Hört zu...", "Kein Sprachmodell für %s.", "Sprache nicht erkannt.",
                "Sprache erkannt.", "Hörfehler", "Kein Stimmmodell. Im Tab Stimme herunterladen.",
                "Assistent-Sprachfehler", "Assistent-Kontext gelöscht.", "speichert Kontext auf diesem Gerät.",
                "Erkennung", "Übersetzung", "Stimme", "Aktualisieren", "Bestes laden", "Laden", "Löschen", "Geladen", "Nicht geladen",
                "bestes Standardmodell", "Modellspeicher", "Modelle. Sie können jedes Modell laden, löschen und prüfen.",
                "Lade beste Modelle in %s...", "Beste Modelle in %s geladen.",
                "Lade %s...", "%s geladen.", "%s konnte nicht geladen werden", "Lösche %s...", "%s gelöscht.", "%s konnte nicht gelöscht werden",
                "Lade %s: %s", "Fortsetzen ab", "bereits verfügbar", "ZIP laden", "ZIP entpacken", "TAR.BZ2 laden", "TAR.BZ2 entpacken",
                "GGUF laden", "GGUF-Modell speichern", "Entpacken fertig", "ML Kit lädt Übersetzungsmodell", "ML Kit-Download fertig",
                "Modelle werden geprüft...");
        addScreenText("es", "Enviar", "Borrar memoria", "Tú", "Hola. Responderé en %s y recordaré esta conversación.",
                "Escribe un mensaje o toca Hablar.", "Procesando con el modelo Asistente...", "Respuesta lista. El contexto se guardó en el dispositivo.",
                "No hay modelo de Asistente. Abre Gestionar modelos > Asistente y descarga el modelo predeterminado.",
                "No se puede cargar el modelo GGUF del Asistente. Elimínalo y descárgalo de nuevo.",
                "El modelo Asistente no generó respuesta. Prueba con una pregunta más corta.",
                "El dispositivo no tiene suficiente RAM para este modelo. Elige un GGUF más pequeño.",
                "Error runtime GGUF/LLM", "Escuchando...", "No hay modelo de voz para %s.", "No se reconoció la voz.",
                "Voz reconocida.", "Error al escuchar", "No hay modelo de voz. Descárgalo en la pestaña Voz.",
                "Error de voz del Asistente", "Contexto del Asistente borrado.", "guarda contexto en este dispositivo.",
                "Reconocimiento", "Traducción", "Voz", "Actualizar", "Descargar mejor", "Descargar", "Eliminar", "Descargado", "No descargado",
                "mejor predeterminado", "Almacenamiento de modelos", "modelos. Puedes descargar, eliminar y revisar cada modelo.",
                "Descargando mejores modelos en %s...", "Mejores modelos descargados en %s.",
                "Descargando %s...", "%s descargado.", "No se pudo descargar %s", "Eliminando %s...", "%s eliminado.", "No se pudo eliminar %s",
                "Descargando %s: %s", "reanudar desde", "ya disponible", "descargar ZIP", "extraer ZIP", "descargar TAR.BZ2", "extraer TAR.BZ2",
                "descargar GGUF", "guardar modelo GGUF", "extracción terminada", "ML Kit descarga el modelo de traducción", "Descarga ML Kit completa",
                "Comprobando modelos...");
        addScreenText("th", "ส่ง", "ล้างความจำ", "คุณ", "สวัสดี ฉันจะตอบเป็น%sและจดจำบทสนทนานี้",
                "พิมพ์ข้อความหรือแตะพูด", "กำลังประมวลผลด้วยโมเดลผู้ช่วย...", "ตอบแล้ว บันทึกบริบทไว้ในเครื่อง",
                "ยังไม่มีโมเดลผู้ช่วย เปิดจัดการโมเดล > ผู้ช่วย แล้วดาวน์โหลดโมเดลเริ่มต้นก่อน",
                "โหลดโมเดล GGUF ผู้ช่วยไม่ได้ โปรบลบแล้วดาวน์โหลดใหม่",
                "โมเดลผู้ช่วยยังสร้างคำตอบไม่ได้ ลองถามให้สั้นลง",
                "RAM ไม่พอสำหรับโมเดลผู้ช่วยนี้ โปรดเลือก GGUF ที่เล็กกว่า",
                "ข้อผิดพลาด runtime GGUF/LLM", "กำลังฟัง...", "ไม่มีโมเดลเสียงสำหรับ %s", "ยังจำเสียงไม่ได้",
                "จำเสียงได้แล้ว", "ข้อผิดพลาดการฟัง", "ไม่มีโมเดลเสียง ดาวน์โหลดในแท็บเสียง",
                "ข้อผิดพลาดเสียงผู้ช่วย", "ล้างบริบทผู้ช่วยแล้ว", "บันทึกบริบทไว้ในเครื่องนี้",
                "รู้จำเสียง", "แปล", "เสียง", "รีเฟรช", "ดาวน์โหลดที่ดีที่สุด", "ดาวน์โหลด", "ลบ", "ดาวน์โหลดแล้ว", "ยังไม่ดาวน์โหลด",
                "ค่าเริ่มต้นที่ดีที่สุด", "พื้นที่โมเดลของแอป", "โมเดล สามารถดาวน์โหลด ลบ และตรวจสอบแต่ละโมเดลได้",
                "กำลังดาวน์โหลดโมเดลที่ดีที่สุดใน %s...", "ดาวน์โหลดโมเดลที่ดีที่สุดใน %s แล้ว",
                "กำลังดาวน์โหลด %s...", "ดาวน์โหลด %s แล้ว", "ดาวน์โหลด %s ไม่ได้", "กำลังลบ %s...", "ลบ %s แล้ว", "ลบ %s ไม่ได้",
                "กำลังดาวน์โหลด %s: %s", "ต่อจาก", "มีอยู่แล้ว", "ดาวน์โหลด ZIP", "แตก ZIP", "ดาวน์โหลด TAR.BZ2", "แตก TAR.BZ2",
                "ดาวน์โหลด GGUF", "บันทึกโมเดล GGUF", "แตกไฟล์เสร็จ", "ML Kit กำลังดาวน์โหลดโมเดลแปล", "ดาวน์โหลด ML Kit เสร็จ",
                "กำลังตรวจสอบโมเดล...");
    }

    private static void addScreenText(String code, String send, String clearMemory, String user, String assistantHello,
                                      String assistantEmpty, String assistantProcessing, String assistantAnswered,
                                      String assistantModelMissing, String assistantModelLoadFailed, String assistantEmptyReply,
                                      String assistantOutOfMemory, String assistantRuntimeError, String assistantListening,
                                      String assistantNoSttModel, String assistantNoSpeech, String assistantSpeechRecognized,
                                      String assistantListenError, String assistantNoTts, String assistantTtsError,
                                      String assistantMemoryCleared, String assistantStoresContext, String tabStt,
                                      String tabTranslation, String tabTts, String refresh, String downloadBest,
                                      String download, String delete, String installed, String notInstalled,
                                      String bestDefault, String modelStorage, String modelCountSuffix,
                                      String downloadingBestInTab, String downloadedBestInTab,
                                      String downloadingNamed, String downloadedNamed, String downloadFailedNamed,
                                      String deletingNamed, String deletedNamed, String deleteFailedNamed,
                                      String downloadProgress, String resumeFrom, String alreadyAvailable,
                                      String downloadZip, String extractZip, String downloadTar, String extractTar,
                                      String downloadGguf, String saveGguf, String extractDone,
                                      String mlkitDownloading, String mlkitDownloaded, String checkingModels) {
        add(code, "send", send);
        add(code, "clear_memory", clearMemory);
        add(code, "user", user);
        add(code, "assistant_hello", assistantHello);
        add(code, "assistant_empty", assistantEmpty);
        add(code, "assistant_processing", assistantProcessing);
        add(code, "assistant_answered", assistantAnswered);
        add(code, "assistant_model_missing", assistantModelMissing);
        add(code, "assistant_model_load_failed", assistantModelLoadFailed);
        add(code, "assistant_empty_reply", assistantEmptyReply);
        add(code, "assistant_out_of_memory", assistantOutOfMemory);
        add(code, "assistant_runtime_error", assistantRuntimeError);
        add(code, "assistant_listening", assistantListening);
        add(code, "assistant_no_stt_model", assistantNoSttModel);
        add(code, "assistant_no_speech", assistantNoSpeech);
        add(code, "assistant_speech_recognized", assistantSpeechRecognized);
        add(code, "assistant_listen_error", assistantListenError);
        add(code, "assistant_no_tts", assistantNoTts);
        add(code, "assistant_tts_error", assistantTtsError);
        add(code, "assistant_memory_cleared", assistantMemoryCleared);
        add(code, "assistant_stores_context", assistantStoresContext);
        add(code, "tab_stt", tabStt);
        add(code, "tab_translation", tabTranslation);
        add(code, "tab_tts", tabTts);
        add(code, "refresh", refresh);
        add(code, "download_best", downloadBest);
        add(code, "download", download);
        add(code, "delete", delete);
        add(code, "installed", installed);
        add(code, "not_installed", notInstalled);
        add(code, "best_default", bestDefault);
        add(code, "model_storage", modelStorage);
        add(code, "model_count_suffix", modelCountSuffix);
        add(code, "downloading_best_in_tab", downloadingBestInTab);
        add(code, "downloaded_best_in_tab", downloadedBestInTab);
        add(code, "downloading_named", downloadingNamed);
        add(code, "downloaded_named", downloadedNamed);
        add(code, "download_failed_named", downloadFailedNamed);
        add(code, "deleting_named", deletingNamed);
        add(code, "deleted_named", deletedNamed);
        add(code, "delete_failed_named", deleteFailedNamed);
        add(code, "download_progress", downloadProgress);
        add(code, "resume_from", resumeFrom);
        add(code, "already_available", alreadyAvailable);
        add(code, "download_zip", downloadZip);
        add(code, "extract_zip", extractZip);
        add(code, "download_tar", downloadTar);
        add(code, "extract_tar", extractTar);
        add(code, "download_gguf", downloadGguf);
        add(code, "save_gguf", saveGguf);
        add(code, "extract_done", extractDone);
        add(code, "mlkit_downloading", mlkitDownloading);
        add(code, "mlkit_downloaded", mlkitDownloaded);
        add(code, "checking_models", checkingModels);
    }
}
