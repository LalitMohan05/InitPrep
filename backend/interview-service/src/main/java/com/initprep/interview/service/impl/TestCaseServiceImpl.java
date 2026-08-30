package com.initprep.interview.service.impl;

import com.initprep.interview.dto.TestCaseRequest;
import com.initprep.interview.dto.TestCaseResponse;
import com.initprep.interview.entity.Question;
import com.initprep.interview.entity.TestCase;
import com.initprep.interview.exception.DuplicateResourceException;
import com.initprep.interview.exception.ResourceNotFoundException;
import com.initprep.interview.mapper.TestCaseMapper;
import com.initprep.interview.repository.QuestionRepo;
import com.initprep.interview.repository.TestCaseRepo;
import com.initprep.interview.service.interfaces.TestCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TestCaseServiceImpl implements TestCaseService {

    private final TestCaseRepo testCaseRepository;
    private final QuestionRepo questionRepository;
    private final TestCaseMapper testCaseMapper;

    @Override
    public TestCaseResponse createTestCase(
        UUID questionId,
        TestCaseRequest request) {

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Question not found " + questionId
                )
            );

        boolean exists =
            testCaseRepository
                .existsByQuestionIdAndInputAndExpectedOutput(
                    questionId,
                    request.getInput(),
                    request.getExpectedOutput()
                );

        if (exists) {
            throw new DuplicateResourceException(
                "This test case already exists for this question"
            );
        }

        TestCase testCase = testCaseMapper.toEntity(request);

        testCase.setQuestion(question);

        TestCase saved = testCaseRepository.save(testCase);

        return testCaseMapper.toResponse(saved);
    }

    @Override
    public TestCaseResponse updateTestCase(
        UUID questionId,
        UUID testCaseId,
        TestCaseRequest request) {

        TestCase testCase = testCaseRepository.findById(testCaseId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Test case not found " + testCaseId
                )
            );

        if (!testCase.getQuestion().getId().equals(questionId)) {
            throw new ResourceNotFoundException(
                "Test case does not belong to this question"
            );
        }
        boolean exists =
            testCaseRepository
                .existsByQuestionIdAndInputAndExpectedOutputAndIdNot(
                    questionId,
                    request.getInput(),
                    request.getExpectedOutput(),
                    testCaseId
                );

        if (exists) {
            throw new DuplicateResourceException(
                "This test case already exists for this question"
            );
        }

        testCaseMapper.updateEntity(request, testCase);

        return testCaseMapper.toResponse(testCase);
    }

    @Override
    public void deleteTestCase(
        UUID questionId,
        UUID testCaseId) {

        TestCase testCase = testCaseRepository.findById(testCaseId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Test case not found " + testCaseId
                )
            );

        if (!testCase.getQuestion().getId().equals(questionId)) {
            throw new ResourceNotFoundException(
                "Test case does not belong to this question"
            );
        }

        testCaseRepository.delete(testCase);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestCaseResponse> getTestCases(
        UUID questionId) {

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Question not found " + questionId
                )
            );

        return question.getTestCases()
            .stream()
            .map(testCaseMapper::toResponse)
            .toList();
    }
}
