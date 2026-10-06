package com.checkers.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.checkers.model.User;
import com.checkers.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;


@ExtendWith(MockitoExtension.class)
class UserServiceTest { 

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService; 

    @Test
    void login_Successful_ReturnsUser() {
        
        String username = "john_doe";
        String plainPassword = "password123";
        String mockHash = "hashed_password_abc";
        
        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setPasswordHash(mockHash);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(plainPassword, mockHash)).thenReturn(true);

        
        User result = userService.login(username, plainPassword);

    
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        
        verify(userRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(plainPassword, mockHash);
    }

    @Test
    void login_InvalidUsername_ThrowsIllegalArgumentException() {

        String username = "unknown_user";
        String password = "anyPassword";

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

 
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.login(username, password);
        });

        assertEquals("Invalid username or password", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_WrongPassword_ThrowsIllegalArgumentException() {

        String username = "john_doe";
        String wrongPassword = "wrongPassword";
        String mockHash = "hashed_password_abc";

        User mockUser = new User();
        mockUser.setUsername(username);
        mockUser.setPasswordHash(mockHash);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(wrongPassword, mockHash)).thenReturn(false);

    
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.login(username, wrongPassword);
        });

        assertEquals("Invalid username or password", exception.getMessage());
    }
}
