package com.example.user_service2.service;

import com.example.user_service2.dto.UserDto;
import com.example.user_service2.jpa.UserEntity;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    UserDto createUser(UserDto userDto);
    UserDto getUserByUserId(String userId); //개별 사용자 목록보기
    Iterable<UserEntity> getUserByAll(); //전체 사용자 목록보기

    UserDto getUserDetailsByEmail(String userName); //사용자 상세 정보
}
