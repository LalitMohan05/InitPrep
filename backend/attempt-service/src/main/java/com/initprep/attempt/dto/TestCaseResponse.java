package com.initprep.attempt.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseResponse {

    private String input;

    private String expectedOutput;
    private boolean hidden;
}
