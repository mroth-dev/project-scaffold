package com.example.scaffold.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/users")
public class UserViewController {

    private final UserService userService;

    public UserViewController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("users", userService.getUsers());
        return "users/index";
    }

    @GetMapping("/table")
    public String table(@RequestParam(required = false) String query, Model model) {
        model.addAttribute("users", userService.searchUsers(query));
        return "users/index :: userTable";
    }
}
