package com.initprep.auth.security;

import com.initprep.auth.entity.User;

public interface JwtService {
    String generateToken(User user);
    boolean isTokenValid(String token, User user);
    String extractUsername(String token);
}
