package com.initprep.attempt.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingFeedbackRequest {

    private String question;

    private String code;

    private String language;

    private String status;

    private Integer passedTestCases;

    private Integer totalTestCases;

    private String input;

    private String expectedOutput;

    private String actualOutput;

    private String compilerOutput;

    private String runtimeOutput;
}
