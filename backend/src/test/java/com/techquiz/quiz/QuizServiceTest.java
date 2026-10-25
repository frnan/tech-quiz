package com.techquiz.quiz;

import static com.techquiz.testsupport.QuestionFixtures.question;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.techquiz.question.AnswerOption;
import com.techquiz.question.CodeSnippet;
import com.techquiz.question.Question;
import com.techquiz.question.QuestionRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    QuestionRepository questions;
    QuizService service;

    @BeforeEach
    void setUp() {
        service = new QuizService(questions, new Random(7));
    }

    @Test
    void quickMixSamplesAcrossAllTopicsWithTheDefaultSize() {
        when(questions.sample(null, QuizRequest.DEFAULT_SIZE))
                .thenReturn(List.of(question("java-001", "java"), question("docker-001", "docker")));

        Quiz quiz = service.createQuiz(new QuizRequest(null, null, null));

        assertThat(quiz.questions()).extracting(QuizQuestion::id).containsExactlyInAnyOrder("java-001", "docker-001");
    }

    @Test
    void quizQuestionsHideWhichOptionIsCorrectAndKeepTheRest() {
        Question withCode = new Question("java-001", "java", "What prints?", new CodeSnippet("java", "int x;"),
                List.of(new AnswerOption("o1", "1", true), new AnswerOption("o2", "2", false),
                        new AnswerOption("o3", "3", false)),
                "Because", List.of());
        when(questions.sample(null, 1)).thenReturn(List.of(withCode));

        QuizQuestion quizQuestion = service.createQuiz(new QuizRequest(List.of(), 1, List.of())).questions().getFirst();

        assertThat(quizQuestion.topicId()).isEqualTo("java");
        assertThat(quizQuestion.prompt()).isEqualTo("What prints?");
        assertThat(quizQuestion.code()).isEqualTo(new CodeSnippet("java", "int x;"));
        assertThat(quizQuestion.options()).containsExactlyInAnyOrder(
                new QuizQuestion.Option("o1", "1"), new QuizQuestion.Option("o2", "2"), new QuizQuestion.Option("o3", "3"));
        assertThat(QuizQuestion.Option.class.getRecordComponents()).extracting("name").containsExactly("id", "text");
    }

    @Test
    void shufflesOptionsSoTheCorrectOneIsNotAlwaysFirst() {
        Question q = question("java-001", "java");
        when(questions.sample(null, 1)).thenReturn(List.of(q));

        Set<String> firstOptions = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            firstOptions.add(service.createQuiz(new QuizRequest(null, 1, null)).questions().getFirst().options()
                    .getFirst().id());
        }

        assertThat(firstOptions).hasSizeGreaterThan(1);
    }

    @Test
    void customQuizSpreadsTheSizeAcrossTheSelectedTopics() {
        when(questions.countByTopic()).thenReturn(Map.of("java", 10L, "docker", 10L, "angular", 10L));
        when(questions.sample(eq("java"), anyInt())).thenReturn(List.of(question("java-001", "java")));
        when(questions.sample(eq("docker"), anyInt())).thenReturn(List.of(question("docker-001", "docker")));

        Quiz quiz = service.createQuiz(new QuizRequest(List.of("java", "docker"), 4, null));

        verify(questions).sample("java", 2);
        verify(questions).sample("docker", 2);
        assertThat(quiz.questions()).extracting(QuizQuestion::topicId).containsExactlyInAnyOrder("java", "docker");
    }

    @Test
    void customQuizRejectsUnknownTopics() {
        when(questions.countByTopic()).thenReturn(Map.of("java", 10L));

        assertThatThrownBy(() -> service.createQuiz(new QuizRequest(List.of("java", "cobol", "fortran"), 5, null)))
                .isInstanceOf(InvalidQuizRequestException.class)
                .hasMessage("Unknown topics: cobol, fortran");
    }

    @Test
    void retryQuizReturnsExactlyTheRequestedQuestions() {
        when(questions.findAllById(Set.of("java-001", "docker-001")))
                .thenReturn(List.of(question("java-001", "java"), question("docker-001", "docker")));

        Quiz quiz = service.createQuiz(new QuizRequest(List.of("ignored"), 1, List.of("java-001", "docker-001", "java-001")));

        assertThat(quiz.questions()).extracting(QuizQuestion::id).containsExactlyInAnyOrder("java-001", "docker-001");
    }

    @Test
    void retryQuizRejectsUnknownQuestionIds() {
        when(questions.findAllById(Set.of("java-001", "gone-001")))
                .thenReturn(List.of(question("java-001", "java")));

        assertThatThrownBy(() -> service.createQuiz(new QuizRequest(null, null, List.of("java-001", "gone-001"))))
                .isInstanceOf(InvalidQuizRequestException.class)
                .hasMessage("Unknown question ids: gone-001");
    }

    @Test
    void aCorrectAnswerIsReportedWithTheExplanation() {
        when(questions.findById("java-001")).thenReturn(Optional.of(question("java-001", "java")));

        AnswerResult result = service.checkAnswer("java-001", "java-001-a");

        assertThat(result).isEqualTo(new AnswerResult(true, "java-001-a", "Explanation of java-001",
                List.of("https://example.com/java-001")));
    }

    @Test
    void aWrongAnswerRevealsTheCorrectOption() {
        when(questions.findById("java-001")).thenReturn(Optional.of(question("java-001", "java")));

        AnswerResult result = service.checkAnswer("java-001", "java-001-c");

        assertThat(result.correct()).isFalse();
        assertThat(result.correctOptionId()).isEqualTo("java-001-a");
    }

    @Test
    void answeringAnUnknownQuestionFails() {
        when(questions.findById("nope-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkAnswer("nope-001", "x"))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessage("Question 'nope-001' does not exist");
    }

    @Test
    void answeringWithAnOptionOfAnotherQuestionFails() {
        when(questions.findById("java-001")).thenReturn(Optional.of(question("java-001", "java")));

        assertThatThrownBy(() -> service.checkAnswer("java-001", "java-002-a"))
                .isInstanceOf(InvalidQuizRequestException.class)
                .hasMessage("Option 'java-002-a' does not belong to question 'java-001'");
    }

    @Test
    void aStoredQuestionWithoutACorrectOptionIsAServerError() {
        Question broken = new Question("java-001", "java", "?", null,
                List.of(new AnswerOption("o1", "1", false)), "", List.of());
        when(questions.findById("java-001")).thenReturn(Optional.of(broken));

        assertThatThrownBy(() -> service.checkAnswer("java-001", "o1")).isInstanceOf(IllegalStateException.class);
    }
}
