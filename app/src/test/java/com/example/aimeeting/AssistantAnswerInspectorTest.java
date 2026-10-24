package com.example.aimeeting;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class AssistantAnswerInspectorTest {
    @Test
    public void detectsCutOffNumberedListItem() {
        String answer = "5. Noi dung da hoan tat.\n\n6. Noi dung khac.\n\n7. Co he thong g";

        assertTrue(AssistantAnswerInspector.isLikelyIncomplete(answer));
    }

    @Test
    public void treatsTerminalPunctuationAsComplete() {
        String answer = "1. Noi dung thu nhat.\n\n2. Noi dung thu hai.";

        assertFalse(AssistantAnswerInspector.isLikelyIncomplete(answer));
    }

    @Test
    public void detectsLongAnswerWithoutTerminalPunctuation() {
        String answer = "Day la mot cau tra loi kha dai duoc tao ra de mo phong truong hop model dung lai "
                + "sau khi da sinh kha nhieu ky tu nhung van chua ket thuc bang dau cau ro rang";

        assertTrue(AssistantAnswerInspector.isLikelyIncomplete(answer));
    }

    @Test
    public void mergesMissingRemainderIntoPartialWord() {
        String merged = AssistantAnswerInspector.mergeContinuation(
                "7. Co he thong g",
                "iao thong cong cong phat trien.");

        assertEquals("7. Co he thong giao thong cong cong phat trien.", merged);
    }

    @Test
    public void avoidsDuplicatingRepeatedPartialWordPrefix() {
        String merged = AssistantAnswerInspector.mergeContinuation(
                "7. Co he thong g",
                "giao thong cong cong phat trien.");

        assertEquals("7. Co he thong giao thong cong cong phat trien.", merged);
    }

    @Test
    public void avoidsDuplicatingLongerPartialWordPrefix() {
        String merged = AssistantAnswerInspector.mergeContinuation(
                "Day la mot tru",
                "truong hop dang chu y.");

        assertEquals("Day la mot truong hop dang chu y.", merged);
    }

    @Test
    public void insertsSpaceForNextCompleteWord() {
        String merged = AssistantAnswerInspector.mergeContinuation(
                "Day la",
                "mot vi du.");

        assertEquals("Day la mot vi du.", merged);
    }
}
