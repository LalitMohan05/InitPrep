package com.initprep.ai.service.implementation;

import com.initprep.ai.dto.CodingFeedbackRequest;
import com.initprep.ai.dto.CodingFeedbackResponse;
import com.initprep.ai.service.interfaces.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ChatClient chatClient;

    @Override
    public CodingFeedbackResponse generateCodingFeedback(
        CodingFeedbackRequest request
    ) {

        String prompt = """
            You are an expert coding interview mentor.

            Analyze the candidate's coding solution.

            Question:
            %s

            Programming Language:
            %s

            Candidate Code:
            %s

            Judge Status:
            %s

            Test Cases Passed:
            %d / %d

            Failed Test Case:
            Input: %s
            Expected Output: %s
            Actual Output: %s

            Compiler Output:
            %s

            Runtime Output:
            %s

            Provide feedback covering:

            1. Summary of the solution
            2. What is wrong with the solution, if anything
            3. Why the mistake occurs
            4. How the candidate can fix it
            5. Time and space complexity
            6. A better approach, if applicable

            Important rules:
            - Use the judge result as the source of truth for execution behavior.
            - Do not invent test results.
            - If the solution is correct, clearly say so.
            - Focus on interview-quality feedback.
            """.formatted(
            request.getQuestion(),
            request.getLanguage(),
            request.getCode(),
            request.getStatus(),
            request.getPassedTestCases(),
            request.getTotalTestCases(),
            request.getInput(),
            request.getExpectedOutput(),
            request.getActualOutput(),
            request.getCompilerOutput(),
            request.getRuntimeOutput()
        );

        return chatClient
            .prompt()
            .user(prompt)
            .call()
            .entity(CodingFeedbackResponse.class);

    }
}
