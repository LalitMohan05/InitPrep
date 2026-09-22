package com.initprep.attempt.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingFeedbackResponse {

    private String summary;

    private String mistake;

    private String explanation;

    private String suggestion;

    private String complexityAnalysis;

    private String optimizedApproach;
}
