package com.example.scaffold.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping(value = "/users")
public class UserController {
    @GetMapping("users")
    public String getUsers(@RequestParam String param) {
        // TODO - Set up connection to database
        return new String();
    }
    
    
}
