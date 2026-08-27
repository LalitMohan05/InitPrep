package com.initprep.interview.security;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.util.UUID;


public interface JwtService {

    Claims extractAllClaims(String token);
    UUID extractUUID(String token);
    String extractRole(String token);
    boolean isValidToken(String token);
}
