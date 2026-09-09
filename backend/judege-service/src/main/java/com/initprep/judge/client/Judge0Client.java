package com.initprep.judge.client;

import com.initprep.judge.dto.JudgeSubmissionRequest;
import com.initprep.judge.dto.JudgeSubmissionResponse;
import com.initprep.judge.dto.TestCaseRequest;
import com.initprep.judge.dto.TestCaseResult;
import com.initprep.judge.enums.JudgeStatus;
import com.initprep.judge.enums.ProgrammingLanguage;
import com.initprep.judge.exception.Judge0Exception;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.initprep.judge.dto.judge0.Judge0SubmissionRequest;
import com.initprep.judge.dto.judge0.Judge0SubmissionResponse;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Judge0Client {

    private final RestClient restClient;

    @Value("${judge0.url}")
    private String judge0Url;
    public JudgeSubmissionResponse execute(
        JudgeSubmissionRequest request
    ) {

        try {

            List<TestCaseResult> results = new ArrayList<>();

            long totalExecutionTime = 0;
            long maxMemoryUsed = 0;
            int passed = 0;

            for (TestCaseRequest testCase : request.getTestCases()) {

                Judge0SubmissionRequest judge0Request =
                    Judge0SubmissionRequest.builder()
                        .sourceCode(request.getSourceCode())
                        .languageId(getLanguageId(request.getLanguage()))
                        .stdin(testCase.getInput())
                        .expectedOutput(testCase.getExpectedOutput())
                        .cpuTimeLimit(
                            request.getTimeLimit() != null
                                ? request.getTimeLimit() / 1000.0
                                : 2.0
                        )
                        .memoryLimit(
                            request.getMemoryLimit() != null
                                ? request.getMemoryLimit()
                                : 128000.0
                        )
                        .build();

                Judge0SubmissionResponse submission =
                    restClient
                        .post()
                        .uri(
                            judge0Url +
                                "/submissions?base64_encoded=false&wait=false"
                        )
                        .body(judge0Request)
                        .retrieve()
                        .body(Judge0SubmissionResponse.class);

                if (submission == null || submission.getToken() == null) {
                    throw new Judge0Exception(
                        "Judge0 did not return a submission token"
                    );
                }

                Judge0SubmissionResponse result =
                    waitForResult(submission.getToken());

                JudgeStatus status = getJudgeStatus(
                    result.getStatus().getId()
                );

                boolean isPassed = status == JudgeStatus.ACCEPTED;

                if (isPassed) {
                    passed++;
                }

                if (result.getTime() != null) {
                    totalExecutionTime +=
                        (long) (result.getTime() * 1000);
                }

                if (result.getMemory() != null) {
                    maxMemoryUsed = Math.max(
                        maxMemoryUsed,
                        result.getMemory().longValue()
                    );
                }

                results.add(
                    TestCaseResult.builder()
                        .passed(isPassed)
                        .input(testCase.getInput())
                        .expectedOutput(testCase.getExpectedOutput())
                        .actualOutput(result.getStdout())
                        .build()
                );

                // Stop if compilation/runtime error
                if (status != JudgeStatus.ACCEPTED &&
                    status != JudgeStatus.WRONG_ANSWER) {

                    return JudgeSubmissionResponse.builder()
                        .status(status)
                        .passedTestCases(passed)
                        .totalTestCases(request.getTestCases().size())
                        .executionTime(totalExecutionTime)
                        .memoryUsed(maxMemoryUsed)
                        .compilerOutput(result.getCompileOutput())
                        .runtimeOutput(result.getStderr())
                        .testCaseResults(results)
                        .build();
                }
            }

            JudgeStatus finalStatus =
                passed == request.getTestCases().size()
                    ? JudgeStatus.ACCEPTED
                    : JudgeStatus.WRONG_ANSWER;

            return JudgeSubmissionResponse.builder()
                .status(finalStatus)
                .passedTestCases(passed)
                .totalTestCases(request.getTestCases().size())
                .executionTime(totalExecutionTime)
                .memoryUsed(maxMemoryUsed)
                .testCaseResults(results)
                .build();

        } catch (Judge0Exception e) {

            throw e;

        } catch (Exception e) {

            throw new Judge0Exception(
                "Failed to communicate with Judge0",
                e
            );
        }
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

    private Judge0SubmissionResponse getSubmission(String token) {

        return restClient
            .get()
            .uri(
                judge0Url +
                    "/submissions/{token}?base64_encoded=false",
                token
            )
            .retrieve()
            .body(Judge0SubmissionResponse.class);
    }

    private Judge0SubmissionResponse waitForResult(String token)
        throws InterruptedException {

        while (true) {

            Judge0SubmissionResponse response =
                getSubmission(token);

            String status = response.getStatus().getDescription();

            if (!status.equals("In Queue")
                && !status.equals("Processing")) {
                return response;
            }

            Thread.sleep(500);
        }
    }

    private JudgeStatus getJudgeStatus(Integer statusId) {

        return switch (statusId) {
            case 3 -> JudgeStatus.ACCEPTED;
            case 4 -> JudgeStatus.WRONG_ANSWER;
            case 5 -> JudgeStatus.TIME_LIMIT_EXCEEDED;
            case 6 -> JudgeStatus.COMPILATION_ERROR;
            case 7 -> JudgeStatus.RUNTIME_ERROR;
            default -> JudgeStatus.INTERNAL_ERROR;
        };
    }

}
