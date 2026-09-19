package com.initprep.ai.service.interfaces;
import com.initprep.ai.dto.CodingFeedbackRequest;
import com.initprep.ai.dto.CodingFeedbackResponse;

public interface AiService {

    CodingFeedbackResponse generateCodingFeedback(
        CodingFeedbackRequest request
    );
}
