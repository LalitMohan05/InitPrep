package com.initprep.attempt.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeSubmissionRequest {

    private String sourceCode;

    private String language;

    private List<TestCaseRequest> testCases;

    private Integer timeLimit;

    private Integer memoryLimit;
}
