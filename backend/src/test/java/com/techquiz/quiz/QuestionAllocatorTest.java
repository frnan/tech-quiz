package com.techquiz.quiz;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class QuestionAllocatorTest {

    @Test
    void splitsEvenlyWhenTopicsHaveEnoughQuestions() {
        Map<String, Integer> allocation = QuestionAllocator.allocate(Map.of("a", 10L, "b", 10L), 10, new Random(1));

        assertThat(allocation).containsOnly(Map.entry("a", 5), Map.entry("b", 5));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2, 3, 4, 5, 42})
    void givesTheRemainderToRandomTopicsOneEach(long seed) {
        Map<String, Integer> allocation = QuestionAllocator.allocate(Map.of("a", 10L, "b", 10L, "c", 10L), 10,
                new Random(seed));

        assertThat(allocation.values()).containsExactlyInAnyOrder(4, 3, 3);
    }

    @Test
    void topicsWithSpareQuestionsMakeUpForSmallOnes() {
        Map<String, Integer> allocation = QuestionAllocator.allocate(Map.of("small", 2L, "big", 20L), 10, new Random(1));

        assertThat(allocation).containsOnly(Map.entry("small", 2), Map.entry("big", 8));
    }

    @Test
    void returnsEverythingAvailableWhenAskedForMore() {
        Map<String, Integer> allocation = QuestionAllocator.allocate(Map.of("a", 2L, "b", 3L), 30, new Random(1));

        assertThat(allocation).containsOnly(Map.entry("a", 2), Map.entry("b", 3));
    }

    @Test
    void leavesOutTopicsThatGetNothing() {
        Map<String, Integer> allocation = QuestionAllocator.allocate(Map.of("a", 5L, "b", 5L, "c", 5L), 1, new Random(1));

        assertThat(allocation).hasSize(1).containsValue(1);
    }
}
