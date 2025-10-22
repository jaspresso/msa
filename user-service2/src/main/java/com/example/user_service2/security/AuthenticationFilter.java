package com.example.user_service2.security;

import com.example.user_service2.dto.UserDto;
import com.example.user_service2.service.UserService;
import com.example.user_service2.vo.RequestLogin;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

/**
 * AuthenticationFilter
 * - Spring Security의 UsernamePasswordAuthenticationFilter를 상속받아
 *   로그인 요청 시 사용자 인증과 JWT 토큰 생성을 처리하는 필터
 */
public class AuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final UserService userService;       // 사용자 정보 조회를 위한 서비스
    private final Environment environment;       // application.yml의 환경 변수 접근용 객체

    /**
     * 생성자
     * @param authenticationManager 스프링 시큐리티의 인증 관리자
     * @param userService           사용자 서비스 (DB에서 사용자 조회)
     * @param environment           환경 설정 정보 (JWT secret 등)
     */
    public AuthenticationFilter(AuthenticationManager authenticationManager,
                                UserService userService, Environment environment) {
        super(authenticationManager);
        this.userService = userService;
        this.environment = environment;
    }

    /**
     * attemptAuthentication()
     * - 사용자가 로그인할 때 (/login 요청 시) 호출되는 메서드
     * - 요청 본문(JSON)에서 email과 password를 추출하고,
     *   AuthenticationManager를 통해 인증을 시도한다.
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {
        try {
            // 요청 본문(JSON)을 RequestLogin 객체로 변환
            RequestLogin creds = new ObjectMapper().readValue(req.getInputStream(), RequestLogin.class);

            // email과 password를 기반으로 인증 토큰 생성 후 AuthenticationManager에 전달
            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            creds.getEmail(),   // 사용자 이메일
                            creds.getPwd(),     // 사용자 비밀번호
                            new ArrayList<>()   // 권한 정보(현재 비어 있음)
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException(e); // JSON 파싱 오류 등 발생 시 런타임 예외로 전달
        }
    }

    /**
     * successfulAuthentication()
     * - 인증이 성공적으로 완료된 후 호출되는 메서드
     * - JWT 토큰을 생성하고, 응답 헤더에 추가한다.
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {

        // 인증 결과(authResult)에서 사용자명(email) 추출
        String userName = ((User) authResult.getPrincipal()).getUsername();

        // 이메일을 기준으로 사용자 세부정보(DTO) 조회
        UserDto userDetails = userService.getUserDetailsByEmail(userName);

        // JWT 서명에 사용할 secret key를 환경설정에서 가져오기
        byte[] secretKeyBytes = environment.getProperty("token.secret").getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = Keys.hmacShaKeyFor(secretKeyBytes); // HS512 등 안전한 HMAC 키 생성

        // 현재 시각(발급일자 및 만료일 계산용)
        Instant now = Instant.now();

        // JWT 토큰 생성
        String token = Jwts.builder()
                .subject(userDetails.getUserId()) // 토큰의 subject로 userId 설정 (사용자 식별자)
                .expiration(Date.from(now.plusMillis( // 만료 시간 설정
                        Long.parseLong(environment.getProperty("token.expiration-time"))
                )))
                .issuedAt(Date.from(now)) // 발급 시각
                .signWith(secretKey)      // 서명(Signature) 추가
                .compact();               // 최종 문자열 형태의 JWT 생성

        // 응답 헤더에 토큰과 사용자 ID 추가
        res.addHeader("token", token);
        res.addHeader("userId", userDetails.getUserId());
    }
}
