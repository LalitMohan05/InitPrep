package com.initprep.judge;

import com.initprep.judge.client.Judge0Client;
import com.initprep.judge.dto.JudgeSubmissionRequest;
import com.initprep.judge.dto.JudgeSubmissionResponse;
import com.initprep.judge.dto.TestCaseRequest;
import com.initprep.judge.enums.JudgeStatus;
import com.initprep.judge.enums.ProgrammingLanguage;
import com.initprep.judge.exception.Judge0Exception;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Judge0ClientBatchTests {
    private static final String JUDGE0 = "http://judge0.test";

    @Test
    void submitsThreeCasesInOneBatchAndKeepsOrder() {
        assertAcceptedBatches(3, 3);
    }

    @Test
    void submitsTwentyCasesInOneMaximumSizedBatch() {
        assertAcceptedBatches(20, 20);
    }

    @Test
    void splitsThirtyCasesIntoTwentyAndTenAndCombinesInOriginalOrder() {
        assertAcceptedBatches(30, 20, 10);
    }

    @Test
    void sendsConfiguredApiKeyForSubmissionAndResultPolling() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        Judge0Client client = client(builder);
        ReflectionTestUtils.setField(client, "judge0ApiKey", "test-only-judge0-key");

        server.expect(requestTo(JUDGE0 + "/submissions/batch?base64_encoded=false"))
            .andExpect(method(POST))
            .andExpect(header("X-Auth-Token", "test-only-judge0-key"))
            .andRespond(withSuccess(tokenJson(List.of("key-test")), APPLICATION_JSON));
        server.expect(requestTo(JUDGE0 + "/submissions/batch?tokens=key-test&base64_encoded=false"))
            .andExpect(method(GET))
            .andExpect(header("X-Auth-Token", "test-only-judge0-key"))
            .andRespond(withSuccess("""
                {"submissions":[{"token":"key-test","stdout":"ok","status":{"id":3,"description":"Accepted"}}]}
                """, APPLICATION_JSON));

        assertEquals(JudgeStatus.ACCEPTED, client.execute(request(1)).getStatus());
        server.verify();
    }

    @Test
    void combinesFatalStatusAndMetricsWithoutDiscardingOtherCaseResults() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        Judge0Client client = client(builder);
        List<String> tokens = List.of("t-0", "t-1", "t-2");
        server.expect(requestTo(JUDGE0 + "/submissions/batch?base64_encoded=false"))
            .andExpect(method(POST))
            .andRespond(withSuccess(tokenJson(tokens), APPLICATION_JSON));
        server.expect(requestTo(JUDGE0 + "/submissions/batch?tokens=t-0,t-1,t-2&base64_encoded=false"))
            .andExpect(method(GET))
            .andRespond(withSuccess("""
                {"submissions":[
                  {"token":"t-0","stdout":"out-0","time":0.001,"memory":1000,"status":{"id":3,"description":"Accepted"}},
                  {"token":"t-1","stdout":null,"stderr":"time exceeded","message":"limit","time":0.002,"memory":1400,"status":{"id":5,"description":"Time Limit Exceeded"}},
                  {"token":"t-2","stdout":"wrong","time":0.003,"memory":1200,"status":{"id":4,"description":"Wrong Answer"}}
                ]}
                """, APPLICATION_JSON));

        JudgeSubmissionResponse response = client.execute(request(3));

        assertEquals(JudgeStatus.TIME_LIMIT_EXCEEDED, response.getStatus());
        assertEquals(1, response.getPassedTestCases());
        assertEquals(3, response.getTotalTestCases());
        assertEquals(6L, response.getExecutionTime());
        assertEquals(1400L, response.getMemoryUsed());
        assertEquals("in-1", response.getFailedTestCase().getInput());
        assertEquals("time exceeded", response.getRuntimeOutput());
        assertEquals(3, response.getTestCaseResults().size());
        assertEquals("in-2", response.getTestCaseResults().get(2).getInput());
        server.verify();
    }

    @Test
    void reportsCompilationErrorAndCompilerOutput() {
        JudgeSubmissionResponse response = executeSingleStatus(
            6, "Compilation Error", "", "Main.java:1: error", 3);

        assertEquals(JudgeStatus.COMPILATION_ERROR, response.getStatus());
        assertEquals(0, response.getPassedTestCases());
        assertEquals("Main.java:1: error", response.getCompilerOutput());
        assertEquals(3, response.getTotalTestCases());
        assertNotNull(response.getFailedTestCase());
    }

    @Test
    void mapsMemoryLimitExceededDescription() {
        JudgeSubmissionResponse response = executeSingleStatus(
            5, "Memory Limit Exceeded", "Killed", null, 3);

        assertEquals(JudgeStatus.MEMORY_LIMIT_EXCEEDED, response.getStatus());
        assertEquals("Killed", response.getRuntimeOutput());
    }

    @Test
    void mapsUnknownJudge0StatusToInternalError() {
        JudgeSubmissionResponse response = executeSingleStatus(
            99, "Unrecognized Judge0 status", "service detail", null, 3);

        assertEquals(JudgeStatus.INTERNAL_ERROR, response.getStatus());
        assertEquals("service detail", response.getRuntimeOutput());
    }

    @Test
    void failsClearlyWhenBatchResultRetrievalFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        Judge0Client client = client(builder);
        server.expect(requestTo(JUDGE0 + "/submissions/batch?base64_encoded=false"))
            .andExpect(method(POST))
            .andRespond(withSuccess(tokenJson(List.of("t-0")), APPLICATION_JSON));
        server.expect(requestTo(JUDGE0 + "/submissions/batch?tokens=t-0&base64_encoded=false"))
            .andExpect(method(GET))
            .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());

        Judge0Exception error = assertThrows(Judge0Exception.class, () -> client.execute(request(1)));
        assertTrue(error.getMessage().contains("batch result retrieval failed"));
        server.verify();
    }

    private void assertAcceptedBatches(int caseCount, int... batchSizes) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        Judge0Client client = client(builder);
        int offset = 0;
        for (int batchSize : batchSizes) {
            List<String> tokens = new ArrayList<>();
            for (int index = 0; index < batchSize; index++) tokens.add("token-" + (offset + index));
            server.expect(requestTo(JUDGE0 + "/submissions/batch?base64_encoded=false"))
                .andExpect(method(POST))
                .andExpect(content().json(submissionRequestJson(offset, batchSize)))
                .andRespond(withSuccess(tokenJson(tokens), APPLICATION_JSON));
            server.expect(requestTo(batchResultUri(tokens)))
                .andExpect(method(GET))
                .andRespond(withSuccess(acceptedResponseJson(tokens), APPLICATION_JSON));
            offset += batchSize;
        }

        JudgeSubmissionResponse response = client.execute(request(caseCount));

        assertEquals(JudgeStatus.ACCEPTED, response.getStatus());
        assertEquals(caseCount, response.getPassedTestCases());
        assertEquals(caseCount, response.getTotalTestCases());
        assertEquals(caseCount, response.getTestCaseResults().size());
        assertEquals((long) caseCount, response.getExecutionTime());
        assertEquals(2000L, response.getMemoryUsed());
        for (int index = 0; index < caseCount; index++) {
            assertEquals("in-" + index, response.getTestCaseResults().get(index).getInput());
            assertEquals("out-" + index, response.getTestCaseResults().get(index).getActualOutput());
        }
        server.verify();
    }

    private JudgeSubmissionResponse executeSingleStatus(int statusId, String description, String stderr,
                                                         String compileOutput, int cases) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        Judge0Client client = client(builder);
        List<String> tokens = new ArrayList<>();
        for (int index = 0; index < cases; index++) tokens.add("status-" + index);
        server.expect(requestTo(JUDGE0 + "/submissions/batch?base64_encoded=false"))
            .andExpect(method(POST))
            .andRespond(withSuccess(tokenJson(tokens), APPLICATION_JSON));
        server.expect(requestTo(batchResultUri(tokens)))
            .andExpect(method(GET))
            .andRespond(withSuccess(errorResponseJson(tokens, statusId, description, stderr, compileOutput), APPLICATION_JSON));
        JudgeSubmissionResponse response = client.execute(request(cases));
        server.verify();
        return response;
    }

    private static Judge0Client client(RestClient.Builder builder) {
        Judge0Client client = new Judge0Client(builder.build());
        ReflectionTestUtils.setField(client, "judge0Url", JUDGE0);
        return client;
    }

    private static JudgeSubmissionRequest request(int count) {
        List<TestCaseRequest> cases = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            cases.add(TestCaseRequest.builder().input("in-" + index).expectedOutput("out-" + index).hidden(index >= 3).build());
        }
        return JudgeSubmissionRequest.builder()
            .sourceCode("class Main {}")
            .language(ProgrammingLanguage.JAVA)
            .testCases(cases)
            .build();
    }

    private static String submissionRequestJson(int offset, int size) {
        List<String> submissions = new ArrayList<>();
        for (int index = offset; index < offset + size; index++) {
            submissions.add("""
                {"source_code":"class Main {}","language_id":62,"stdin":"in-%d","expected_output":"out-%d","cpu_time_limit":2.0,"memory_limit":128000.0}
                """.formatted(index, index).trim());
        }
        return "{\"submissions\":[" + String.join(",", submissions) + "]}";
    }

    private static String tokenJson(List<String> tokens) {
        return "[" + String.join(",", tokens.stream().map(token -> "{\"token\":\"" + token + "\"}").toList()) + "]";
    }

    private static String batchResultUri(List<String> tokens) {
        return JUDGE0 + "/submissions/batch?tokens=" + String.join(",", tokens) + "&base64_encoded=false";
    }

    private static String acceptedResponseJson(List<String> tokens) {
        List<String> submissions = new ArrayList<>();
        for (String token : tokens) {
            int index = Integer.parseInt(token.substring("token-".length()));
            submissions.add("{\"token\":\"" + token + "\",\"stdout\":\"out-" + index + "\",\"time\":0.001,\"memory\":2000,\"status\":{\"id\":3,\"description\":\"Accepted\"}}");
        }
        return "{\"submissions\":[" + String.join(",", submissions) + "]}";
    }

    private static String errorResponseJson(List<String> tokens, int statusId, String description,
                                            String stderr, String compileOutput) {
        List<String> submissions = new ArrayList<>();
        for (String token : tokens) {
            String stderrField = stderr == null ? "" : "\"stderr\":\"" + stderr + "\",";
            String compileField = compileOutput == null ? "" : "\"compile_output\":\"" + compileOutput + "\",\"message\":\"" + compileOutput + "\",";
            submissions.add("{\"token\":\"" + token + "\",\"stdout\":null," + stderrField + compileField
                + "\"time\":0.001,\"memory\":2500,\"status\":{\"id\":" + statusId + ",\"description\":\"" + description + "\"}}");
        }
        return "{\"submissions\":[" + String.join(",", submissions) + "]}";
    }
}
