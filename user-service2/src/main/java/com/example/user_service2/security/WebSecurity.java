package com.example.user_service2.security;

import com.example.user_service2.service.UserService;
import jakarta.ws.rs.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * ✅ WebSecurity
 * - Spring Security의 핵심 설정 클래스
 * - 사용자 인증(Authentication) 및 접근 제어(Authorization) 정책을 정의한다.
 */
@Configuration
@EnableWebSecurity
public class WebSecurity {
    private final UserService userService; // 사용자 정보를 가져오는 서비스 (UserDetailsService 구현체)
    private final Environment env; // application.yml 환경 변수 접근용 (token.secret, expiration-time 등)
    private final BCryptPasswordEncoder bCryptPasswordEncoder;  // 비밀번호 암호화용 객체

    // 허용할 IP 주소 및 서브넷 정의 (로컬호스트만 허용)
    public static final String ALLOWED_IP_ADDRESS = "127.0.0.1";
    public static final String SUBNET = "/32"; // 단일 IP만 허용 (CIDR 표기)
    public static final IpAddressMatcher ALLOWED_IP_ADDRESS_MATCHER =
            new IpAddressMatcher(ALLOWED_IP_ADDRESS + SUBNET); // IP 매칭용 객체 생성

    /**
     * 생성자
     * - Spring이 자동으로 필요한 Bean(Environment, UserService, PasswordEncoder)을 주입
     */
    public WebSecurity(Environment env, UserService userService, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.env = env;
        this.userService = userService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    /**
     * SecurityFilterChain 설정
     * - Spring Boot 2.7 이후부터는 WebSecurityConfigurerAdapter 대신
     * SecurityFilterChain Bean을 등록하는 방식으로 보안 설정을 구성한다.
     */
    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {

        // 1) AuthenticationManager 생성 및 사용자 서비스(userService) 등록
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        // userService를 인증 처리용으로 등록하고, 암호화 방식 지정(BCrypt)
        authenticationManagerBuilder.userDetailsService(userService).passwordEncoder(bCryptPasswordEncoder);

        // AuthenticationManager 인스턴스 생성
        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();

        // 2) 보안 규칙 설정 시작
        http.csrf((csrf) -> csrf.disable()) // CSRF 보호 비활성화 (REST API는 일반적으로 CSRF를 사용하지 않음)
                .authorizeHttpRequests(auth -> auth
                        // /h2-console 경로는 개발용 DB 콘솔 접근 허용
                        .requestMatchers("/h2-console/**").permitAll()

                        // 회원가입 요청(POST /users)은 인증 없이 접근 가능
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()

                        .requestMatchers("/actuator/**").permitAll()  // 특정 경로 허용
                        .requestMatchers("/health-check/**").permitAll()  // 특정 경로 허용

                        // 나머지 모든 요청은 IP 기반 접근 제어 (로컬 또는 지정된 IP만 허용)
                        .requestMatchers("/**").access(
                                new WebExpressionAuthorizationManager(
                                        "hasIpAddress('127.0.0.1') " +
                                                "or hasIpAddress('192.168.219.104') " +
                                                "or hasIpAddress('::1')")) // ::1은 IPv6 로컬주소
                        // 위에서 명시하지 않은 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 3) 인증 관리자 설정
                // (AuthenticationFilter에서 인증 시 사용할 AuthenticationManager를 등록)
                .authenticationManager(authenticationManager)

                // 4) 커스텀 AuthenticationFilter 추가 (로그인 요청 시 본문(JSON)에서 email/password를 읽어와 인증 처리)
                // (코딩참고) 붉은색 에러에 마우스 올려서 Create Method ~ 해서 아래 private AuthenticationFilter getAuthenticationFilter()만듦
                .addFilter(getAuthenticationFilter(authenticationManager))

                // 5) 기본 HTTP Basic 인증 활성화 (테스트용)
                // → 브라우저에서 요청 시 간단한 로그인 팝업이 뜨며, 아이디/비밀번호 입력으로 인증 처리 (디버깅용)
                .httpBasic(Customizer.withDefaults())

                // 6) HTTP 헤더 설정 - H2 콘솔 접근 허용 (iframe/frame 차단 방지)
                // H2 콘솔은 frame 태그를 사용하므로, 같은 출처(sameOrigin)에서의 frame 접근 허용
                .headers((headers) -> headers
                        .frameOptions((frameOptions) -> frameOptions.sameOrigin()));

        // 7) 설정이 끝난 HttpSecurity 객체를 빌드하여 SecurityFilterChain Bean으로 등록
        return http.build();
    }

    /**
     * AuthenticationFilter(인증 필터)를 생성하고 설정하여 반환하는 메서드
     * Spring Security의 인증 과정에서 UsernamePasswordAuthenticationFilter 대신
     * 우리가 직접 만든 커스텀 AuthenticationFilter를 사용하기 위한 설정 메서드
     * - 로그인 요청(/login)을 처리하며, 성공 시 JWT를 생성해 응답 헤더에 담는다.
     */
    private AuthenticationFilter getAuthenticationFilter(AuthenticationManager authenticationManager) throws Exception {
        // 커스텀 AuthenticationFilter 객체 생성
        // 생성자에서 userService, env(환경 설정), authenticationManager를 주입한다.
        //  - userService: DB에서 사용자 정보를 조회하는 서비스
        //  - env: application.yml 같은 설정 파일의 환경 변수 접근용
        AuthenticationFilter authenticationFilter =
                new AuthenticationFilter(authenticationManager, userService, env);

        // 명시적으로 AuthenticationManager 설정 (상위 클래스의 protected 필드 접근용)
        authenticationFilter.setAuthenticationManager(authenticationManager);

        // 최종적으로 구성된 AuthenticationFilter 객체를 반환
        // SecurityFilterChain 등록 시 이 반환 객체를 필터로 추가하게 된다.
        return authenticationFilter;
    }
}
