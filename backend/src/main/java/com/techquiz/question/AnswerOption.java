package com.techquiz.question;

/**
 * One option of a stored question. The id is opaque (derived from the question id and the option's
 * position in the file, see {@link QuestionFileLoader#optionId}) so it gives nothing away about
 * which option is correct.
 */
public record AnswerOption(String id, String text, boolean correct) {
}
