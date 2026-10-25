package com.techquiz.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/** Splits a quiz size across topics as evenly as their question counts allow. */
final class QuestionAllocator {

    private QuestionAllocator() {
    }

    /**
     * Round-robin over the topics in a random order, one question at a time, skipping a topic once
     * it has no more questions. So with 10 questions over 3 topics it's 4/3/3, and the topic that
     * gets the 4th is random. The total is {@code size}, or every available question if fewer.
     *
     * @param available questions available per topic id
     * @return how many questions to draw per topic id (only topics with at least one)
     */
    static Map<String, Integer> allocate(Map<String, Long> available, int size, RandomGenerator random) {
        List<String> order = new ArrayList<>(available.keySet());
        Collections.sort(order);
        Collections.shuffle(order, random);

        Map<String, Integer> allocation = new LinkedHashMap<>();
        int remaining = size;
        boolean progress = true;
        while (remaining > 0 && progress) {
            progress = false;
            for (String topicId : order) {
                if (remaining == 0) {
                    break;
                }
                int taken = allocation.getOrDefault(topicId, 0);
                if (taken < available.get(topicId)) {
                    allocation.put(topicId, taken + 1);
                    remaining--;
                    progress = true;
                }
            }
        }
        return allocation;
    }
}
