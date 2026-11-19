package com.bookdhara.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Login identity. */
    @Column(unique = true, length = 254)
    private String email;

    /** Optional; needed later for courier deliveries. */
    @Column(unique = true, length = 13)
    private String phone;

    @Column(length = 80)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** BCrypt hash; null until the user sets a password. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "failed_logins", nullable = false)
    private int failedLogins;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected User() {
        // for JPA
    }

    public static User withEmail(String email, Instant createdAt) {
        User user = new User();
        user.email = email;
        user.createdAt = createdAt;
        return user;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.failedLogins = 0;
        this.lockedUntil = null;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    /** Counts a wrong password; after too many, locks password login for a while. */
    public void recordFailedLogin(Instant now, int maxFailures, java.time.Duration lockFor) {
        failedLogins++;
        if (failedLogins >= maxFailures) {
            lockedUntil = now.plus(lockFor);
            failedLogins = 0;
        }
    }

    public void recordSuccessfulLogin() {
        failedLogins = 0;
        lockedUntil = null;
    }
}
