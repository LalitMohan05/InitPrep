package com.initprep.auth.security;

import com.initprep.auth.entity.User;
import com.initprep.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
        throws UsernameNotFoundException {

        User user = userRepository.findByEmail(username)
            .orElseThrow(() ->
                new UsernameNotFoundException("User not found"));

        return new CustomUserDetails(user);
    }
}
