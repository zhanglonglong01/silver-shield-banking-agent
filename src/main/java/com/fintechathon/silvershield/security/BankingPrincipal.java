package com.fintechathon.silvershield.security;

import com.fintechathon.silvershield.identity.domain.AppUser;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class BankingPrincipal implements UserDetails {

    private final UUID userId;
    private final String username;
    private final String password;
    private final String displayName;
    private final AppUser.Role role;
    private final AppUser.Status status;
    private final AssuranceLevel assuranceLevel;

    public BankingPrincipal(
            UUID userId,
            String username,
            String password,
            String displayName,
            AppUser.Role role,
            AppUser.Status status,
            AssuranceLevel assuranceLevel) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
        this.assuranceLevel = assuranceLevel;
    }

    public static BankingPrincipal fromUser(AppUser user) {
        return new BankingPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(), user.getDisplayName(),
                user.getRole(), user.getStatus(), AssuranceLevel.AAL1);
    }

    public static BankingPrincipal fromSession(AuthSession session) {
        return new BankingPrincipal(session.userId(), session.username(), null, session.displayName(),
                AppUser.Role.valueOf(session.role()), AppUser.Status.ACTIVE, session.assuranceLevel());
    }

    public UUID userId() { return userId; }
    public String displayName() { return displayName; }
    public AppUser.Role role() { return role; }
    public AssuranceLevel assuranceLevel() { return assuranceLevel; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return status != AppUser.Status.LOCKED; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return status == AppUser.Status.ACTIVE; }
}
