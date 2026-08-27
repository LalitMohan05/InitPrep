package com.initprep.interview.controller;

import com.initprep.interview.dto.CreateQuestionRequest;
import com.initprep.interview.dto.QuestionResponse;
import com.initprep.interview.dto.QuestionSummaryResponse;
import com.initprep.interview.dto.UpdateQuestionRequest;
import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import com.initprep.interview.service.interfaces.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {
    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<QuestionResponse> createQuestion(
        @Valid @RequestBody CreateQuestionRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.createQuestion(request));

    }

    @PatchMapping("{questionId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<QuestionResponse> updateQuestion(
        @PathVariable UUID questionId,
        @Valid @RequestBody UpdateQuestionRequest request
    ){
        return ResponseEntity.status(HttpStatus.OK).body(questionService.updateQuestion(questionId, request));
    }

    @DeleteMapping("{questionId}")
    public ResponseEntity<Void> deleteQuestion(
        @PathVariable UUID questionId
    ) {
        questionService.DeleteQuestion(questionId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    public ResponseEntity<Page<QuestionSummaryResponse>> getAllQuestions(
        @RequestParam(required = false) Difficulty difficulty,
        @RequestParam(required = false)QuestionType type,
        @RequestParam(required = false) String company,
        @RequestParam(required = false) String topic,
        @PageableDefault(size = 10 , sort = "title")
        Pageable pageable) {
        return ResponseEntity.ok(questionService.getQuestions(difficulty,type,company,topic,pageable));
    }
}

