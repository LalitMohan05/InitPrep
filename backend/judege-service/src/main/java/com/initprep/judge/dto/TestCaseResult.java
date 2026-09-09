package com.initprep.judge.dto;

import lombok.*;

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
}
