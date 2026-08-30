package com.initprep.interview.service.impl;

import com.initprep.interview.dto.CreateQuestionRequest;
import com.initprep.interview.dto.QuestionResponse;
import com.initprep.interview.dto.QuestionSummaryResponse;
import com.initprep.interview.dto.UpdateQuestionRequest;
import com.initprep.interview.entity.Company;
import com.initprep.interview.entity.Question;
import com.initprep.interview.entity.TestCase;
import com.initprep.interview.entity.Topic;
import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import com.initprep.interview.exception.ResourceNotFoundException;
import com.initprep.interview.mapper.QuestionMapper;
import com.initprep.interview.mapper.TestCaseMapper;
import com.initprep.interview.repository.CompanyRepo;
import com.initprep.interview.repository.QuestionRepo;
import com.initprep.interview.repository.TopicRepo;
import com.initprep.interview.repository.specification.QuestionSpecification;
import com.initprep.interview.service.interfaces.QuestionService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionServiceImpl implements QuestionService {
    private final QuestionRepo questionRepository;
    private final CompanyRepo companyRepository;
    private final TopicRepo topicRepository;
    private final QuestionMapper questionMapper;
    private final TestCaseMapper testCaseMapper;

    @Override
    public QuestionResponse createQuestion(CreateQuestionRequest request) {

        Question question = questionMapper.toEntity(request);
        if (request.getTestCases() != null &&
            !request.getTestCases().isEmpty()) {

            List<TestCase> testCases = request.getTestCases()
                .stream()
                .map(testCaseRequest -> {
                    TestCase testCase =
                        testCaseMapper.toEntity(testCaseRequest);

                    testCase.setQuestion(question);

                    return testCase;
                })
                .toList();

            question.setTestCases(testCases);
        }

        if (request.getCompanyIds() != null && !request.getCompanyIds().isEmpty()) {

            List<Company> companies =
                companyRepository.findAllById(request.getCompanyIds());

            if (companies.size() != request.getCompanyIds().size()) {
                throw new ResourceNotFoundException("One or more companies not found");
            }

            question.setCompanies(new HashSet<>(companies));
        }

        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {

            List<Topic> topics =
                topicRepository.findAllById(request.getTopicIds());

            if (topics.size() != request.getTopicIds().size()) {
                throw new ResourceNotFoundException("One or more topics not found");
            }

            question.setTopics(new HashSet<>(topics));
        }

        Question savedQuestion = questionRepository.save(question);

        return questionMapper.toResponse(savedQuestion);
    }

    @Override
    public QuestionResponse updateQuestion(UUID questionId, UpdateQuestionRequest request) {
        Question question=questionRepository.findById(questionId)
            .orElseThrow(()->new ResourceNotFoundException("Question not found " + questionId));

        questionMapper.updateEntity(request,question);

        if (request.getCompanyIds() != null) {

            Set<Company> companies =
                new HashSet<>(
                    companyRepository.findAllById(request.getCompanyIds())
                );

            question.setCompanies(companies);
        }

        if (request.getTopicIds() != null) {

            Set<Topic> topics =
                new HashSet<>(
                    topicRepository.findAllById(request.getTopicIds())
                );

            question.setTopics(topics);
        }

        Question updated = questionRepository.save(question);

        return questionMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void DeleteQuestion(UUID questionId) {
        Question question=questionRepository.findById(questionId)
            .orElseThrow(()->new ResourceNotFoundException("Question not found " + questionId));
        questionRepository.delete(question);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionSummaryResponse> getQuestions(
        Difficulty difficulty,
        QuestionType type,
        String companyName,
        String topicName,
        Pageable pageable) {

        List<Specification<Question>> specification = new ArrayList<>();


        if (difficulty != null) {
            specification.add(
                QuestionSpecification.hasDifficulty(difficulty)
            );
        }

        if (type != null) {
            specification.add(
                QuestionSpecification.hasType(type)
            );
        }

        if (companyName != null && !companyName.isBlank()) {
            specification.add(
                QuestionSpecification.hasCompany(companyName)
            );
        }

        if (topicName != null && !topicName.isBlank()) {
            specification.add(
                QuestionSpecification.hasTopic(topicName)
            );
        }

        Specification<Question> specifications =
            Specification.allOf(specification);

        Page<Question> questions =
            questionRepository.findAll(specifications, pageable);

        return questions.map(questionMapper::toSummaryResponse);
    }

}
