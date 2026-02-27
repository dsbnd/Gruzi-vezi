package com.rzd.dispatcher.model.entity;

import com.rzd.dispatcher.model.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements org.springframework.security.core.userdetails.UserDetails {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "inn", nullable = false, length = 20)
    private String inn;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.USER;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public String getPassword() {
        return passwordHash; // Spring Security будет использовать наше поле passwordHash
    }

    public String getUsername() {
        return email; // В качестве логина используем email
    }

    public boolean isAccountNonExpired() { return true; }

    public boolean isAccountNonLocked() { return true; }

    public boolean isCredentialsNonExpired() { return true; }

    public boolean isEnabled() { return true; }
}