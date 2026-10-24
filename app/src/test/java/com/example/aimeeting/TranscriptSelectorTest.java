package com.example.aimeeting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TranscriptSelectorTest {
    @Test
    public void shouldUseNonEmptyTranscript() {
        assertTrue(TranscriptSelector.shouldUseFirst("hello", 0.1f, " ", 0.9f));
        assertFalse(TranscriptSelector.shouldUseFirst("", 0.9f, "xin chao", 0.1f));
    }

    @Test
    public void shouldPreferClearlyHigherLanguageScore() {
        assertTrue(TranscriptSelector.shouldUseFirst("short", 0.72f, "longer text", 0.50f));
        assertFalse(TranscriptSelector.shouldUseFirst("longer text", 0.50f, "short", 0.72f));
    }

    @Test
    public void shouldPreferLongerTranscriptWhenScoresAreClose() {
        assertTrue(TranscriptSelector.shouldUseFirst("hello everyone", 0.52f, "hello", 0.48f));
        assertFalse(TranscriptSelector.shouldUseFirst("hi", 0.51f, "hello everyone", 0.49f));
    }

    @Test
    public void shouldPreferFirstWhenScoresAndLengthsAreClose() {
        assertTrue(TranscriptSelector.shouldUseFirst("test", 0.50f, "tent", 0.46f));
    }

    @Test
    public void emptyMarkerKeepsVisibleTextAndMarksBlankValues() {
        assertEquals("--", TranscriptSelector.emptyMarker(null));
        assertEquals("--", TranscriptSelector.emptyMarker("   "));
        assertEquals("  hello  ", TranscriptSelector.emptyMarker("  hello  "));
    }
}
