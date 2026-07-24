package com.example.scaffold.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.example.scaffold.config.CacheConfig;
import com.example.scaffold.exception.NotFoundException;

/**
 * Test class for UserService with cache behavior validation
 */
@SpringBootTest
@SpringJUnitConfig
@ExtendWith(MockitoExtension.class)
@EnableCaching
class UserServiceTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                CacheConfig.USER_CACHE,
                CacheConfig.USER_BY_EMAIL_CACHE,
                CacheConfig.USER_SEARCH_CACHE
            );
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Mock
    private UserRepository userRepository;

    private UserService userService;
    private CacheManager cacheManager;
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private UserRequest testUserRequest;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager(
            CacheConfig.USER_CACHE,
            CacheConfig.USER_BY_EMAIL_CACHE,
            CacheConfig.USER_SEARCH_CACHE
        );
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));
        testUser.setGender(Gender.MALE);
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setStatus(AccountStatus.ACTIVE);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // Create test user request
        testUserRequest = new UserRequest(
            "test@example.com",
            "password123",
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1),
            Gender.MALE,
            UserRole.CUSTOMER
        );
    }

    @Test
    void testGetUser_CacheEnabled() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When - First call
        UserDto result1 = userService.getUser(1L);
        
        // Then
        assertNotNull(result1);
        assertEquals(testUser.getId(), result1.id());
        assertEquals(testUser.getEmail(), result1.email());
        verify(userRepository, times(1)).findById(1L);

        // When - Second call (should hit cache)
        UserDto result2 = userService.getUser(1L);

        // Then - Repository should not be called again due to caching
        assertNotNull(result2);
        assertEquals(result1.id(), result2.id());
        verify(userRepository, times(1)).findById(1L); // Still only called once
    }

    @Test
    void testGetUserByEmail_CacheEnabled() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When - First call
        UserDto result1 = userService.getUserByEmail("test@example.com");
        
        // Then
        assertNotNull(result1);
        assertEquals(testUser.getEmail(), result1.email());
        verify(userRepository, times(1)).findByEmail("test@example.com");

        // When - Second call (should hit cache)
        UserDto result2 = userService.getUserByEmail("test@example.com");

        // Then - Repository should not be called again due to caching
        assertNotNull(result2);
        assertEquals(result1.email(), result2.email());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void testSearchUsers_CacheEnabled() {
        // Given
        String query = "John";
        List<User> users = List.of(testUser);
        when(userRepository.findByNameContainingIgnoreCase(query)).thenReturn(users);

        // When - First call
        List<UserDto> result1 = userService.searchUsers(query);
        
        // Then
        assertNotNull(result1);
        assertEquals(1, result1.size());
        assertEquals(testUser.getFirstName(), result1.get(0).firstName());
        verify(userRepository, times(1)).findByNameContainingIgnoreCase(query);

        // When - Second call (should hit cache)
        List<UserDto> result2 = userService.searchUsers(query);

        // Then - Repository should not be called again due to caching
        assertNotNull(result2);
        assertEquals(result1.size(), result2.size());
        verify(userRepository, times(1)).findByNameContainingIgnoreCase(query);
    }

    @Test
    void testSearchUsers_EmptyResult_NotCached() {
        // Given
        String query = "NonExistentName";
        List<User> emptyUsers = List.of();
        when(userRepository.findByNameContainingIgnoreCase(query)).thenReturn(emptyUsers);

        // When - First call
        List<UserDto> result1 = userService.searchUsers(query);
        
        // Then
        assertNotNull(result1);
        assertTrue(result1.isEmpty());
        verify(userRepository, times(1)).findByNameContainingIgnoreCase(query);

        // When - Second call (should NOT hit cache due to empty result)
        List<UserDto> result2 = userService.searchUsers(query);

        // Then - Repository should be called again because empty results are not cached
        assertNotNull(result2);
        assertTrue(result2.isEmpty());
        verify(userRepository, times(2)).findByNameContainingIgnoreCase(query);
    }

    @Test
    void testUpdateUser_CacheEviction() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // First, populate the cache
        userService.getUser(1L);
        userService.getUserByEmail("test@example.com");
        verify(userRepository, times(1)).findById(1L);

        // When - Update user (should evict caches)
        UserDto updatedUser = userService.updateUser(1L, testUserRequest);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository, times(1)).save(any(User.class));

        // When - Get user again (should hit repository as cache was evicted)
        userService.getUser(1L);
        
        // Then - Repository should be called again
        verify(userRepository, times(2)).findById(1L);
    }

    @Test
    void testDeleteUser_CacheEviction() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);

        // First, populate the cache
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        userService.getUser(1L);
        verify(userRepository, times(1)).findById(1L);

        // When - Delete user (should evict caches)
        userService.deleteUser(1L);

        // Then
        verify(userRepository, times(1)).deleteById(1L);

        // When - Try to get user again (should hit repository as cache was evicted)
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(NotFoundException.class, () -> userService.getUser(1L));
        
        // Then - Repository should be called again
        verify(userRepository, times(2)).findById(1L);
    }

    @Test
    void testCreateUser_NoCacheInteraction() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserDto createdUser = userService.createUser(testUserRequest);

        // Then
        assertNotNull(createdUser);
        assertEquals(testUserRequest.email(), createdUser.email());
        assertEquals(testUserRequest.firstName(), createdUser.firstName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testWarmupUserCache() {
        // Given
        List<User> activeUsers = List.of(testUser);
        when(userRepository.findByStatusOrderByCreatedAtDesc(AccountStatus.ACTIVE))
            .thenReturn(activeUsers);

        // When
        userService.warmupUserCache();

        // Then
        verify(userRepository, times(1)).findByStatusOrderByCreatedAtDesc(AccountStatus.ACTIVE);
    }

    @Test
    void testClearUserCaches() {
        // Given - populate caches first
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        userService.getUser(1L);
        userService.getUserByEmail("test@example.com");

        // When
        userService.clearUserCaches();

        // Then - Next calls should hit repository again
        userService.getUser(1L);
        userService.getUserByEmail("test@example.com");
        
        verify(userRepository, times(2)).findById(1L);
        verify(userRepository, times(2)).findByEmail("test@example.com");
    }

    @Test
    void testGetUser_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> userService.getUser(999L));
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    void testGetUserByEmail_NotFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.getUserByEmail("nonexistent@example.com"));
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void testDeleteUser_NotFound() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () -> userService.deleteUser(999L));
        verify(userRepository, times(1)).existsById(999L);
        verify(userRepository, never()).deleteById(999L);
    }
}