package com.checkers.controller;

import com.checkers.datatrans.LoginRequest;
import com.checkers.datatrans.RegisterRequest;
import com.checkers.model.User;
import com.checkers.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private HttpSession session;

    @BeforeEach
    void setUp() {
        session = mock(HttpSession.class);
    }

    //extra tests to verify registration/login/logout/me endpoints

    @Test
    void register_ValidRequest_ReturnsUserResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setEmail("john@example.com");
        request.setPassword("Password1");

        User user = new User();
        user.setId(12L);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 30));

        when(userService.register(request)).thenReturn(user);

        ResponseEntity<?> response = authController.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userService).register(request);
    }

    @Test
    void register_InvalidRequest_ReturnsBadRequestErrorMap() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ab");
        request.setEmail("john@example.com");
        request.setPassword("Password1");

        when(userService.register(request)).thenThrow(new IllegalArgumentException("Username must be at least 3 characters"));

        ResponseEntity<?> response = authController.register(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        assertEquals("Username must be at least 3 characters", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void login_ValidCredentials_SetsSessionAndReturnsUserResponse() {
        LoginRequest request = new LoginRequest();
        request.setUsername("john_doe");
        request.setPassword("Password1");

        User user = new User();
        user.setId(5L);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setCreatedAt(LocalDateTime.now());

        when(userService.login("john_doe", "Password1")).thenReturn(user);

        ResponseEntity<?> response = authController.login(request, session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(session).setAttribute("userId", 5L);
        verify(session).setAttribute("username", "john_doe");
    }

    @Test
    void logout_ValidSession_InvalidatesSession() {
        ResponseEntity<Void> response = authController.logout(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(session).invalidate();
    }

    @Test
    void me_WhenUserMissing_ReturnsUnauthorized() {
        when(session.getAttribute("userId")).thenReturn(null);

        ResponseEntity<?> response = authController.me(session);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void me_WhenUserExists_ReturnsUserResponse() {
        User user = new User();
        user.setId(7L);
        user.setUsername("john_doe");
        user.setEmail("john@example.com");
        user.setCreatedAt(LocalDateTime.of(2026, 1, 2, 9, 15));

        when(session.getAttribute("userId")).thenReturn(7L);
        when(userService.findById(7L)).thenReturn(user);

        ResponseEntity<?> response = authController.me(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userService).findById(7L);
    }
}
