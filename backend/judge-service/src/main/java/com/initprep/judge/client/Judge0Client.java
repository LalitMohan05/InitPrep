package com.initprep.judge.client;

import com.initprep.judge.dto.JudgeSubmissionRequest;
import com.initprep.judge.dto.JudgeSubmissionResponse;
import com.initprep.judge.dto.TestCaseRequest;
import com.initprep.judge.dto.TestCaseResult;
import com.initprep.judge.dto.judge0.*;
import com.initprep.judge.enums.JudgeStatus;
import com.initprep.judge.enums.ProgrammingLanguage;
import com.initprep.judge.exception.Judge0Exception;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
public class Judge0Client {
    private static final int MAX_BATCH_SIZE = 20;
    private static final long POLL_INTERVAL_MILLIS = 100;
    private static final Duration POLL_TIMEOUT = Duration.ofMinutes(3);

    private final RestClient restClient;

    @Value("${judge0.url}")
    private String judge0Url;

    public JudgeSubmissionResponse execute(JudgeSubmissionRequest request) {
        try {
            List<TestCaseRequest> testCases = request.getTestCases();
            if (testCases == null || testCases.isEmpty()) {
                throw new Judge0Exception("At least one test case is required");
            }

            List<Batch> batches = submitInBatches(request, testCases);
            List<List<Judge0SubmissionResponse>> batchResults = pollBatchesConcurrently(batches);

            List<CaseResult> orderedResults = new ArrayList<>(testCases.size());
            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                Batch batch = batches.get(batchIndex);
                List<Judge0SubmissionResponse> responses = batchResults.get(batchIndex);
                for (int index = 0; index < batch.testCases().size(); index++) {
                    orderedResults.add(new CaseResult(batch.testCases().get(index), responses.get(index)));
                }
            }
            return combineResults(orderedResults);
        } catch (Judge0Exception e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Judge0Exception("Interrupted while waiting for Judge0", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Judge0Exception judge0Exception) throw judge0Exception;
            throw new Judge0Exception("Failed to retrieve Judge0 batch results", cause);
        } catch (Exception e) {
            throw new Judge0Exception("Failed to communicate with Judge0", e);
        }
    }

    private List<Batch> submitInBatches(JudgeSubmissionRequest request, List<TestCaseRequest> testCases) {
        List<Batch> batches = new ArrayList<>();
        for (int start = 0; start < testCases.size(); start += MAX_BATCH_SIZE) {
            List<TestCaseRequest> cases = List.copyOf(
                testCases.subList(start, Math.min(start + MAX_BATCH_SIZE, testCases.size()))
            );
            List<Judge0SubmissionRequest> submissions = cases.stream()
                .map(testCase -> Judge0SubmissionRequest.builder()
                    .sourceCode(request.getSourceCode())
                    .languageId(getLanguageId(request.getLanguage()))
                    .stdin(testCase.getInput())
                    .expectedOutput(testCase.getExpectedOutput())
                    .cpuTimeLimit(request.getTimeLimit() != null ? request.getTimeLimit() / 1000.0 : 2.0)
                    .memoryLimit(request.getMemoryLimit() != null ? request.getMemoryLimit() : 128000.0)
                    .build())
                .toList();

            List<Judge0SubmissionToken> tokens = submitBatch(submissions);
            if (tokens == null || tokens.size() != cases.size()) {
                throw new Judge0Exception("Judge0 returned an invalid batch response");
            }
            List<String> tokenValues = tokens.stream().map(Judge0SubmissionToken::getToken).toList();
            for (int index = 0; index < tokenValues.size(); index++) {
                if (tokenValues.get(index) == null || tokenValues.get(index).isBlank()) {
                    throw new Judge0Exception("Judge0 did not return a token for test case " + (start + index + 1));
                }
            }
            batches.add(new Batch(cases, tokenValues));
        }
        return batches;
    }

    private List<List<Judge0SubmissionResponse>> pollBatchesConcurrently(List<Batch> batches)
        throws InterruptedException, ExecutionException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<List<Judge0SubmissionResponse>>> tasks = batches.stream()
                .map(batch -> (Callable<List<Judge0SubmissionResponse>>) () -> waitForBatchResults(batch.tokens()))
                .toList();
            List<Future<List<Judge0SubmissionResponse>>> futures = executor.invokeAll(tasks);
            List<List<Judge0SubmissionResponse>> results = new ArrayList<>(futures.size());
            for (Future<List<Judge0SubmissionResponse>> future : futures) results.add(future.get());
            return results;
        }
    }

    private List<Judge0SubmissionResponse> waitForBatchResults(List<String> tokens) throws InterruptedException {
        long deadline = System.nanoTime() + POLL_TIMEOUT.toNanos();
        Map<String, Judge0SubmissionResponse> completed = new HashMap<>();
        List<String> pending = new ArrayList<>(tokens);

        while (!pending.isEmpty()) {
            if (System.nanoTime() >= deadline) {
                throw new Judge0Exception("Timed out while waiting for Judge0 batch results");
            }
            List<Judge0SubmissionResponse> responses = getBatchResults(pending);
            if (responses == null) throw new Judge0Exception("Judge0 returned an empty batch result response");

            for (Judge0SubmissionResponse response : responses) {
                if (response == null || response.getToken() == null || !pending.contains(response.getToken())) {
                    throw new Judge0Exception("Judge0 returned an invalid batch result");
                }
                Judge0Status status = response.getStatus();
                if (status == null) throw new Judge0Exception("Judge0 returned a result without a status");
                if (!isPending(status)) completed.put(response.getToken(), response);
            }

            pending.removeIf(completed::containsKey);
            if (!pending.isEmpty()) Thread.sleep(POLL_INTERVAL_MILLIS);
        }

        List<Judge0SubmissionResponse> ordered = new ArrayList<>(tokens.size());
        for (String token : tokens) {
            Judge0SubmissionResponse response = completed.get(token);
            if (response == null) throw new Judge0Exception("Judge0 did not return a result for token " + token);
            ordered.add(response);
        }
        return ordered;
    }

    private List<Judge0SubmissionToken> submitBatch(List<Judge0SubmissionRequest> submissions) {
        if (submissions.isEmpty() || submissions.size() > MAX_BATCH_SIZE) {
            throw new Judge0Exception("Judge0 batches must contain between 1 and 20 submissions");
        }
        try {
            Judge0BatchSubmissionRequest request = Judge0BatchSubmissionRequest.builder()
                .submissions(submissions)
                .build();
            return restClient.post()
                .uri(judge0Url + "/submissions/batch?base64_encoded=false")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Judge0SubmissionToken>>() { });
        } catch (Exception e) {
            throw new Judge0Exception("Judge0 batch submission failed", e);
        }
    }

    private List<Judge0SubmissionResponse> getBatchResults(List<String> tokens) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(judge0Url)
                .path("/submissions/batch")
                .queryParam("tokens", String.join(",", tokens))
                .queryParam("base64_encoded", "false")
                .build()
                .encode()
                .toUri();
            Judge0BatchSubmissionResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(Judge0BatchSubmissionResponse.class);
            return response == null ? null : response.getSubmissions();
        } catch (Exception e) {
            throw new Judge0Exception("Judge0 batch result retrieval failed", e);
        }
    }

    private JudgeSubmissionResponse combineResults(List<CaseResult> caseResults) {
        List<TestCaseResult> results = new ArrayList<>(caseResults.size());
        TestCaseResult failedTestCase = null;
        JudgeStatus fatalStatus = null;
        Judge0SubmissionResponse fatalResponse = null;
        String compilerOutput = null;
        String runtimeOutput = null;
        long totalExecutionTime = 0;
        long maxMemoryUsed = 0;
        int passed = 0;

        for (CaseResult caseResult : caseResults) {
            TestCaseRequest testCase = caseResult.testCase();
            Judge0SubmissionResponse response = caseResult.response();
            JudgeStatus status = getJudgeStatus(response.getStatus());
            boolean accepted = status == JudgeStatus.ACCEPTED;
            if (accepted) passed++;

            TestCaseResult result = TestCaseResult.builder()
                .passed(accepted)
                .input(testCase.getInput())
                .expectedOutput(testCase.getExpectedOutput())
                .actualOutput(response.getStdout())
                .hidden(testCase.isHidden())
                .build();
            results.add(result);
            if (!accepted && failedTestCase == null) failedTestCase = result;

            if (status != JudgeStatus.ACCEPTED && status != JudgeStatus.WRONG_ANSWER && fatalStatus == null) {
                fatalStatus = status;
                fatalResponse = response;
            }
            if (status == JudgeStatus.COMPILATION_ERROR && isBlank(compilerOutput)) {
                compilerOutput = firstNonBlank(response.getCompileOutput(), response.getMessage());
            }
            if (status != JudgeStatus.ACCEPTED && status != JudgeStatus.WRONG_ANSWER
                && status != JudgeStatus.COMPILATION_ERROR && isBlank(runtimeOutput)) {
                runtimeOutput = firstNonBlank(response.getStderr(), response.getMessage());
            }
            if (response.getTime() != null) totalExecutionTime += (long) (response.getTime() * 1000);
            if (response.getMemory() != null) maxMemoryUsed = Math.max(maxMemoryUsed, response.getMemory().longValue());
        }

        JudgeStatus finalStatus = fatalStatus != null
            ? fatalStatus
            : passed == caseResults.size() ? JudgeStatus.ACCEPTED : JudgeStatus.WRONG_ANSWER;
        if (fatalResponse != null && fatalStatus == JudgeStatus.COMPILATION_ERROR && isBlank(compilerOutput)) {
            compilerOutput = firstNonBlank(fatalResponse.getCompileOutput(), fatalResponse.getMessage());
        }
        if (fatalResponse != null && fatalStatus != JudgeStatus.COMPILATION_ERROR && isBlank(runtimeOutput)) {
            runtimeOutput = firstNonBlank(fatalResponse.getStderr(), fatalResponse.getMessage());
        }

        return JudgeSubmissionResponse.builder()
            .status(finalStatus)
            .passedTestCases(passed)
            .totalTestCases(caseResults.size())
            .executionTime(totalExecutionTime)
            .memoryUsed(maxMemoryUsed)
            .compilerOutput(compilerOutput)
            .runtimeOutput(runtimeOutput)
            .failedTestCase(failedTestCase)
            .testCaseResults(results)
            .build();
    }

    private boolean isPending(Judge0Status status) {
        Integer id = status.getId();
        String description = status.getDescription();
        return id != null && (id == 1 || id == 2)
            || description != null && (description.equalsIgnoreCase("In Queue") || description.equalsIgnoreCase("Processing"));
    }

    private JudgeStatus getJudgeStatus(Judge0Status status) {
        if (status == null) throw new Judge0Exception("Judge0 returned a result without a status");
        String description = status.getDescription() == null ? "" : status.getDescription().toLowerCase(Locale.ROOT);
        if (description.contains("memory limit exceeded")) return JudgeStatus.MEMORY_LIMIT_EXCEEDED;
        if (description.contains("time limit exceeded")) return JudgeStatus.TIME_LIMIT_EXCEEDED;
        if (description.contains("compilation error")) return JudgeStatus.COMPILATION_ERROR;
        if (description.contains("wrong answer")) return JudgeStatus.WRONG_ANSWER;
        if (description.contains("runtime error") || description.contains("exec format error")) return JudgeStatus.RUNTIME_ERROR;
        if (description.contains("internal error") || description.contains("system error")) return JudgeStatus.INTERNAL_ERROR;

        Integer id = status.getId();
        if (id == null) return JudgeStatus.INTERNAL_ERROR;
        return switch (id) {
            case 3 -> JudgeStatus.ACCEPTED;
            case 4 -> JudgeStatus.WRONG_ANSWER;
            case 5 -> JudgeStatus.TIME_LIMIT_EXCEEDED;
            case 6 -> JudgeStatus.COMPILATION_ERROR;
            case 7, 9, 10, 11, 12, 13, 14, 15, 16 -> JudgeStatus.RUNTIME_ERROR;
            default -> JudgeStatus.INTERNAL_ERROR;
        };
    }

    private int getLanguageId(ProgrammingLanguage language) {
        return switch (language) {
            case JAVA -> 62;
            case PYTHON -> 71;
            case CPP -> 54;
            case C -> 50;
            case JAVASCRIPT -> 63;
        };
    }

    private static boolean isBlank(String value) { return value == null || value.isBlank(); }
    private static String firstNonBlank(String first, String second) { return !isBlank(first) ? first : second; }
    private record Batch(List<TestCaseRequest> testCases, List<String> tokens) { }
    private record CaseResult(TestCaseRequest testCase, Judge0SubmissionResponse response) { }
}
