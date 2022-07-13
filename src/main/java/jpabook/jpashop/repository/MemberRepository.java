package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberRepository {
//    @PersistenceContext // spring이 entityManager를 알아서 주입해줌.
    // => springBoot에서는 spring data jpa가 @Autowired로 em을 주입할 수 있도록 지원해준다. 그래서 생성자 주입이 가능함!
    private final EntityManager em;

//    @PersistenceUnit // EntityManagerFactory를 직접 주입받고 싶을 때 사용하는 어노테이션
//    private EntityManagerFactory emf;

    public void save(Member member) {
        em.persist(member);
    }

    public Member findOne(Long id) {
        return em.find(Member.class, id);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class) // jpql 작성 (sql은 테이블을 대상으로 쿼리하지만, jpql은 entity 객체를 대상으로 쿼리를한다.)
                .getResultList();
    }

    public List<Member> findByName(String name) {
        return em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();
    }
}
