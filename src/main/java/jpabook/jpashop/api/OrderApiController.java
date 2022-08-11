package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class OrderApiController {
    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * entity 직접 사용
     * @return Order list
     */
    @GetMapping("/api/v1/orders")
    public List<Order> orderV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());

        // LAZY 초기화 (OrderSimpleApiController 참고)
        for (Order order : all) {
            order.getMember().getName(); // OSIV off시 could not initialize proxy 에러 발생 -> 지연 로딩을 초기화 할 수 없다.
            order.getDelivery().getAddress();

            List<OrderItem> orderItems = order.getOrderItems();
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
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        // Order 개수만큼 루프 돌며 LAZY 조회 -> Member, Delivery, OrderItems 2개 조회 -> 개수만큼 루프돌며 Item 조회
        // => 쿼리가 어마어마하게 나감..
        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());
        return result;
    }

    /**
     * fetch join distinct (xToOne인 경우에 사용) 성능 최적화
     * 단점
     * * 1. xToOne을 페치 조인하는 순간 페이징이 안된다.
     * * 2. 컬렉션을 하나 이상 페치 조인 할 수 없다.
     * @return List<OrderDto>
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> orderV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 페이징이 가능한 컬렉션 fetch join
     * @param offset
     * @param limit
     * @return List<OrderDto>
     * - ToOne 관계만 우선 모두 페치 조인으로 최적화
     * - 컬렉션 관계는 hibernate.default_batch_fetch_size, @BatchSize로 최적화
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> orderV3_page(@RequestParam(value = "offset", defaultValue = "0") int offset,
                                       @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit); // Orders와 관련된 ToOne 관계들은 한번에 가져온다 -> fetch join
        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Dto로 직접 조회 -> 루트 1, 컬렉션 N
     * @return List<OrderQueryDto>
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    /**
     * v4를 최적화 -> 쿼리 2번(루트 1, 컬렉션 1)
     * @return List<OrderQueryDto>
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    /**
     * v5를 최적화 -> 쿼리 1번
     * @return List<OrderQueryDto>
     * 단점
     * 1. 쿼리는 한번이지만 조인으로 인해 DB에서 애플리케이션에 전달하는 데이터에 중복 데이터가 추가되므로 상황에 따라 V5 보다 더 느릴 수 도 있다.
     * 2. 애플리케이션에서 추가 작업이 크다.
     * 3. 페이징 불가능(Order를 기준으로 불가능, OrderItem을 기준으로는 가능하다. 다쪽인 OrderItem 개수에 맞게 나오기 때문)
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();
        return flats.stream()
                .collect(Collectors.groupingBy(o -> new OrderQueryDto(o.getOrderId(),
                                o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()), // key 설정
                        Collectors.mapping(o -> new OrderItemQueryDto(o.getOrderId(),
                                o.getItemName(), o.getOrderPrice(), o.getCount()), Collectors.toList()) // value 설정
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(),
                        e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                        e.getKey().getAddress(), e.getValue()))
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

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();

            // >>>>>> orderItem도 dto로 받아야한다. orderItem은 엔티티인데 이거 자체를 반환하면 안된다!
//            order.getOrderItems().stream().forEach(o -> o.getItem().getName()); // LAZY 초기화
//            orderItems = order.getOrderItems();

            orderItems = order.getOrderItems().stream()
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
