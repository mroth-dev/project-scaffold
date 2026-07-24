package com.example.scaffold.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.scaffold.user.AccountStatus;
import com.example.scaffold.user.User;
import com.example.scaffold.user.UserRepository;
import com.example.scaffold.user.UserRole;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthenticationService authenticationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(userRepository, passwordEncoder, jwtTokenProvider);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setStatus(AccountStatus.ACTIVE);
    }

    @Test
    void authenticate_ValidCredentials_ReturnsToken() {
        // Arrange
        String email = "test@example.com";
        String password = "password";
        String expectedToken = "jwt-token";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(testUser)).thenReturn(expectedToken);

        // Act
        String result = authenticationService.authenticate(email, password);

        // Assert
        assertEquals(expectedToken, result);
    }

    @Test
    void authenticate_InvalidEmail_ThrowsBadCredentialsException() {
        // Arrange
        String email = "nonexistent@example.com";
        String password = "password";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> 
            authenticationService.authenticate(email, password));
    }

    @Test
    void authenticate_InvalidPassword_ThrowsBadCredentialsException() {
        // Arrange
        String email = "test@example.com";
        String password = "wrongPassword";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> 
            authenticationService.authenticate(email, password));
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String token = "valid-token";
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);

        // Act
        boolean result = authenticationService.validateToken(token);

        // Assert
        assertTrue(result);
    }

    @Test
    void getEmailFromToken_ValidToken_ReturnsEmail() {
        // Arrange
        String token = "valid-token";
        String expectedEmail = "test@example.com";
        when(jwtTokenProvider.getEmailFromToken(token)).thenReturn(expectedEmail);

        // Act
        String result = authenticationService.getEmailFromToken(token);

        // Assert
        assertEquals(expectedEmail, result);
    }
}