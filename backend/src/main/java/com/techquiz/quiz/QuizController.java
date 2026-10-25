package com.techquiz.quiz;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping("/quizzes")
    public Quiz createQuiz(@Valid @RequestBody QuizRequest request) {
        return quizService.createQuiz(request);
    }

    @PostMapping("/questions/{questionId}/answer")
    public AnswerResult answer(@PathVariable String questionId, @Valid @RequestBody AnswerRequest request) {
        return quizService.checkAnswer(questionId, request.optionId());
    }
}
