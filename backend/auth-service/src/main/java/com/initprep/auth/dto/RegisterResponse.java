package com.initprep.auth.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterResponse {
    private String message;
    private String accessToken;
    private String tokenType;
}
