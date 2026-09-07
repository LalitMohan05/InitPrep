package com.initprep.attempt.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    @Override
    public UUID extractUserId(String token) {
        String userId =
            extractAllClaims(token)
                .get("userId", String.class);

        return UUID.fromString(userId);
    }

    @Override
    public String extractRole(String token) {
        return extractAllClaims(token)
            .get("role", String.class);
    }

    @Override
    public Claims extractAllClaims(String token) {

        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    @Override
    public boolean isTokenValid(String token) {

        try {
            Claims claims = extractAllClaims(token);

            return claims.getExpiration()
                .after(new Date());

        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey getSigningKey() {

        byte[] keyBytes =
            Decoders.BASE64.decode(secret);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
