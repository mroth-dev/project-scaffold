package com.example.scaffold.user;

import org.springframework.stereotype.Service;

@Service
public class UserService {
    public String handleUser() {
        return new String ("Handler");
    }
}
