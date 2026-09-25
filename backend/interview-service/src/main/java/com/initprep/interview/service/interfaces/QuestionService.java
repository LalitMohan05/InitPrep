package com.initprep.interview.service.interfaces;

import com.initprep.interview.dto.*;
import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import com.initprep.interview.enums.TargetRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface QuestionService {
    QuestionResponse createQuestion(CreateQuestionRequest request);

    QuestionResponse updateQuestion(UUID questionId , UpdateQuestionRequest request);

    void DeleteQuestion(UUID questionId);

    Page<QuestionSummaryResponse> getQuestions(Difficulty difficulty , QuestionType type,String companyName,
                                               String topicName, TargetRole role, Pageable pageable);

    boolean questionExists(UUID questionId);
    QuestionJudgeResponse getJudgeData(UUID questionId);
    QuestionJudgeResponse getPublicTestCases(UUID questionId);

    QuestionDetailsResponse getQuestionDetails(UUID questionId);

    boolean checkMcqAnswer(UUID questionId, String answer);
}
