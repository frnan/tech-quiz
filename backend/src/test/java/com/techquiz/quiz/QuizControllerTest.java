package com.techquiz.quiz;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.techquiz.question.CodeSnippet;
import com.techquiz.web.ApiExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuizController.class)
@Import(ApiExceptionHandler.class)
class QuizControllerTest {

    @Autowired
    MockMvc mvc;
    @MockitoBean
    QuizService quizService;

    @Test
    void createsAQuiz() throws Exception {
        when(quizService.createQuiz(new QuizRequest(List.of("java"), 5, null))).thenReturn(new Quiz(List.of(
                new QuizQuestion("java-001", "java", "What prints?", new CodeSnippet("java", "int x;"),
                        List.of(new QuizQuestion.Option("o1", "1"))))));

        mvc.perform(post("/api/v1/quizzes").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topicIds":["java"],"size":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"questions":[{"id":"java-001","topicId":"java","prompt":"What prints?",
                          "code":{"language":"java","source":"int x;"},"options":[{"id":"o1","text":"1"}]}]}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void rejectsAnOutOfRangeSizeAsAProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/quizzes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"size":51}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(quizService);
    }

    @Test
    void mapsAnInvalidRequestToA400() throws Exception {
        when(quizService.createQuiz(any())).thenThrow(new InvalidQuizRequestException("Unknown topics: cobol"));

        mvc.perform(post("/api/v1/quizzes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"topicIds":["cobol"]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown topics: cobol"));
    }

    @Test
    void checksAnAnswer() throws Exception {
        when(quizService.checkAnswer("java-001", "o2"))
                .thenReturn(new AnswerResult(false, "o1", "Because", List.of("https://example.com")));

        mvc.perform(post("/api/v1/questions/java-001/answer").contentType(MediaType.APPLICATION_JSON).content("""
                        {"optionId":"o2"}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"correct":false,"correctOptionId":"o1","explanation":"Because","references":["https://example.com"]}
                        """));
    }

    @Test
    void rejectsAnAnswerWithoutAnOption() throws Exception {
        mvc.perform(post("/api/v1/questions/java-001/answer").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(quizService);
    }

    @Test
    void mapsAnUnknownQuestionToA404() throws Exception {
        when(quizService.checkAnswer("nope-001", "o1")).thenThrow(new QuestionNotFoundException("nope-001"));

        mvc.perform(post("/api/v1/questions/nope-001/answer").contentType(MediaType.APPLICATION_JSON).content("""
                        {"optionId":"o1"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Question 'nope-001' does not exist"));
    }
}
