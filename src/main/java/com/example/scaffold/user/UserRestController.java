package com.example.scaffold.user;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController(value = "/api/users")
public class UserRestController {
    @GetMapping
    public String getUsers(@RequestParam String param) {
        return new String("User List");
    }

    @GetMapping("{userId}")
    public String getUser(@RequestParam Integer userId) {
        return new String("Single User");
    }
    
}
