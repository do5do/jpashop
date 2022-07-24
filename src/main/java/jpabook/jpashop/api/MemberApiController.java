package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

@RestController // @Controller + @ResponseBody가 합쳐진 것 => 데이터 자체를 json이나 xml로 보낼때 사용하는 어노테이션
@RequiredArgsConstructor
public class MemberApiController {
    private final MemberService memberService;

    @PostMapping("/api/v1/members") // @Valid: Member를 검증 -> Member entity안에 javax.validation을 검증한다. -> @NotEmpty 등
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    // entity로 직접 받고(파라미터로 받고) 보내는 건(외부에 노출) api를 만들때 절대 하면 안된다! => request, response에 맞는 dto를 사용한다.
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName()); // entity가 바뀌어도(ex. name 필드명이 username으로 변경) api 스펙에 영향을 주지 않는다.
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

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
