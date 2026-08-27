package com.initprep.interview.dto;

import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuestionRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private QuestionType type;

    @NotNull
    private Difficulty difficulty;

    private String constraints;

    private String examples;

    private String hints;

    private String starterCode;

    @Size(max = 100)
    private String expectedComplexity;

    private String options;

    private String correctAnswer;

    private Set<UUID> companyIds;

    private Set<UUID> topicIds;
    private List<TestCaseRequest> testCases;
}
