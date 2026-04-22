package com.rzd.dispatcher.common.model.entity;

import com.rzd.dispatcher.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
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

    @Column(name = "user_type", nullable = false)
    private String userType = "LEGAL_ENTITY";

    // Юр лицо
    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "inn", length = 20)
    private String inn;

    // Физ лицо
    @Column(name = "last_name", length = 255)
    private String lastName;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "patronymic", length = 255)
    private String patronymic;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "passport_series", length = 4)
    private String passportSeries;

    @Column(name = "passport_number", length = 6)
    private String passportNumber;

    @Column(name = "passport_issued_by", length = 255)
    private String passportIssuedBy;

    @Column(name = "passport_issued_date")
    private LocalDate passportIssuedDate;

    @Column(name = "registration_address")
    private String registrationAddress;

    @Column(name = "snils", length = 14)
    private String snils;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.USER;

    public String getDisplayName() {
        if ("INDIVIDUAL".equals(userType)) {
            return String.format("%s %s %s",
                    lastName != null ? lastName : "",
                    firstName != null ? firstName : "",
                    patronymic != null ? patronymic : ""
            ).trim();
        }
        return companyName;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
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
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}