package com.example.aimeeting;

final class TranscriptSelector {
    private TranscriptSelector() {
    }

    static boolean shouldUseFirst(String firstText, float firstScore, String secondText, float secondScore) {
        boolean firstEmpty = isBlank(firstText);
        boolean secondEmpty = isBlank(secondText);
        if (firstEmpty != secondEmpty) {
            return !firstEmpty;
        }
        if (Math.abs(firstScore - secondScore) >= 0.10f) {
            return firstScore > secondScore;
        }
        int firstLength = trimmedLength(firstText);
        int secondLength = trimmedLength(secondText);
        if (Math.abs(firstLength - secondLength) >= 4) {
            return firstLength > secondLength;
        }
        return firstScore >= secondScore;
    }

    static String emptyMarker(String text) {
        return isBlank(text) ? "--" : text;
    }

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private static int trimmedLength(String text) {
        return text == null ? 0 : text.trim().length();
    }
}
