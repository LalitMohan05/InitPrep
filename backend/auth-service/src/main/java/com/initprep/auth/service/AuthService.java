package com.initprep.auth.service;

import com.initprep.auth.dto.LoginRequest;
import com.initprep.auth.dto.LoginResponse;
import com.initprep.auth.dto.RegisterRequest;
import com.initprep.auth.dto.RegisterResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest registerRequest);
    LoginResponse login(LoginRequest loginRequest);
}
