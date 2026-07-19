package com.example.scaffold.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.scaffold.user.AccountStatus;
import com.example.scaffold.user.User;
import com.example.scaffold.user.UserRole;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider("mySecretKey1234567890123456789012345678901234567890", 86400000);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setStatus(AccountStatus.ACTIVE);
    }

    @Test
    void generateToken_ValidUser_ReturnsToken() {
        // Act
        String token = jwtTokenProvider.generateToken(testUser);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void getEmailFromToken_ValidToken_ReturnsEmail() {
        // Arrange
        String token = jwtTokenProvider.generateToken(testUser);

        // Act
        String email = jwtTokenProvider.getEmailFromToken(token);

        // Assert
        assertEquals("test@example.com", email);
    }

    @Test
    void getUserIdFromToken_ValidToken_ReturnsUserId() {
        // Arrange
        String token = jwtTokenProvider.generateToken(testUser);

        // Act
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // Assert
        assertEquals(1L, userId);
    }

    @Test
    void getRoleFromToken_ValidToken_ReturnsRole() {
        // Arrange
        String token = jwtTokenProvider.generateToken(testUser);

        // Act
        String role = jwtTokenProvider.getRoleFromToken(token);

        // Assert
        assertEquals("CUSTOMER", role);
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String token = jwtTokenProvider.generateToken(testUser);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_EmptyToken_ReturnsFalse() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken("");

        // Assert
        assertFalse(isValid);
    }
}