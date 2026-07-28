package com.initprep.auth.controller;

import com.initprep.auth.dto.*;
import com.initprep.auth.entity.User;
import com.initprep.auth.security.CustomUserDetails;
import com.initprep.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Authentication APIs")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Get current authenticated user")
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {

        CustomUserDetails userDetails =
            (CustomUserDetails) authentication.getPrincipal();

        User user = userDetails.getUser();

        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.isEnabled()
        );
    }
    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {

        RegisterResponse response = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }
    @Operation(summary = "Login user")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

}
