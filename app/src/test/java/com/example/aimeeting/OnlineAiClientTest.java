package com.example.aimeeting;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class OnlineAiClientTest {
    @Test
    public void parseChatCompletionHandlesMissingMessageObject() throws Exception {
        String response = "{\"choices\":[null]}";

        assertEquals("", invokeStringParser("parseChatCompletion", response));
    }

    @Test
    public void parseTextResponseHandlesNullChunkObject() throws Exception {
        String response = "{\"chunks\":[null,{\"text\":\"hello\"},{\"text\":\"world\"}]}";

        assertEquals("hello world", invokeStringParser("parseTextResponse", response));
    }

    private static String invokeStringParser(String methodName, String response) throws Exception {
        Method method = OnlineAiClient.class.getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, response);
    }
}
