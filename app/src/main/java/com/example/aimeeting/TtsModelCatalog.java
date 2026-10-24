package com.example.aimeeting;

import com.k2fsa.sherpa.onnx.OfflineTtsConfig;
import com.k2fsa.sherpa.onnx.TtsKt;

import java.io.File;
import java.util.Locale;

final class TtsModelCatalog {
    static final String SUPERTONIC_MODEL_NAME = "sherpa-onnx-supertonic-3-tts-int8-2026-05-11";
    static final String SUPERTONIC_MODEL_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/"
            + SUPERTONIC_MODEL_NAME + ".tar.bz2";

    static final String VIETNAMESE_VITS_MODEL_NAME = "vits-mimic3-vi_VN-vais1000_low";
    static final String VIETNAMESE_VITS_MODEL_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/"
            + VIETNAMESE_VITS_MODEL_NAME + ".tar.bz2";
    static final String VIETNAMESE_PIPER_VIVOS_MODEL_NAME = "vits-piper-vi_VN-vivos-x_low";
    static final String VIETNAMESE_PIPER_VAIS1000_MODEL_NAME = "vits-piper-vi_VN-vais1000-medium";
    static final String VIETNAMESE_PIPER_25H_MODEL_NAME = "vits-piper-vi_VN-25hours_single-low";
    static final String VIETNAMESE_PIPER_VIVOS_MODEL_URL = modelUrl(VIETNAMESE_PIPER_VIVOS_MODEL_NAME);
    static final String VIETNAMESE_PIPER_VAIS1000_MODEL_URL = modelUrl(VIETNAMESE_PIPER_VAIS1000_MODEL_NAME);
    static final String VIETNAMESE_PIPER_25H_MODEL_URL = modelUrl(VIETNAMESE_PIPER_25H_MODEL_NAME);

    private TtsModelCatalog() {
    }

    static File defaultModelDir(File ttsParent, String languageCode) {
        String modelName = "vi".equals(languageCode) ? VIETNAMESE_PIPER_VAIS1000_MODEL_NAME : SUPERTONIC_MODEL_NAME;
        return new File(ttsParent, modelName);
    }

    static String defaultModelUrl(String modelName) {
        if (VIETNAMESE_VITS_MODEL_NAME.equals(modelName)) {
            return VIETNAMESE_VITS_MODEL_URL;
        }
        if (VIETNAMESE_PIPER_VIVOS_MODEL_NAME.equals(modelName)) {
            return VIETNAMESE_PIPER_VIVOS_MODEL_URL;
        }
        if (VIETNAMESE_PIPER_VAIS1000_MODEL_NAME.equals(modelName)) {
            return VIETNAMESE_PIPER_VAIS1000_MODEL_URL;
        }
        if (VIETNAMESE_PIPER_25H_MODEL_NAME.equals(modelName)) {
            return VIETNAMESE_PIPER_25H_MODEL_URL;
        }
        return SUPERTONIC_MODEL_URL;
    }

    static boolean isVietnameseModelName(String modelName) {
        return VIETNAMESE_VITS_MODEL_NAME.equals(modelName)
                || VIETNAMESE_PIPER_VIVOS_MODEL_NAME.equals(modelName)
                || VIETNAMESE_PIPER_VAIS1000_MODEL_NAME.equals(modelName)
                || VIETNAMESE_PIPER_25H_MODEL_NAME.equals(modelName);
    }

    static boolean looksLikeUsableModel(File dir) {
        return looksLikeSupertonicModel(dir) || looksLikeVitsModel(dir);
    }

    static boolean looksLikeSupertonicModel(File dir) {
        return dir != null
                && new File(dir, "duration_predictor.int8.onnx").isFile()
                && new File(dir, "text_encoder.int8.onnx").isFile()
                && new File(dir, "vector_estimator.int8.onnx").isFile()
                && new File(dir, "vocoder.int8.onnx").isFile()
                && new File(dir, "tts.json").isFile()
                && new File(dir, "unicode_indexer.bin").isFile()
                && new File(dir, "voice.bin").isFile();
    }

    static boolean looksLikeVitsModel(File dir) {
        return dir != null
                && dir.isDirectory()
                && new File(dir, "tokens.txt").isFile()
                && findVitsModelFile(dir) != null;
    }

    static OfflineTtsConfig createConfig(File modelDir, String provider, int threads) {
        if (looksLikeSupertonicModel(modelDir)) {
            return TtsKt.getOfflineTtsConfig(modelDir.getAbsolutePath(), "", "", "", "", "", "", "", "", "",
                    threads, false, true, "duration_predictor.int8.onnx", "text_encoder.int8.onnx", "vector_estimator.int8.onnx",
                    "vocoder.int8.onnx", "tts.json", "unicode_indexer.bin", "voice.bin", provider);
        }

        File vitsModel = findVitsModelFile(modelDir);
        if (vitsModel == null) {
            throw new IllegalArgumentException("Unsupported TTS model: " + modelDir);
        }
        File dataDir = new File(modelDir, "espeak-ng-data");
        return TtsKt.getOfflineTtsConfig(modelDir.getAbsolutePath(), vitsModel.getName(), "", "", "", "",
                dataDir.isDirectory() ? dataDir.getAbsolutePath() : "", "", "", "",
                threads, false, false, "", "", "", "", "", "", "", provider);
    }

    static String displayName(File dir) {
        if (dir == null) {
            return "";
        }
        if (VIETNAMESE_VITS_MODEL_NAME.equals(dir.getName())) {
            return "Tiếng Việt - VITS Mimic3 VAIS1000";
        }
        if (VIETNAMESE_PIPER_VIVOS_MODEL_NAME.equals(dir.getName())) {
            return "Ti\u1ebfng Vi\u1ec7t - Piper VIVOS X low";
        }
        if (VIETNAMESE_PIPER_VAIS1000_MODEL_NAME.equals(dir.getName())) {
            return "Ti\u1ebfng Vi\u1ec7t - Piper VAIS1000 medium";
        }
        if (VIETNAMESE_PIPER_25H_MODEL_NAME.equals(dir.getName())) {
            return "Ti\u1ebfng Vi\u1ec7t - Piper 25 hours single low";
        }
        if (SUPERTONIC_MODEL_NAME.equals(dir.getName())) {
            return "Supertonic 3 multilingual";
        }
        return dir.getName();
    }

    static String runtimeName(File dir) {
        return looksLikeVitsModel(dir) ? "Sherpa ONNX / VITS" : "Sherpa ONNX / Supertonic";
    }

    static String requiredFilesText() {
        return "Supertonic: tts.json, voice.bin, duration_predictor/text_encoder/vector_estimator/vocoder; "
                + "VITS: *.onnx, tokens.txt, espeak-ng-data/";
    }

    private static File findVitsModelFile(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return null;
        }
        File preferred = new File(dir, "model.onnx");
        if (preferred.isFile()) {
            return preferred;
        }
        File[] files = dir.listFiles(file -> file.isFile()
                && file.getName().toLowerCase(Locale.US).endsWith(".onnx")
                && !file.getName().toLowerCase(Locale.US).contains("vocoder")
                && !file.getName().toLowerCase(Locale.US).contains("encoder")
                && !file.getName().toLowerCase(Locale.US).contains("predictor")
                && !file.getName().toLowerCase(Locale.US).contains("estimator"));
        if (files == null || files.length == 0) {
            return null;
        }
        return files[0];
    }

    private static String modelUrl(String modelName) {
        return "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/"
                + modelName + ".tar.bz2";
    }
}
