package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simpleQuery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simpleQuery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * xToOne(ManyToOne, OneToOne: collection이 아닌 연관관계)
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {
    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    /**
     * Order entity 직접 노출
     * @return 무한 루프에 빠짐(에러) -> Order에 Member가 있고 Member에 Orders가 있어서 양방향 연관관계 문제가 생김.
     * 에러 해결 방법
     * * 1. 양방향일때 둘중에 한쪽은 @JsonIgnore를 해줘야 한다.
     * * 2. Hibernate5Module을 추가하여 bean으로 생성해야 한다.
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch()); // orderSearch를 그냥 넘기면 검색 조건이 없기때문에 order 목록을 모두 가져온다.
        for (Order order : all) {
            order.getMember().getName(); // getMember()까지는 프록시 객체(db에 쿼리가 안 날라감)인데 getName을 하면 db에서 조회(member에 쿼리를 날림) -> Lazy 강제 초기화
            order.getDelivery().getAddress(); // Lazy 강제 초기화
        }
        return all;
    }

    /**
     * 지연 로딩으로 인한 N + 1의 문제 (EAGER로 바꾼다고해서 문제가 해결되지 않음. 더 예측하기 어려워진다.)
     * @return SimpleOrderDto를 반환하여 원하는 결과(Order list)를 만들어내지만 성능 문제가 발생한다.
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        // ORDER 2개 조회
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        // loop를 돌면서 lazy 초기화 객체 조회 (member, delivery)
        return orders.stream()
                .map(SimpleOrderDto::new) // .map(o -> new SimpleOrderDto(o))와 같음
                .collect(Collectors.toList()); // 조회한 order list를 SimpleOrderDto로 변환
        // 총 쿼리 5번 나감 -> N + 1의 문제 (1 + N)
        // => N은 2
        // 1 + 회원 N + 배송 N = 5
    }

    /**
     * fetch join을 사용하여 성능 최적화 -> 엔티티와 연관관계까지 한번에 조회를 해와서 쿼리 1번으로 끝난다.
     * @return List<SimpleOrderDto>
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        return orders.stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Dto를 사용하여 직접 원하는 값만 select를 해온다.(Dto로 조회)
     * -> v3보다 select 부분을 줄였기에 성능이 조금 더 오르지만, repository 재사용성 떨어진다. API 스펙에 맞춘 코드가 repository에 들어간다는 단점이 있다.
     * => 여기서 v3와 v4의 성능차이는 생각보다 미비하다.
     * @return List<OrderSimpleQueryDto>
     */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) { // dto에서는 엔티티를 파라미터로 받는 것은 크게 문제가 되지 않는다. -> ok
            orderId = order.getId();
            name = order.getMember().getName(); // LAZY 초기화 -> 영속성 컨텍스트에서 찾음 -> 없으면 db에 쿼리 날려서 조회
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); // LAZY 초기화
        }
    }
}
