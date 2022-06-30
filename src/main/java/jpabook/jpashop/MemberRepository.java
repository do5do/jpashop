package jpabook.jpashop;

import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
public class MemberRepository {
    @PersistenceContext
    private EntityManager em;

    public Long save(Member member) {
        em.persist(member);
        return member.getId(); // 저장을 하고 나면 sideEffect를 일으키는 것이기 때문에 return 값을 거의 만들지 않는다. 대신에 id를 넘겨서 조회는 가능하도록 설계한다.
    }

    public Member find(Long id) {
        return em.find(Member.class, id);
    }
}
