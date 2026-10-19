package com.checkers.service;

import com.checkers.datatrans.RegisterRequest;
import com.checkers.model.User;
import com.checkers.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
// implements user service for the application
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    // register a new user with the provided registration request
    // throws IllegalArgumentException if any validation fails
    // returns the saved user entity if registration is successful
    public User register(RegisterRequest request) {
        // validate the username
        if (request.getUsername().isBlank()) {
        throw new IllegalArgumentException("Username is required");
        }

        //check username length
        //no special characters in username


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

        //other password requirements 

        if (request.getPassword().length() < 8) {

        throw new IllegalArgumentException("Password must be at least 8 characters");

        }


        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        return userRepository.save(user);
    }

    // authenticate a user with the provided username and password
    // throws IllegalArgumentException if the username or password is invalid
    // returns the user entity if authentication is successful
    public User login(String username, String password) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return user;
    }

    // find a user by their ID
    // throws IllegalArgumentException if the user is not found
    // returns the user entity if found
    public User findById(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }
}