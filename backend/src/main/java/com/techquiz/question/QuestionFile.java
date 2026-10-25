package com.techquiz.question;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** The shape of one {@code questions/<topic>.yaml} file. Documented for editors in {@code questions/schema.json}. */
record QuestionFile(@NotNull @Valid TopicDefinition topic, @NotEmpty List<@NotNull @Valid QuestionDefinition> questions) {

    record TopicDefinition(
            @NotBlank @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*", message = "must be kebab-case") String id,
            @NotBlank String name,
            @NotBlank String description) {
    }

    record QuestionDefinition(
            @NotBlank String id,
            @NotBlank String prompt,
            @Valid CodeSnippet code,
            @NotNull @Size(min = 3, max = 5) List<@NotNull @Valid OptionDefinition> options,
            @NotBlank String explanation,
            List<@NotBlank @Pattern(regexp = "https?://\\S+", message = "must be an http(s) URL") String> references) {
    }

    record OptionDefinition(@NotBlank String text, boolean correct) {
    }
}
