package com.initprep.ai.service.implementation;

import com.initprep.ai.dto.CodingFeedbackRequest;
import com.initprep.ai.dto.CodingFeedbackResponse;
import com.initprep.ai.dto.TheoryEvaluationRequest;
import com.initprep.ai.dto.TheoryEvaluationResponse;
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

    @Override
    public TheoryEvaluationResponse evaluateTheoryAnswer(TheoryEvaluationRequest request) {
        String prompt = """
            You are an experienced technical interviewer. Evaluate the candidate's response to the interview question.

            Target role: %s
            Difficulty: %s
            Relevant topics: %s
            Question: %s
            Expected answer or evaluation outline: %s
            Candidate response: %s

            Compare the response with the expected answer or evaluation outline when one is provided. If it is absent, assess only the question and supplied topics; do not invent a canonical answer.
            Return a fair score from 0 to 100 based only on correctness, completeness, clarity, and relevant reasoning.
            Provide concise strengths, weaknesses, actionable feedback, and a short list of recommended topics to study.
            Do not assume information absent from the candidate's response. Do not invent experience or facts.
            Return the result in the requested structured response format.
            """.formatted(
                valueOrNotProvided(request.getTargetRole()),
                valueOrNotProvided(request.getDifficulty()),
                request.getTopics() == null || request.getTopics().isEmpty() ? "Not provided" : String.join(", ", request.getTopics()),
                request.getQuestion(),
                valueOrNotProvided(request.getExpectedAnswer()),
                request.getAnswer()
            );

        TheoryEvaluationResponse response = chatClient.prompt()
            .user(prompt)
            .call()
            .entity(TheoryEvaluationResponse.class);
        if (response == null || response.getScore() == null) {
            throw new IllegalStateException("AI returned no theory evaluation score");
        }
        response.setScore(Math.max(0, Math.min(100, response.getScore())));
        return response;
    }

    private static String valueOrNotProvided(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }
}
