package com.example.user_service2.service;

import com.example.user_service2.dto.UserDto;
import com.example.user_service2.jpa.UserEntity;
import org.springframework.security.core.userdetails.UserDetailsService;

// UserDetailsService는 Spring Security에서 사용자 인증(Authentication)을 처리하기 위해 사용하는 핵심 인터페이스
public interface UserService extends UserDetailsService {
    UserDto createUser(UserDto userDto);
    UserDto getUserByUserId(String userId); //개별 사용자 목록보기
    Iterable<UserEntity> getUserByAll(); //전체 사용자 목록보기

    UserDto getUserDetailsByEmail(String userName); //사용자 상세 정보
}
