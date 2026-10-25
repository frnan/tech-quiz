package com.techquiz.question;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TopicService {

    private final TopicRepository topics;
    private final QuestionRepository questions;

    public TopicService(TopicRepository topics, QuestionRepository questions) {
        this.topics = topics;
        this.questions = questions;
    }

    /** Every topic that has at least one question, sorted by name. */
    public List<TopicSummary> listTopics() {
        Map<String, Long> counts = questions.countByTopic();
        return topics.findAll().stream()
                .filter(topic -> counts.getOrDefault(topic.id(), 0L) > 0)
                .map(topic -> new TopicSummary(topic.id(), topic.name(), topic.description(), counts.get(topic.id())))
                .sorted(Comparator.comparing(TopicSummary::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
