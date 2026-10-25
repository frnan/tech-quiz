package com.techquiz.question;

/** A question file can't be read or breaks a rule. The message names the file and the problem. */
public class QuestionFileException extends RuntimeException {

    public QuestionFileException(String message) {
        super(message);
    }

    public QuestionFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
