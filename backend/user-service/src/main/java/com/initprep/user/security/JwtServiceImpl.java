package com.initprep.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public Claims extractClaims(String token) {

        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    @Override
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString(
            extractClaims(token).get("userId", String.class)
        );
    }

    @Override
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    @Override
    public boolean isTokenValid(String token) {
        return extractClaims(token)
            .getExpiration()
            .after(new Date());
    }

    private SecretKey getSigningKey() {

        byte[] keyBytes = Decoders.BASE64.decode(secret);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
