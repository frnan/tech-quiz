package com.techquiz.question;

import java.util.List;

/** Everything read from the question files, ready to be stored. */
public record QuestionCatalog(List<Topic> topics, List<Question> questions) {
}
