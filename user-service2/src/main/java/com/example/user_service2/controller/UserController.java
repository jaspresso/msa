package com.example.user_service2.controller;

import com.example.user_service2.dto.UserDto;
import com.example.user_service2.jpa.UserEntity;
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

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/user-service2")
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
        // - STRICT: 소스와 대상의 프로퍼티 이름이 엄격히 일치해야 매핑
        // - STANDARD(기본): 약간 유연한 매핑 허용
        // - LOOSE: 더 느슨한 매핑
        // MatchingStrategies.STRICT는 필드명의 정확한 일치를 요구
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        //user의 필드 값을 UserDto 타입의 새 인스턴스로 복사하여 반환
        UserDto userDto = mapper.map(user, UserDto.class);
        userService.createUser(userDto);

        ResponseUser responseUser = mapper.map(userDto, ResponseUser.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseUser);
        //return "Create user method called.";
    }

    // 전체 사용자 목록을 조회하는 엔드포인트
    @GetMapping("/users")
    public ResponseEntity getUsers() {
        // userService를 통해 모든 사용자 정보를 가져옴 (JPA Repository의 findAll()과 유사)
        Iterable<UserEntity> userList = userService.getUserByAll();

        // 결과를 담을 리스트 (엔티티를 그대로 반환하지 않고, 응답 전용 DTO로 변환)
        List<ResponseUser> result = new ArrayList<>();

        // 각 UserEntity 객체를 ResponseUser DTO로 변환
        userList.forEach(v -> {
            // ModelMapper를 사용해 Entity → DTO 자동 매핑
            result.add(new ModelMapper().map(v, ResponseUser.class));
        });

        // HTTP 200 OK 상태 코드와 함께 결과 리스트 반환
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    // 특정 사용자 ID(userId)에 해당하는 사용자 정보를 조회하는 엔드포인트
    @GetMapping("/users/{userId}")
    public ResponseEntity getUser(@PathVariable("userId") String userId) {
        // userService를 통해 userId로 사용자 정보를 조회 (존재하지 않으면 예외 발생)
        UserDto userDto = userService.getUserByUserId(userId);

        // 서비스 계층의 UserDto를 응답용 DTO(ResponseUser)로 변환
        ResponseUser returnValue = new ModelMapper().map(userDto, ResponseUser.class);

        // HTTP 200 OK 상태 코드와 함께 변환된 사용자 정보를 반환
        return ResponseEntity.status(HttpStatus.OK).body(returnValue);
    }

}
