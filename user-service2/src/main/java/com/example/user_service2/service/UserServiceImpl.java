package com.example.user_service2.service;

import com.example.user_service2.dto.UserDto;
import com.example.user_service2.jpa.UserEntity;
import com.example.user_service2.jpa.UserRepository;
import com.example.user_service2.vo.ResponseOrder;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
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

    /**
     * 사용자 생성 메서드
     * - 전달받은 UserDto 데이터를 DB에 저장
     * - 비밀번호는 암호화(BCryptPasswordEncoder) 후 저장
     */
    @Override
    public UserDto createUser(UserDto userDto) {
        userDto.setUserId(UUID.randomUUID().toString());

//        ModelMapper 사용 대신 직접 setter,getter메소드를 사용할 수도 있다.
//        UserEntity userEntity = new UserEntity();
//        userEntity.setName(userDto.getName());
//        userEntity.setEmail(userDto.getEmail());
//        ...

        // ModelMapper 설정 (DTO ↔ Entity 간 필드 매칭 방식 설정)
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // DTO → Entity 변환
        UserEntity userEntity = mapper.map(userDto, UserEntity.class);

        //userEntity.setEncryptedPwd("encrypted_password");
        // 비밀번호 암호화 후 Entity에 저장
        userEntity.setEncryptedPwd(passwordEncoder.encode(userDto.getPwd()));

        // JPA Repository를 통해 DB에 저장
        userRepository.save(userEntity);

        // Entity → DTO 변환 후 반환
        UserDto returnUserDto = mapper.map(userEntity, UserDto.class);

        return returnUserDto;
    }

    /**
     * userId를 기준으로 회원 정보 조회
     */
    @Override
    public UserDto getUserByUserId(String userId) {
        // find~ 라는 메소드명 규칙에 따라 JPA가 내부적으로
        // UserEntity 클래스의 필드명 userId를 기준으로
        // 자동으로 다음 SQL을 생성한다.
        // SELECT * FROM user_entity WHERE user_id = ?;
        UserEntity userEntity = userRepository.findByUserId(userId);

        // 데이터가 없을 때 Exception에러를 반환하도록 조치한다.
        if (userEntity == null)
            throw new UsernameNotFoundException("User not found");

        // Entity → DTO 변환
        UserDto userDto = new ModelMapper().map(userEntity, UserDto.class);

        // 주문 내역은 외부 주문 서비스에서 가져오도록 설계되어 있음
        // 여기서는 일단 빈 리스트로 초기화
        List<ResponseOrder> orderList = new ArrayList<>();
        userDto.setOrders(orderList);

        return userDto;
    }

    /**
     * 모든 사용자 조회
     * - DB의 모든 사용자 목록을 반환
     */
    @Override
    public Iterable<UserEntity> getUserByAll() {
        return userRepository.findAll();
    }

    /**
     * 이메일 기준 사용자 상세 조회
     * - 이메일(email)을 기준으로 사용자 정보를 DB에서 조회 후 DTO로 변환
     */
    @Override
    public UserDto getUserDetailsByEmail(String email) {
        UserEntity userEntity = userRepository.findByEmail(email);
        if (userEntity == null)
            throw new UsernameNotFoundException(email);

        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        UserDto userDto = mapper.map(userEntity, UserDto.class);
        return userDto;
    }

    /**
     * 스프링 시큐리티의 필수 메서드 loadUserByUsername()
     * - 사용자가 로그인할 때 호출됨
     * - username(email)을 기준으로 DB에서 사용자 정보를 조회
     * - 조회된 정보를 스프링 시큐리티의 User 객체로 변환하여 반환
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // DB에서 이메일로 사용자 조회
        UserEntity userEntity = userRepository.findByEmail(username);

        // 사용자 정보가 없을 경우 예외 발생 (Spring Security 표준 Exception)
        if (userEntity == null)
            throw new UsernameNotFoundException(username + ": not found");

        // import org.springframework.security.core.userdetails.User;
        // UserDetails 타입의 객체 생성
        // new User(아이디, 암호화된 비밀번호, 계정 활성화 여부 등, 권한 목록)
        return new User(userEntity.getEmail(), // principal
                userEntity.getEncryptedPwd(), // credentials
                true, true, true, true, // 계정 상태 (만료, 잠금 등 전부 true = 정상)
                new ArrayList<>()); // 권한 목록 (현재 비워둠)
    }
}