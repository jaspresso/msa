package com.example.user_service2.controller;

import com.example.user_service2.dto.UserDto;
import com.example.user_service2.service.UserService;
import com.example.user_service2.vo.Greeting;
import com.example.user_service2.vo.RequestUser;
import com.example.user_service2.vo.ResponseUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@Slf4j
public class UserController {
    private Environment env;
    private Greeting greeting;
    private UserService userService;

    @Autowired
    public UserController(Environment env, Greeting greeting, UserService userService) {
        this.env = env;
        this.greeting = greeting;
        this.userService = userService;
    }

    @GetMapping("/health-check") // http://localhost:60000/health-check
    public String status() {
        return String.format("It's Working in User Service"
                + ", port(local.server.port)=" + env.getProperty("local.server.port")
                + ", port(server.port)=" + env.getProperty("server.port"));
    }

    @GetMapping("/welcome")
    public String welcome(HttpServletRequest request) {
        log.info("users.welcome ip: {}, {}, {}, {}", request.getRemoteAddr()
                , request.getRemoteHost(), request.getRequestURI(), request.getRequestURL());

//        return env.getProperty("greeting.message");
        return greeting.getMessage();//vo 패키지 내 Greeting객체를 활용할 수도 있다.
    }

    @PostMapping("/users")
    public ResponseEntity<ResponseUser> createUser(@RequestBody RequestUser user){
    //public String createUser(@RequestBody RequestUser user) {
        // ModelMapper는 서로 다른 타입(여기서는 RequestUser -> UserDto)의 필드 값을
        // 자동으로 복사(mapping)해주는 라이브러리
        ModelMapper mapper = new ModelMapper();

        // 매칭 전략 설정
        // MatchingStrategies.STRICT는 필드명의 정확한 일치를 요구
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        //user의 필드 값을 UserDto 타입의 새 인스턴스로 복사하여 반환
        UserDto userDto = mapper.map(user, UserDto.class);
        userService.createUser(userDto);

        ResponseUser responseUser = mapper.map(userDto, ResponseUser.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseUser);
        //return "Create user method called.";
    }
}
