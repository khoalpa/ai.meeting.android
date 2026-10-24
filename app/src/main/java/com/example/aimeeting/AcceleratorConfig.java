package com.example.aimeeting;

import android.os.Build;

import java.util.Locale;

final class AcceleratorConfig {
    static final String CPU_PROVIDER = "cpu";
    static final String NNAPI_PROVIDER = "nnapi";

    private AcceleratorConfig() {
    }

    static String preferredOnnxProvider() {
        return isLikelyQualcommDevice() ? NNAPI_PROVIDER : CPU_PROVIDER;
    }

    static boolean prefersNnapi() {
        return NNAPI_PROVIDER.equals(preferredOnnxProvider());
    }

    static int ttsThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        if (prefersNnapi()) {
            return Math.min(4, Math.max(2, cores / 2));
        }
        return Math.min(4, Math.max(2, cores - 2));
    }

    static int llmThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        if (isLikelyHighEndSnapdragon()) {
            return Math.min(6, Math.max(4, cores - 2));
        }
        return Math.min(4, Math.max(2, cores - 1));
    }

    static int llmGpuLayers() {
        // Keep llama.cpp on CPU by default. Some Android Vulkan drivers can abort the
        // process from native code during model load/generation, which Java cannot catch.
        return 0;
    }

    static String runtimeSummary() {
        String provider = preferredOnnxProvider();
        String llamaBackend = llmGpuLayers() > 0 ? "Vulkan GPU with CPU fallback" : "CPU (Vulkan packaged, disabled)";
        return "ONNX/Sherpa provider: " + provider
                + "\nTTS threads: " + ttsThreads()
                + "\nLLM backend: " + llamaBackend
                + "\nLLM threads: " + llmThreads()
                + "\nLLM GPU layers: " + llmGpuLayers()
                + "\nDevice hint: " + deviceHint();
    }

    static String deviceHint() {
        return normalize(Build.MANUFACTURER) + " "
                + normalize(Build.BRAND) + " "
                + normalize(Build.MODEL) + " "
                + normalize(Build.HARDWARE) + " "
                + normalize(socManufacturer()) + " "
                + normalize(socModel());
    }

    private static boolean isLikelyHighEndSnapdragon() {
        String hint = deviceHint();
        return hint.contains("snapdragon")
                || hint.contains("qualcomm")
                || hint.contains("qcom")
                || hint.contains("redmi turbo 4 pro")
                || hint.contains("poco f7");
    }

    private static boolean isLikelyQualcommDevice() {
        String hint = deviceHint();
        return hint.contains("qualcomm")
                || hint.contains("qcom")
                || hint.contains("snapdragon")
                || hint.contains("redmi turbo 4 pro")
                || hint.contains("poco f7");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }

    private static String socManufacturer() {
        if (Build.VERSION.SDK_INT < 31) {
            return "";
        }
        try {
            return Build.SOC_MANUFACTURER;
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String socModel() {
        if (Build.VERSION.SDK_INT < 31) {
            return "";
        }
        try {
            return Build.SOC_MODEL;
        } catch (Throwable ignored) {
            return "";
        }
    }
}
