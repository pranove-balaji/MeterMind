package com.example.MeterMind.identity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final Long organizationId;
    private final Long customerId;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(AppUser user) {
        this.userId = user.getId();
        this.organizationId = user.getOrganization().getId();
        this.customerId = user.getCustomerId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(user.getRole().getAuthority()));
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
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
        return enabled;
    }
}
