package com.techquiz.quiz;

import com.techquiz.question.CodeSnippet;
import java.util.List;

/** A question as the player sees it: the options carry no {@code correct} flag (ADR 0004). */
public record QuizQuestion(String id, String topicId, String prompt, CodeSnippet code, List<Option> options) {

    public record Option(String id, String text) {
    }
}
