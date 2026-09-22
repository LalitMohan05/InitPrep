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

            List<TestCaseRequest> testCases =
                request.getTestCases();

            /*
             * Step 1:
             * Build all Judge0 submissions locally.
             */
            List<Judge0SubmissionRequest> judge0Requests =
                new ArrayList<>();

            for (TestCaseRequest testCase : testCases) {

                Judge0SubmissionRequest judge0Request =
                    Judge0SubmissionRequest.builder()
                        .sourceCode(request.getSourceCode())
                        .languageId(
                            getLanguageId(
                                request.getLanguage()
                            )
                        )
                        .stdin(testCase.getInput())
                        .expectedOutput(
                            testCase.getExpectedOutput()
                        )
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

                judge0Requests.add(judge0Request);
            }

            /*
             * Step 2:
             * Submit all testcases to Judge0 in one batch.
             */
            List<Judge0SubmissionToken> tokens =
                submitBatch(judge0Requests);

            if (tokens == null
                || tokens.size() != testCases.size()) {

                throw new Judge0Exception(
                    "Judge0 returned an invalid batch response"
                );
            }

            /*
             * Step 3:
             * Process the result of every testcase.
             */
            List<TestCaseResult> results =
                new ArrayList<>();

            TestCaseResult failedTestCase = null;

            long totalExecutionTime = 0;
            long maxMemoryUsed = 0;

            int passed = 0;

            /*
             * Judge0 returns tokens in the same order
             * as the submitted batch.
             */
            for (int i = 0; i < testCases.size(); i++) {

                TestCaseRequest testCase =
                    testCases.get(i);

                String token =
                    tokens
                        .get(i)
                        .getToken();

                if (token == null || token.isBlank()) {

                    throw new Judge0Exception(
                        "Judge0 did not return a token for test case "
                            + (i + 1)
                    );
                }

                Judge0SubmissionResponse result =
                    waitForResult(token);

                JudgeStatus status =
                    getJudgeStatus(
                        result.getStatus().getId()
                    );

                boolean isPassed =
                    status == JudgeStatus.ACCEPTED;

                if (isPassed) {
                    passed++;
                }

                /*
                 * Store the first failed testcase.
                 */
                if (!isPassed && failedTestCase == null) {

                    failedTestCase =
                        TestCaseResult.builder()
                            .passed(false)
                            .input(testCase.getInput())
                            .expectedOutput(
                                testCase.getExpectedOutput()
                            )
                            .actualOutput(
                                result.getStdout()
                            )
                            .hidden(
                                testCase.isHidden()
                            )
                            .build();
                }

                /*
                 * Aggregate execution time.
                 */
                if (result.getTime() != null) {

                    totalExecutionTime +=
                        (long) (
                            result.getTime() * 1000
                        );
                }

                /*
                 * Keep maximum memory used by any testcase.
                 */
                if (result.getMemory() != null) {

                    maxMemoryUsed =
                        Math.max(
                            maxMemoryUsed,
                            result.getMemory().longValue()
                        );
                }

                /*
                 * Store individual testcase result.
                 */
                results.add(
                    TestCaseResult.builder()
                        .passed(isPassed)
                        .input(testCase.getInput())
                        .expectedOutput(
                            testCase.getExpectedOutput()
                        )
                        .actualOutput(
                            result.getStdout()
                        )
                        .hidden(
                            testCase.isHidden()
                        )
                        .build()
                );

                /*
                 * Stop processing if the program itself failed.
                 *
                 * WRONG_ANSWER is not a fatal execution error,
                 * so we continue checking the remaining testcases.
                 */
                if (status != JudgeStatus.ACCEPTED
                    && status != JudgeStatus.WRONG_ANSWER) {

                    return JudgeSubmissionResponse.builder()
                        .status(status)
                        .passedTestCases(passed)
                        .totalTestCases(testCases.size())
                        .executionTime(totalExecutionTime)
                        .memoryUsed(maxMemoryUsed)
                        .compilerOutput(
                            result.getCompileOutput()
                        )
                        .runtimeOutput(
                            result.getStderr()
                        )
                        .failedTestCase(
                            failedTestCase
                        )
                        .testCaseResults(results)
                        .build();
                }
            }

            /*
             * Step 4:
             * Determine final submission status.
             */
            JudgeStatus finalStatus =
                passed == testCases.size()
                    ? JudgeStatus.ACCEPTED
                    : JudgeStatus.WRONG_ANSWER;

            return JudgeSubmissionResponse.builder()
                .status(finalStatus)
                .passedTestCases(passed)
                .totalTestCases(testCases.size())
                .executionTime(totalExecutionTime)
                .memoryUsed(maxMemoryUsed)
                .failedTestCase(failedTestCase)
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

    private List<Judge0SubmissionToken> submitBatch(
        List<Judge0SubmissionRequest> submissions
    ) {

        try {

            Judge0BatchSubmissionRequest request =
                Judge0BatchSubmissionRequest.builder()
                    .submissions(submissions)
                    .build();

            return restClient
                .post()
                .uri(
                    judge0Url +
                        "/submissions/batch?base64_encoded=false"
                )
                .body(request)
                .retrieve()
                .body(
                    new ParameterizedTypeReference<
                        List<Judge0SubmissionToken>
                        >() {}
                );

        } catch (Exception e) {

            e.printStackTrace();

            throw new Judge0Exception(
                "Judge0 batch submission failed",
                e
            );
        }
    }

    private Judge0SubmissionResponse getSubmission(
        String token
    ) {

        return restClient
            .get()
            .uri(
                judge0Url +
                    "/submissions/{token}" +
                    "?base64_encoded=false",
                token
            )
            .retrieve()
            .body(Judge0SubmissionResponse.class);
    }

    private Judge0SubmissionResponse waitForResult(
        String token
    ) throws InterruptedException {

        while (true) {

            Judge0SubmissionResponse response =
                getSubmission(token);

            String status =
                response.getStatus().getDescription();

            if (!status.equals("In Queue")
                && !status.equals("Processing")) {

                return response;
            }

            Thread.sleep(100);
        }
    }

    private int getLanguageId(
        ProgrammingLanguage language
    ) {

        return switch (language) {

            case JAVA -> 62;
            case PYTHON -> 71;
            case CPP -> 54;
            case C -> 50;
            case JAVASCRIPT -> 63;
        };
    }

    private JudgeStatus getJudgeStatus(
        Integer statusId
    ) {

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
