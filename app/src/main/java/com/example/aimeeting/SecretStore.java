package com.example.aimeeting;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecretStore {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "ai_meeting_provider_secrets";
    private static final String ENCRYPTED_SUFFIX = "_encrypted";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private SecretStore() {
    }

    static String get(Context context, SharedPreferences preferences, String key) {
        String encrypted = OnlineProviderConfig.safeString(preferences, encryptedKey(key), "");
        if (encrypted != null && !encrypted.isEmpty()) {
            try {
                return decrypt(encrypted);
            } catch (Exception e) {
                return "";
            }
        }

        String plaintext = OnlineProviderConfig.safeString(preferences, key, "");
        if (plaintext != null && !plaintext.trim().isEmpty()) {
            put(preferences, key, plaintext);
            return plaintext.trim();
        }
        return "";
    }

    static void put(SharedPreferences preferences, String key, String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) {
            preferences.edit()
                    .remove(key)
                    .remove(encryptedKey(key))
                    .apply();
            return;
        }
        try {
            String encrypted = encrypt(clean);
            preferences.edit()
                    .remove(key)
                    .putString(encryptedKey(key), encrypted)
                    .apply();
        } catch (Exception e) {
            // Keep the existing secret if Android Keystore is temporarily unavailable.
        }
    }

    private static String encryptedKey(String key) {
        return key + ENCRYPTED_SUFFIX;
    }

    private static String encrypt(String plaintext) throws Exception {
        byte[] iv = new byte[GCM_IV_BYTES];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    private static String decrypt(String encoded) throws Exception {
        byte[] combined = Base64.decode(encoded, Base64.NO_WRAP);
        if (combined.length <= GCM_IV_BYTES) {
            return "";
        }
        byte[] iv = new byte[GCM_IV_BYTES];
        byte[] encrypted = new byte[combined.length - GCM_IV_BYTES];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8).trim();
    }

    private static SecretKey secretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        KeyStore.Entry entry = keyStore.getEntry(KEY_ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) {
            return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        }

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
