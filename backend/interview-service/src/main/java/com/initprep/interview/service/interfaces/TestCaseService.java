package com.initprep.interview.service.interfaces;

import com.initprep.interview.dto.TestCaseRequest;
import com.initprep.interview.dto.TestCaseResponse;

import java.util.List;
import java.util.UUID;

public interface TestCaseService {

    TestCaseResponse createTestCase(
        UUID questionId,
        TestCaseRequest request
    );

    TestCaseResponse updateTestCase(
        UUID questionId,
        UUID testCaseId,
        TestCaseRequest request
    );

    void deleteTestCase(
        UUID questionId,
        UUID testCaseId
    );

    List<TestCaseResponse> getTestCases(
        UUID questionId
    );
}
