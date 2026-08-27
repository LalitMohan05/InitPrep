package com.initprep.interview.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService{
    @Value("${jwt.secret}")
    String secret;

    @Override
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSignatureKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    @Override
    public UUID extractUUID(String token) {
        String userId=extractAllClaims(token).get("userId", String.class);
        return UUID.fromString(userId);
    }

    @Override
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    @Override
    public boolean isValidToken(String token) {
        try{
            extractAllClaims(token);
            return true;
        }
        catch(Exception e){
            return false;
        }
    }

    private SecretKey getSignatureKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
