package com.techquiz.question;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;

/** Fragment implementation picked up by Spring Data through the {@code Impl} suffix. */
class QuestionQueriesImpl implements QuestionQueries {

    private final MongoTemplate mongo;

    QuestionQueriesImpl(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<Question> sample(String topicId, int size) {
        List<AggregationOperation> stages = new ArrayList<>();
        if (topicId != null) {
            stages.add(match(Criteria.where("topicId").is(topicId)));
        }
        stages.add(Aggregation.sample(size));
        return mongo.aggregate(newAggregation(Question.class, stages), Question.class).getMappedResults();
    }

    @Override
    public Map<String, Long> countByTopic() {
        return mongo.aggregate(newAggregation(Question.class, group("topicId").count().as("count")), Document.class)
                .getMappedResults().stream()
                .collect(Collectors.toMap(doc -> doc.getString("_id"), doc -> doc.get("count", Number.class).longValue()));
    }
}
