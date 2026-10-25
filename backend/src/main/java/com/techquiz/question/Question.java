package com.techquiz.question;

import java.util.List;
import java.util.Optional;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("questions")
public record Question(
        @Id String id,
        @Indexed String topicId,
        String prompt,
        CodeSnippet code,
        List<AnswerOption> options,
        String explanation,
        List<String> references) {

    public Optional<AnswerOption> correctOption() {
        return options.stream().filter(AnswerOption::correct).findFirst();
    }

    public boolean hasOption(String optionId) {
        return options.stream().anyMatch(option -> option.id().equals(optionId));
    }
}
