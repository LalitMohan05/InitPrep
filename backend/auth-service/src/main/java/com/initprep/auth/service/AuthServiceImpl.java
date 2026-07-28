package com.initprep.auth.service;

import com.initprep.auth.dto.LoginRequest;
import com.initprep.auth.dto.LoginResponse;
import com.initprep.auth.dto.RegisterRequest;
import com.initprep.auth.dto.RegisterResponse;
import com.initprep.auth.entity.Role;
import com.initprep.auth.entity.User;
import com.initprep.auth.exception.EmailAlreadyExistException;
import com.initprep.auth.repository.UserRepository;
import com.initprep.auth.security.CustomUserDetails;
import com.initprep.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        if(userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyExistException("Email already in use");
        }
        User user = User.builder()
            .email(registerRequest.getEmail())
            .password(passwordEncoder.encode(registerRequest.getPassword()))
            .role(Role.USER)
            .enabled(true)
            .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return RegisterResponse.builder()
            .message("User registered successfully")
            .accessToken(token)
            .tokenType("Bearer")
            .build();

    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {

        Authentication authentication =
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

        CustomUserDetails userDetails =
            (CustomUserDetails) authentication.getPrincipal();

        String token = jwtService.generateToken(userDetails.getUser());

        return LoginResponse.builder()
            .accessToken(token)
            .tokenType("Bearer")
            .build();
    }
}
