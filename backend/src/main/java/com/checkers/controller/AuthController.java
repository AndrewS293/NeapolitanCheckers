package com.checkers.controller;

import com.checkers.datatrans.LoginRequest;
import com.checkers.datatrans.RegisterRequest;
import com.checkers.datatrans.UserResponse;
import com.checkers.model.User;
import com.checkers.service.UserService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
// implements authcontroller component of application
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

   @PostMapping("/register")
   // submits registration request and displays success or error feedback
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

        try {

            User user = userService.register(request);

            return ResponseEntity.ok(
                    new UserResponse(user)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }

    @PostMapping("/login")
    // submits login request and displays success or error feedback
    public ResponseEntity<UserResponse> login(
            @RequestBody LoginRequest request,
            HttpSession session) {

        User user = userService.login(
                request.getUsername(),
                request.getPassword()
        );

        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());

        return ResponseEntity.ok(new UserResponse(user));
    }

    @PostMapping("/logout")
    // submits logout request and invalidates the session
    public ResponseEntity<Void> logout(HttpSession session) {

        session.invalidate();

        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/me")
    // retrieves the current user's information from the session
    public ResponseEntity<UserResponse> me(HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findById(userId);

        return ResponseEntity.ok(new UserResponse(user));
    }
}