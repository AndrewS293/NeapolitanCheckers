package com.checkers.service;

import com.checkers.datatrans.RegisterRequest;
import com.checkers.model.User;
import com.checkers.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(RegisterRequest request) {
       

        //all error messages for registration
        if (request.getUsername().isBlank()) {
        throw new IllegalArgumentException("Username is required");
        }

        if (request.getUsername().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }

        if (!request.getUsername().matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores");
        }

        if (request.getUsername().length() > 25) {
            throw new IllegalArgumentException("Username cannot exceed 25 characters");
        }


        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (request.getEmail().isBlank()) {

        throw new IllegalArgumentException("Email is required");

        }

        if (!request.getEmail().contains("@")) {
        
        throw new IllegalArgumentException("Invalid email address");
        }   

        if (request.getPassword().isBlank()) {
        throw new IllegalArgumentException("Password is required");
       

        }

        if (request.getPassword().length() > 50) {
            throw new IllegalArgumentException("Password cannot exceed 50 characters");
        }

        if (request.getPassword().contains(" ")) {
            throw new IllegalArgumentException("Password cannot contain spaces");
        }

        if (request.getPassword().matches(".*[<>\"'%;)(&+].*")) {
            throw new IllegalArgumentException("Password cannot contain special characters like <, >, \", ', %, ;, ), (, &, +");
        }
        
        if (request.getPassword().matches(".*[A-Z].*") &&
            request.getPassword().matches(".*[a-z].*") &&
            request.getPassword().matches(".*\\d.*")) {
        } else {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter, one lowercase letter, and one number");
        }

        if (request.getPassword().length() < 8) {

        throw new IllegalArgumentException("Password must be at least 8 characters");

        }

        //if no errors creates a new user and saves it to the database
        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        return userRepository.save(user);
    }



    public User login(String username, String password) {
        //checks if the username exists and if the password matches the hashed password in the database
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return user;
    }

    //searches for a user by their id, if not found throws an error
    public User findById(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }
}