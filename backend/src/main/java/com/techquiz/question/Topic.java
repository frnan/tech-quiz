package com.techquiz.question;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("topics")
public record Topic(@Id String id, String name, String description) {
}
