package com.techquiz.web;

import com.techquiz.quiz.InvalidQuizRequestException;
import com.techquiz.quiz.QuestionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** RFC 7807 errors. Spring's own exceptions (validation, bad JSON...) are mapped by the parent class. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(QuestionNotFoundException.class)
    ProblemDetail questionNotFound(QuestionNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidQuizRequestException.class)
    ProblemDetail invalidRequest(InvalidQuizRequestException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
