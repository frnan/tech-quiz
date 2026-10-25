package com.techquiz.quiz;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Three ways to build a quiz:
 * <ul>
 *   <li>Quick mix: no topics, no question ids. {@code size} random questions from every topic.</li>
 *   <li>Custom: {@code topicIds}. {@code size} questions spread evenly across those topics.</li>
 *   <li>Retry: {@code questionIds}. Exactly those questions (e.g. the ones answered wrong).</li>
 * </ul>
 */
public record QuizRequest(
        @Size(max = 50) List<@NotBlank String> topicIds,
        @Min(1) @Max(QuizRequest.MAX_SIZE) Integer size,
        @Size(max = QuizRequest.MAX_SIZE) List<@NotBlank String> questionIds) {

    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 50;

    public int sizeOrDefault() {
        return size == null ? DEFAULT_SIZE : size;
    }

    public List<String> topicIdsOrEmpty() {
        return topicIds == null ? List.of() : topicIds;
    }

    public List<String> questionIdsOrEmpty() {
        return questionIds == null ? List.of() : questionIds;
    }
}
