package com.initprep.attempt.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
            request.getHeader("Authorization");

        if (authHeader == null ||
            !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String jwtToken = authHeader.substring(7);

        if (!jwtService.isTokenValid(jwtToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            UUID userId =
                jwtService.extractUserId(jwtToken);

            String role =
                jwtService.extractRole(jwtToken);

            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(
                        new SimpleGrantedAuthority(
                            "ROLE_" + role
                        )
                    )
                );

            SecurityContextHolder
                .getContext()
                .setAuthentication(authToken);

        } catch (Exception e) {

            SecurityContextHolder
                .clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
