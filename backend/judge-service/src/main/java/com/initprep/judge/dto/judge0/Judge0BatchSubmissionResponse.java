package com.initprep.judge.dto.judge0;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Judge0BatchSubmissionResponse {

    private List<Judge0SubmissionResponse> submissions;
}
