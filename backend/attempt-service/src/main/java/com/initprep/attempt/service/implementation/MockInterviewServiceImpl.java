package com.initprep.attempt.service.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.initprep.attempt.client.AiServiceClient;
import com.initprep.attempt.client.InterviewServiceClient;
import com.initprep.attempt.dto.*;
import com.initprep.attempt.entity.Attempt;
import com.initprep.attempt.entity.MockInterview;
import com.initprep.attempt.entity.MockInterviewQuestion;
import com.initprep.attempt.enums.AttemptResult;
import com.initprep.attempt.enums.AttemptStatus;
import com.initprep.attempt.enums.AttemptType;
import com.initprep.attempt.enums.MockInterviewStatus;
import com.initprep.attempt.enums.MockInterviewDifficulty;
import com.initprep.attempt.enums.MockInterviewTargetRole;
import com.initprep.attempt.exception.ResourceForbiddenException;
import com.initprep.attempt.exception.ResourceNotFoundException;
import com.initprep.attempt.repository.AttemptRepo;
import com.initprep.attempt.repository.MockInterviewRepository;
import com.initprep.attempt.service.interfaces.AttemptService;
import com.initprep.attempt.service.interfaces.MockInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MockInterviewServiceImpl implements MockInterviewService {
    private static final List<AttemptType> MIX_ORDER = List.of(AttemptType.CODING, AttemptType.THEORY, AttemptType.MCQ);

    private final MockInterviewRepository sessionRepository;
    private final AttemptRepo attemptRepository;
    private final AttemptService attemptService;
    private final InterviewServiceClient interviewServiceClient;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    public MockInterviewSessionResponse start(UUID userId, MockInterviewStartRequest request) {
        String difficulty = request.getDifficulty().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("EASY", "MEDIUM", "HARD", "MIXED").contains(difficulty)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported interview difficulty");
        }
        if (request.getTargetRole().trim().length() > 80 || request.getTotalQuestions() == null || request.getTotalQuestions() < 1 || request.getTotalQuestions() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mock interview selection");
        }

        String targetRole = canonicalRole(request.getTargetRole().trim().toUpperCase(Locale.ROOT));
        MockInterviewTargetRole targetRoleEnum;
        try {
            targetRoleEnum = MockInterviewTargetRole.valueOf(targetRole);
        } catch (IllegalArgumentException invalidRole) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported mock interview target role");
        }
        Map<AttemptType, Integer> mix = expectedMix(request.getTotalQuestions());
        List<QuestionDetailsResponse> questions = new ArrayList<>();
        Set<UUID> selectedIds = new HashSet<>();
        for (AttemptType type : MIX_ORDER) {
            int wanted = mix.getOrDefault(type, 0);
            if (wanted == 0) continue;
            List<UUID> candidates = new ArrayList<>(interviewServiceClient.findQuestionIds(
                targetRole, "MIXED".equals(difficulty) ? null : difficulty, type.name()));
            Collections.shuffle(candidates);
            List<UUID> eligible = candidates.stream().filter(selectedIds::add).limit(wanted).toList();
            if (eligible.size() < wanted) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Not enough " + type.name().toLowerCase(Locale.ROOT) + " questions for the selected role and difficulty");
            }
            eligible.stream().map(interviewServiceClient::getQuestionDetails).forEach(questions::add);
        }
        Map<AttemptType, Integer> actualMix = new EnumMap<>(AttemptType.class);
        for (AttemptType type : MIX_ORDER) actualMix.put(type, 0);
        for (QuestionDetailsResponse question : questions) {
            AttemptType type = parseType(question.getType());
            actualMix.merge(type, 1, Integer::sum);
            if (question.getRoles() == null || !question.getRoles().contains(targetRole)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is not assigned to the selected target role");
            }
            if (!"MIXED".equals(difficulty) && !difficulty.equals(question.getDifficulty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question difficulty does not match the selected interview difficulty");
            }
        }
        if (!expectedMix(questions.size()).equals(actualMix)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question selection must use a 40% coding, 30% theory, 30% MCQ mix");
        }

        MockInterview session = MockInterview.builder()
            .userId(userId)
            .targetRole(targetRoleEnum)
            .difficulty(difficulty)
            .totalQuestions(questions.size())
            .status(MockInterviewStatus.IN_PROGRESS)
            .startedAt(LocalDateTime.now())
            .build();
        for (int index = 0; index < questions.size(); index++) {
            QuestionDetailsResponse selected = questions.get(index);
            session.getQuestions().add(MockInterviewQuestion.builder()
                .mockInterview(session)
                .questionId(selected.getId())
                .questionType(parseType(selected.getType()))
                .difficulty(MockInterviewDifficulty.valueOf(selected.getDifficulty()))
                .sequenceNumber(index + 1)
                .questionTitleSnapshot(selected.getTitle())
                .topicSnapshot(Optional.ofNullable(selected.getTopics()).orElse(Set.of()).stream().sorted().collect(Collectors.joining(", ")))
                .roleSnapshot(targetRole)
                .build());
        }
        return toResponse(sessionRepository.save(session), true);
    }

    @Override
    @Transactional(readOnly = true)
    public MockInterviewSessionResponse get(UUID userId, UUID sessionId) {
        return toResponse(getOwnedSession(userId, sessionId), true);
    }

    @Override
    public MockInterviewSessionResponse complete(UUID userId, UUID mockInterviewId) {
        MockInterview interview = getOwnedSession(userId, mockInterviewId);
        if (interview.getStatus() == MockInterviewStatus.ABANDONED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An abandoned interview cannot be completed");
        }
        if (interview.getStatus() == MockInterviewStatus.IN_PROGRESS) {
            List<Attempt> attempts = loadAttempts(interview);
            interview.setOverallScore(averageScore(attempts));
            interview.setCompletedAt(LocalDateTime.now());
            interview.setStatus(MockInterviewStatus.COMPLETED);
            sessionRepository.save(interview);
        }
        return toResponse(interview, true);
    }

    @Override
    @Transactional(readOnly = true)
    public MockInterviewReportResponse report(UUID userId, UUID mockInterviewId) {
        MockInterview interview = getOwnedSession(userId, mockInterviewId);
        MockInterviewSessionResponse summary = toResponse(interview, true);
        List<MockInterviewQuestion> questions = interview.getQuestions();
        Map<String, List<MockInterviewQuestion>> byTopic = new TreeMap<>();
        for (MockInterviewQuestion question : questions) {
            for (String topic : splitTopics(question.getTopicSnapshot())) {
                byTopic.computeIfAbsent(topic, ignored -> new ArrayList<>()).add(question);
            }
        }
        Map<String, List<MockInterviewQuestion>> byDifficulty = new TreeMap<>();
        for (MockInterviewQuestion question : questions) {
            String difficulty = question.getDifficulty() == null ? "UNKNOWN" : question.getDifficulty().name();
            byDifficulty.computeIfAbsent(difficulty, ignored -> new ArrayList<>()).add(question);
        }
        return MockInterviewReportResponse.builder()
            .id(summary.getId()).targetRole(summary.getTargetRole()).difficulty(summary.getDifficulty())
            .totalQuestions(summary.getTotalQuestions()).status(summary.getStatus()).overallScore(summary.getOverallScore())
            .startedAt(summary.getStartedAt()).completedAt(summary.getCompletedAt())
            .attemptedQuestions(summary.getAttemptedQuestions()).correctQuestions(summary.getCorrectQuestions())
            .codingScore(summary.getCodingScore()).mcqScore(summary.getMcqScore()).theoryScore(summary.getTheoryScore())
            .codingAccepted(summary.getCodingAccepted()).codingAttempted(summary.getCodingAttempted())
            .difficultyPerformance(byDifficulty.entrySet().stream().map(entry -> performance(entry.getKey(), entry.getValue())).toList())
            .topicPerformance(byTopic.entrySet().stream().map(entry -> performance(entry.getKey(), entry.getValue())).toList())
            .strengths(summary.getStrengths()).weaknesses(summary.getWeaknesses())
            .recommendedTopics(summary.getRecommendedTopics()).questions(summary.getQuestions()).build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MockInterviewSessionResponse> history(UUID userId, Pageable pageable) {
        return sessionRepository.findByUserId(userId, pageable)
            .map(session -> toResponse(session, false));
    }

    @Override
    public MockInterviewSessionResponse submitAnswer(UUID userId, UUID sessionId, MockInterviewAnswerRequest request) {
        MockInterview session = getOwnedSession(userId, sessionId);
        if (session.getStatus() != MockInterviewStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This interview is already complete");
        }
        MockInterviewQuestion next = session.getQuestions().stream()
            .filter(question -> question.getAttemptId() == null)
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "There are no unanswered questions"));
        if (!next.getQuestionId().equals(request.getQuestionId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Submit the current interview question before moving on");
        }

        QuestionDetailsResponse question = interviewServiceClient.getQuestionDetails(next.getQuestionId());
        AttemptType type = parseType(question.getType());
        AttemptResponse created;
        TheoryEvaluationResponse theoryEvaluation = null;
        boolean mcqCorrect = false;
        if (type == AttemptType.MCQ) {
            mcqCorrect = interviewServiceClient.checkMcqAnswer(question.getId(), request.getAnswer());
            created = createAttempt(question, request, type, userId);
        } else if (type == AttemptType.THEORY) {
            theoryEvaluation = aiServiceClient.evaluateTheory(TheoryEvaluationRequest.builder()
                .question(question.getTitle() + "\n\n" + question.getDescription())
                .answer(request.getAnswer())
                .targetRole(session.getTargetRole().name())
                .build());
            if (theoryEvaluation == null || theoryEvaluation.getScore() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI did not return a theory evaluation");
            }
            created = createAttempt(question, request, type, userId);
        } else {
            if (request.getLanguage() == null || request.getLanguage().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A programming language is required for coding questions");
            }
            created = createAttempt(question, request, type, userId);
        }

        Attempt attempt = attemptRepository.findById(created.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Attempt was not saved"));
        attempt.setMockInterviewId(session.getId());
        if (type == AttemptType.MCQ) {
            attempt.setStatus(AttemptStatus.COMPLETED);
            attempt.setResult(mcqCorrect ? AttemptResult.CORRECT : AttemptResult.WRONG);
            attempt.setScore(mcqCorrect ? 100.0 : 0.0);
        } else if (type == AttemptType.THEORY) {
            double score = Math.max(0, Math.min(100, theoryEvaluation.getScore()));
            attempt.setStatus(AttemptStatus.COMPLETED);
            attempt.setScore(score);
            attempt.setResult(score == 100 ? AttemptResult.CORRECT : score == 0 ? AttemptResult.WRONG : AttemptResult.PARTIALLY_CORRECT);
            try {
                attempt.setFeedback(objectMapper.writeValueAsString(theoryEvaluation));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Could not save the theory evaluation", exception);
            }
        }
        attemptRepository.save(attempt);
        next.setAttemptId(attempt.getId());

        List<Attempt> answers = session.getQuestions().stream()
            .map(MockInterviewQuestion::getAttemptId)
            .filter(Objects::nonNull)
            .map(id -> attemptRepository.findById(id).orElse(null))
            .filter(Objects::nonNull)
            .toList();
        if (answers.size() == session.getTotalQuestions()) {
            session.setStatus(MockInterviewStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            session.setOverallScore(averageScore(answers));
        }
        sessionRepository.save(session);
        return toResponse(session, true);
    }

    private AttemptResponse createAttempt(QuestionDetailsResponse question, MockInterviewAnswerRequest request, AttemptType type, UUID userId) {
        return attemptService.createAttempt(userId, CreateAttemptRequest.builder()
            .questionId(question.getId())
            .answer(request.getAnswer())
            .language(type == AttemptType.CODING ? request.getLanguage() : null)
            .type(type)
            .build());
    }

    private MockInterview getOwnedSession(UUID userId, UUID sessionId) {
        MockInterview session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Mock interview not found: " + sessionId));
        if (!session.getUserId().equals(userId)) throw new ResourceForbiddenException("Mock interview does not belong to this user");
        return session;
    }

    private MockInterviewSessionResponse toResponse(MockInterview session, boolean includeQuestions) {
        List<MockInterviewQuestionResponse> questionResponses = new ArrayList<>();
        if (includeQuestions) {
            for (MockInterviewQuestion item : session.getQuestions()) {
                AttemptResponse attempt = item.getAttemptId() == null ? null : attemptService.getAttempt(session.getUserId(), item.getAttemptId());
                TheoryEvaluationResponse theory = attempt == null ? null : readTheoryEvaluation(attempt.getFeedback());
                QuestionDetailsResponse question = getQuestionWithSnapshot(item);
                AttemptType questionType = item.getQuestionType() == null ? parseType(question.getType()) : item.getQuestionType();
                MockInterviewDifficulty questionDifficulty = item.getDifficulty() == null
                    ? safeDifficulty(question.getDifficulty()) : item.getDifficulty();
                questionResponses.add(MockInterviewQuestionResponse.builder()
                    .questionId(item.getQuestionId())
                    .questionType(questionType)
                    .difficulty(questionDifficulty)
                    .sequenceNumber(item.getSequenceNumber())
                    .questionTitleSnapshot(question.getTitle())
                    .topicSnapshot(item.getTopicSnapshot() == null ? String.join(", ", Optional.ofNullable(question.getTopics()).orElse(Set.of())) : item.getTopicSnapshot())
                    .roleSnapshot(Optional.ofNullable(item.getRoleSnapshot()).orElse(session.getTargetRole().name()))
                    .question(question)
                    .attempt(attempt)
                    .theoryEvaluation(theory)
                    .build());
            }
        }

        List<Attempt> attempts = loadAttempts(session);
        Double codingScore = averageScoreOfType(attempts, AttemptType.CODING);
        Double mcqScore = averageScoreOfType(attempts, AttemptType.MCQ);
        Double theoryScore = averageScoreOfType(attempts, AttemptType.THEORY);
        List<TheoryEvaluationResponse> evaluations = attempts.stream()
            .filter(attempt -> attempt.getType() == AttemptType.THEORY)
            .map(attempt -> readTheoryEvaluation(attempt.getFeedback()))
            .filter(Objects::nonNull)
            .toList();
        int acceptedCoding = (int) attempts.stream().filter(attempt -> attempt.getType() == AttemptType.CODING && attempt.getResult() == AttemptResult.ACCEPTED).count();
        int correct = (int) attempts.stream().filter(attempt -> attempt.getResult() == AttemptResult.CORRECT || attempt.getResult() == AttemptResult.ACCEPTED).count();

        return MockInterviewSessionResponse.builder()
            .id(session.getId())
            .targetRole(session.getTargetRole().name())
            .difficulty(session.getDifficulty())
            .totalQuestions(session.getTotalQuestions())
            .status(session.getStatus())
            .startedAt(session.getStartedAt())
            .completedAt(session.getCompletedAt())
            .overallScore(session.getOverallScore())
            .attemptedQuestions(attempts.size())
            .correctQuestions(correct)
            .codingScore(codingScore)
            .mcqScore(mcqScore)
            .theoryScore(theoryScore)
            .codingAccepted(acceptedCoding)
            .codingAttempted((int) attempts.stream().filter(attempt -> attempt.getType() == AttemptType.CODING).count())
            .strengths(distinctNonBlank(evaluations.stream().map(TheoryEvaluationResponse::getStrengths).toList()))
            .weaknesses(distinctNonBlank(evaluations.stream().map(TheoryEvaluationResponse::getWeaknesses).toList()))
            .recommendedTopics(distinctNonBlank(evaluations.stream().flatMap(evaluation -> Optional.ofNullable(evaluation.getRecommendedTopics()).orElse(List.of()).stream()).toList()))
            .questions(questionResponses)
            .build();
    }

    private List<Attempt> loadAttempts(MockInterview interview) {
        return interview.getQuestions().stream().map(MockInterviewQuestion::getAttemptId)
            .filter(Objects::nonNull).map(id -> attemptRepository.findById(id).orElse(null))
            .filter(Objects::nonNull).toList();
    }

    private QuestionDetailsResponse getQuestionWithSnapshot(MockInterviewQuestion item) {
        try {
            QuestionDetailsResponse question = interviewServiceClient.getQuestionDetails(item.getQuestionId());
            if (item.getQuestionTitleSnapshot() != null) question.setTitle(item.getQuestionTitleSnapshot());
            if (item.getQuestionType() != null) question.setType(item.getQuestionType().name());
            if (item.getDifficulty() != null) question.setDifficulty(item.getDifficulty().name());
            if (item.getTopicSnapshot() != null) question.setTopics(new LinkedHashSet<>(splitTopics(item.getTopicSnapshot())));
            if (item.getRoleSnapshot() != null) question.setRoles(Set.of(item.getRoleSnapshot()));
            return question;
        } catch (RuntimeException unavailable) {
            return QuestionDetailsResponse.builder().id(item.getQuestionId())
                .title(Optional.ofNullable(item.getQuestionTitleSnapshot()).orElse("Question " + item.getQuestionId()))
                .description("The original question is no longer available.")
                .type(Optional.ofNullable(item.getQuestionType()).orElse(AttemptType.THEORY).name())
                .difficulty(Optional.ofNullable(item.getDifficulty()).orElse(MockInterviewDifficulty.MIXED).name())
                .topics(new LinkedHashSet<>(splitTopics(item.getTopicSnapshot())))
                .roles(item.getRoleSnapshot() == null ? Set.of() : Set.of(item.getRoleSnapshot())).build();
        }
    }

    private MockInterviewPerformanceResponse performance(String category, List<MockInterviewQuestion> questions) {
        List<Attempt> attempts = questions.stream().map(MockInterviewQuestion::getAttemptId).filter(Objects::nonNull)
            .map(id -> attemptRepository.findById(id).orElse(null)).filter(Objects::nonNull).toList();
        long correct = attempts.stream().filter(attempt -> attempt.getResult() == AttemptResult.CORRECT || attempt.getResult() == AttemptResult.ACCEPTED).count();
        return MockInterviewPerformanceResponse.builder().category(category).totalQuestions(questions.size())
            .attemptedQuestions(attempts.size()).correctQuestions(Math.toIntExact(correct))
            .score(attempts.stream().map(Attempt::getScore).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().stream().boxed().findFirst().orElse(null))
            .build();
    }

    private static List<String> splitTopics(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) return List.of("Uncategorized");
        return Arrays.stream(snapshot.split(",\\s*")).filter(value -> !value.isBlank()).distinct().toList();
    }

    private TheoryEvaluationResponse readTheoryEvaluation(String feedback) {
        if (feedback == null || feedback.isBlank()) return null;
        try { return objectMapper.readValue(feedback, TheoryEvaluationResponse.class); }
        catch (JsonProcessingException ignored) { return null; }
    }

    private static AttemptType parseType(String type) {
        try { return AttemptType.valueOf(type); }
        catch (Exception exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported question type in interview"); }
    }

    private static MockInterviewDifficulty safeDifficulty(String difficulty) {
        try { return MockInterviewDifficulty.valueOf(difficulty); }
        catch (Exception ignored) { return MockInterviewDifficulty.MIXED; }
    }

    private static String canonicalRole(String role) {
        return "FULLSTACK_DEVELOPER".equals(role) ? "FULL_STACK_DEVELOPER" : role;
    }

    private static Map<AttemptType, Integer> expectedMix(int size) {
        Map<AttemptType, Integer> counts = new EnumMap<>(AttemptType.class);
        Map<AttemptType, Double> remainders = new EnumMap<>(AttemptType.class);
        int assigned = 0;
        double[] proportions = {0.4, 0.3, 0.3};
        for (int index = 0; index < MIX_ORDER.size(); index++) {
            double exact = size * proportions[index];
            int base = (int) Math.floor(exact);
            counts.put(MIX_ORDER.get(index), base);
            remainders.put(MIX_ORDER.get(index), exact - base);
            assigned += base;
        }
        List<AttemptType> remainderOrder = new ArrayList<>(MIX_ORDER);
        remainderOrder.sort(Comparator.comparingDouble((AttemptType type) -> remainders.get(type)).reversed());
        for (int index = 0; index < size - assigned; index++) counts.merge(remainderOrder.get(index), 1, Integer::sum);
        return counts;
    }

    private static Double averageScore(List<Attempt> attempts) {
        return attempts.stream().map(Attempt::getScore).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().stream().boxed().findFirst().orElse(null);
    }

    private static Double averageScoreOfType(List<Attempt> attempts, AttemptType type) {
        return attempts.stream().filter(attempt -> attempt.getType() == type).map(Attempt::getScore).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().stream().boxed().findFirst().orElse(null);
    }

    private static List<String> distinctNonBlank(List<String> values) {
        return values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).distinct().toList();
    }
}
