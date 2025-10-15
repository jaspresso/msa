package com.example.user_service2.service;

import com.example.user_service2.dto.UserDto;
import com.example.user_service2.jpa.UserEntity;
import com.example.user_service2.jpa.UserRepository;
import com.example.user_service2.vo.ResponseOrder;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    Environment env;
    UserRepository userRepository;
    BCryptPasswordEncoder passwordEncoder;

    public UserServiceImpl(Environment env, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.env = env;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        userDto.setUserId(UUID.randomUUID().toString());

        //ModelMapper 사용 대신 직접 setter,getter메소드를 사용할 수도 있다.
//        UserEntity userEntity = new UserEntity();
//        userEntity.setName(userDto.getName());
//        userEntity.setEmail(userDto.getEmail());
//        ...

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        UserEntity userEntity = mapper.map(userDto, UserEntity.class);
        //userEntity.setEncryptedPwd("encrypted_password");
        userEntity.setEncryptedPwd(passwordEncoder.encode(userDto.getPwd()));

        userRepository.save(userEntity);

        UserDto returnUserDto = mapper.map(userEntity, UserDto.class);

        return returnUserDto;
    }

    @Override
    public UserDto getUserByUserId(String userId) {
        UserEntity userEntity = userRepository.findByUserId(userId);
        // find~ 라는 메소드명 규칙에 따라 JPA가 내부적으로
        // UserEntity 클래스의 필드명 userId를 기준으로
        // 자동으로 다음 SQL을 생성한다.
        // SELECT * FROM user_entity WHERE user_id = ?;

        if (userEntity == null)
            throw new UsernameNotFoundException("User not found");
        // 데이터가 없을 때 Exception에러를 반환하도록 조치한다.

        UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);

        List<ResponseOrder> orderList = new ArrayList<>();
        userDto.setOrders(orderList); //주문 데이터 셋팅

        return userDto;
    }

    @Override
    public Iterable<UserEntity> getUserByAll() {
        return userRepository.findAll();
    }

}
