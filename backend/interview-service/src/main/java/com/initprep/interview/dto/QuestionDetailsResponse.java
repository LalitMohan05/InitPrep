package com.initprep.interview.dto;

import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import com.initprep.interview.enums.TargetRole;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDetailsResponse {

    private UUID id;
    private String title;
    private String description;
    private Difficulty difficulty;
    private QuestionType type;
    private String constraints;
    private String examples;
    private String hints;
    private String starterCode;
    private String expectedComplexity;
    private String options;
    private Set<TargetRole> roles;
    private Set<String> topics;
}
