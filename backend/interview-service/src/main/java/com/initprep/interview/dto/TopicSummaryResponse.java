package com.initprep.interview.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class TopicSummaryResponse {

    private UUID id;
    private String name;
}
