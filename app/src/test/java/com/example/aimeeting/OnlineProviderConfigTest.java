package com.example.aimeeting;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OnlineProviderConfigTest {
    @Test
    public void mapsGroupedProvidersToCredentialOwner() {
        assertEquals("openai", OnlineProviderConfig.providerId("provider:openai-chat"));
        assertEquals("groq", OnlineProviderConfig.providerId("provider:groq-whisper-free"));
        assertEquals("google", OnlineProviderConfig.providerId("google:translate-service"));
    }

    @Test
    public void trimsTrailingSlashesFromBaseUrl() {
        OnlineProviderConfig.ProviderSettings settings = new OnlineProviderConfig.ProviderSettings(
                "provider:test",
                "test",
                "token",
                " https://example.com/v1/// ",
                "",
                "",
                "",
                "",
                "model");

        assertEquals("https://example.com/v1", settings.baseUrl);
    }

    @Test
    public void safeStringFallsBackWhenPreferenceTypeIsWrong() {
        FakeSharedPreferences preferences = new FakeSharedPreferences()
                .putRaw("provider", 42);

        assertEquals("fallback", OnlineProviderConfig.safeString(preferences, "provider", "fallback"));
    }

    @Test
    public void safeStringReturnsStoredString() {
        FakeSharedPreferences preferences = new FakeSharedPreferences()
                .putRaw("provider", " value ");

        assertEquals(" value ", OnlineProviderConfig.safeString(preferences, "provider", "fallback"));
    }
}
