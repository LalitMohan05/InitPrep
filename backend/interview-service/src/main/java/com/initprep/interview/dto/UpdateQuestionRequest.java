package com.initprep.interview.dto;

import com.initprep.interview.enums.Difficulty;
import com.initprep.interview.enums.QuestionType;
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
public class UpdateQuestionRequest {

    @Size(max = 200)
    private String title;

    private String description;

    private QuestionType type;
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
}
