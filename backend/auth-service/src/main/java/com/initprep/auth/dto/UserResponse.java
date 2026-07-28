package com.initprep.auth.dto;

import com.initprep.auth.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private Role role;
    private boolean enabled;
}
