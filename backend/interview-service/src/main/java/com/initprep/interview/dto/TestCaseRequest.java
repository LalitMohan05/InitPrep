package com.initprep.interview.dto;

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
