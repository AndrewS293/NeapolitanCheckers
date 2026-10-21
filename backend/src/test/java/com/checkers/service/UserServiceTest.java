package com.checkers.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.checkers.datatrans.RegisterRequest;
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


    // A.C. 3.1 Successful User Login
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

    
    // A.C. 3.2 Invalid Credentials
    @Test
    void login_InvalidUsername() {

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
    void login_WrongPassword() {

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


    //A.C. 2.1 Successful User Registration 
    @Test
    void register_Successful_ReturnsUserAndSavesIt() {
        String username = "new_user";
        String plainPassword = "newPassword123";
        String email = "new_user@example.com";
        String encodedPassword = "encoded_password";
        RegisterRequest request = createRegisterRequest(username, email, plainPassword);
        User savedUser = new User();
        savedUser.setUsername(username);
        savedUser.setEmail(email);
        savedUser.setPasswordHash(encodedPassword);

        when(passwordEncoder.encode(plainPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.register(request);

        assertSame(savedUser, result);
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(encodedPassword, result.getPasswordHash());
        verify(userRepository).existsByUsername(username);
        verify(passwordEncoder).encode(plainPassword);
        verify(userRepository).save(any(User.class));
    }

    //A.C. 2.2 Invalid Field Registration
    @Test
    void register_UsernameTooShort() {
        RegisterRequest request = createRegisterRequest("ab", "new_user@example.com", "Password123");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));

        assertEquals("Username must be at least 3 characters", exception.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }


    //A.C. 2.3 Empty Field Registration
    @Test
    void register_EmptyUsername() {
        RegisterRequest request = createRegisterRequest("   ", "new_user@example.com", "Password123");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));

        assertEquals("Username is required", exception.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    

    @Test
    void register_UsernameWithInvalidCharacters() {
       //add a test for username with a "-" or different special character - follow the tests above for structure
    }

    @Test
    void register_UsernameTooLong() {
        //add a test for username with 26+ characters - follow the tests above for structure
    }

    @Test
    void register_ExistingUsername() {
        RegisterRequest request = createRegisterRequest("existing_user", "new_user@example.com", "Password123");
        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository).existsByUsername("existing_user");
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_EmptyEmail() {
        RegisterRequest request = createRegisterRequest("new_user", "   ", "Password123");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));

        assertEquals("Email is required", exception.getMessage());
        verify(userRepository).existsByUsername("new_user");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_InvalidEmail() {
        RegisterRequest request = createRegisterRequest("new_user", "not-an-email", "Password123");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));

        assertEquals("Invalid email address", exception.getMessage());
        verify(userRepository).existsByUsername("new_user");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_EmptyPassword() {
        RegisterRequest request = createRegisterRequest("new_user", "new_user@example.com", "   ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));

        assertEquals("Password is required", exception.getMessage());
        verify(userRepository).existsByUsername("new_user");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_PasswordTooLong() {
        //add a test for password with 51+ characters - follow the test above for structure  also there is a .repeat() method you can use on strings to repeat x amount of times
    }

    @Test
    void register_PasswordWithSpaces() {
        RegisterRequest request = createRegisterRequest("new_user", "new_user@example.com", "Password 123");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));

        assertEquals("Password cannot contain spaces", exception.getMessage());
        verify(userRepository).existsByUsername("new_user");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_PasswordWithSpecialCharacters() {
        RegisterRequest request = createRegisterRequest("new_user", "new_user@example.com", "Password<123");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));

        assertEquals("Password cannot contain special characters like <, >, \", ', %, ;, ), (, &, +", exception.getMessage());
        verify(userRepository).existsByUsername("new_user");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_PasswordWithoutRequiredCharacterTypes() {
        RegisterRequest request = createRegisterRequest("new_user", "new_user@example.com", "password123");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(request));

        assertEquals("Password must contain at least one uppercase letter, one lowercase letter, and one number",
                exception.getMessage());
        verify(userRepository).existsByUsername("new_user");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_PasswordTooShort() {
        //add a test for password with 7 or fewer characters - follow the test above for structure
    }

    private RegisterRequest createRegisterRequest(String username, String email, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }


        
}
