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

        Analyze the candidate's coding solution using ONLY the information provided below.

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


        Analyze the solution according to the Judge Status.

        If the status is ACCEPTED:
        - Explain what the solution does correctly.
        - Analyze its time and space complexity.
        - Suggest an optimization only if a meaningful one exists.

        If the status is WRONG_ANSWER:
        - Analyze the provided failed test case.
        - Identify the likely logical mistake from the provided code and execution evidence.
        - Explain why the expected and actual outputs differ.
        - Suggest a concrete correction.

        If the status is COMPILATION_ERROR:
        - Explain the compiler error.
        - Identify the relevant code issue if possible.
        - Suggest how to fix it.

        If the status is RUNTIME_ERROR:
        - Analyze the runtime output and provided code.
        - Explain the likely cause.
        - Suggest a correction.

        If the status is TIME_LIMIT_EXCEEDED:
        - Analyze the algorithm's complexity.
        - Identify the likely performance bottleneck.
        - Suggest a more efficient approach if applicable.

        If the status is MEMORY_LIMIT_EXCEEDED:
        - Analyze the memory usage of the approach.
        - Identify the likely source of excessive memory consumption.
        - Suggest a more memory-efficient approach.

        Provide the response in these six areas:

        1. Summary
        2. Mistake
        3. Explanation
        4. Suggestion
        5. Complexity Analysis
        6. Optimized Approach

        Important rules:
        - When the candidate code is available, inspect the actual code before identifying a specific bug.
        - If the code does not provide enough evidence to determine the exact cause, explicitly say that the exact cause cannot be confirmed.
        - Do not infer a specific implementation bug solely from the input and output.
        - The Judge result is the source of truth for execution behavior.
        - Never invent test results.
        - Never invent compiler errors or runtime errors.
        - Do not claim a testcase passed or failed unless that information is provided.
        - Do not assume the exact cause of a bug when the provided evidence is insufficient.
        - Clearly distinguish confirmed facts from likely causes.
        - If the solution is correct, clearly say so.
        - Focus on useful coding-interview feedback.
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
