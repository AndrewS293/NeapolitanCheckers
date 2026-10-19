package com.checkers.datatrans;

// implements login request data transfer object for the application
public class LoginRequest {

    private String username;
    private String password;

    // return the username of the login request
    public String getUsername() {
        return username;
    }

    // set the username of the login request
    public void setUsername(String username) {
        this.username = username;
    }

    // return the password of the login request
    public String getPassword() {
        return password;
    }

    // set the password of the login request
    public void setPassword(String password) {
        this.password = password;
    }
}