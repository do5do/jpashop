package jpabook.jpashop.repository.order.simpleQuery;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.OrderStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderSimpleQueryDto {
    private Long orderId;
    private String name;
    private LocalDateTime orderDate;
    private OrderStatus orderStatus;
    private Address address;

    public OrderSimpleQueryDto(Long orderId, String name, LocalDateTime orderDate, OrderStatus orderStatus, Address address) { // dto에서는 엔티티를 파라미터로 받는 것은 크게 문제가 되지 않는다. -> ok
        this.orderId = orderId;
        this.name = name; // LAZY 초기화 -> 영속성 컨텍스트에서 찾음 -> 없으면 db에 쿼리 날려서 조회
        this.orderDate = orderDate;
        this.orderStatus = orderStatus;
        this.address = address; // LAZY 초기화
    }
}
