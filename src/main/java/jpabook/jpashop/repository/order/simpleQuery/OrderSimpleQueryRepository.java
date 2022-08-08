package jpabook.jpashop.repository.order.simpleQuery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;


/**
 * API 스펙에 맞춘 repository는 따로 구분하는 것이 유지보수에 좋다.
 * 기존 repository는 순수하게 해당 엔티티를 쿼리하는 용도(페치 조인까지)로만 사용한다.
 */
@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {
    private final EntityManager em;

    public List<OrderSimpleQueryDto> findOrderDtos() {
        // OrderSimpleQueryDto에 entity를 바로 넘기는게 불가능하다.
        // address는 값타입이라 값처럼 동작하기때문에 아래와 같이 넣어줄 수 있다.
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.simpleQuery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                                " from Order o" +
                                " join o.member m" +
                                " join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }
}
