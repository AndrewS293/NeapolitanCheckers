package com.checkers.datatrans;

import com.checkers.model.User;

import java.time.LocalDateTime;

// implements user response data transfer object for the application
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.createdAt = user.getCreatedAt();
        this.lastLogin = user.getLastLogin();
    }

    // return the ID of the user response
    public Long getId() {
        return id;
    }

    // return the username of the user response
    public String getUsername() {
        return username;
    }

    // return the email of the user response
    public String getEmail() {
        return email;
    }

    // return the creation date of the user response
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // return the last login date of the user response
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
}