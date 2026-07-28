package com.initprep.auth.service;

import com.initprep.auth.repository.UserRepository;
import com.initprep.auth.security.CustomUserDetails;
import com.initprep.auth.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwtToken = authHeader.substring(7);
        String email= jwtService.extractUsername(jwtToken);

        if(email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            userRepository.findByEmail(email)
                .filter(user -> jwtService.isTokenValid(jwtToken, user))
                .ifPresent(user -> {

                    Collection<GrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                    );

                    CustomUserDetails userDetails = new CustomUserDetails(user);

                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );

                    authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                });
        }
        filterChain.doFilter(request, response);
    }
}
