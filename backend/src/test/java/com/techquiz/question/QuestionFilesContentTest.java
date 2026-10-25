package com.techquiz.question;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Loads the real {@code questions/} directory (path set by the POM), so a broken contribution fails
 * CI instead of the app's startup.
 */
class QuestionFilesContentTest {

    /** Languages the frontend highlights (frontend/src/app/quiz/code-block/highlight.ts) and in questions/schema.json. Keep in sync. */
    private static final Set<String> HIGHLIGHTED_LANGUAGES = Set.of(
            "java", "typescript", "javascript", "html", "sql", "yaml", "dockerfile", "bash", "json", "xml", "plaintext");

    private static ValidatorFactory validators;
    private static QuestionCatalog catalog;

    @BeforeAll
    static void load() {
        validators = Validation.buildDefaultValidatorFactory();
        Path dir = Path.of(Objects.requireNonNull(System.getProperty("questions.dir"), "questions.dir is set by the POM"));
        catalog = new QuestionFileLoader(validators.getValidator()).load(dir);
    }

    @AfterAll
    static void close() {
        validators.close();
    }

    @Test
    void everyTopicHasAtLeastFiveQuestions() {
        Map<String, Long> perTopic = catalog.questions().stream()
                .collect(Collectors.groupingBy(Question::topicId, Collectors.counting()));

        assertThat(catalog.topics()).isNotEmpty().allSatisfy(topic ->
                assertThat(perTopic.getOrDefault(topic.id(), 0L)).as(topic.id()).isGreaterThanOrEqualTo(5));
    }

    @Test
    void codeSnippetsUseAHighlightedLanguage() {
        assertThat(catalog.questions())
                .filteredOn(question -> question.code() != null)
                .allSatisfy(question ->
                        assertThat(HIGHLIGHTED_LANGUAGES).as(question.id()).contains(question.code().language()));
    }

    @Test
    void optionsOfAQuestionAreDistinct() {
        assertThat(catalog.questions()).allSatisfy(question ->
                assertThat(question.options()).as(question.id()).extracting(AnswerOption::text).doesNotHaveDuplicates());
    }
}
