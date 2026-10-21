package com.checkers.service;

import com.checkers.datatrans.RegisterRequest;
import com.checkers.model.User;
import com.checkers.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceValidationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setEmail("john@example.com");
        request.setPassword("Password1");
        return request;
    }

    @Test
    void register_ValidRequest_SavesUserAndEncodesPassword() {
        RegisterRequest request = validRequest();
        User saved = new User();
        saved.setUsername("john_doe");
        saved.setEmail("john@example.com");
        saved.setPasswordHash("encoded-pass");

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = userService.register(request);

        assertNotNull(result);
        assertEquals("john_doe", result.getUsername());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("encoded-pass", result.getPasswordHash());
        verify(userRepository).save(any(User.class));
    }

    @ParameterizedTest
    @MethodSource("invalidUsernameCases")
    void register_InvalidUsername_ThrowsExpectedError(String username, String expectedMessage) {
        RegisterRequest request = validRequest();
        request.setUsername(username);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));

        assertEquals(expectedMessage, exception.getMessage());
    }

    static Stream<Arguments> invalidUsernameCases() {
        return Stream.of(
                Arguments.of("", "Username is required"),
                Arguments.of("ab", "Username must be at least 3 characters"),
                Arguments.of("jane-doe", "Username can only contain letters, numbers, and underscores"),
                Arguments.of("a".repeat(26), "Username cannot exceed 25 characters")
        );
    }

    @Test
    void register_DuplicateUsername_Throws() {
        RegisterRequest request = validRequest();
        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));

        assertEquals("Username already exists", ex.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidEmailPasswordCases")
    void register_InvalidEmailOrPassword_Throws(String email, String password, String expectedMessage) {
        RegisterRequest request = validRequest();
        request.setEmail(email);
        request.setPassword(password);

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));

        assertEquals(expectedMessage, ex.getMessage());
    }

    static Stream<Arguments> invalidEmailPasswordCases() {
        return Stream.of(
                Arguments.of("", "Password1", "Email is required"),
                Arguments.of("invalid-email", "Password1", "Invalid email address"),
                Arguments.of("john@example.com", "", "Password is required"),
                Arguments.of("john@example.com", "pass word", "Password cannot contain spaces"),
                Arguments.of("john@example.com", "Password<1", "Password cannot contain special characters like <, >, \", ', %, ;, ), (, &, +"),
                Arguments.of("john@example.com", "password1", "Password must contain at least one uppercase letter, one lowercase letter, and one number"),
                Arguments.of("john@example.com", "Pass1", "Password must be at least 8 characters"),
                Arguments.of("john@example.com", "A".repeat(51), "Password cannot exceed 50 characters")
        );
    }

    @Test
    void login_ValidCredentials_ReturnsUser() {
        String username = "john_doe";
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hashed");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1", "hashed")).thenReturn(true);

        User result = userService.login(username, "Password1");

        assertNotNull(result);
        assertEquals(username, result.getUsername());
    }

    @Test
    void login_UnknownUser_Throws() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.login("unknown", "Password1"));

        assertEquals("Invalid username or password", ex.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_BadPassword_Throws() {
        User user = new User();
        user.setUsername("john_doe");
        user.setPasswordHash("hashed");

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass1", "hashed")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.login("john_doe", "WrongPass1"));

        assertEquals("Invalid username or password", ex.getMessage());
    }

    @Test
    void findById_ExistingUser_ReturnsUser() {
        User user = new User();
        user.setId(8L);
        user.setUsername("john_doe");
        when(userRepository.findById(8L)).thenReturn(Optional.of(user));

        User result = userService.findById(8L);

        assertEquals(8L, result.getId());
        assertEquals("john_doe", result.getUsername());
    }

    @Test
    void findById_MissingUser_Throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.findById(99L));

        assertEquals("User not found", ex.getMessage());
    }
}
