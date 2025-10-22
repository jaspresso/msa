package com.example.apigateway_service.filter;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    private final Environment env; // application.yml 등 환경 설정 파일의 값을 읽기 위한 객체

    // 생성자: Spring이 Environment를 주입
    public AuthorizationHeaderFilter(Environment env) {
        super(Config.class);
        this.env = env;
    }

    // GatewayFilter 설정용 내부 클래스 (필요 시 필드 추가 가능)
    public static class Config {

    }

    /**
     * 실제 필터 로직 정의
     * - 요청 헤더에 Authorization 값이 있는지 확인
     * - JWT 토큰의 유효성을 검증
     * - 문제가 있으면 401 Unauthorized 응답 반환
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Authorization 헤더가 없으면 에러 응답 반환
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
            }

            // 2. "Bearer <JWT토큰>" 형식에서 JWT 부분만 추출
            String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String jwt = authorizationHeader.replace("Bearer ", "");

            // 3. JWT 토큰 유효성 검증
            if (!isJwtValid(jwt)) {
                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
            }

            // 4. 검증 통과 시 다음 필터 체인으로 요청 전달
            return chain.filter(exchange);
        };
    }

    /**
     * 에러 응답 처리 메서드
     * - 응답 상태코드 설정
     * - 로그 출력
     * - 에러 메시지 본문 작성
     */
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus); // HTTP 상태 코드 설정
        log.error(err); // 로그 출력

        // 응답 본문 내용 작성 ("The requested token is invalid.")
        byte[] bytes = "The requested token is invalid.".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(buffer)); // 비동기 방식으로 응답 반환
    }

    /**
     * JWT 유효성 검증 메서드
     * - 서명 키(secret key)를 이용해 토큰을 파싱
     * - subject(사용자 식별자)가 존재하면 유효한 토큰으로 간주
     */
    private boolean isJwtValid(String jwt) {
        // 환경 변수에서 secret key 가져오기 (application.yml의 token.secret)
        byte[] secretKeyBytes = env.getProperty("token.secret").getBytes();

        // HMAC-SHA512 알고리즘 기반으로 서명 키 생성
        SecretKey signingKey = new SecretKeySpec(secretKeyBytes, SignatureAlgorithm.HS512.getJcaName());

        boolean returnValue = true;
        String subject = null;

        try {
            // JWT 파서 생성 (서명 검증용 키 설정)
            JwtParser jwtParser = Jwts.parser()
                    .setSigningKey(signingKey)
                    .build();

            // 토큰 파싱 및 subject(사용자 아이디 등) 추출
            subject = jwtParser.parseClaimsJws(jwt).getBody().getSubject();
        } catch (Exception ex) {
            // 토큰 검증 실패 시 false 반환
            returnValue = false;
        }

        // subject가 비어있으면 유효하지 않은 토큰
        if (subject == null || subject.isEmpty()) {
            returnValue = false;
        }

        return returnValue;
    }
}
