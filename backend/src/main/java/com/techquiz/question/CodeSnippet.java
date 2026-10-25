package com.techquiz.question;

import jakarta.validation.constraints.NotBlank;

/** A block of code shown with the prompt; {@code language} drives syntax highlighting. */
public record CodeSnippet(@NotBlank String language, @NotBlank String source) {
}
