package com.initprep.judge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCaseRequest {

    @NotBlank
    private String input;

    @NotBlank
    private String expectedOutput;
}
