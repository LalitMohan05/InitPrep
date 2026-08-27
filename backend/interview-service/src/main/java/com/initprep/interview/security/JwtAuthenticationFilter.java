package com.initprep.interview.security;

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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if(authorizationHeader != null && !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authorizationHeader.substring(7);

        if(!jwtService.isValidToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }
        UUID userId= jwtService.extractUUID(token);

        String role =jwtService.extractRole(token);

        var authority = new SimpleGrantedAuthority("ROLE_" + role);
        var authentication = new UsernamePasswordAuthenticationToken(
            userId,
            null,
            List.of(authority)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        System.out.println(
            "AUTH = " +
                SecurityContextHolder.getContext().getAuthentication()
        );

        System.out.println(
            "AUTHORITIES = " +
                SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getAuthorities()
        );

        filterChain.doFilter(request, response);
    }
}
