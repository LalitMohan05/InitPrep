package com.initprep.user.security;

import io.jsonwebtoken.Claims;

import java.util.UUID;

public interface JwtService {

    Claims extractClaims(String token);

    String extractUsername(String token);

    UUID extractUserId(String token);

    String extractRole(String token);

    boolean isTokenValid(String token);
}
