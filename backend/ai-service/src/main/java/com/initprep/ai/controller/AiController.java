package com.initprep.ai.controller;

import com.initprep.ai.dto.CodingFeedbackRequest;
import com.initprep.ai.dto.CodingFeedbackResponse;
import com.initprep.ai.service.interfaces.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/coding-feedback")
    public ResponseEntity<CodingFeedbackResponse> generateCodingFeedback(
        @Valid @RequestBody CodingFeedbackRequest request
    ) {

        return ResponseEntity.ok(
            aiService.generateCodingFeedback(request)
        );
    }

    @GetMapping("/env-test")
    public String envTest() {
        String key = System.getenv("GEMINI_API_KEY");

        if (key == null || key.isBlank()) {
            return "OpenRoute is NOT set";
        }

        return "OpenRoute API is set. Length = " + key.length();
    }
}
