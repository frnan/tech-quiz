package com.techquiz.question;

import java.util.List;
import java.util.Map;

/** Queries Spring Data can't derive: random sampling and counts per topic. */
public interface QuestionQueries {

    /** Up to {@code size} random questions, from one topic or from all of them when {@code topicId} is null. */
    List<Question> sample(String topicId, int size);

    /** Number of questions per topic id. Topics without questions are absent. */
    Map<String, Long> countByTopic();
}
