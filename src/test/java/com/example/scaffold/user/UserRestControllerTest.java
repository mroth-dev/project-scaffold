package com.example.scaffold.user;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.scaffold.common.Gender;
import com.example.scaffold.config.SecurityConfig;
import com.example.scaffold.exception.NotFoundException;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(UserRestController.class)
@Import(SecurityConfig.class)
class UserRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void getUsersReturnsList() throws Exception {
        UserDto user = new UserDto(1L, "Sir Admin", "admin@mail.com", LocalDate.of(1999, 8, 30), Gender.MALE);
        given(userService.getUsers()).willReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Sir Admin"));
    }

    @Test
    void getUserReturnsSingleUser() throws Exception {
        UserDto user = new UserDto(1L, "Sir Admin", "admin@mail.com", LocalDate.of(1999, 8, 30), Gender.MALE);
        given(userService.getUser(1L)).willReturn(user);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@mail.com"));
    }

    @Test
    void getUserReturns404WhenMissing() throws Exception {
        given(userService.getUser(99L)).willThrow(new NotFoundException("User", 99L));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createUserReturns201() throws Exception {
        UserRequest request = new UserRequest("New User", "new.user@mail.com", LocalDate.of(2000, 1, 1), Gender.FEMALE);
        UserDto created = new UserDto(2L, "New User", "new.user@mail.com", LocalDate.of(2000, 1, 1), Gender.FEMALE);
        given(userService.createUser(any(UserRequest.class))).willReturn(created);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void createUserReturns400WhenInvalid() throws Exception {
        UserRequest invalid = new UserRequest("", "not-an-email", null, null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUserReturns204() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(eq(1L));
    }
}
