package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Orders;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {
    private final EntityManager em;

    public void save(Orders orders) {
        em.persist(orders);
    }

    public Orders findOne(Long id) {
        return em.find(Orders.class, id);
    }

    // jpql을 동적으로 생성
    public List<Orders> findAllByString(OrderSearch orderSearch) {
        String jpql = "select o from Orders o join o.member m";
        boolean isFirstCondition = true;

        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }

        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }

        TypedQuery<Orders> query = em.createQuery(jpql, Orders.class)
                .setMaxResults(1000);

        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }

        return query.getResultList();
    }

    // jpa criteria로 해결 -> 복잡하고 유지보수성이 거의 없어서 실무에서는 사용하지 않음. queryDsl로 대체
    public List<Orders> findAllByCriteria(OrderSearch orderSearch) { // criteria: jpql을 자바 코드로 작성할 수 있게 해주는 표준
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Orders> cq = cb.createQuery(Orders.class);
        Root<Orders> o = cq.from(Orders.class);
        Join<Object, Object> m = o.join("member", JoinType.INNER);

        List<Predicate> criteria = new ArrayList<>();

        // 주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"), orderSearch.getOrderStatus());
            criteria.add(status);
        }

        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name =
                    cb.like(m.<String>get("name"), "%" +
                            orderSearch.getMemberName() + "%");
            criteria.add(name);
        }

        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Orders> query = em.createQuery(cq).setMaxResults(1000); //최대 1000건
        return query.getResultList();
    }

    // 검색 시 동적 쿼리를 사용하지 않을 때의 jpql
    public List<Orders> findAll(OrderSearch orderSearch) {
        return em.createQuery("select o from Orders o join o.member m" +
                        " where o.status = :status" +
                        " and m.name like :name", Orders.class) // order와 order와 연관 된 member를 조인
                .setParameter("status", orderSearch.getOrderStatus())
                .setParameter("name", orderSearch.getMemberName())
//                .setFirstResult(100) paging시 start position으로 100부터 시작해서 1000개를 가져온다는 의미.
                .setMaxResults(1000) // 최대 1000개까지만 조회
                .getResultList();
    }

    // fetch join 사용
    public List<Orders> findAllWithMemberDelivery() {
        return em.createQuery(
                "select o from Orders o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d", Orders.class)
                .getResultList();
    }

    public List<Orders> findAllWithItem() {
        return em.createQuery(
                "select distinct o from Orders o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d" +
                        " join fetch o.orderItems oi" +
                        " join fetch oi.item i", Orders.class)
                .setFirstResult(1)
                .setMaxResults(100)
                .getResultList();
        // distinct는 db에 실제로 distinct를 날려준다.
        // -> db에서는 모든 컬럼들이 다 같아야 중복이 제거된다. 실제로는 지금 상황에서 distinct를 써도 중복이 제거되지 않지만,
        // jpa에서 distinct를 사용하여 조회하면 id로 중복을 제거하여 반환해주기 때문에 원하는 결과(중복 제거 된)를 얻을 수 있다.
    }
}
