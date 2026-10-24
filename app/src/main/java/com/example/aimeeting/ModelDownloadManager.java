package com.example.aimeeting;

import android.content.Context;

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
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class ModelDownloadManager {
    interface ProgressReporter {
        void report(String phase, long doneBytes, long totalBytes);
    }

    private static final int BUFFER_BYTES = 32 * 1024;

    private final Context context;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    ModelDownloadManager(Context context, int connectTimeoutMs, int readTimeoutMs) {
        this.context = context.getApplicationContext();
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    void downloadZip(
            String urlString,
            File destinationParent,
            String cachePrefix,
            String downloadPhase,
            String extractPhase,
            String extractDonePhase,
            String resumeFromLabel,
            String alreadyAvailableLabel,
            ProgressReporter progress
    ) throws IOException {
        ensureDirectory(destinationParent, "Could not create model folder.");
        File zipFile = resumableCacheFile(cachePrefix, urlString, ".zip.part");
        downloadArchive(urlString, zipFile, downloadPhase, resumeFromLabel, alreadyAvailableLabel, progress);
        report(progress, extractPhase, zipFile.length(), zipFile.length());
        unzip(zipFile, destinationParent);
        report(progress, extractDonePhase, zipFile.length(), zipFile.length());
        deleteOrSchedule(zipFile);
    }

    void downloadTarBz2(
            String urlString,
            File destinationParent,
            String cachePrefix,
            String downloadPhase,
            String extractPhase,
            String extractDonePhase,
            String resumeFromLabel,
            String alreadyAvailableLabel,
            ProgressReporter progress
    ) throws IOException {
        ensureDirectory(destinationParent, "Could not create model folder.");
        File archiveFile = resumableCacheFile(cachePrefix, urlString, ".tar.bz2.part");
        downloadArchive(urlString, archiveFile, downloadPhase, resumeFromLabel, alreadyAvailableLabel, progress);
        report(progress, extractPhase, archiveFile.length(), archiveFile.length());
        extractTarBz2(archiveFile, destinationParent);
        report(progress, extractDonePhase, archiveFile.length(), archiveFile.length());
        deleteOrSchedule(archiveFile);
    }

    void downloadFile(
            String urlString,
            File outputFile,
            String downloadPhase,
            String savePhase,
            String resumeFromLabel,
            String alreadyAvailableLabel,
            ProgressReporter progress
    ) throws IOException {
        File parent = outputFile.getAbsoluteFile().getParentFile();
        if (parent != null) {
            ensureDirectory(parent, "Could not create model folder.");
        }
        File tempFile = new File(parent, outputFile.getName() + ".part");
        downloadArchive(urlString, tempFile, downloadPhase, resumeFromLabel, alreadyAvailableLabel, progress);
        report(progress, savePhase, tempFile.length(), tempFile.length());
        if (outputFile.exists() && !outputFile.delete()) {
            throw new IOException("Could not delete the old model.");
        }
        if (!tempFile.renameTo(outputFile)) {
            throw new IOException("Could not save the model.");
        }
    }

    private void downloadArchive(
            String urlString,
            File outputFile,
            String phase,
            String resumeFromLabel,
            String alreadyAvailableLabel,
            ProgressReporter progress
    ) throws IOException {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new IOException("Missing download URL.");
        }
        long resumeBytes = outputFile.isFile() ? outputFile.length() : 0L;
        String rangeHeader = resumeBytes > 0 ? "bytes=" + resumeBytes + "-" : null;
        HttpURLConnection connection = DownloadConnection.open(urlString, connectTimeoutMs, readTimeoutMs, rangeHeader);
        int responseCode;
        try {
            connection.connect();
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            connection.disconnect();
            connection = DownloadConnection.reopenAfterTlsFailure(
                    urlString,
                    connectTimeoutMs,
                    readTimeoutMs,
                    rangeHeader,
                    e
            );
            connection.connect();
            responseCode = connection.getResponseCode();
        }
        if (responseCode == 416 && resumeBytes > 0) {
            report(progress, phase + " " + alreadyAvailableLabel, resumeBytes, resumeBytes);
            connection.disconnect();
            return;
        }
        if (responseCode == HttpURLConnection.HTTP_OK && resumeBytes > 0) {
            if (!outputFile.delete()) {
                connection.disconnect();
                throw new IOException("Could not delete the partial download.");
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
            byte[] buffer = new byte[BUFFER_BYTES];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                doneBytes += count;
                long now = System.currentTimeMillis();
                if (now - lastReportAt > 450L || doneBytes == totalBytes) {
                    report(progress, resumedPhase(phase, resumeFromLabel, resumeBytes)
                            + speedSuffix(doneBytes - resumeBytes, startedAt), doneBytes, totalBytes);
                    lastReportAt = now;
                }
            }
        } finally {
            connection.disconnect();
        }
        report(progress, resumedPhase(phase, resumeFromLabel, resumeBytes)
                + speedSuffix(doneBytes - resumeBytes, startedAt), doneBytes, totalBytes);
    }

    private File resumableCacheFile(String prefix, String urlString, String suffix) {
        String key = Integer.toHexString(urlString.hashCode());
        return new File(context.getCacheDir(), prefix + "-" + key + suffix);
    }

    private static String resumedPhase(String phase, String resumeFromLabel, long resumeBytes) {
        if (resumeBytes <= 0) {
            return phase;
        }
        return phase + " - " + resumeFromLabel + " " + formatBytes(resumeBytes);
    }

    private static String speedSuffix(long doneBytes, long startedAt) {
        long elapsedMs = Math.max(1L, System.currentTimeMillis() - startedAt);
        long bytesPerSecond = doneBytes * 1000L / elapsedMs;
        return " - " + formatBytes(bytesPerSecond) + "/s";
    }

    private static void unzip(File zipFile, File destinationParent) throws IOException {
        byte[] buffer = new byte[BUFFER_BYTES];
        String destinationRoot = destinationParent.getCanonicalPath() + File.separator;
        try (ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                File outputFile = new File(destinationParent, entry.getName());
                String outputPath = outputFile.getCanonicalPath();
                if (!outputPath.startsWith(destinationRoot)) {
                    throw new IOException("Invalid ZIP archive.");
                }
                writeEntry(zipInput, entry.isDirectory(), outputFile, buffer);
                zipInput.closeEntry();
            }
        }
    }

    private static void extractTarBz2(File archiveFile, File destinationParent) throws IOException {
        byte[] buffer = new byte[BUFFER_BYTES];
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
                    throw new IOException("Invalid TAR.BZ2 archive.");
                }
                writeEntry(tarInput, entry.isDirectory(), outputFile, buffer);
            }
        }
    }

    private static void writeEntry(InputStream input, boolean directory, File outputFile, byte[] buffer)
            throws IOException {
        if (directory) {
            ensureDirectory(outputFile, "Could not create model folder.");
            return;
        }
        File parent = outputFile.getParentFile();
        if (parent != null) {
            ensureDirectory(parent, "Could not create model folder.");
        }
        try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
    }

    static String formatBytes(long bytes) {
        if (bytes < 1024L * 1024L) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024f);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(Locale.US, "%.1f MB", bytes / 1024f / 1024f);
        }
        return String.format(Locale.US, "%.2f GB", bytes / 1024f / 1024f / 1024f);
    }

    private static void ensureDirectory(File directory, String message) throws IOException {
        if (directory == null) {
            throw new IOException(message);
        }
        if (directory.exists() && !directory.isDirectory()) {
            throw new IOException(message);
        }
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException(message);
        }
    }

    private static void report(ProgressReporter progress, String phase, long doneBytes, long totalBytes) {
        if (progress != null) {
            progress.report(phase, doneBytes, totalBytes);
        }
    }

    private static void deleteOrSchedule(File file) {
        if (!file.delete()) {
            file.deleteOnExit();
        }
    }
}
