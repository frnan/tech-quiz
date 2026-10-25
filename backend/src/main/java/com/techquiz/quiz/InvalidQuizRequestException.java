package com.techquiz.quiz;

/** The request is well-formed but can't be served, e.g. an unknown topic or option id. */
public class InvalidQuizRequestException extends RuntimeException {

    public InvalidQuizRequestException(String message) {
        super(message);
    }
}
