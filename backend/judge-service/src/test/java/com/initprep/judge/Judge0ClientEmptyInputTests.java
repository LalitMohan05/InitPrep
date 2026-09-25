package com.initprep.judge;

import com.initprep.judge.client.Judge0Client;
import com.initprep.judge.dto.JudgeSubmissionRequest;
import com.initprep.judge.dto.JudgeSubmissionResponse;
import com.initprep.judge.dto.TestCaseRequest;
import com.initprep.judge.enums.ProgrammingLanguage;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class Judge0ClientEmptyInputTests {

    @Test
    void sendsEmptyInputToJudge0WithoutChangingIt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        Judge0Client client = new Judge0Client(builder.build());
        ReflectionTestUtils.setField(client, "judge0Url", "http://judge0.test");

        server.expect(requestTo("http://judge0.test/submissions/batch?base64_encoded=false"))
            .andExpect(method(POST))
            .andExpect(content().json("""
                {"submissions":[{"source_code":"class Main {}","language_id":62,"stdin":"","expected_output":"0","cpu_time_limit":2.0,"memory_limit":128000.0}]}
                """))
            .andRespond(withSuccess("[{\"token\":\"empty-input-case\"}]", APPLICATION_JSON));
        server.expect(requestTo("http://judge0.test/submissions/batch?tokens=empty-input-case&base64_encoded=false"))
            .andExpect(method(GET))
            .andRespond(withSuccess("{\"submissions\":[{\"token\":\"empty-input-case\",\"stdout\":\"0\",\"time\":0.001,\"memory\":1000,\"status\":{\"id\":3,\"description\":\"Accepted\"}}]}", APPLICATION_JSON));

        JudgeSubmissionRequest request = JudgeSubmissionRequest.builder()
            .sourceCode("class Main {}")
            .language(ProgrammingLanguage.JAVA)
            .testCases(List.of(TestCaseRequest.builder()
                .input("")
                .expectedOutput("0")
                .hidden(false)
                .build()))
            .build();

        JudgeSubmissionResponse response = client.execute(request);

        assertEquals("", response.getTestCaseResults().get(0).getInput());
        assertEquals("ACCEPTED", response.getStatus().name());
        server.verify();
    }
}
