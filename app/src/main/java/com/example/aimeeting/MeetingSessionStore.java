package com.example.aimeeting;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class MeetingSessionStore {
    private static final String PREFS = "meeting_session";
    private static final String KEY_STARTED_AT = "started_at";
    private static final String KEY_TURNS = "turns";
    private static final int MAX_TURNS = 100;

    private final SharedPreferences preferences;

    MeetingSessionStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (preferences.getLong(KEY_STARTED_AT, 0L) == 0L) {
            preferences.edit().putLong(KEY_STARTED_AT, System.currentTimeMillis()).apply();
        }
    }

    synchronized void addTurn(String original, String translated, String sourceName, String targetName,
                              String sourceCode, String targetCode) {
        try {
            JSONArray turns = readArray();
            JSONObject turn = new JSONObject()
                    .put("timestamp", System.currentTimeMillis())
                    .put("sourceName", clean(sourceName))
                    .put("targetName", clean(targetName))
                    .put("sourceCode", clean(sourceCode))
                    .put("targetCode", clean(targetCode))
                    .put("original", clean(original))
                    .put("translated", clean(translated));
            turns.put(turn);
            while (turns.length() > MAX_TURNS) {
                turns.remove(0);
            }
            preferences.edit().putString(KEY_TURNS, turns.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    synchronized List<Turn> turns() {
        JSONArray array = readArray();
        List<Turn> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) {
                continue;
            }
            result.add(new Turn(
                    item.optLong("timestamp", 0L),
                    item.optString("sourceName", ""),
                    item.optString("targetName", ""),
                    item.optString("sourceCode", ""),
                    item.optString("targetCode", ""),
                    item.optString("original", ""),
                    item.optString("translated", "")
            ));
        }
        return result;
    }

    synchronized boolean hasTurns() {
        return readArray().length() > 0;
    }

    synchronized void reset() {
        preferences.edit()
                .putLong(KEY_STARTED_AT, System.currentTimeMillis())
                .putString(KEY_TURNS, "[]")
                .apply();
    }

    synchronized String exportText(String title) {
        StringBuilder builder = new StringBuilder();
        builder.append(title == null || title.trim().isEmpty() ? "AI Meeting" : title.trim()).append('\n');
        builder.append("Started: ").append(formatTime(preferences.getLong(KEY_STARTED_AT, 0L))).append('\n');
        builder.append("Exported: ").append(formatTime(System.currentTimeMillis())).append("\n\n");

        List<Turn> turns = turns();
        if (turns.isEmpty()) {
            builder.append("No translated turns yet.");
            return builder.toString();
        }
        for (int i = 0; i < turns.size(); i++) {
            Turn turn = turns.get(i);
            builder.append("Turn ").append(i + 1).append(" - ").append(formatTime(turn.timestamp)).append('\n');
            builder.append(turn.sourceName).append(" -> ").append(turn.targetName).append('\n');
            builder.append("Source: ").append(turn.original).append('\n');
            builder.append("Translation: ").append(turn.translated).append("\n\n");
        }
        return builder.toString().trim();
    }

    private JSONArray readArray() {
        String raw = preferences.getString(KEY_TURNS, "[]");
        try {
            return new JSONArray(raw == null ? "[]" : raw);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String formatTime(long timestamp) {
        long value = timestamp > 0L ? timestamp : System.currentTimeMillis();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
        return format.format(new Date(value));
    }

    static final class Turn {
        final long timestamp;
        final String sourceName;
        final String targetName;
        final String sourceCode;
        final String targetCode;
        final String original;
        final String translated;

        Turn(long timestamp, String sourceName, String targetName, String sourceCode, String targetCode,
             String original, String translated) {
            this.timestamp = timestamp;
            this.sourceName = sourceName;
            this.targetName = targetName;
            this.sourceCode = sourceCode;
            this.targetCode = targetCode;
            this.original = original;
            this.translated = translated;
        }
    }
}
