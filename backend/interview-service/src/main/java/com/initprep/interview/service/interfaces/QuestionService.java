package com.initprep.interview.service.interfaces;

import com.initprep.interview.dto.CreateQuestionRequest;
import com.initprep.interview.dto.QuestionResponse;
import com.initprep.interview.dto.QuestionSummaryResponse;
import com.initprep.interview.dto.UpdateQuestionRequest;
import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface QuestionService {
    QuestionResponse createQuestion(CreateQuestionRequest request);

    QuestionResponse updateQuestion(UUID questionId , UpdateQuestionRequest request);

    void DeleteQuestion(UUID questionId);

    Page<QuestionSummaryResponse> getQuestions(Difficulty difficulty , QuestionType type,String companyName,
                                               String topicName, Pageable pageable);
}
