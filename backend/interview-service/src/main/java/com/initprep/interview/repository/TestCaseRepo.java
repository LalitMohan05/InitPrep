package com.initprep.interview.repository;

import com.initprep.interview.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TestCaseRepo extends JpaRepository<TestCase, UUID> , JpaSpecificationExecutor<TestCase> {
    boolean existsByQuestionIdAndInputAndExpectedOutput(
        UUID questionId,
        String input,
        String expectedOutput
    );

    boolean existsByQuestionIdAndInputAndExpectedOutputAndIdNot(
        UUID questionId,
        String input,
        String expectedOutput,
        UUID id
    );
}
