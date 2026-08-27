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
public class QuestionSummaryResponse {

    private UUID id;
    private String title;
    private QuestionType type;
    private Difficulty difficulty;

    private Set<CompanySummaryResponse> companies;
    private Set<TopicSummaryResponse> topics;
}
