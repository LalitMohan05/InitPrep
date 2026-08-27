package com.initprep.interview.dto;

import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResponse {

    private UUID id;

    private String title;

    private String description;

    private QuestionType type;

    private Difficulty difficulty;

    private String constraints;

    private String examples;

    private String hints;

    private String starterCode;
    private String expectedComplexity;

    private String options;

    private Set<CompanySummaryResponse> companies;
    private Set<TopicSummaryResponse> topics;
}
