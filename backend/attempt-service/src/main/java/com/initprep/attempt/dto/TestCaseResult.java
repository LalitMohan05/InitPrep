package com.initprep.attempt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseResult {

    private boolean passed;

    private String input;

    private String expectedOutput;

    private String actualOutput;

    private boolean hidden;
}
