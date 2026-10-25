package com.techquiz.question;

import com.techquiz.question.QuestionFile.OptionDefinition;
import com.techquiz.question.QuestionFile.QuestionDefinition;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** Reads and validates every {@code *.yaml}/{@code *.yml} file of a directory (ADR 0002). */
@Component
public class QuestionFileLoader {

    private final YAMLMapper yaml = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // An option without `correct:` is a wrong one.
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();
    private final Validator validator;

    public QuestionFileLoader(Validator validator) {
        this.validator = validator;
    }

    public QuestionCatalog load(Path directory) {
        if (!Files.isDirectory(directory)) {
            throw new QuestionFileException("Question directory not found: " + directory.toAbsolutePath());
        }
        List<Path> files = listYamlFiles(directory);
        if (files.isEmpty()) {
            throw new QuestionFileException("No question files (*.yaml) in " + directory.toAbsolutePath());
        }

        List<Topic> topics = new ArrayList<>();
        List<Question> questions = new ArrayList<>();
        Map<String, Path> seenQuestionIds = new HashMap<>();
        for (Path file : files) {
            QuestionFile content = read(file);
            topics.add(new Topic(content.topic().id(), content.topic().name(), content.topic().description()));
            for (QuestionDefinition definition : content.questions()) {
                Path previous = seenQuestionIds.putIfAbsent(definition.id(), file);
                if (previous != null) {
                    throw error(file, "question id '" + definition.id() + "' is already used in " + previous.getFileName());
                }
                questions.add(toQuestion(content.topic().id(), definition));
            }
        }
        return new QuestionCatalog(List.copyOf(topics), List.copyOf(questions));
    }

    /** Opaque, stable id of the option at {@code index} of a question. */
    static String optionId(String questionId, int index) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((questionId + ":" + index).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }

    private List<Path> listYamlFiles(Path directory) {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries
                    .filter(path -> path.toString().endsWith(".yaml") || path.toString().endsWith(".yml"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new QuestionFileException("Can't list " + directory.toAbsolutePath(), e);
        }
    }

    private QuestionFile read(Path file) {
        String text;
        try {
            text = Files.readString(file);
        } catch (IOException e) {
            throw new QuestionFileException("Can't read " + file.toAbsolutePath(), e);
        }
        if (text.isBlank()) {
            throw error(file, "file is empty");
        }
        QuestionFile content;
        try {
            content = yaml.readValue(text, QuestionFile.class);
        } catch (JacksonException e) {
            throw new QuestionFileException(file.getFileName() + ": not a valid question file: " + e.getMessage(), e);
        }
        validate(file, content);
        return content;
    }

    private void validate(Path file, QuestionFile content) {
        Set<ConstraintViolation<QuestionFile>> violations = validator.validate(content);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw error(file, details);
        }

        String topicId = content.topic().id();
        String baseName = file.getFileName().toString().replaceFirst("\\.ya?ml$", "");
        if (!baseName.equals(topicId)) {
            throw error(file, "file name must match the topic id (" + topicId + ".yaml)");
        }
        for (QuestionDefinition question : content.questions()) {
            if (!question.id().matches(java.util.regex.Pattern.quote(topicId) + "-\\d{3}")) {
                throw error(file, "question id '" + question.id() + "' must look like " + topicId + "-001");
            }
            long correct = question.options().stream().filter(OptionDefinition::correct).count();
            if (correct != 1) {
                throw error(file, "question '" + question.id() + "' must have exactly one correct option, found " + correct);
            }
        }
    }

    private static Question toQuestion(String topicId, QuestionDefinition definition) {
        List<AnswerOption> options = IntStream.range(0, definition.options().size())
                .mapToObj(index -> new AnswerOption(
                        optionId(definition.id(), index),
                        definition.options().get(index).text(),
                        definition.options().get(index).correct()))
                .toList();
        List<String> references = definition.references() == null ? List.of() : List.copyOf(definition.references());
        return new Question(definition.id(), topicId, definition.prompt(), definition.code(), options,
                definition.explanation(), references);
    }

    private static QuestionFileException error(Path file, String message) {
        return new QuestionFileException(file.getFileName() + ": " + message);
    }
}
