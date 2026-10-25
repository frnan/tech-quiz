package com.techquiz.quiz;

public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException(String questionId) {
        super("Question '" + questionId + "' does not exist");
    }
}
