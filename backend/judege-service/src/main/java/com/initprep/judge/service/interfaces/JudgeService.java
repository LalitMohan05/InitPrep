package com.initprep.judge.service.interfaces;

import com.initprep.judge.dto.JudgeSubmissionRequest;
import com.initprep.judge.dto.JudgeSubmissionResponse;

public interface JudgeService {

    JudgeSubmissionResponse judge(
        JudgeSubmissionRequest request
    );
}
