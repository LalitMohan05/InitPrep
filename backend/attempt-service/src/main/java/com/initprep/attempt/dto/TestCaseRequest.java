package com.initprep.attempt.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseRequest {

    private String input;

    private String expectedOutput;
    private boolean hidden;
}
