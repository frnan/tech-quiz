package com.techquiz.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QuestionFileLoaderTest {

    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private final QuestionFileLoader loader = new QuestionFileLoader(VALIDATORS.getValidator());

    @TempDir
    Path dir;

    @AfterAll
    static void closeValidators() {
        VALIDATORS.close();
    }

    @Test
    void loadsTopicsAndQuestionsWithOpaqueOptionIds() throws IOException {
        write("java.yaml", """
                topic: { id: java, name: Java, description: The language }
                questions:
                  - id: java-001
                    prompt: What does this print?
                    code:
                      language: java
                      source: |
                        System.out.println(1 + 1);
                    options:
                      - text: "2"
                        correct: true
                      - text: "11"
                      - text: Nothing
                    explanation: Integer addition.
                    references: [https://docs.oracle.com/javase/tutorial/]
                """);
        write("docker.yaml", topicWithQuestion("docker", "docker-001"));

        QuestionCatalog catalog = loader.load(dir);

        assertThat(catalog.topics()).extracting(Topic::id).containsExactly("docker", "java");
        Question question = catalog.questions().stream().filter(q -> q.id().equals("java-001")).findFirst().orElseThrow();
        assertThat(question.topicId()).isEqualTo("java");
        assertThat(question.code()).isEqualTo(new CodeSnippet("java", "System.out.println(1 + 1);\n"));
        assertThat(question.options()).extracting(AnswerOption::text).containsExactly("2", "11", "Nothing");
        assertThat(question.correctOption()).map(AnswerOption::text).hasValue("2");
        assertThat(question.options()).extracting(AnswerOption::id)
                .containsExactly(QuestionFileLoader.optionId("java-001", 0), QuestionFileLoader.optionId("java-001", 1),
                        QuestionFileLoader.optionId("java-001", 2))
                .allMatch(id -> id.matches("[0-9a-f]{12}"))
                .doesNotHaveDuplicates();
        assertThat(question.references()).containsExactly("https://docs.oracle.com/javase/tutorial/");
    }

    @Test
    void optionIdsAreStable() {
        assertThat(QuestionFileLoader.optionId("java-001", 2)).isEqualTo(QuestionFileLoader.optionId("java-001", 2))
                .isNotEqualTo(QuestionFileLoader.optionId("java-002", 2));
    }

    @Test
    void missingReferencesBecomeAnEmptyList() throws IOException {
        write("docker.yaml", topicWithQuestion("docker", "docker-001"));

        assertThat(loader.load(dir).questions().getFirst().references()).isEmpty();
    }

    @Test
    void acceptsYmlExtensionAndIgnoresOtherFiles() throws IOException {
        write("docker.yml", topicWithQuestion("docker", "docker-001"));
        write("README.md", "not a question file");

        assertThat(loader.load(dir).topics()).extracting(Topic::id).containsExactly("docker");
    }

    @Test
    void rejectsAMissingDirectory() {
        assertThatThrownBy(() -> loader.load(dir.resolve("nope")))
                .isInstanceOf(QuestionFileException.class)
                .hasMessageContaining("Question directory not found");
    }

    @Test
    void rejectsADirectoryWithoutYamlFiles() {
        assertThatThrownBy(() -> loader.load(dir))
                .isInstanceOf(QuestionFileException.class)
                .hasMessageContaining("No question files");
    }

    @Test
    void rejectsMalformedYaml() throws IOException {
        write("docker.yaml", "topic: [unclosed");

        assertThatThrownBy(() -> loader.load(dir))
                .isInstanceOf(QuestionFileException.class)
                .hasMessageStartingWith("docker.yaml: not a valid question file");
    }

    @Test
    void rejectsUnknownFields() throws IOException {
        write("docker.yaml", topicWithQuestion("docker", "docker-001").replace("explanation:", "difficulty: hard\n    explanation:"));

        assertThatThrownBy(() -> loader.load(dir))
                .isInstanceOf(QuestionFileException.class)
                .hasMessageContaining("difficulty");
    }

    @Test
    void rejectsAnEmptyFile() throws IOException {
        write("docker.yaml", "");

        assertThatThrownBy(() -> loader.load(dir))
                .isInstanceOf(QuestionFileException.class)
                .hasMessage("docker.yaml: file is empty");
    }

    @Test
    void reportsEveryConstraintViolation() throws IOException {
        write("docker.yaml", """
                topic: { id: Docker Stuff, name: "", description: Containers }
                questions:
                  - id: docker-001
                    prompt: ""
                    options:
                      - text: A
                        correct: true
                      - text: B
                    explanation: Because.
                    references: [not-a-url]
                """);

        assertThatThrownBy(() -> loader.load(dir))
                .isInstanceOf(QuestionFileException.class)
                .hasMessageContaining("topic.id must be kebab-case")
                .hasMessageContaining("topic.name must not be blank")
                .hasMessageContaining("questions[0].prompt must not be blank")
                .hasMessageContaining("questions[0].options size must be between 3 and 5")
                .hasMessageContaining("must be an http(s) URL");
    }

    @Test
    void rejectsAFileNameThatDoesNotMatchTheTopic() throws IOException {
        write("containers.yaml", topicWithQuestion("docker", "docker-001"));

        assertThatThrownBy(() -> loader.load(dir))
                .isInstanceOf(QuestionFileException.class)
                .hasMessage("containers.yaml: file name must match the topic id (docker.yaml)");
    }

    @Test
    void rejectsAQuestionIdWithoutTheTopicPrefix() throws IOException {
        write("docker.yaml", topicWithQuestion("docker", "java-001"));

        assertThatThrownBy(() -> loader.load(dir))
                .isInstanceOf(QuestionFileException.class)
                .hasMessageContaining("question id 'java-001' must look like docker-001");
    }

    @Test
    void rejectsZeroOrSeveralCorrectOptions() throws IOException {
        write("docker.yaml", topicWithQuestion("docker", "docker-001").replace("- text: B", "- text: B\n        correct: true"));

        assertThatThrownBy(() -> loader.load(dir))
                .isInstanceOf(QuestionFileException.class)
                .hasMessageContaining("must have exactly one correct option, found 2");

        write("docker.yaml", topicWithQuestion("docker", "docker-001").replace("correct: true", "correct: false"));

        assertThatThrownBy(() -> loader.load(dir))
                .isInstanceOf(QuestionFileException.class)
                .hasMessageContaining("found 0");
    }

    @Test
    void rejectsDuplicateQuestionIds() throws IOException {
        String twice = topicWithQuestion("docker", "docker-001") + """
                  - id: docker-001
                    prompt: Again?
                    options:
                      - text: A
                        correct: true
                      - text: B
                      - text: C
                    explanation: Duplicate.
                """;
        write("docker.yaml", twice);

        assertThatThrownBy(() -> loader.load(dir))
                .isInstanceOf(QuestionFileException.class)
                .hasMessage("docker.yaml: question id 'docker-001' is already used in docker.yaml");
    }

    private void write(String fileName, String content) throws IOException {
        Files.writeString(dir.resolve(fileName), content);
    }

    private static String topicWithQuestion(String topicId, String questionId) {
        return """
                topic: { id: %s, name: %s, description: A topic }
                questions:
                  - id: %s
                    prompt: Which one?
                    options:
                      - text: A
                        correct: true
                      - text: B
                      - text: C
                    explanation: Because.
                """.formatted(topicId, topicId, questionId);
    }
}
