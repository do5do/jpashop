package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
// transactional안에서 데이터 변경하는게 있어야 LAZY등이 적용이 된다.
@Transactional(readOnly = true) // readOnly를 하면 읽기를 최적화해준다.
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    /**
     * 회원 가입
     * @param member
     * @return
     */
    @Transactional
    public Long join(Member member) {
        validateDuplicateMember(member); // 중복회원 검증
        memberRepository.save(member);
        return member.getId();
    }

    private void validateDuplicateMember(Member member) {
        List<Member> findMembers = memberRepository.findByName(member.getName());
        if (!findMembers.isEmpty()) {
            throw new IllegalStateException("이미 존재하는 회원입니다.");
        }
    }

    // 회원 전체 조회
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    public Member findOne(Long memberId) {
        return memberRepository.findOne(memberId);
    }

    @Transactional
    public void update(Long id, String name) { // 변경 감지
        Member member = memberRepository.findOne(id); // 1. 영속성 컨텍스트에서 member 조회(없으면 DB에서 조회) -> Transactional이 있는 상태에서 조회하면 영속성 컨텍스트에서 가져옴.
        member.setName(name); // 2. 영속성 엔티티 값 변경
        // 3. 변경 감지(dirty checking) flush
        // 4. Transaction commit
    }
}
