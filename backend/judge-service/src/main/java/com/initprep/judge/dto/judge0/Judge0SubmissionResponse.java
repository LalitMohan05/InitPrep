package com.initprep.judge.dto.judge0;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Judge0SubmissionResponse {

    private String stdout;

    private String stderr;

    @JsonProperty("compile_output")
    private String compileOutput;

    private String message;

    private Double time;

    private Double memory;

    private String token;

    private Judge0Status status;
}
