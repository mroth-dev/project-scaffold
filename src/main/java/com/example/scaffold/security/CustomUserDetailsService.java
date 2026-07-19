package com.example.scaffold.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.scaffold.user.AccountStatus;
import com.example.scaffold.user.User;
import com.example.scaffold.user.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                mapToAuthorities(user),
                user.getStatus() == AccountStatus.ACTIVE
        );
    }

    private Collection<? extends GrantedAuthority> mapToAuthorities(User user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    public static class CustomUserPrincipal implements UserDetails {
        private final Long id;
        private final String email;
        private final String password;
        private final Collection<? extends GrantedAuthority> authorities;
        private final boolean enabled;

        public CustomUserPrincipal(Long id, String email, String password,
                                 Collection<? extends GrantedAuthority> authorities, boolean enabled) {
            this.id = id;
            this.email = email;
            this.password = password;
            this.authorities = authorities;
            this.enabled = enabled;
        }

        public Long getId() {
            return id;
        }

        @Override
        public String getUsername() {
            return email;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }
    }
}