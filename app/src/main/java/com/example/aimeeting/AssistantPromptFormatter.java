package com.example.aimeeting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kotlin.Pair;

final class AssistantPromptFormatter {
    private AssistantPromptFormatter() {
    }

    static List<Pair<String, String>> buildMessages(String systemPrompt, String context, String userPrompt) {
        List<Pair<String, String>> messages = new ArrayList<>();
        messages.add(new Pair<>("system", safe(systemPrompt).trim()));
        String trimmedContext = safe(context).trim();
        if (!trimmedContext.isEmpty()) {
            messages.add(new Pair<>("user", "Conversation context stored on this device:\n" + trimmedContext));
        }
        messages.add(new Pair<>("user", safe(userPrompt).trim()));
        return messages;
    }

    static String buildPlainPrompt(String systemPrompt, String context, String userPrompt) {
        StringBuilder prompt = new StringBuilder();
        String trimmedSystem = safe(systemPrompt).trim();
        if (!trimmedSystem.isEmpty()) {
            prompt.append("System:\n").append(trimmedSystem).append("\n\n");
        }
        String trimmedContext = safe(context).trim();
        if (!trimmedContext.isEmpty()) {
            prompt.append("Context stored on this device:\n").append(trimmedContext).append("\n\n");
        }
        prompt.append("User:\n").append(safe(userPrompt).trim()).append("\n\nAssistant:");
        return prompt.toString();
    }

    static String cleanAnswer(String rawAnswer) {
        if (rawAnswer == null) {
            return "";
        }
        String answer = rawAnswer.replace("\r\n", "\n").replace('\r', '\n');
        answer = afterLastAssistantMarker(answer);
        answer = removeTemplateTokens(answer);
        answer = cutAtNextTurn(answer);
        answer = stripLeadingRolePrefixes(answer);
        return answer.trim();
    }

    private static String afterLastAssistantMarker(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        String[] markers = {
                "<start_of_turn>assistant\n",
                "<start_of_turn>assistant",
                "<|im_start|>assistant\n",
                "<|assistant|>",
                "### assistant:",
                "### response:",
                "[/inst]",
                "\nassistant:",
                "\ntr\u1ee3 l\u00fd:",
                "\ntro ly:",
                "\ntr\u00e1\u00bb\u00a3 l\u00c3\u00bd:"
        };
        int markerAt = -1;
        int markerLength = 0;
        for (String marker : markers) {
            int index = lower.lastIndexOf(marker.toLowerCase(Locale.ROOT));
            if (index >= 0 && index >= markerAt) {
                markerAt = index;
                markerLength = marker.length();
            }
        }
        if (markerAt >= 0) {
            return value.substring(markerAt + markerLength);
        }
        return value;
    }

    private static String removeTemplateTokens(String value) {
        return value
                .replace("<end_of_turn>", "")
                .replace("<start_of_turn>system", "")
                .replace("<start_of_turn>user", "")
                .replace("<start_of_turn>assistant", "")
                .replace("<start_of_turn>", "")
                .replace("<|endoftext|>", "")
                .replace("<|eot_id|>", "")
                .replace("<|im_end|>", "")
                .replace("<|im_start|>", "")
                .replace("<s>", "")
                .replace("</s>", "");
    }

    private static String cutAtNextTurn(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        String[] stopMarkers = {
                "\n<start_of_turn>user",
                "\n<start_of_turn>system",
                "\n<|im_start|>user",
                "\n<|im_start|>system",
                "\nquestion:",
                "\nuser:",
                "\nassistant:",
                "\nb\u1ea1n:",
                "\nban:",
                "\ntr\u1ee3 l\u00fd:",
                "\ntro ly:",
                "\ntr\u00e1\u00bb\u00a3 l\u00c3\u00bd:"
        };
        int cutAt = -1;
        for (String marker : stopMarkers) {
            int index = lower.indexOf(marker.toLowerCase(Locale.ROOT));
            if (index > 0 && (cutAt < 0 || index < cutAt)) {
                cutAt = index;
            }
        }
        return cutAt >= 0 ? value.substring(0, cutAt) : value;
    }

    private static String stripLeadingRolePrefixes(String value) {
        String answer = value.trim();
        String[] prefixes = {
                "assistant:",
                "tr\u1ee3 l\u00fd:",
                "tro ly:",
                "tr\u00e1\u00bb\u00a3 l\u00c3\u00bd:",
                "answer:",
                "c\u00e2u tr\u1ea3 l\u1eddi:",
                "cau tra loi:"
        };
        boolean changed;
        do {
            changed = false;
            String lower = answer.toLowerCase(Locale.ROOT);
            for (String prefix : prefixes) {
                if (lower.startsWith(prefix.toLowerCase(Locale.ROOT))) {
                    answer = answer.substring(prefix.length()).trim();
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return answer;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
