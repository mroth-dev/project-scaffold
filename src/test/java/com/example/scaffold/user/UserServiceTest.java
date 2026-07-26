package com.example.scaffold.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Test class for UserService with cache behavior validation.
 *
 * UserService is registered as a real Spring bean (not manually {@code new}'d) so
 * that {@code @Cacheable}/{@code @CacheEvict} are actually applied through the AOP
 * proxy - without this, "cache hit" assertions would pass or fail independently of
 * whether caching is really wired up.
 */
@SpringJUnitConfig(UserServiceTest.CacheTestConfig.class)
class UserServiceTest {

    @Configuration
    @EnableCaching
    static class CacheTestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                CacheConfig.USER_CACHE,
                CacheConfig.USER_BY_EMAIL_CACHE,
                CacheConfig.USER_SEARCH_CACHE
            );
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        UserService userService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                CacheManager cacheManager) {
            return new UserService(userRepository, passwordEncoder, cacheManager);
        }
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    private User testUser;
    private UserRequest testUserRequest;

    @BeforeEach
    void setUp() {
        reset(userRepository);
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });

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
    void testGetUser_cacheEnabled() {
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
    void testGetUserByEmail_cacheEnabled() {
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
    void testSearchUsers_cacheEnabled() {
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
    void testSearchUsers_emptyResult_notCached() {
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
    void testUpdateUser_cacheEviction() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // First, populate the cache
        userService.getUser(1L);
        userService.getUserByEmail("test@example.com");
        verify(userRepository, times(1)).findById(1L);

        // When - Update user (should evict caches). updateUser() loads the entity
        // to mutate via a direct, uncached repository call, so this alone accounts
        // for a second findById(1L) regardless of what the cache holds.
        UserDto updatedUser = userService.updateUser(1L, testUserRequest);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository, times(1)).save(any(User.class));
        verify(userRepository, times(2)).findById(1L);

        // When - Get user again (should hit repository as the cache was evicted,
        // rather than reusing the stale pre-update entry)
        userService.getUser(1L);

        // Then - Repository should be called a third time
        verify(userRepository, times(3)).findById(1L);
    }

    @Test
    void testDeleteUser_cacheEviction() {
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
    void testCreateUser_noCacheInteraction() {
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
    void testCreateUser_throwsWhenEmailAlreadyExists() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When / Then
        assertThrows(DuplicateEmailException.class, () -> userService.createUser(testUserRequest));
        verify(userRepository, never()).save(any(User.class));
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

        // And - the user cache should now be pre-populated, so a subsequent
        // getUser() call must not hit the repository at all
        UserDto cached = userService.getUser(1L);
        assertEquals(testUser.getEmail(), cached.email());
        verify(userRepository, never()).findById(1L);
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
    void testGetUser_notFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> userService.getUser(999L));
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    void testGetUserByEmail_notFound() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> userService.getUserByEmail("nonexistent@example.com"));
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void testDeleteUser_notFound() {
        // Given
        when(userRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () -> userService.deleteUser(999L));
        verify(userRepository, times(1)).existsById(999L);
        verify(userRepository, never()).deleteById(999L);
    }
}
