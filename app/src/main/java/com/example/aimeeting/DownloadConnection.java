package com.example.aimeeting;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;

final class DownloadConnection {
    private static final String USER_AGENT = "AI-Meeting-Android/1.0";

    private DownloadConnection() {
    }

    static HttpURLConnection open(String urlString, int connectTimeoutMs, int readTimeoutMs) throws IOException {
        return openConnection(urlString, connectTimeoutMs, readTimeoutMs, null);
    }

    static HttpURLConnection open(
            String urlString,
            int connectTimeoutMs,
            int readTimeoutMs,
            String rangeHeader
    ) throws IOException {
        return openConnection(urlString, connectTimeoutMs, readTimeoutMs, rangeHeader);
    }

    static HttpURLConnection reopenAfterTlsFailure(
            String urlString,
            int connectTimeoutMs,
            int readTimeoutMs,
            IOException failure
    ) throws IOException {
        throw tlsFailure(failure);
    }

    static HttpURLConnection reopenAfterTlsFailure(
            String urlString,
            int connectTimeoutMs,
            int readTimeoutMs,
            String rangeHeader,
            IOException failure
    ) throws IOException {
        throw tlsFailure(failure);
    }

    static boolean isChainValidationFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (current instanceof SSLHandshakeException
                    || (message != null && message.toLowerCase(java.util.Locale.US).contains("chain validation failed"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static HttpURLConnection openConnection(
            String urlString,
            int connectTimeoutMs,
            int readTimeoutMs,
            String rangeHeader
    ) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "*/*");
        if (rangeHeader != null && !rangeHeader.isEmpty()) {
            connection.setRequestProperty("Range", rangeHeader);
        }
        return connection;
    }

    private static IOException tlsFailure(IOException failure) {
        if (!isChainValidationFailure(failure)) {
            return failure;
        }
        return new IOException("TLS certificate validation failed. Check device date/time, Android CA store, or network proxy.", failure);
    }
}
