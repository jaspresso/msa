package com.example.user_service2.vo;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class Greeting {
    //import org.springframework.beans.factory.annotation.Value;
    @Value("${greeting.message}")
    private String message;
}
