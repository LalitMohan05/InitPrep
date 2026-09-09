package com.initprep.judge.service.implementation;

import com.initprep.judge.client.Judge0Client;
import com.initprep.judge.dto.JudgeSubmissionRequest;
import com.initprep.judge.dto.JudgeSubmissionResponse;
import com.initprep.judge.service.interfaces.JudgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private final Judge0Client judge0Client;

    @Override
    public JudgeSubmissionResponse judge(
        JudgeSubmissionRequest request
    ) {

        return judge0Client.execute(request);
    }
}
