package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.stream.Collectors;

@RestController // @Controller + @ResponseBody가 합쳐진 것 => 데이터 자체를 json이나 xml로 보낼때 사용하는 어노테이션
@RequiredArgsConstructor
public class MemberApiController {
    private final MemberService memberService;

    /**
     * 회원가입 v1
     * @param member
     * @return
     */
    @PostMapping("/api/v1/members") // @Valid: Member를 검증 -> Member entity안에 javax.validation을 검증한다. -> @NotEmpty 등
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     * 회원가입 v2
     * @param request
     * @return entity로 직접 받고(파라미터로 받고) 보내는 건(외부에 노출) api를 만들때 절대 하면 안된다!
     * => request, response에 맞는 dto를 사용한다.
     */
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName()); // entity가 바뀌어도(ex. name 필드명이 username으로 변경) api 스펙에 영향을 주지 않는다.
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     * 회원 수정 v2
     * @param id
     * @param request
     * @return
     */
    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id,
                                               @RequestBody @Valid UpdateMemberRequest request) {
        //==command와 query를 분리하는 스타일==// => 유지보수성 증대
        memberService.update(id, request.getName()); // command
        // => update에서 Member를 반환할 수도 있는데, 그러면 영속 상태가 끊긴 member를 반환하는 거기 때문에 애매해진다.
        // => 업데이트를 하면서 Member를 쿼리 해버리는 꼴. 즉, update의 id를 가지고 다시 Member를 조회하는 꼴을 의미한다.
        // => 가급적이면 update로 끝내버리거나 아니면 id값 정도만 반환해 준다.
        Member findMember = memberService.findOne(id); // query => 수정이 잘 됐는지 id로 쿼리하여 member를 다시 가져온다.
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    /**
     * 회원 목록 조회 v1
     * @return 응답값으로 엔티티를 직접 노출하게 되면 엔티티(회원)에 대한 모든 정보가 노출된다.
     * (ex. 회원만 조회했지만 order까지 다 나옴)
     */
    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        return memberService.findMembers();
    }

    @GetMapping("/api/v2/members")
    public Result memberV2() {
        List<Member> findMembers = memberService.findMembers();
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .collect(Collectors.toList());
        return new Result(collect); // 바로 list를 내보내지 않고, Object(껍데기)로 감싸는 이유는
        // 리스트만 보내면 json을 보면 [{}, {}, ...] 이렇게 리스트만 딱 반환된다. 나중에 다른 데이터가 추가 될때 유연성이 떨어진다.
        // 그래서 {"data":[{}, {}, ...], ...} 이런 형식으로 보내기 위해 겉에 Object로 감싸준다.
    }

    @Data
    @AllArgsConstructor
    static class Result<T> { // generic type
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
    }

    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }

    @Data
    static class CreateMemberRequest {
        @NotEmpty
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }
}
