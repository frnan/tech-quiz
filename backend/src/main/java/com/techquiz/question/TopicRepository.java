package com.techquiz.question;

import java.util.Collection;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TopicRepository extends MongoRepository<Topic, String> {

    long deleteByIdNotIn(Collection<String> ids);
}
