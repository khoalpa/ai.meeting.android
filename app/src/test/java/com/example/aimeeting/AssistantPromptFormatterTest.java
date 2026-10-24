package com.example.aimeeting;

import org.junit.Test;

import java.util.List;

import kotlin.Pair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AssistantPromptFormatterTest {
    @Test
    public void stripsEnglishAssistantRolePrefix() {
        String cleaned = AssistantPromptFormatter.cleanAnswer("Assistant: Viet Nam has about 100 million people.");

        assertEquals("Viet Nam has about 100 million people.", cleaned);
    }

    @Test
    public void stripsVietnameseAssistantRolePrefix() {
        String cleaned = AssistantPromptFormatter.cleanAnswer("Tr\u1ee3 l\u00fd: D\u00e2n s\u1ed1 Vi\u1ec7t Nam kho\u1ea3ng 100 tri\u1ec7u ng\u01b0\u1eddi.");

        assertEquals("D\u00e2n s\u1ed1 Vi\u1ec7t Nam kho\u1ea3ng 100 tri\u1ec7u ng\u01b0\u1eddi.", cleaned);
    }

    @Test
    public void keepsAnswerWhenModelEchoesChatTemplate() {
        String raw = "<start_of_turn>user\nD\u00e2n s\u1ed1 c\u1ee7a Vi\u1ec7t Nam?\n<end_of_turn>\n"
                + "<start_of_turn>assistant\nD\u00e2n s\u1ed1 Vi\u1ec7t Nam kho\u1ea3ng 100 tri\u1ec7u ng\u01b0\u1eddi.<end_of_turn>";

        String cleaned = AssistantPromptFormatter.cleanAnswer(raw);

        assertEquals("D\u00e2n s\u1ed1 Vi\u1ec7t Nam kho\u1ea3ng 100 tri\u1ec7u ng\u01b0\u1eddi.", cleaned);
    }

    @Test
    public void keepsAnswerWhenModelEchoesPlainPrompt() {
        String prompt = AssistantPromptFormatter.buildPlainPrompt(
                "System prompt",
                "User: previous question\nAssistant: previous answer",
                "T\u1ea1o 1 c\u00e2u chuy\u1ec7n ng\u1eabu nhi\u00ean.");
        String raw = prompt + "\nNg\u00e0y x\u01b0a c\u00f3 m\u1ed9t ng\u01b0\u1eddi b\u1ea1n nh\u1ecf lu\u00f4n t\u00f2 m\u00f2 v\u1ec1 b\u1ea7u tr\u1eddi.";

        String cleaned = AssistantPromptFormatter.cleanAnswer(raw);

        assertEquals("Ng\u00e0y x\u01b0a c\u00f3 m\u1ed9t ng\u01b0\u1eddi b\u1ea1n nh\u1ecf lu\u00f4n t\u00f2 m\u00f2 v\u1ec1 b\u1ea7u tr\u1eddi.", cleaned);
    }

    @Test
    public void keepsValidVietnameseContent() {
        String answer = "\u0110\u00e2y l\u00e0 c\u00e2u tr\u1ea3 l\u1eddi ti\u1ebfng Vi\u1ec7t c\u00f3 d\u1ea5u, kh\u00f4ng b\u1ecb c\u1eaft nh\u1ea7m.";

        String cleaned = AssistantPromptFormatter.cleanAnswer(answer);

        assertEquals(answer, cleaned);
    }

    @Test
    public void buildsPlainPromptWithAssistantCue() {
        String prompt = AssistantPromptFormatter.buildPlainPrompt(
                "System prompt",
                "User: previous question\nAssistant: previous answer",
                "Current question");

        assertTrue(prompt.contains("System:\nSystem prompt"));
        assertTrue(prompt.contains("Context stored on this device:\nUser: previous question\nAssistant: previous answer"));
        assertTrue(prompt.contains("User:\nCurrent question"));
        assertTrue(prompt.endsWith("Assistant:"));
        assertFalse(prompt.endsWith("Assistant:\n"));
    }

    @Test
    public void buildsChatMessagesWithSystemContextAndUserPrompt() {
        List<Pair<String, String>> messages = AssistantPromptFormatter.buildMessages(
                "System prompt",
                "User: previous question\nAssistant: previous answer",
                "Current question");

        assertEquals(3, messages.size());
        assertEquals("system", messages.get(0).getFirst());
        assertEquals("System prompt", messages.get(0).getSecond());
        assertEquals("user", messages.get(1).getFirst());
        assertEquals("Conversation context stored on this device:\nUser: previous question\nAssistant: previous answer",
                messages.get(1).getSecond());
        assertEquals("user", messages.get(2).getFirst());
        assertEquals("Current question", messages.get(2).getSecond());
    }
}
