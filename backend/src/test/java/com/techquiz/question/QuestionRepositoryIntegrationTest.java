package com.techquiz.question;

import static com.techquiz.testsupport.QuestionFixtures.question;
import static org.assertj.core.api.Assertions.assertThat;

import com.techquiz.testsupport.AbstractMongoIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;

@DataMongoTest
class QuestionRepositoryIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    QuestionRepository questions;

    @BeforeEach
    void seed() {
        questions.deleteAll();
        questions.saveAll(List.of(question("java-001", "java"), question("java-002", "java"),
                question("java-003", "java"), question("docker-001", "docker")));
    }

    @Test
    void samplesFromOneTopic() {
        List<Question> sample = questions.sample("java", 2);

        assertThat(sample).hasSize(2).extracting(Question::topicId).containsOnly("java");
        assertThat(sample).extracting(Question::id).doesNotHaveDuplicates();
    }

    @Test
    void samplesFromAllTopicsAndNeverMoreThanAvailable() {
        assertThat(questions.sample(null, 10)).hasSize(4);
    }

    @Test
    void sampledQuestionsKeepTheirOptions() {
        assertThat(questions.sample("docker", 1).getFirst().correctOption()).isPresent();
    }

    @Test
    void countsQuestionsPerTopic() {
        assertThat(questions.countByTopic()).containsOnly(
                Map.entry("java", 3L), Map.entry("docker", 1L));
    }

    @Test
    void deletesQuestionsNotInTheGivenIds() {
        long deleted = questions.deleteByIdNotIn(List.of("java-001", "docker-001"));

        assertThat(deleted).isEqualTo(2);
        assertThat(questions.findAll()).extracting(Question::id).containsExactlyInAnyOrder("java-001", "docker-001");
    }
}
