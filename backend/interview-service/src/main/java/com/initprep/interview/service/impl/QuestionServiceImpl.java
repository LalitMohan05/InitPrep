package com.initprep.interview.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.initprep.interview.dto.*;
import com.initprep.interview.entity.Company;
import com.initprep.interview.entity.Question;
import com.initprep.interview.entity.QuestionRole;
import com.initprep.interview.entity.TestCase;
import com.initprep.interview.entity.Topic;
import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import com.initprep.interview.enums.TargetRole;
import com.initprep.interview.exception.ResourceNotFoundException;
import com.initprep.interview.mapper.QuestionMapper;
import com.initprep.interview.mapper.TestCaseMapper;
import com.initprep.interview.repository.CompanyRepo;
import com.initprep.interview.repository.QuestionRepo;
import com.initprep.interview.repository.QuestionRoleRepo;
import com.initprep.interview.repository.TopicRepo;
import com.initprep.interview.repository.specification.QuestionSpecification;
import com.initprep.interview.service.interfaces.QuestionService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionServiceImpl implements QuestionService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final QuestionRepo questionRepository;
    private final CompanyRepo companyRepository;
    private final TopicRepo topicRepository;
    private final QuestionRoleRepo questionRoleRepository;
    private final QuestionMapper questionMapper;
    private final TestCaseMapper testCaseMapper;

    @Override
    public QuestionResponse createQuestion(CreateQuestionRequest request) {

        Question question = questionMapper.toEntity(request);
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            question.setRoles(resolveRoles(request.getRoles()));
        }
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

        if (request.getRoles() != null) {
            question.setRoles(resolveRoles(request.getRoles()));
        }

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
        TargetRole role,
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

        if (role != null) {
            specification.add(QuestionSpecification.hasRole(role));
        }

        Specification<Question> specifications =
            Specification.allOf(specification);

        Page<Question> questions =
            questionRepository.findAll(specifications, pageable);

        return questions.map(questionMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean questionExists(UUID questionId) {
        return questionRepository.existsById(questionId);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionJudgeResponse getJudgeData(UUID questionId) {

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Question not found: " + questionId
                )
            );

        return QuestionJudgeResponse.builder()
            .questionId(question.getId())
            .testCases(
                question.getTestCases()
                    .stream()
                    .map(testCaseMapper::toResponse)
                    .toList()
            )
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionJudgeResponse getPublicTestCases(UUID questionId) {
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        return QuestionJudgeResponse.builder()
            .questionId(question.getId())
            .testCases(question.getTestCases().stream()
                .filter(testCase -> !testCase.isHidden())
                .map(testCaseMapper::toResponse)
                .toList())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDetailsResponse getQuestionDetails(UUID questionId) {

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Question not found: " + questionId
                )
            );

        return QuestionDetailsResponse.builder()
            .id(question.getId())
            .title(question.getTitle())
            .description(question.getDescription())
            .difficulty(question.getDifficulty())
            .type(question.getType())
            .constraints(question.getConstraints())
            .examples(question.getExamples())
            .hints(question.getHints())
            .starterCode(question.getStarterCode())
            .expectedComplexity(question.getExpectedComplexity())
            .options(question.getOptions())
            .roles(question.getRoles().stream().map(QuestionRole::getCode).collect(Collectors.toSet()))
            .topics(question.getTopics().stream().map(Topic::getName).collect(Collectors.toSet()))
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkMcqAnswer(UUID questionId, String answer) {
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        if (question.getType() != QuestionType.MCQ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer checking is available for MCQ questions only");
        }
        String correctAnswer = question.getCorrectAnswer();
        return correctAnswer != null && answer != null
            && canonicalAnswer(question.getOptions(), correctAnswer)
                .equals(canonicalAnswer(question.getOptions(), answer));
    }

    private static String canonicalAnswer(String options, String answer) {
        String normalized = answer.trim().toLowerCase(Locale.ROOT);
        if (options == null) return normalized;
        if (options.stripLeading().startsWith("[")) {
            try {
                JsonNode parsed = OBJECT_MAPPER.readTree(options);
                if (parsed.isArray()) {
                    for (int index = 0; index < parsed.size(); index++) {
                        JsonNode option = parsed.get(index);
                        String key = String.valueOf((char) ('a' + index));
                        String value = option.isObject()
                            ? firstText(option, "key", "id", "value")
                            : option.asText();
                        String label = option.isObject()
                            ? firstText(option, "label", "text", "value")
                            : option.asText();
                        if (matches(normalized, key, value, label)) return key;
                    }
                }
            } catch (Exception ignored) {
                // Fall through to the multiline format used by the question form.
            }
        }
        for (String line : options.split("\\R")) {
            String option = line.trim();
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^([a-z0-9])\\s*[).:\\-]\\s*(.*)$", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(option);
            if (matcher.matches()) {
                String key = matcher.group(1).toLowerCase(Locale.ROOT);
                String label = matcher.group(2).trim().toLowerCase(Locale.ROOT);
                if (normalized.equals(key) || normalized.equals(option.toLowerCase(Locale.ROOT)) || normalized.equals(label)) {
                    return key;
                }
            } else if (normalized.equals(option.toLowerCase(Locale.ROOT))) {
                return normalized;
            }
        }
        return normalized;
    }

    private static String firstText(JsonNode value, String... fields) {
        for (String field : fields) {
            JsonNode candidate = value.get(field);
            if (candidate != null && candidate.isValueNode() && !candidate.asText().isBlank()) return candidate.asText().trim().toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private static boolean matches(String answer, String... aliases) {
        for (String alias : aliases) {
            if (alias != null && answer.equals(alias.trim().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private Set<QuestionRole> resolveRoles(Set<TargetRole> codes) {
        if (codes == null || codes.isEmpty()) throw new IllegalArgumentException("At least one target role is required");
        Map<TargetRole, QuestionRole> roles = questionRoleRepository.findByCodeIn(codes).stream()
            .collect(Collectors.toMap(QuestionRole::getCode, role -> role));
        List<QuestionRole> missing = codes.stream().filter(code -> !roles.containsKey(code))
            .map(code -> QuestionRole.builder().code(code).build()).toList();
        questionRoleRepository.saveAll(missing).forEach(role -> roles.put(role.getCode(), role));
        return new HashSet<>(roles.values());
    }

}
