package com.initprep.judge;

import com.initprep.judge.controller.JudgeController;
import com.initprep.judge.dto.JudgeSubmissionRequest;
import com.initprep.judge.dto.JudgeSubmissionResponse;
import com.initprep.judge.enums.JudgeStatus;
import com.initprep.judge.service.interfaces.JudgeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class JudgeControllerValidationTests {

    @Test
    void acceptsEmptyStringAsTestCaseInput() throws Exception {
        AtomicReference<JudgeSubmissionRequest> receivedRequest = new AtomicReference<>();
        JudgeService judgeService = request -> {
            receivedRequest.set(request);
            return JudgeSubmissionResponse.builder()
                .status(JudgeStatus.ACCEPTED)
                .build();
        };

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        MockMvc mockMvc = standaloneSetup(new JudgeController(judgeService))
            .setValidator(validator)
            .build();

        mockMvc.perform(post("/api/judge/submissions")
                .contentType("application/json")
                .content("""
                    {
                      "sourceCode": "class Main {}",
                      "language": "JAVA",
                      "testCases": [{"input": "", "expectedOutput": "0", "hidden": false}]
                    }
                    """))
            .andExpect(status().isOk());

        assertEquals("", receivedRequest.get().getTestCases().get(0).getInput());
    }
}
