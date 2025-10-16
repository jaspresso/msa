package com.example.order_service.controller;

import com.example.order_service.dto.OrderDto;
import com.example.order_service.jpa.OrderEntity;
import com.example.order_service.service.OrderService;
import com.example.order_service.vo.RequestOrder;
import com.example.order_service.vo.ResponseOrder;
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

/**
 * OrderController
 * - 주문(Order) 관련 요청을 처리하는 REST API 컨트롤러
 * - 클라이언트로부터 요청을 받아 서비스 계층(OrderService)을 호출하고, 응답 데이터를 반환함
 * - @RestController를 통해 JSON 형태로 응답 처리
 * - @Slf4j 로깅 사용 (log.info 등)
 */
@RestController
@RequestMapping("/order-service") // 모든 엔드포인트의 공통 URL prefix
@Slf4j
public class OrderController {

    // 환경 설정 파일(application.yml)의 속성값을 접근하기 위한 객체
    Environment env;

    // 비즈니스 로직을 수행하는 서비스 객체
    OrderService orderService;

    /**
     * 생성자 주입 방식 (@Autowired)
     * - 스프링이 자동으로 Environment와 OrderService를 주입함
     * - 생성자 주입을 사용하면 의존성 불변성 확보 및 테스트 용이
     */
    @Autowired
    public OrderController(Environment env, OrderService orderService) {
        this.env = env;
        this.orderService = orderService;
    }

    /**
     * 서비스 상태 확인용 API (Health Check)
     * - GET /order-service/health-check
     * - 현재 서비스가 정상 동작 중인지 확인하기 위한 간단한 응답을 반환
     */
    @GetMapping("/health-check")
    public String status() {
        return String.format(
                "It's Working in Order Service on LOCAL PORT %s (SERVER PORT %s)",
                env.getProperty("local.server.port"), // 실제 로컬 실행 포트
                env.getProperty("server.port")        // 설정된 서버 포트
        );
    }

    /**
     * 주문 생성 API
     * - POST /order-service/{userId}/orders
     * - 요청 본문(RequestBody)으로 주문 정보(RequestOrder)를 전달받아 새 주문 생성
     * - 주문 생성 후 ResponseOrder 형태로 결과 반환
     *
     * @param userId 사용자 ID (URL Path 변수)
     * @param orderDetails 요청 바디의 주문 정보 (RequestOrder)
     * @return 생성된 주문 정보 (ResponseOrder)
     */
    @PostMapping("/{userId}/orders")
    public ResponseEntity<ResponseOrder> createOrder(
            @PathVariable("userId") String userId,        // URL 경로의 userId 값
            @RequestBody RequestOrder orderDetails) {     // JSON 요청 본문 매핑
        log.info("Before add orders data");

        // ModelMapper를 이용해 DTO 간 매핑 전략 설정
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // RequestOrder → OrderDto 변환
        OrderDto orderDto = mapper.map(orderDetails, OrderDto.class);

        // 경로에서 받은 userId를 DTO에 설정
        orderDto.setUserId(userId);

        /* JPA를 이용해 주문 생성 (DB 저장) */
        OrderDto createdOrder = orderService.createOrder(orderDto);

        // DB 저장 결과를 응답 객체(ResponseOrder)로 매핑
        ResponseOrder responseOrder = mapper.map(createdOrder, ResponseOrder.class);

        log.info("After added orders data");

        // HTTP 201(CREATED) 상태코드와 함께 응답 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(responseOrder);
    }

    /**
     * 주문 목록 조회 API
     * - GET /order-service/{userId}/orders
     * - 특정 사용자(userId)의 모든 주문 정보를 조회
     * - DB에서 가져온 OrderEntity 목록을 ResponseOrder 리스트로 변환하여 반환
     * @param userId 사용자 ID
     * @return 해당 사용자의 주문 리스트
     */
    @GetMapping("/{userId}/orders")
    public ResponseEntity<List<ResponseOrder>> getOrder(@PathVariable("userId") String userId) throws Exception {
        log.info("Before retrieve orders data");

        // 서비스 계층을 통해 사용자별 주문 목록 조회
        Iterable<OrderEntity> orderList = orderService.getOrdersByUserId(userId);

        // 응답용 리스트 생성
        List<ResponseOrder> result = new ArrayList<>();

        // Entity → ResponseOrder 매핑 반복 수행
        orderList.forEach(v -> {
            result.add(new ModelMapper().map(v, ResponseOrder.class));
        });

        log.info("Add retrieved orders data");

        // HTTP 200(OK) 상태코드와 함께 JSON 배열로 반환
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
