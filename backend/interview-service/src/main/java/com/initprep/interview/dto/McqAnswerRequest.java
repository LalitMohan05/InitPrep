package com.initprep.interview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class McqAnswerRequest {
    @NotBlank
    private String answer;
}
