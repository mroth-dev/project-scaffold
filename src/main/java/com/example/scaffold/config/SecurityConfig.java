package com.example.scaffold.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.scaffold.security.JwtAuthenticationFilter;
import com.example.scaffold.security.WebAwareAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, RateLimitingFilter rateLimitingFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users", "/api/users/**").permitAll() // Temporarily permit all for development
                        .requestMatchers("/api/products", "/api/products/**").permitAll() // Temporarily permit all for development
                        .requestMatchers("/api/categories", "/api/categories/**").permitAll() // Temporarily permit all for development
                        .requestMatchers("/users/**", "/webjars/**", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Public web pages - browsing and cart don't require an account;
                        // checkout enforces authentication itself (redirects to /login)
                        .requestMatchers("/", "/login", "/register").permitAll()
                        .requestMatchers("/products", "/products/**").permitAll()
                        .requestMatchers("/cart", "/cart/**").permitAll()
                        // Protected endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/manager/**").hasAnyRole("ADMIN", "MANAGER")
                        // Orders contain customer data - require authentication; listing all
                        // orders and changing status are restricted to staff
                        .requestMatchers(HttpMethod.PUT, "/api/orders/*/status").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/orders").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/orders", "/orders/**").authenticated()
                        // All other requests require authentication
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new WebAwareAuthenticationEntryPoint()))
                // Spring Security wires up a default /logout handler regardless; point it at
                // our JWT cookie instead of leaving a second, unreachable controller method.
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .deleteCookies(JwtAuthenticationFilter.AUTH_COOKIE_NAME)
                        .logoutSuccessUrl("/login")
                        .permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // Runs after JWT auth so an authenticated principal is already on the
                // SecurityContext, letting rate limits key off the user instead of just the IP.
                .addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
