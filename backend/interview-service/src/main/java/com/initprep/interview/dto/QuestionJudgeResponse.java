package com.initprep.interview.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionJudgeResponse {

    private UUID questionId;

    private List<TestCaseResponse> testCases;
}
