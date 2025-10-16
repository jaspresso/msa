package com.example.order_service.service;

import com.example.order_service.dto.OrderDto;
import com.example.order_service.jpa.OrderEntity;
import com.example.order_service.jpa.OrderRepository;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * OrderServiceImpl
 * - 주문(Order) 관련 비즈니스 로직을 구현하는 서비스 클래스
 * - 인터페이스 OrderService를 구현함
 * - @Service 애너테이션을 통해 Spring Bean으로 등록됨
 */
@Service
public class OrderServiceImpl implements OrderService {
    // JPA를 통해 DB 접근을 수행하는 Repository
    OrderRepository orderRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 주문 생성(createOrder)
     * - 주문 정보를 받아 새 주문을 생성하고 DB에 저장함
     * - OrderDto → OrderEntity로 매핑하여 저장 후,
     *   다시 OrderEntity → OrderDto로 변환하여 결과 반환
     * @param orderDto 주문 요청 정보 (수량, 단가, 사용자 ID 등)
     * @return 저장된 주문 정보 DTO
     */
    @Override
    public OrderDto createOrder(OrderDto orderDto) {
        // 주문 ID를 UUID로 자동 생성 (고유 식별자)
        orderDto.setOrderId(UUID.randomUUID().toString());
        // 총 금액 계산(totalPrice): 수량 × 단가
        orderDto.setTotalPrice(orderDto.getQty() * orderDto.getUnitPrice());

        // ModelMapper 설정: 매핑 전략을 STRICT로 설정 (필드명이 정확히 일치해야 매핑)
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        // DTO → Entity 변환
        OrderEntity orderEntity = mapper.map(orderDto, OrderEntity.class);

        // DB에 주문 정보 저장
        orderRepository.save(orderEntity);

        // Entity → DTO로 다시 변환하여 반환
        OrderDto returnValue = mapper.map(orderEntity, OrderDto.class);

        return returnValue;
    }

    /**
     * 주문 ID로 주문 조회(getOrderByOrderId)
     * - 특정 주문 ID에 해당하는 주문 데이터를 조회
     * - JPA Repository의 findByOrderId() 메서드 호출 해서
     *  SELECT * FROM orders WHERE orderId = ? 쿼리를 자동 생성시킴
     */
    @Override
    public OrderDto getOrderByOrderId(String orderId) {
        OrderEntity orderEntity = orderRepository.findByOrderId(orderId);
        OrderDto orderDto = new ModelMapper().map(orderEntity, OrderDto.class);

        return orderDto;
    }

    /**
     * 사용자 ID로 모든 주문 목록 조회(getOrdersByUserId)
     * - 특정 사용자(userId)의 모든 주문 목록을 반환
     * - Repository에서 findByUserId() 메서드를 호출
     */
    @Override
    public Iterable<OrderEntity> getOrdersByUserId(String userId) {
        return orderRepository.findByUserId(userId);
    }
}
