package com.example.scaffold.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Read-only user directory for the admin section (ADMIN/MANAGER only).
 */
@Controller
@RequestMapping("/admin/users")
public class AdminUserViewController {

    private final UserService userService;

    public AdminUserViewController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("users", userService.getUsers());
        return "admin/users/index";
    }

    @GetMapping("/table")
    public String table(@RequestParam(required = false) String query, Model model) {
        model.addAttribute("users", userService.searchUsers(query));
        return "admin/users/index :: userTable";
    }
}
