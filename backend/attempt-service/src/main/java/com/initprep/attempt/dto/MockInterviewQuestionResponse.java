package com.initprep.attempt.dto;

import com.initprep.attempt.enums.AttemptType;
import com.initprep.attempt.enums.MockInterviewDifficulty;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockInterviewQuestionResponse {
    private UUID questionId;
    private AttemptType questionType;
    private MockInterviewDifficulty difficulty;
    private Integer sequenceNumber;
    private String questionTitleSnapshot;
    private String topicSnapshot;
    private String roleSnapshot;
    private QuestionDetailsResponse question;
    private AttemptResponse attempt;
    private TheoryEvaluationResponse theoryEvaluation;
}
