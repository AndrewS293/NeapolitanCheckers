package com.checkers.datatrans;

// implements register request data transfer object for the application
public class RegisterRequest {

    private String username;
    private String email;
    private String password;

    // return the username of the register request
    public String getUsername() {
        return username;
    }

    // set the username of the register request
    public void setUsername(String username) {
        this.username = username;
    }

    // return the email of the register request
    public String getEmail() {
        return email;
    }

    // set the email of the register request
    public void setEmail(String email) {
        this.email = email;
    }

    // return the password of the register request
    public String getPassword() {
        return password;
    }

    // set the password of the register request
    public void setPassword(String password) {
        this.password = password;
    }
}