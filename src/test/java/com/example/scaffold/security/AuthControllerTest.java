package com.example.scaffold.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.scaffold.config.RateLimitConfig;
import com.example.scaffold.config.RateLimitService;
import com.example.scaffold.config.SecurityConfig;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // RateLimitingFilter sits in the security chain; rateLimitConfig.isEnabled()
    // defaults to false when mocked, so it's a no-op here.
    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        // Arrange
        AuthController.LoginRequest request = new AuthController.LoginRequest("test@example.com", "password");
        String expectedToken = "jwt-token";
        given(authenticationService.authenticate("test@example.com", "password")).willReturn(expectedToken);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(expectedToken))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        // Arrange
        AuthController.LoginRequest request = new AuthController.LoginRequest("test@example.com", "wrong-password");
        given(authenticationService.authenticate(anyString(), anyString()))
                .willThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_invalidEmail_returnsBadRequest() throws Exception {
        // Arrange
        AuthController.LoginRequest request = new AuthController.LoginRequest("invalid-email", "password");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_returnsOk() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
    }
}
