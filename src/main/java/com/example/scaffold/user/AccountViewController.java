package com.example.scaffold.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.scaffold.exception.BusinessException;
import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;

@Controller
@RequestMapping("/account")
public class AccountViewController {

    private final UserService userService;

    public AccountViewController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserPrincipal principal, Model model) {
        model.addAttribute("account", userService.getAccount(principal.getId()));
        return "account/index";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal CustomUserPrincipal principal,
            @ModelAttribute ProfileUpdateRequest form, Model model) {
        try {
            model.addAttribute("account", userService.updateProfile(principal.getId(), form));
        } catch (BusinessException ex) {
            model.addAttribute("profileError", ex.getMessage());
            model.addAttribute("account", accountWithPendingProfile(principal, form));
        }
        return "account/index";
    }

    @PostMapping("/address")
    public String updateAddress(@AuthenticationPrincipal CustomUserPrincipal principal,
            @ModelAttribute AddressUpdateRequest form, Model model) {
        try {
            model.addAttribute("account", userService.updateAddress(principal.getId(), form));
        } catch (BusinessException ex) {
            model.addAttribute("addressError", ex.getMessage());
            model.addAttribute("account", userService.getAccount(principal.getId()));
        }
        return "account/index";
    }

    private AccountDto accountWithPendingProfile(CustomUserPrincipal principal, ProfileUpdateRequest form) {
        AccountDto current = userService.getAccount(principal.getId());
        return new AccountDto(current.id(), form.firstName(), form.lastName(), form.email(), current.address());
    }
}
