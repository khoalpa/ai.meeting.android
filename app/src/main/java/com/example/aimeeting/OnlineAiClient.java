package com.example.aimeeting;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

final class OnlineAiClient {
    private static final int CONNECT_TIMEOUT_MS = 20000;
    private static final int READ_TIMEOUT_MS = 120000;
    private static final int ASSISTANT_MAX_OUTPUT_TOKENS = 1024;

    private OnlineAiClient() {
    }

    static String transcribePcm(
            Context context,
            String providerValue,
            List<short[]> chunks,
            float sampleRate,
            String languageCode
    ) throws IOException, JSONException {
        OnlineProviderConfig.ProviderSettings settings = OnlineProviderConfig.settings(context, providerValue);
        byte[] wav = wavBytes(chunks, (int) sampleRate);
        if ("provider:huggingface-whisper-free".equals(providerValue)) {
            settings.requireApiKey();
            String endpoint = settings.baseUrl.contains("/v1")
                    ? "https://api-inference.huggingface.co/models/" + settings.sttModel
                    : settings.baseUrl + "/models/" + settings.sttModel;
            String response = postBinaryText(endpoint, settings.apiKey, "audio/wav", wav);
            return parseTextResponse(response);
        }
        if ("provider:openai-stt".equals(providerValue) || "provider:groq-whisper-free".equals(providerValue)) {
            settings.requireApiKey();
            String endpoint = settings.baseUrl + "/audio/transcriptions";
            MultipartBody body = new MultipartBody();
            body.addField("model", settings.sttModel);
            body.addField("response_format", "json");
            if (languageCode != null && !languageCode.trim().isEmpty()) {
                body.addField("language", languageCode);
            }
            body.addFile("file", "audio.wav", "audio/wav", wav);
            String response = postMultipartText(endpoint, settings.apiKey, body);
            return parseTextResponse(response);
        }
        throw new IOException("Unsupported online STT provider: " + providerValue);
    }

    static String recognizeText(Context context, String providerValue, Bitmap bitmap, String languageCode)
            throws IOException, JSONException {
        OnlineProviderConfig.ProviderSettings settings = OnlineProviderConfig.settings(context, providerValue);
        settings.requireApiKey();
        String base64Image = Base64.encodeToString(jpegBytes(bitmap), Base64.NO_WRAP);
        if ("google:cloud-vision-ocr".equals(providerValue)) {
            String endpoint = "https://vision.googleapis.com/v1/images:annotate?key=" + settings.apiKey;
            JSONObject feature = new JSONObject().put("type", "TEXT_DETECTION");
            JSONObject image = new JSONObject().put("content", base64Image);
            JSONObject request = new JSONObject()
                    .put("image", image)
                    .put("features", new JSONArray().put(feature));
            JSONObject payload = new JSONObject().put("requests", new JSONArray().put(request));
            JSONObject json = new JSONObject(postJsonText(endpoint, null, payload));
            JSONArray responses = json.optJSONArray("responses");
            if (responses == null || responses.length() == 0) {
                return "";
            }
            JSONObject firstResponse = responses.optJSONObject(0);
            JSONObject annotation = firstResponse == null ? null : firstResponse.optJSONObject("fullTextAnnotation");
            return annotation == null ? "" : annotation.optString("text", "").trim();
        }
        if ("provider:openai-ocr".equals(providerValue)) {
            String endpoint = settings.baseUrl + "/chat/completions";
            String prompt = "Extract all readable text from this image. Return only the extracted text.";
            JSONArray content = new JSONArray()
                    .put(new JSONObject().put("type", "text").put("text", prompt))
                    .put(new JSONObject()
                            .put("type", "image_url")
                            .put("image_url", new JSONObject()
                                    .put("url", "data:image/jpeg;base64," + base64Image)));
            JSONArray messages = new JSONArray()
                    .put(new JSONObject().put("role", "user").put("content", content));
            JSONObject payload = new JSONObject()
                    .put("model", settings.ocrModel.isEmpty() ? settings.chatModel : settings.ocrModel)
                    .put("messages", messages)
                    .put("temperature", 0);
            return parseChatCompletion(postJsonText(endpoint, settings.apiKey, payload));
        }
        throw new IOException("Unsupported online OCR provider: " + providerValue);
    }

    static File synthesizeSpeech(Context context, String providerValue, String text, String languageCode)
            throws IOException, JSONException {
        OnlineProviderConfig.ProviderSettings settings = OnlineProviderConfig.settings(context, providerValue);
        settings.requireApiKey();
        if ("provider:openai-tts".equals(providerValue)) {
            JSONObject payload = new JSONObject()
                    .put("model", settings.ttsModel)
                    .put("voice", settings.ttsVoice.isEmpty() ? "alloy" : settings.ttsVoice)
                    .put("input", text)
                    .put("response_format", "mp3");
            File output = new File(context.getCacheDir(), "online-tts-openai.mp3");
            postJsonToFile(settings.baseUrl + "/audio/speech", settings.apiKey, payload, output);
            return output;
        }
        if ("provider:huggingface-speecht5-free".equals(providerValue)) {
            String endpoint = settings.baseUrl.contains("/v1")
                    ? "https://api-inference.huggingface.co/models/" + settings.ttsModel
                    : settings.baseUrl + "/models/" + settings.ttsModel;
            JSONObject payload = new JSONObject().put("inputs", text);
            File output = new File(context.getCacheDir(), "online-tts-huggingface.wav");
            postJsonToFile(endpoint, settings.apiKey, payload, output);
            return output;
        }
        throw new IOException("Unsupported online TTS provider: " + providerValue);
    }

    static String generateAssistantReply(
            Context context,
            String providerValue,
            String languageName,
            String conversation,
            String prompt
    ) throws IOException, JSONException {
        OnlineProviderConfig.ProviderSettings settings = OnlineProviderConfig.settings(context, providerValue);
        settings.requireApiKey();
        String systemPrompt = "You are an online meeting assistant. Answer in " + languageName
                + ". Keep answers concise, helpful, and conversational.";
        if ("google:gemini-service".equals(providerValue)) {
            return generateWithGemini(settings, systemPrompt, conversation, prompt);
        }
        return generateWithOpenAiCompatible(settings, systemPrompt, conversation, prompt);
    }

    private static String generateWithOpenAiCompatible(
            OnlineProviderConfig.ProviderSettings settings,
            String systemPrompt,
            String conversation,
            String prompt
    ) throws IOException, JSONException {
        JSONArray messages = new JSONArray()
                .put(new JSONObject().put("role", "system").put("content", systemPrompt));
        if (conversation != null && !conversation.trim().isEmpty()) {
            messages.put(new JSONObject().put("role", "user")
                    .put("content", "Conversation so far:\n" + conversation.trim()));
        }
        messages.put(new JSONObject().put("role", "user").put("content", prompt));
        JSONObject payload = new JSONObject()
                .put("model", settings.chatModel)
                .put("messages", messages)
                .put("temperature", 0.6)
                .put("max_tokens", ASSISTANT_MAX_OUTPUT_TOKENS);
        String response = postJsonText(settings.baseUrl + "/chat/completions", settings.apiKey, payload);
        return parseChatCompletion(response);
    }

    private static String generateWithGemini(
            OnlineProviderConfig.ProviderSettings settings,
            String systemPrompt,
            String conversation,
            String prompt
    ) throws IOException, JSONException {
        String endpoint = settings.baseUrl + "/models/" + settings.chatModel + ":generateContent?key=" + settings.apiKey;
        String userText = (conversation == null || conversation.trim().isEmpty())
                ? prompt
                : "Conversation so far:\n" + conversation.trim() + "\n\nUser:\n" + prompt;
        JSONObject payload = new JSONObject()
                .put("systemInstruction", new JSONObject()
                        .put("parts", new JSONArray().put(new JSONObject().put("text", systemPrompt))))
                .put("generationConfig", new JSONObject()
                        .put("temperature", 0.6)
                        .put("maxOutputTokens", ASSISTANT_MAX_OUTPUT_TOKENS))
                .put("contents", new JSONArray().put(new JSONObject()
                        .put("role", "user")
                        .put("parts", new JSONArray().put(new JSONObject().put("text", userText)))));
        JSONObject json = new JSONObject(postJsonText(endpoint, null, payload));
        JSONArray candidates = json.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            return "";
        }
        JSONObject candidate = candidates.optJSONObject(0);
        JSONObject content = candidate == null ? null : candidate.optJSONObject("content");
        JSONArray parts = content == null ? null : content.optJSONArray("parts");
        if (parts == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.optJSONObject(i);
            builder.append(part == null ? "" : part.optString("text", ""));
        }
        return builder.toString().trim();
    }

    private static String parseChatCompletion(String response) throws JSONException {
        JSONObject json = new JSONObject(response);
        JSONArray choices = json.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return "";
        }
        JSONObject choice = choices.optJSONObject(0);
        JSONObject message = choice == null ? null : choice.optJSONObject("message");
        return message == null ? "" : message.optString("content", "").trim();
    }

    private static String parseTextResponse(String response) throws JSONException {
        String trimmed = response == null ? "" : response.trim();
        if (trimmed.startsWith("[")) {
            JSONArray array = new JSONArray(trimmed);
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                String chunk = item == null ? "" : item.optString("text", "");
                if (!chunk.trim().isEmpty()) {
                    if (builder.length() > 0) {
                        builder.append(' ');
                    }
                    builder.append(chunk.trim());
                }
            }
            return builder.toString();
        }
        JSONObject json = new JSONObject(trimmed);
        String text = json.optString("text", "");
        if (!text.trim().isEmpty()) {
            return text.trim();
        }
        JSONArray chunks = json.optJSONArray("chunks");
        if (chunks == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chunks.length(); i++) {
            JSONObject item = chunks.optJSONObject(i);
            String chunk = item == null ? "" : item.optString("text", "");
            if (!chunk.trim().isEmpty()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(chunk.trim());
            }
        }
        return builder.toString();
    }

    private static String postJsonText(String endpoint, String bearerToken, JSONObject payload) throws IOException {
        HttpURLConnection connection = open(endpoint, bearerToken);
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload.toString().getBytes("UTF-8"));
            }
            return readTextResponse(connection);
        } catch (IOException e) {
            connection.disconnect();
            throw e;
        }
    }

    private static void postJsonToFile(String endpoint, String bearerToken, JSONObject payload, File outputFile)
            throws IOException {
        HttpURLConnection connection = open(endpoint, bearerToken);
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "audio/mpeg,audio/wav,application/octet-stream,*/*");
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload.toString().getBytes("UTF-8"));
            }
            readBinaryResponse(connection, outputFile);
        } catch (IOException e) {
            connection.disconnect();
            throw e;
        }
    }

    private static String postBinaryText(String endpoint, String bearerToken, String contentType, byte[] body)
            throws IOException {
        HttpURLConnection connection = open(endpoint, bearerToken);
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", contentType);
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
            return readTextResponse(connection);
        } catch (IOException e) {
            connection.disconnect();
            throw e;
        }
    }

    private static String postMultipartText(String endpoint, String bearerToken, MultipartBody body) throws IOException {
        HttpURLConnection connection = open(endpoint, bearerToken);
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + body.boundary);
            connection.setDoOutput(true);
            try (OutputStream output = connection.getOutputStream()) {
                body.write(output);
            }
            return readTextResponse(connection);
        } catch (IOException e) {
            connection.disconnect();
            throw e;
        }
    }

    private static HttpURLConnection open(String endpoint, String bearerToken) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", "AI-Meeting-Android/1.0");
        connection.setRequestProperty("Accept", "application/json,text/plain,*/*");
        if (bearerToken != null && !bearerToken.trim().isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
        }
        return connection;
    }

    private static String readTextResponse(HttpURLConnection connection) throws IOException {
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

    private static void readBinaryResponse(HttpURLConnection connection, File outputFile) throws IOException {
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        if (code < 200 || code >= 300) {
            String error = "";
            try {
                if (stream != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
                        error = reader.readLine();
                    }
                }
            } finally {
                connection.disconnect();
            }
            throw new IOException("HTTP " + code + ": " + error);
        }
        if (stream == null) {
            connection.disconnect();
            throw new IOException("HTTP " + code + ": empty response");
        }
        boolean saved = false;
        try (InputStream input = new BufferedInputStream(stream);
             FileOutputStream output = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            saved = true;
        } finally {
            connection.disconnect();
            if (!saved && outputFile.exists()) {
                outputFile.delete();
            }
        }
    }

    private static byte[] jpegBytes(Bitmap bitmap) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output);
        return output.toByteArray();
    }

    private static byte[] wavBytes(List<short[]> chunks, int sampleRate) throws IOException {
        int sampleCount = 0;
        for (short[] chunk : chunks) {
            sampleCount += chunk.length;
        }
        int dataSize = sampleCount * 2;
        ByteArrayOutputStream output = new ByteArrayOutputStream(44 + dataSize);
        writeAscii(output, "RIFF");
        writeInt(output, 36 + dataSize);
        writeAscii(output, "WAVE");
        writeAscii(output, "fmt ");
        writeInt(output, 16);
        writeShort(output, (short) 1);
        writeShort(output, (short) 1);
        writeInt(output, sampleRate);
        writeInt(output, sampleRate * 2);
        writeShort(output, (short) 2);
        writeShort(output, (short) 16);
        writeAscii(output, "data");
        writeInt(output, dataSize);
        ByteBuffer buffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN);
        for (short[] chunk : chunks) {
            for (short sample : chunk) {
                buffer.putShort(sample);
            }
        }
        output.write(buffer.array());
        return output.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream output, String value) throws IOException {
        output.write(value.getBytes("US-ASCII"));
    }

    private static void writeInt(ByteArrayOutputStream output, int value) throws IOException {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
        output.write((value >> 16) & 0xff);
        output.write((value >> 24) & 0xff);
    }

    private static void writeShort(ByteArrayOutputStream output, short value) throws IOException {
        output.write(value & 0xff);
        output.write((value >> 8) & 0xff);
    }

    private static final class MultipartBody {
        final String boundary = "AIMeetingBoundary" + Long.toHexString(System.nanoTime());
        private final ByteArrayOutputStream data = new ByteArrayOutputStream();

        void addField(String name, String value) throws IOException {
            if (value == null || value.isEmpty()) {
                return;
            }
            writeBoundary();
            writeAscii(data, "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
            data.write(value.getBytes("UTF-8"));
            writeAscii(data, "\r\n");
        }

        void addFile(String name, String filename, String contentType, byte[] bytes) throws IOException {
            writeBoundary();
            writeAscii(data, "Content-Disposition: form-data; name=\"" + name
                    + "\"; filename=\"" + filename + "\"\r\n");
            writeAscii(data, "Content-Type: " + contentType + "\r\n\r\n");
            data.write(bytes);
            writeAscii(data, "\r\n");
        }

        void write(OutputStream output) throws IOException {
            writeAscii(data, "--" + boundary + "--\r\n");
            output.write(data.toByteArray());
        }

        private void writeBoundary() throws IOException {
            writeAscii(data, "--" + boundary + "\r\n");
        }
    }
}
