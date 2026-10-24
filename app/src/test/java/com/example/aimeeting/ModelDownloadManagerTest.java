package com.example.aimeeting;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ModelDownloadManagerTest {
    @Test
    public void formatBytesUsesExpectedUnits() {
        assertEquals("1.0 KB", ModelDownloadManager.formatBytes(1024));
        assertEquals("1.5 MB", ModelDownloadManager.formatBytes(1536L * 1024L));
        assertEquals("2.00 GB", ModelDownloadManager.formatBytes(2L * 1024L * 1024L * 1024L));
    }
}
