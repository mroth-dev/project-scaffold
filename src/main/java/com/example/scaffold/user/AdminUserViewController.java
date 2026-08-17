package com.example.scaffold.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.scaffold.exception.ValidationException;
import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;

/**
 * User directory and account management for the admin section (ADMIN/MANAGER
 * only). Role and status changes are guarded against a staff member editing
 * their own account, so no one can lock themselves out or self-demote.
 */
@Controller
@RequestMapping("/admin/users")
public class AdminUserViewController {

    private final UserService userService;

    public AdminUserViewController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserPrincipal principal, Model model) {
        model.addAttribute("users", userService.getUsers());
        model.addAttribute("currentUserId", principal.getId());
        model.addAttribute("roles", UserRole.values());
        return "admin/users/index";
    }

    @GetMapping("/table")
    public String table(@RequestParam(required = false) String query,
            @AuthenticationPrincipal CustomUserPrincipal principal, Model model) {
        model.addAttribute("users", userService.searchUsers(query));
        model.addAttribute("currentUserId", principal.getId());
        model.addAttribute("roles", UserRole.values());
        return "admin/users/index :: userTable";
    }

    @PostMapping("/{userId}/role")
    public String updateRole(@PathVariable Long userId, @RequestParam UserRole role,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        assertNotSelf(userId, principal, "You cannot change your own role");
        userService.updateRole(userId, role);
        return "redirect:/admin/users";
    }

    @PostMapping("/{userId}/disable")
    public String disable(@PathVariable Long userId, @AuthenticationPrincipal CustomUserPrincipal principal) {
        assertNotSelf(userId, principal, "You cannot disable your own account");
        userService.updateStatus(userId, AccountStatus.INACTIVE);
        return "redirect:/admin/users";
    }

    @PostMapping("/{userId}/enable")
    public String enable(@PathVariable Long userId) {
        userService.updateStatus(userId, AccountStatus.ACTIVE);
        return "redirect:/admin/users";
    }

    private void assertNotSelf(Long userId, CustomUserPrincipal principal, String message) {
        if (principal.getId().equals(userId)) {
            throw new ValidationException(message);
        }
    }
}
