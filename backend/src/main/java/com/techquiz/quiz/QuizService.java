package com.techquiz.quiz;

import com.techquiz.question.AnswerOption;
import com.techquiz.question.Question;
import com.techquiz.question.QuestionRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Service;

/** Builds quizzes and checks answers. Stateless: nothing about a quiz is stored (ADR 0004). */
@Service
public class QuizService {

    private final QuestionRepository questions;
    private final RandomGenerator random;

    public QuizService(QuestionRepository questions, RandomGenerator random) {
        this.questions = questions;
        this.random = random;
    }

    public Quiz createQuiz(QuizRequest request) {
        List<Question> picked;
        if (!request.questionIdsOrEmpty().isEmpty()) {
            picked = byIds(request.questionIdsOrEmpty());
        } else if (!request.topicIdsOrEmpty().isEmpty()) {
            picked = fromTopics(request.topicIdsOrEmpty(), request.sizeOrDefault());
        } else {
            picked = questions.sample(null, request.sizeOrDefault());
        }

        List<Question> shuffled = new ArrayList<>(picked);
        Collections.shuffle(shuffled, random);
        return new Quiz(shuffled.stream().map(this::toQuizQuestion).toList());
    }

    public AnswerResult checkAnswer(String questionId, String optionId) {
        Question question = questions.findById(questionId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        if (!question.hasOption(optionId)) {
            throw new InvalidQuizRequestException(
                    "Option '" + optionId + "' does not belong to question '" + questionId + "'");
        }
        AnswerOption correct = question.correctOption()
                .orElseThrow(() -> new IllegalStateException("Question '" + questionId + "' has no correct option"));
        return new AnswerResult(correct.id().equals(optionId), correct.id(), question.explanation(),
                question.references());
    }

    private List<Question> byIds(List<String> questionIds) {
        Set<String> wanted = new LinkedHashSet<>(questionIds);
        List<Question> found = questions.findAllById(wanted);
        if (found.size() != wanted.size()) {
            Set<String> missing = new LinkedHashSet<>(wanted);
            found.forEach(question -> missing.remove(question.id()));
            throw new InvalidQuizRequestException("Unknown question ids: " + String.join(", ", missing));
        }
        return found;
    }

    private List<Question> fromTopics(List<String> topicIds, int size) {
        Map<String, Long> counts = questions.countByTopic();
        Set<String> unknown = new LinkedHashSet<>(topicIds);
        unknown.removeAll(counts.keySet());
        if (!unknown.isEmpty()) {
            throw new InvalidQuizRequestException("Unknown topics: " + String.join(", ", unknown));
        }

        Map<String, Long> available = new HashMap<>();
        topicIds.forEach(topicId -> available.put(topicId, counts.get(topicId)));
        List<Question> picked = new ArrayList<>();
        QuestionAllocator.allocate(available, size, random)
                .forEach((topicId, count) -> picked.addAll(questions.sample(topicId, count)));
        return picked;
    }

    private QuizQuestion toQuizQuestion(Question question) {
        List<QuizQuestion.Option> options = new ArrayList<>(question.options().stream()
                .map(option -> new QuizQuestion.Option(option.id(), option.text()))
                .toList());
        Collections.shuffle(options, random);
        return new QuizQuestion(question.id(), question.topicId(), question.prompt(), question.code(), options);
    }
}
