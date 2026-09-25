package com.initprep.judge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseRequest {

    private boolean passed;

    private String input;

    @NotBlank
    private String expectedOutput;
    private String actualOutput;
    private boolean hidden;
}
