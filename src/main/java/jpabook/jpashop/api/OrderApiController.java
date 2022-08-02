package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Orders;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderApiController {
    private final OrderRepository orderRepository;

    /**
     * entity 직접 사용
     * @return Order list
     */
    @GetMapping("/api/v1/orders")
    public List<Orders> orderV1() {
        List<Orders> all = orderRepository.findAllByString(new OrderSearch());
        // LAZY 초기화 (OrderSimpleApiController 참고)
        for (Orders orders : all) {
            orders.getMember().getName();
            orders.getDelivery().getAddress();

            List<OrderItem> orderItems = orders.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }
        return all;
    }

    /**
     * Dto를 생성하여 반환하지만, 성능 문제 발생
     * @return List<OrderDto>
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> orderV2() {
        // ORDER 2개 조회
        List<Orders> orders = orderRepository.findAllByString(new OrderSearch());

        // Order 개수만큼 루프 돌며 LAZY 조회 -> Member, Delivery, OrderItems 2개 조회 -> 개수만큼 루프돌며 Item 조회
        // => 쿼리가 어마어마하게 나감..
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * fetch join distinct(xToOne인 경우에 사용) 성능 최적화
     * 단점
     * * 1. xToOne을 페치 조인하는 순간 페이징이 안된다.
     * * 2. 컬렉션을 하나 이상 페치 조인 할 수 없다.
     * @return List<OrderDto>
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> orderV3() {
        List<Orders> orders = orderRepository.findAllWithItem();
        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
    }

    @Getter
    static class OrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
//        private List<OrderItem> orderItems;
        private List<OrderItemDto> orderItems;

        public OrderDto(Orders orders) {
            orderId = orders.getId();
            name = orders.getMember().getName();
            orderDate = orders.getOrderDate();
            orderStatus = orders.getStatus();
            address = orders.getDelivery().getAddress();

            // >>>>>> orderItem도 dto로 받아야한다. orderItem은 엔티티인데 이거 자체를 반환하면 안된다!
//            order.getOrderItems().stream().forEach(o -> o.getItem().getName()); // LAZY 초기화
//            orderItems = order.getOrderItems();

            orderItems = orders.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDto {
        private String itemName; // 상품 명
        private int orderPrice; // 상품 가격
        private int count; // 주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
