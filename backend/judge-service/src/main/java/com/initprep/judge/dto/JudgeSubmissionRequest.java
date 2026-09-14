package com.initprep.judge.dto;

import com.initprep.judge.enums.ProgrammingLanguage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeSubmissionRequest {

    @NotBlank
    private String sourceCode;

    @NotNull
    private ProgrammingLanguage language;

    @Valid
    @NotEmpty
    private List<TestCaseRequest> testCases;

    private Integer timeLimit;

    private Integer memoryLimit;
}
