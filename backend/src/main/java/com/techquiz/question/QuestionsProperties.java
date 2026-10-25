package com.techquiz.question;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param path        directory holding the question YAML files
 * @param syncOnStartup whether to load the files into MongoDB when the app starts
 */
@ConfigurationProperties("quiz.questions")
public record QuestionsProperties(@DefaultValue("../questions") Path path, @DefaultValue("true") boolean syncOnStartup) {
}
