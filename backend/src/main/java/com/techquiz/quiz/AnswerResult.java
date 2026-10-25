package com.techquiz.quiz;

import java.util.List;

public record AnswerResult(boolean correct, String correctOptionId, String explanation, List<String> references) {
}
