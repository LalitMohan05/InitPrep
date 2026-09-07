package com.initprep.attempt.security;

import io.jsonwebtoken.Claims;

import java.util.UUID;

public interface JwtService {

    String extractUsername(String token);

    UUID extractUserId(String token);

    String extractRole(String token);

    Claims extractAllClaims(String token);

    boolean isTokenValid(String token);
}
