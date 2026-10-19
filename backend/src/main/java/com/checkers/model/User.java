package com.checkers.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
// implements user entity for the application
// defines the structure and behavior of a user in the database
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    public User() {
    }

    @PrePersist
    // defines on creation of a new user entity
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // return the ID of the user entity
    public Long getId() {
        return id;
    }

    // set the ID of the user entity
    public void setId(Long id) {
        this.id = id;
    }

    // return the username of the user entity
    public String getUsername() {
        return username;
    }

    // set the username of the user entity
    public void setUsername(String username) {
        this.username = username;
    }

    // return the email of the user entity
    public String getEmail() {
        return email;
    }

    // set the email of the user entity
    public void setEmail(String email) {
        this.email = email;
    }

    // return the password hash of the user entity
    public String getPasswordHash() {
        return passwordHash;
    }

    // set the password hash of the user entity
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    // return the creation date of the user entity
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // set the creation date of the user entity
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // return the last login date of the user entity
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    // set the last login date of the user entity
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}

