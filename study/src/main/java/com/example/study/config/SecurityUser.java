package com.example.study.config;

import com.example.study.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class SecurityUser implements UserDetails {

    private final User user;

    public SecurityUser(User user) { this.user = user; }

    public Long getUserId() { return user.getId(); }
    public User getUser() { return user; }

    @Override public String getUsername() { return user.getUsername() != null ? user.getUsername() : "social_" + user.getId(); }
    @Override public String getPassword() { return user.getPassword() != null ? user.getPassword() : ""; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
