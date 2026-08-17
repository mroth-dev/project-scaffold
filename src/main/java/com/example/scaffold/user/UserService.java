package com.example.scaffold.user;

import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.scaffold.config.CacheConfig;
import com.example.scaffold.exception.NotFoundException;
import com.example.scaffold.exception.ValidationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cacheManager = cacheManager;
    }

    public List<UserDto> getUsers() {
        log.debug("Fetching all users (not cached)");
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Cacheable(value = CacheConfig.USER_SEARCH_CACHE, key = "#query", unless = "#result.isEmpty()")
    public List<UserDto> searchUsers(String query) {
        log.debug("Searching users with query: {}", query);
        List<User> users = (query == null || query.isBlank())
                ? userRepository.findAll()
                : userRepository.findByNameContainingIgnoreCase(query);
        return users.stream().map(this::toDto).toList();
    }

    @Cacheable(value = CacheConfig.USER_CACHE, key = "#id")
    public UserDto getUser(Long id) {
        log.debug("Fetching user by id: {}", id);
        return toDto(findUserOrThrow(id));
    }

    @Cacheable(value = CacheConfig.USER_BY_EMAIL_CACHE, key = "#email")
    public UserDto getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User with email: " + email + " not found"));
        return toDto(user);
    }

    public AccountDto getAccount(Long id) {
        log.debug("Fetching account for user: {}", id);
        return toAccountDto(findUserOrThrow(id));
    }

    public boolean hasCompleteShippingAddress(Long id) {
        return findUserOrThrow(id).getAddress().isComplete();
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.USER_BY_EMAIL_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.USER_SEARCH_CACHE, allEntries = true)
    })
    public AccountDto updateProfile(Long id, ProfileUpdateRequest request) {
        log.debug("Updating profile for user: {}", id);
        User user = findUserOrThrow(id);

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(request.email());
        boolean changingPassword = request.newPassword() != null && !request.newPassword().isBlank();

        if (emailChanged || changingPassword) {
            if (request.currentPassword() == null
                    || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new ValidationException("Current password is incorrect");
            }
        }
        if (emailChanged && userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException(request.email());
        }

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        if (changingPassword) {
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        return toAccountDto(userRepository.save(user));
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.USER_BY_EMAIL_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.USER_SEARCH_CACHE, allEntries = true)
    })
    public AccountDto updateAddress(Long id, AddressUpdateRequest request) {
        log.debug("Updating address for user: {}", id);
        User user = findUserOrThrow(id);
        Address address = user.getAddress();
        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setRegion(request.region());
        address.setPostcode(request.postcode());
        address.setCountry(request.country());
        address.setPhone(request.phone());

        return toAccountDto(userRepository.save(user));
    }

    public UserDto createUser(UserRequest request) {
        log.debug("Creating new user with email: {}", request.email());
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException(request.email());
        }
        User user = new User();
        applyRequest(user, request);
        User savedUser = userRepository.save(user);
        return toDto(savedUser);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.USER_BY_EMAIL_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.USER_SEARCH_CACHE, allEntries = true)
    })
    public UserDto updateUser(Long id, UserRequest request) {
        log.debug("Updating user with id: {}", id);
        User user = findUserOrThrow(id);
        applyRequest(user, request);
        User savedUser = userRepository.save(user);
        log.debug("Cache evicted for user update: {}", id);
        return toDto(savedUser);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.USER_BY_EMAIL_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.USER_SEARCH_CACHE, allEntries = true)
    })
    public UserDto updateRole(Long id, UserRole role) {
        log.debug("Updating role for user {} to {}", id, role);
        User user = findUserOrThrow(id);
        user.setRole(role);
        User savedUser = userRepository.save(user);
        log.debug("Cache evicted for user role update: {}", id);
        return toDto(savedUser);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.USER_BY_EMAIL_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.USER_SEARCH_CACHE, allEntries = true)
    })
    public UserDto updateStatus(Long id, AccountStatus status) {
        log.debug("Updating status for user {} to {}", id, status);
        User user = findUserOrThrow(id);
        user.setStatus(status);
        User savedUser = userRepository.save(user);
        log.debug("Cache evicted for user status update: {}", id);
        return toDto(savedUser);
    }

    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.USER_BY_EMAIL_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.USER_SEARCH_CACHE, allEntries = true)
    })
    public void deleteUser(Long id) {
        log.debug("Deleting user with id: {}", id);
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User", id);
        }
        userRepository.deleteById(id);
        log.debug("Cache evicted for user deletion: {}", id);
    }

    private User findUserOrThrow(Long id) {
        log.debug("Finding user by id: {}", id);
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    private void applyRequest(User user, UserRequest request) {
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setBirthDate(request.birthDate());
        user.setGender(request.gender());
        if (request.role() != null) {
            user.setRole(request.role());
        }
        // Status defaults to ACTIVE via entity default
    }

    private AccountDto toAccountDto(User user) {
        return new AccountDto(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getAddress());
    }

    private UserDto toDto(User user) {
        return new UserDto(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getBirthDate(),
            user.getGender(),
            user.getRole(),
            user.getStatus(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    /**
     * Warm up user cache with frequently accessed users
     * This method is called during application startup
     */
    public void warmupUserCache() {
        log.info("Starting user cache warmup...");

        try {
            Cache userCache = cacheManager.getCache(CacheConfig.USER_CACHE);
            if (userCache == null) {
                log.warn("User cache '{}' is not available; skipping warmup", CacheConfig.USER_CACHE);
                return;
            }

            // Preload active users (limit to avoid memory issues)
            List<User> activeUsers = userRepository.findByStatusOrderByCreatedAtDesc(
                AccountStatus.ACTIVE
            ).stream()
                .limit(100) // Limit to 100 most recent active users
                .toList();

            // Populate the cache directly (matching @Cacheable's `key = "#id"` on getUser)
            // rather than calling getUser(id), since that self-invocation would bypass
            // the caching proxy entirely.
            for (User user : activeUsers) {
                userCache.put(user.getId(), toDto(user));
                log.debug("Preloaded user to cache: {}", user.getId());
            }

            log.info("User cache warmup completed. Preloaded {} users", activeUsers.size());

        } catch (Exception e) {
            log.error("Error during user cache warmup: {}", e.getMessage(), e);
        }
    }

    /**
     * Clear user-related caches
     */
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.USER_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.USER_BY_EMAIL_CACHE, allEntries = true),
        @CacheEvict(value = CacheConfig.USER_SEARCH_CACHE, allEntries = true)
    })
    public void clearUserCaches() {
        log.info("All user caches cleared");
    }
}
