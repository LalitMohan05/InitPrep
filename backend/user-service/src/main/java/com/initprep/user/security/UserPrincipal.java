package com.initprep.user.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserPrincipal {

    private UUID UserId;
    private String email;
    private Collection<? extends GrantedAuthority> authorities;

}
