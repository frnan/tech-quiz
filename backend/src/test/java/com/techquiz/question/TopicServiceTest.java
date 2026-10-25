package com.techquiz.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock
    TopicRepository topics;
    @Mock
    QuestionRepository questions;
    @InjectMocks
    TopicService service;

    @Test
    void listsTopicsWithQuestionsSortedByNameIgnoringCase() {
        when(topics.findAll()).thenReturn(List.of(
                new Topic("spring-boot", "Spring Boot", "Boot"),
                new Topic("empty", "Empty", "No question yet"),
                new Topic("angular", "angular", "Front")));
        when(questions.countByTopic()).thenReturn(Map.of("spring-boot", 10L, "angular", 4L));

        assertThat(service.listTopics()).containsExactly(
                new TopicSummary("angular", "angular", "Front", 4),
                new TopicSummary("spring-boot", "Spring Boot", "Boot", 10));
    }
}
