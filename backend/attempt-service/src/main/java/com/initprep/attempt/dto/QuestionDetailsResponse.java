package com.initprep.attempt.dto;

import lombok.*;
import java.util.UUID;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDetailsResponse {

    private UUID id;
    private String title;
    private String description;
    private String difficulty;
    private String type;
    private String constraints;
    private String examples;
    private String hints;
    private String starterCode;
    private String expectedComplexity;
    private String options;
    private Set<String> roles;
    private Set<String> topics;
}
