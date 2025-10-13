package com.example.user_service2.security;

import com.example.user_service2.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * WebSecurity 클래스
 * -------------------
 * Spring Security의 전반적인 웹 보안 설정을 담당하는 구성 클래스(Configuration)입니다.
 *
 * - 어떤 URL을 인증 없이 접근할 수 있는지
 * - 어떤 요청은 인증(로그인) 또는 IP 제한이 필요한지
 * - CSRF, H2-console 접근 권한, HTTP 헤더 설정 등을 제어합니다.
 */
@Configuration                  // 스프링 설정 클래스임을 명시
@EnableWebSecurity              // 웹 보안을 활성화 (Spring Security 필터체인 작동)
public class WebSecurity {

    private UserService userService;   // 사용자 관련 서비스 (회원 인증 등에 사용 가능)
    private Environment env;           // 환경변수 및 설정값 접근용 (application.yml 등에서 읽음)

    // 접속을 허용할 IP 주소 상수 정의
    public static final String ALLOWED_IP_ADDRESS = "127.0.0.1";  // 로컬호스트(자기 PC)
    public static final String SUBNET = "/32";                    // 단일 IP만 허용 (CIDR 표기)
    public static final IpAddressMatcher ALLOWED_IP_ADDRESS_MATCHER =
            new IpAddressMatcher(ALLOWED_IP_ADDRESS + SUBNET);    // IP 매칭용 객체 생성

    // 생성자 주입: Spring이 Environment와 UserService를 자동으로 주입
    public WebSecurity(Environment env, UserService userService) {
        this.env = env;
        this.userService = userService;
    }

    /**
     * SecurityFilterChain Bean 등록
     * -----------------------------
     * HttpSecurity 객체를 통해 보안 정책을 설정합니다.
     * - CSRF 비활성화
     * - URL별 접근 허용/제한
     * - HTTP Basic 인증 방식 활성화
     * - H2 콘솔 접근 허용 (frameOptions.sameOrigin)
     */
    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {

                // 🔹 CSRF(Cross Site Request Forgery) 보호 비활성화
                //    → REST API 서버나 테스트 환경에서는 일반적으로 비활성화함.
            http.csrf(csrf -> csrf.disable())

                // 🔹 URL 접근 제어
                .authorizeHttpRequests(auth -> auth
                        // H2 콘솔 경로는 인증 없이 접근 허용
                        .requestMatchers("/h2-console/**").permitAll()
                        // 나머지 모든 요청은 인증(로그인) 필요
                        .anyRequest().authenticated()
                )

                // 🔹 HTTP Basic 인증 방식 활성화
                //    → 브라우저에서 요청 시 간단한 로그인 팝업이 뜨며, 아이디/비밀번호 입력으로 인증 처리
                .httpBasic(Customizer.withDefaults())

                // 🔹 HTTP 헤더 설정
                .headers(headers -> headers
                        // H2 콘솔은 frame 태그를 사용하므로, 같은 출처(sameOrigin)에서의 frame 접근 허용
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                );

        // 설정이 끝난 HttpSecurity 객체를 빌드하여 SecurityFilterChain Bean으로 등록
        return http.build();
    }
}
