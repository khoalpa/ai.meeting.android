package com.example.aimeeting;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class FakeSharedPreferences implements SharedPreferences {
    private final Map<String, Object> values = new HashMap<>();

    FakeSharedPreferences putRaw(String key, Object value) {
        values.put(key, value);
        return this;
    }

    @Override
    public Map<String, ?> getAll() {
        return Collections.unmodifiableMap(values);
    }

    @Override
    public String getString(String key, String defValue) {
        Object value = values.get(key);
        if (value == null) {
            return defValue;
        }
        if (value instanceof String) {
            return (String) value;
        }
        throw new ClassCastException(key + " is not a String");
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        return defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return defValue;
    }

    @Override
    public boolean contains(String key) {
        return values.containsKey(key);
    }

    @Override
    public Editor edit() {
        throw new UnsupportedOperationException("Not needed for tests");
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }
}
