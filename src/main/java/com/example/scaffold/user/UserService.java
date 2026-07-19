package com.example.scaffold.user;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.scaffold.exception.NotFoundException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserDto> getUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<UserDto> searchUsers(String query) {
        List<User> users = (query == null || query.isBlank())
                ? userRepository.findAll()
                : userRepository.findByNameContainingIgnoreCase(query);
        return users.stream().map(this::toDto).toList();
    }

    public UserDto getUser(Long id) {
        return toDto(findUserOrThrow(id));
    }

    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User with email: " + email + " not found"));
        return toDto(user);
    }

    public UserDto createUser(UserRequest request) {
        User user = new User();
        applyRequest(user, request);
        return toDto(userRepository.save(user));
    }

    public UserDto updateUser(Long id, UserRequest request) {
        User user = findUserOrThrow(id);
        applyRequest(user, request);
        return toDto(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User", id);
        }
        userRepository.deleteById(id);
    }

    private User findUserOrThrow(Long id) {
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
}
