package com.example.aimeeting;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SourceEncodingSmokeTest {
    @Test
    public void importantVietnameseStringsDoNotContainKnownMojibake() throws Exception {
        String mainActivity = readSource("src/main/java/com/example/aimeeting/MainActivity.java");
        String modelManager = readSource("src/main/java/com/example/aimeeting/ModelManagerActivity.java");

        assertFalse(mainActivity.contains("Xu\u00c3"));
        assertFalse(mainActivity.contains("Xu\u00e1\u00ba"));
        assertFalse(mainActivity.contains("Ch\u00c6\u00b0a c\u00c3\u00b3 phi\u00c3\u00aan"));
        assertFalse(mainActivity.contains("\u00c2\u00b7"));
        assertFalse(mainActivity.contains("\u00e2\u2020\u2019"));
        assertFalse(mainActivity.contains("B\u00e1\u00bb\u008f qua \u00c3\u00a2m"));
        assertFalse(modelManager.contains("Archive kh\u00c3"));
    }

    private String readSource(String path) throws Exception {
        Path modulePath = Paths.get(path);
        if (Files.exists(modulePath)) {
            return Files.readString(modulePath, StandardCharsets.UTF_8);
        }
        Path rootPath = Paths.get("app").resolve(path);
        assertTrue("Missing source file: " + path, Files.exists(rootPath));
        return Files.readString(rootPath, StandardCharsets.UTF_8);
    }
}
