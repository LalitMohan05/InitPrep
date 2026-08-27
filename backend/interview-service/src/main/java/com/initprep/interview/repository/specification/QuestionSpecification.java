package com.initprep.interview.repository.specification;

import com.initprep.interview.entity.Company;
import com.initprep.interview.entity.Question;
import com.initprep.interview.entity.Topic;
import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

public class QuestionSpecification {

    public static Specification<Question> hasDifficulty(
        Difficulty difficulty) {

        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(
                root.get("difficulty"),
                difficulty
            );
    }

    public static Specification<Question> hasType(
        QuestionType type) {

        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(
                root.get("type"),
                type
            );
    }

    public static Specification<Question> hasCompany(
        String companyName) {

        return (root, query, criteriaBuilder) -> {

            Join<Question, Company> company =
                root.join("companies");

            return criteriaBuilder.equal(
                criteriaBuilder.lower(company.get("name")),
                companyName.toLowerCase()
            );
        };
    }

    public static Specification<Question> hasTopic(
        String topicName) {

        return (root, query, criteriaBuilder) -> {

            Join<Question, Topic> topic =
                root.join("topics");

            return criteriaBuilder.equal(
                criteriaBuilder.lower(topic.get("name")),
                topicName.toLowerCase()
            );
        };
    }
}
