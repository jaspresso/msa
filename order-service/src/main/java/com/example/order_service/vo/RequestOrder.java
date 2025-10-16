package com.example.order_service.vo;

import lombok.Data;

// RequestBody에 사용될 데이터
// 최소 아래 3가지 정보가 있어야 주문 가능함.
// 사용자id는 엔드포인트를 호출할 때 중간에 userId가 들어갈 것이기 때문에 여기에 따로 포함시키지는 않는다.
@Data
public class RequestOrder {
    private String productId; //제품id
    private Integer qty; //수량
    private Integer unitPrice; //단가
}
