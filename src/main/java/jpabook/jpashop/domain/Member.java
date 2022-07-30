package jpabook.jpashop.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    // @NotEmpty // validation check
    private String name;

    @Embedded // 내장 타입 포함
    private Address address;

    @JsonIgnore // json으로 생성할때 제외 시킴
    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>(); // 컬랙션은 필드에서 바로 초기화하는 것이 좋다.
    // -> null 문제에서 안전, 하이버네이트 내부 메커니즘에 문제 발생 예방
}
