package com.checkers.controller;

import com.checkers.datatrans.RegisterRequest;
import com.checkers.model.User;
import com.checkers.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(
            @RequestBody RegisterRequest request) {

        User user = userService.register(request);

        return ResponseEntity.ok(user);
    }
}

