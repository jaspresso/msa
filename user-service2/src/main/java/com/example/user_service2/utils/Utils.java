package com.example.user_service2.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class Utils {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("1234"));
    }
}
