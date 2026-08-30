package com.initprep.interview.config;

import com.initprep.interview.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth->auth
                .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                )
                .permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/questions"
                )
                .hasRole("ADMIN")
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/questions/**"
                )
                .hasRole("ADMIN")
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/questions/**"
                )
                .hasRole("ADMIN")
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/questions/*/test-cases"
                ).hasRole("ADMIN")
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/questions/*/test-cases"
                ).hasRole("ADMIN")
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/questions/*/test-cases/*"
                ).hasRole("ADMIN")
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/questions/*/test-cases/*"
                ).hasRole("ADMIN")

                .anyRequest().authenticated()
                )
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );
        return http.build();



    }
}
