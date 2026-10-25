package com.techquiz.question;

import java.util.Collection;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuestionRepository extends MongoRepository<Question, String>, QuestionQueries {

    long deleteByIdNotIn(Collection<String> ids);
}
