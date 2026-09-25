package com.initprep.ai.service.interfaces;
import com.initprep.ai.dto.CodingFeedbackRequest;
import com.initprep.ai.dto.CodingFeedbackResponse;
import com.initprep.ai.dto.TheoryEvaluationRequest;
import com.initprep.ai.dto.TheoryEvaluationResponse;

public interface AiService {

    CodingFeedbackResponse generateCodingFeedback(
        CodingFeedbackRequest request
    );

    TheoryEvaluationResponse evaluateTheoryAnswer(TheoryEvaluationRequest request);
}
