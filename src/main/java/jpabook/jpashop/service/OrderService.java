package jpabook.jpashop.service;

import jpabook.jpashop.domain.*;
import jpabook.jpashop.repository.ItemRepository;
import jpabook.jpashop.repository.MemberRepository;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;

    /**
     * 주문
     * @param memberId
     * @param itemId
     * @param count
     * @return orderId
     */
    @Transactional
    public Long order(Long memberId, Long itemId, int count) {
        // 엔티티 조회
        Member member = memberRepository.findOne(memberId);
        Item item = itemRepository.findOne(itemId);

        // 배송정보 생성
        Delivery delivery = new Delivery();
        delivery.setAddress(member.getAddress());

        // 주문 상품 생성
        OrderItem orderItem = OrderItem.createOrderItem(item, item.getPrice(), count);

        // 주문 생성
        Order order = Order.createOrder(member, delivery, orderItem);

        // 주문 저장
        orderRepository.save(order); // 원래라면 delivery도 생성 뒤에 저장하고 orderItem도 생성하고 save를 해줘야하는데,
        // Order에 cascade속성이 있어서 Order만 저장해도 모두 저장된다.

        // cascade의 범위: 현재 사용한 범위에서만 사용해야한다. -> 참조하는 주인이 private owner인 경우에만 쓴다.
        // => 이는 deliveery, orderItem은 order외에는 사용하지 않는다. order만 두 객체를 참조한다는 의미.
        return order.getId();
    }

    /**
     * 주문 취소
     * @param orderId
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        // 엔티티 조회
        Order order = orderRepository.findOne(orderId);
        // 주문 취소
        order.cancel();
        // 로직이 간단한 이유 -> jpa의 가장 큰 장점인데, entity의 값이 변경되면 jpa가 더티 체킹으로 변경 내역을 감지하여 db에 update query를 알아서 날려준다.
        // 그렇기 때문에 엔티티의 값을 변경하고나서 다시 그 값을 꺼내와서 직접 sql을 짜서 update를 시켜줄 필요가 없어진다.
    }

    // 검색
    public List<Order> findOrders(OrderSearch orderSearch) {
        return orderRepository.findAllByString(orderSearch);
    }
}
