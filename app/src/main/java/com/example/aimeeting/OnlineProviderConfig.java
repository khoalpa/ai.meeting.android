package com.example.aimeeting;

import android.content.Context;
import android.content.SharedPreferences;

final class OnlineProviderConfig {
    static final String PREF_PREFIX = "online_provider_";

    private OnlineProviderConfig() {
    }

    static boolean isOnlineSttProvider(String value) {
        return "provider:openai-stt".equals(value)
                || "provider:groq-whisper-free".equals(value)
                || "provider:huggingface-whisper-free".equals(value);
    }

    static boolean isOnlineOcrProvider(String value) {
        return "provider:openai-ocr".equals(value)
                || "google:cloud-vision-ocr".equals(value);
    }

    static boolean isOnlineTtsProvider(String value) {
        return "provider:openai-tts".equals(value)
                || "provider:huggingface-speecht5-free".equals(value);
    }

    static boolean isOnlineAssistantProvider(String value) {
        return "provider:openai-chat".equals(value)
                || "google:gemini-service".equals(value)
                || "provider:openrouter-free".equals(value)
                || "provider:groq-llama-free".equals(value)
                || "provider:huggingface-chat-free".equals(value);
    }

    static ProviderSettings settings(Context context, String providerValue) {
        SharedPreferences preferences = context.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE);
        String providerId = providerId(providerValue);
        ProviderDefaults defaults = defaultsFor(providerId);
        return new ProviderSettings(
                providerValue,
                providerId,
                SecretStore.get(context, preferences, key(providerId, "api_key")),
                string(preferences, providerId, "base_url", defaults.baseUrl),
                string(preferences, providerId, "stt_model", defaults.sttModel),
                string(preferences, providerId, "ocr_model", defaults.ocrModel),
                string(preferences, providerId, "tts_model", defaults.ttsModel),
                string(preferences, providerId, "tts_voice", defaults.ttsVoice),
                string(preferences, providerId, "chat_model", defaults.chatModel)
        );
    }

    static String key(String providerId, String field) {
        return PREF_PREFIX + providerId + "_" + field;
    }

    static String providerId(String providerValue) {
        if ("provider:openai-stt".equals(providerValue)
                || "provider:openai-ocr".equals(providerValue)
                || "provider:openai-tts".equals(providerValue)
                || "provider:openai-chat".equals(providerValue)) {
            return "openai";
        }
        if ("provider:groq-whisper-free".equals(providerValue)
                || "provider:groq-llama-free".equals(providerValue)) {
            return "groq";
        }
        if ("provider:openrouter-free".equals(providerValue)) {
            return "openrouter";
        }
        if ("provider:huggingface-whisper-free".equals(providerValue)
                || "provider:huggingface-speecht5-free".equals(providerValue)
                || "provider:huggingface-chat-free".equals(providerValue)) {
            return "huggingface";
        }
        if ("google:gemini-service".equals(providerValue)
                || "google:cloud-vision-ocr".equals(providerValue)
                || "google:translate-service".equals(providerValue)) {
            return "google";
        }
        if ("provider:libretranslate-free".equals(providerValue)) {
            return "libretranslate";
        }
        if ("provider:mymemory-free".equals(providerValue)) {
            return "mymemory";
        }
        return providerValue == null ? "" : providerValue.replace(":", "_");
    }

    static ProviderDefaults defaultsFor(String providerId) {
        if ("openai".equals(providerId)) {
            return new ProviderDefaults(
                    "https://api.openai.com/v1",
                    "gpt-4o-mini-transcribe",
                    "gpt-4.1-mini",
                    "gpt-4o-mini-tts",
                    "alloy",
                    "gpt-4.1-mini"
            );
        }
        if ("groq".equals(providerId)) {
            return new ProviderDefaults(
                    "https://api.groq.com/openai/v1",
                    "whisper-large-v3-turbo",
                    "",
                    "",
                    "",
                    "llama-3.3-70b-versatile"
            );
        }
        if ("openrouter".equals(providerId)) {
            return new ProviderDefaults(
                    "https://openrouter.ai/api/v1",
                    "",
                    "",
                    "",
                    "",
                    "meta-llama/llama-3.1-8b-instruct:free"
            );
        }
        if ("huggingface".equals(providerId)) {
            return new ProviderDefaults(
                    "https://router.huggingface.co/v1",
                    "openai/whisper-large-v3",
                    "",
                    "microsoft/speecht5_tts",
                    "",
                    "meta-llama/Llama-3.1-8B-Instruct"
            );
        }
        if ("google".equals(providerId)) {
            return new ProviderDefaults(
                    "https://generativelanguage.googleapis.com/v1beta",
                    "",
                    "",
                    "",
                    "",
                    "gemini-2.5-flash"
            );
        }
        if ("libretranslate".equals(providerId)) {
            return new ProviderDefaults("https://libretranslate.de", "", "", "", "", "");
        }
        return new ProviderDefaults("", "", "", "", "", "");
    }

    private static String string(SharedPreferences preferences, String providerId, String field, String fallback) {
        String value = safeString(preferences, key(providerId, field), null);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    static String safeString(SharedPreferences preferences, String key, String fallback) {
        if (preferences == null || key == null) {
            return fallback;
        }
        try {
            String value = preferences.getString(key, fallback);
            return value == null ? fallback : value;
        } catch (ClassCastException e) {
            return fallback;
        }
    }

    static final class ProviderSettings {
        final String providerValue;
        final String providerId;
        final String apiKey;
        final String baseUrl;
        final String sttModel;
        final String ocrModel;
        final String ttsModel;
        final String ttsVoice;
        final String chatModel;

        ProviderSettings(String providerValue, String providerId, String apiKey, String baseUrl,
                         String sttModel, String ocrModel, String ttsModel, String ttsVoice, String chatModel) {
            this.providerValue = providerValue;
            this.providerId = providerId;
            this.apiKey = apiKey;
            this.baseUrl = trimTrailingSlash(baseUrl);
            this.sttModel = sttModel;
            this.ocrModel = ocrModel;
            this.ttsModel = ttsModel;
            this.ttsVoice = ttsVoice;
            this.chatModel = chatModel;
        }

        void requireApiKey() throws java.io.IOException {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new java.io.IOException("Missing API key for " + providerId + ". Open Manage models and fill provider credentials.");
            }
        }

        private static String trimTrailingSlash(String value) {
            if (value == null) {
                return "";
            }
            String result = value.trim();
            while (result.endsWith("/")) {
                result = result.substring(0, result.length() - 1);
            }
            return result;
        }
    }

    static final class ProviderDefaults {
        final String baseUrl;
        final String sttModel;
        final String ocrModel;
        final String ttsModel;
        final String ttsVoice;
        final String chatModel;

        ProviderDefaults(String baseUrl, String sttModel, String ocrModel, String ttsModel,
                         String ttsVoice, String chatModel) {
            this.baseUrl = baseUrl;
            this.sttModel = sttModel;
            this.ocrModel = ocrModel;
            this.ttsModel = ttsModel;
            this.ttsVoice = ttsVoice;
            this.chatModel = chatModel;
        }
    }
}
