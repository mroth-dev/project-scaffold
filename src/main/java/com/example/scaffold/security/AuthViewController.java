package com.example.scaffold.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.scaffold.user.UserRepository;
import com.example.scaffold.user.UserRequest;
import com.example.scaffold.user.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AuthViewController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final long jwtExpirationMillis;

    public AuthViewController(AuthenticationService authenticationService, UserService userService,
            UserRepository userRepository, @Value("${app.jwt.expiration:86400000}") long jwtExpirationMillis) {
        this.authenticationService = authenticationService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtExpirationMillis = jwtExpirationMillis;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute LoginForm form, Model model, HttpServletResponse response) {
        try {
            String token = authenticationService.authenticate(form.email(), form.password());
            addAuthCookie(response, token);
            return "redirect:/";
        } catch (BadCredentialsException e) {
            model.addAttribute("error", "Invalid email or password");
            model.addAttribute("email", form.email());
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerPage() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterForm form, Model model, HttpServletResponse response) {
        if (form.password() == null || form.password().length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters");
            model.addAttribute("form", form);
            return "auth/register";
        }
        if (userRepository.findByEmail(form.email()).isPresent()) {
            model.addAttribute("error", "An account with that email already exists");
            model.addAttribute("form", form);
            return "auth/register";
        }

        userService.createUser(new UserRequest(form.email(), form.password(), form.firstName(), form.lastName(),
                null, null, null));

        String token = authenticationService.authenticate(form.email(), form.password());
        addAuthCookie(response, token);
        return "redirect:/";
    }

    /**
     * HTMX-driven live validation: checked on blur while filling out the
     * registration form, well before the form is ever submitted.
     */
    @GetMapping("/register/email-check")
    public String emailCheck(@RequestParam(required = false) String email, Model model) {
        boolean taken = StringUtils.hasText(email) && userRepository.findByEmail(email).isPresent();
        model.addAttribute("taken", taken);
        return "auth/fragments/email-feedback :: emailFeedback";
    }

    private void addAuthCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(JwtAuthenticationFilter.AUTH_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpirationMillis / 1000));
        response.addCookie(cookie);
    }

    public record LoginForm(String email, String password) {
    }

    public record RegisterForm(String email, String password, String firstName, String lastName) {
    }
}
