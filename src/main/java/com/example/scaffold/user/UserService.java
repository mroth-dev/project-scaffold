package com.example.scaffold.user;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.scaffold.exception.NotFoundException;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        user.setName(request.name());
        user.setEmail(request.email());
        user.setBirthDate(request.birthDate());
        user.setGender(request.gender());
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getBirthDate(), user.getGender());
    }
}
