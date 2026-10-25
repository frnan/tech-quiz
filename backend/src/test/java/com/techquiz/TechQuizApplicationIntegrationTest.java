package com.techquiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.techquiz.question.Question;
import com.techquiz.question.QuestionRepository;
import com.techquiz.question.QuestionSyncService;
import com.techquiz.question.Topic;
import com.techquiz.question.TopicRepository;
import com.techquiz.testsupport.AbstractMongoIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** The whole app against a real MongoDB, synced from {@code src/test/resources/questions} at startup. */
@SpringBootTest(properties = "quiz.questions.path=src/test/resources/questions")
@AutoConfigureMockMvc
class TechQuizApplicationIntegrationTest extends AbstractMongoIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    QuestionRepository questions;
    @Autowired
    TopicRepository topics;
    @Autowired
    QuestionSyncService sync;

    @Test
    void startupSyncLoadedTheFilesAndListsTopics() throws Exception {
        mvc.perform(get("/api/v1/topics"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"id":"docker","name":"Docker","questionCount":1},{"id":"java","name":"Java","questionCount":2}]
                        """));
    }

    @Test
    void syncRemovesWhatTheFilesNoLongerContain() {
        questions.save(new Question("old-001", "old", "?", null, List.of(), "", List.of()));
        topics.save(new Topic("old", "Old", "Removed topic"));

        QuestionSyncService.SyncResult result = sync.sync();

        assertThat(result).isEqualTo(new QuestionSyncService.SyncResult(2, 3, 1, 1));
        assertThat(questions.existsById("old-001")).isFalse();
        assertThat(topics.existsById("old")).isFalse();
    }

    @Test
    void playsAQuestionEndToEndWithoutLeakingTheAnswer() throws Exception {
        String quiz = mvc.perform(post("/api/v1/quizzes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicIds":["java"],"size":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[*].options[*].correct").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String questionId = JsonPath.read(quiz, "$.questions[0].id");
        List<String> optionIds = JsonPath.read(quiz, "$.questions[0].options[*].id");
        String correct = JsonPath.read(mvc.perform(post("/api/v1/questions/{id}/answer", questionId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"optionId\":\"" + optionIds.getFirst() + "\"}"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(), "$.correctOptionId");

        mvc.perform(post("/api/v1/questions/{id}/answer", questionId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"optionId\":\"" + correct + "\"}"))
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.explanation").isNotEmpty());
    }

    @Test
    void quickMixDrawsFromEveryTopic() throws Exception {
        mvc.perform(post("/api/v1/quizzes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(3));
    }

    @Test
    void servesTheSpaForClientRoutesButNotForUnknownApiPaths() throws Exception {
        mvc.perform(get("/quiz")).andExpect(status().isOk())
                .andExpect(content().string(containsString("tech-quiz test index")));
        mvc.perform(get("/results/anything")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/nope")).andExpect(status().isNotFound());
        mvc.perform(get("/missing.js")).andExpect(status().isNotFound());
    }

    @Test
    void exposesHealthProbes() throws Exception {
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }
}
