package com.techquiz.quiz;

import jakarta.validation.constraints.NotBlank;

public record AnswerRequest(@NotBlank String optionId) {
}
