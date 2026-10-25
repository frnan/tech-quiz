package com.techquiz.question;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

/**
 * Makes MongoDB mirror the question files (ADR 0002): upserts every topic and question by id, then
 * deletes what the files no longer contain. Runs at startup; an invalid file stops the app.
 */
@Service
public class QuestionSyncService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QuestionSyncService.class);

    private final QuestionFileLoader loader;
    private final TopicRepository topics;
    private final QuestionRepository questions;
    private final QuestionsProperties properties;

    public QuestionSyncService(QuestionFileLoader loader, TopicRepository topics, QuestionRepository questions,
            QuestionsProperties properties) {
        this.loader = loader;
        this.topics = topics;
        this.questions = questions;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.syncOnStartup()) {
            sync();
        } else {
            log.info("Question sync on startup is disabled");
        }
    }

    public SyncResult sync() {
        QuestionCatalog catalog = loader.load(properties.path());

        topics.saveAll(catalog.topics());
        questions.saveAll(catalog.questions());
        long deletedQuestions = questions.deleteByIdNotIn(catalog.questions().stream().map(Question::id).toList());
        long deletedTopics = topics.deleteByIdNotIn(catalog.topics().stream().map(Topic::id).toList());

        SyncResult result = new SyncResult(catalog.topics().size(), catalog.questions().size(), deletedTopics,
                deletedQuestions);
        log.info("Synced questions from {}: {}", properties.path().toAbsolutePath(), result);
        return result;
    }

    public record SyncResult(int topics, int questions, long deletedTopics, long deletedQuestions) {
    }
}
