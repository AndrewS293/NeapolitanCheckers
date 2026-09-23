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

    public User login(String username, String password) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return user;
    }

    public User findById(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));
    }
}