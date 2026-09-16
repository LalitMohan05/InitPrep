package com.initprep.interview.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkTestCaseRequest {

    @NotEmpty
    @Valid
    private List<TestCaseRequest> testCases;
}
