package com.techquiz.testsupport;

import com.techquiz.question.AnswerOption;
import com.techquiz.question.Question;
import java.util.List;

public final class QuestionFixtures {

    private QuestionFixtures() {
    }

    /** A question whose options are {@code <id>-a} (correct), {@code <id>-b} and {@code <id>-c}. */
    public static Question question(String id, String topicId) {
        return new Question(id, topicId, "Prompt of " + id, null,
                List.of(new AnswerOption(id + "-a", "A", true), new AnswerOption(id + "-b", "B", false),
                        new AnswerOption(id + "-c", "C", false)),
                "Explanation of " + id, List.of("https://example.com/" + id));
    }
}
