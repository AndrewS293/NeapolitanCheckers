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
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }


    //Our Registration endpoint
   @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

        //if valid puts into databse        
        try {

            User user = userService.register(request);

            return ResponseEntity.ok(
                    new UserResponse(user)
            );

        //if invalid returns error message
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


    //Our Login endpoint
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @RequestBody LoginRequest request,
            HttpSession session) {

        //sends the username and password to the service to check if valid
        User user = userService.login(
                request.getUsername(),
                request.getPassword()
        );

        //if valid, sets the session attributes
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());

        return ResponseEntity.ok(new UserResponse(user));
    }


    //Our Logout endpoint
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {

        //just invalidates the session, thus logging the user out
        session.invalidate();

        return ResponseEntity.ok().build();
    }
    

    //Our endpoint to get the current logged in user
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findById(userId);

        return ResponseEntity.ok(new UserResponse(user));
    }
}