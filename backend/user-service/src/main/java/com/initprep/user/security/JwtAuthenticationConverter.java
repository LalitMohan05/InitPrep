package com.initprep.user.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationConverter {

    private final JwtService jwtService;

    public Authentication convert(String token) {

        UUID userId = jwtService.extractUserId(token);

        String email = jwtService.extractUsername(token);

        String role = jwtService.extractRole(token);

        Collection<GrantedAuthority> authorities =
            List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
            );

        UserPrincipal principal =
            new UserPrincipal(
                userId,
                email,
                authorities
            );

        return new UsernamePasswordAuthenticationToken(
            principal,
            null,
            authorities
        );
    }
}
