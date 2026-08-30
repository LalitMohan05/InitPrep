package com.initprep.interview.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TestCaseResponse {
    private UUID id;
    private String input;
    private String expectedOutput;
    private boolean hidden;


}
