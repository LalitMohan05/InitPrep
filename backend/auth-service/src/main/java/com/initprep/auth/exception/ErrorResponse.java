package com.initprep.auth.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String path;

    @Builder.Default
    private Map<String, String> validationErrors = Collections.emptyMap();
}
