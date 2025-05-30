package com.mercadolibre.itarc.climatehub_ms_user.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.mercadolibre.itarc.climatehub_ms_user.model.entity.UserEntity;

public class UserDetailsImpl implements UserDetails {
    private final UserEntity user;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(UserEntity user) {
        this.user = user;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHashed();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }

    public UserEntity getUser() {
        return user;
    }
} 