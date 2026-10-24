package com.example.aimeeting;

import java.util.Locale;

final class AssistantAnswerInspector {
    private static final int MIN_LONG_ANSWER_CHARS = 120;
    private static final int MAX_LAST_TOKEN_FOR_INLINE_JOIN = 12;

    private AssistantAnswerInspector() {
    }

    static boolean isLikelyIncomplete(String answer) {
        if (answer == null) {
            return false;
        }
        String trimmed = answer.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        char last = trimmed.charAt(trimmed.length() - 1);
        if (last == ',' || last == ':' || last == ';' || last == '-' || last == '(' || last == '[' || last == '{') {
            return true;
        }

        String lastLine = lastLine(trimmed);
        if (lastLine.matches("\\s*\\d+[\\.)]\\s+.+") && !endsWithTerminalPunctuation(lastLine)) {
            return true;
        }

        if (endsWithTerminalPunctuation(trimmed)) {
            return false;
        }

        return trimmed.length() >= MIN_LONG_ANSWER_CHARS
                && (Character.isLetterOrDigit(last) || last == '\'' || last == '"');
    }

    static String continuationPrompt(String languageName, String userPrompt, String answerSoFar) {
        return "Continue the assistant answer that was cut off. "
                + "Answer in " + safe(languageName) + ". "
                + "Do not repeat earlier content. Start with the missing remainder of the final unfinished sentence. "
                + "If the final word is unfinished, repeat that full word from the beginning rather than only the suffix. "
                + "If the answer is already complete, return an empty response.\n\n"
                + "Original user request:\n" + safe(userPrompt) + "\n\n"
                + "Assistant answer so far:\n" + safe(answerSoFar);
    }

    static String mergeContinuation(String answerSoFar, String continuation) {
        String base = safe(answerSoFar).trim();
        String next = safe(continuation).trim();
        if (base.isEmpty()) {
            return next;
        }
        if (next.isEmpty()) {
            return base;
        }

        int overlap = commonOverlap(base, next, 120);
        if (overlap >= 3) {
            return (base + next.substring(overlap)).trim();
        }

        char last = base.charAt(base.length() - 1);
        char first = next.charAt(0);
        if (Character.isLetterOrDigit(last) && Character.isLetterOrDigit(first)) {
            String token = lastToken(base);
            if (!token.isEmpty()
                    && token.length() <= MAX_LAST_TOKEN_FOR_INLINE_JOIN
                    && next.toLowerCase(Locale.ROOT).startsWith(token.toLowerCase(Locale.ROOT))) {
                return (base + next.substring(token.length())).trim();
            }
            if (token.length() == 1) {
                return (base + next).trim();
            }
            return (base + " " + next).trim();
        }

        if (Character.isWhitespace(last) || isClosingContinuation(first)) {
            return (base + next).trim();
        }
        return (base + "\n\n" + next).trim();
    }

    private static boolean endsWithTerminalPunctuation(String text) {
        return text.endsWith(".")
                || text.endsWith("!")
                || text.endsWith("?")
                || text.endsWith("...")
                || text.endsWith("\u2026")
                || text.endsWith("\u3002")
                || text.endsWith("\uff01")
                || text.endsWith("\uff1f");
    }

    private static String lastLine(String text) {
        int index = text.lastIndexOf('\n');
        return index < 0 ? text : text.substring(index + 1);
    }

    private static String lastToken(String text) {
        int end = text.length();
        int start = end;
        while (start > 0 && Character.isLetterOrDigit(text.charAt(start - 1))) {
            start--;
        }
        return text.substring(start, end);
    }

    private static int commonOverlap(String base, String next, int max) {
        String baseLower = base.toLowerCase(Locale.ROOT);
        String nextLower = next.toLowerCase(Locale.ROOT);
        int limit = Math.min(max, Math.min(baseLower.length(), nextLower.length()));
        for (int length = limit; length >= 1; length--) {
            if (baseLower.regionMatches(baseLower.length() - length, nextLower, 0, length)) {
                return length;
            }
        }
        return 0;
    }

    private static boolean isClosingContinuation(char first) {
        return first == '.' || first == ',' || first == ';' || first == ':' || first == '!' || first == '?'
                || first == ')' || first == ']' || first == '}';
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
